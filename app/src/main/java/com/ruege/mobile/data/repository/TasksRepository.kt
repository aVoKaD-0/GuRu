package com.ruege.mobile.data.repository

import androidx.lifecycle.asFlow
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.TaskDao
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.data.network.api.TaskApiService
import com.ruege.mobile.data.local.entity.TaskEntity
import com.ruege.mobile.data.network.dto.response.TaskDto
import com.ruege.mobile.model.AnswerType
import com.ruege.mobile.model.TaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOn
import com.ruege.mobile.data.local.dao.TaskTextDao
import com.ruege.mobile.data.local.entity.TaskTextEntity
import kotlinx.coroutines.launch
import com.ruege.mobile.data.local.dao.UserDao

data class TasksPage(val tasks: List<TaskItem>, val hasMore: Boolean)

@Singleton
class TasksRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val contentDao: ContentDao,
    private val taskApiService: TaskApiService,
    private val taskTextDao: TaskTextDao,
    private val externalScope: CoroutineScope,
    private val userDao: UserDao
) {
    private val TAG = "TasksRepository"

    private val _tasksContentLoaded = MutableStateFlow(false)
    val tasksContentLoaded: StateFlow<Boolean> = _tasksContentLoaded.asStateFlow()

    private val tasksCategoryCache = mutableMapOf<String, List<TaskItem>>()
    private val taskTextCache = mutableMapOf<String, String>()
    private val taskDetailCache = mutableMapOf<Int, TaskItem>()
    private val pageNumberByCategory = mutableMapOf<String, Int>()

    companion object {
        const val NO_DATA_AND_NETWORK_ISSUE_FLAG = "NO_DATA_AND_NETWORK_ISSUE"
    }

    init {
        externalScope.launch {
            userDao.getFirstUserFlow().collect { user ->
                if (user == null) {
                    _tasksContentLoaded.value = false
                    tasksCategoryCache.clear()
                    pageNumberByCategory.clear()
                    Timber.d("User logged out or not present, content loaded flags reset.")
                }
            }
        }
    }

    fun getTasksTopicsStream(): Flow<List<ContentEntity>> {
        Timber.d("Getting tasks topics stream from DB")
        return contentDao.getContentsByType("task_group").asFlow()
            .combine(taskDao.getDownloadedEgeNumbersStream()) { entities, downloadedEgeNumbers ->
                val downloadedSet = downloadedEgeNumbers.toSet()
                entities.map { group ->
                    val egeNumber = group.contentId.removePrefix("task_group_")
                    group.apply {
                        this.isDownloaded = downloadedSet.contains(egeNumber)
                    }
                }
            }
    }

    suspend fun refreshTasksTopics() {
        withContext(Dispatchers.IO) {
            try {
                val response = taskApiService.getAllTasks()
                if (response.isSuccessful && response.body() != null) {
                    val taskGroups = response.body()!!
                    val newTasksEntities = taskGroups.map { group ->
                        val egeNumber = group["ege_number"] as? String ?: ""
                        val title = group["title"] as? String ?: "Задание $egeNumber"
                        val count = (group["count"] as? Number)?.toInt() ?: 0
                        ContentEntity().apply {
                            setContentId("task_group_$egeNumber")
                            setTitle(title)
                            setType("task_group")
                            setDescription("$count заданий")
                        }
                    }
                    contentDao.insertAll(newTasksEntities)
                }
            } catch (e: Exception) {
                Timber.e(e, "refreshTasksTopics failed")
            } finally {
                _tasksContentLoaded.value = true
            }
        }
    }

    fun getTasksByCategory(categoryId: String, pageSize: Int = 20): Flow<Result<TasksPage>> = flow {
        val actualCategoryId = categoryId.replace("task_group_", "")
        val page = pageNumberByCategory.getOrDefault(actualCategoryId, 1)

        // Local first strategy
        if (page == 1) {
            val localTasks = taskDao.getTasksByEgeNumberSync(actualCategoryId).map { it.toTaskItem() }
            if(localTasks.isNotEmpty()){
                tasksCategoryCache[actualCategoryId] = localTasks
                emit(Result.Success(TasksPage(tasks = localTasks, hasMore = false))) // No pagination for local data
                return@flow
            }
        }

        // Network if no local data
        try {
            emit(Result.Loading)
            val response = taskApiService.getTasksByEgeNumberPaginated(actualCategoryId, limit = pageSize, pageNumber = page)
            if (response.isSuccessful) {
                val taskDtos = response.body()?.tasks ?: emptyList()
                val hasMore = taskDtos.size >= pageSize
                val newTasks = taskDtos.map { it.toTaskItem() }

                val currentTasks = tasksCategoryCache.getOrDefault(actualCategoryId, emptyList())
                tasksCategoryCache[actualCategoryId] = currentTasks + newTasks
                
                if (hasMore) {
                    pageNumberByCategory[actualCategoryId] = page + 1
                }

                emit(Result.Success(TasksPage(tasks = newTasks, hasMore = hasMore)))
            } else {
                emit(Result.Error("Ошибка сервера: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Ошибка сети"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Получает детальную информацию о задании.
     * При первом обращении к заданию в рамках сессии всегда запрашивает обновление с сервера.
     * Для последующих обращений сначала проверяет локальную БД, затем сервер.
     */
    fun getTaskDetail(taskIdString: String): Flow<Result<TaskItem>> = flow {
        Timber.d("Запрос деталей задания ID: $taskIdString")

        val taskIdInt = try {
            taskIdString.toInt()
        } catch (e: NumberFormatException) {
            Timber.e(e, "Неверный формат ID задания: $taskIdString")
            emit(Result.Error("Неверный формат ID задания: $taskIdString"))
            return@flow
        }

        taskDetailCache[taskIdInt]?.let {
            Timber.d("Задание $taskIdInt найдено в кэше.")
            emit(Result.Success(it))
            return@flow
        }

        val localTask = withContext(Dispatchers.IO) { taskDao.getTaskByIdSync(taskIdInt) }
        if (localTask != null) {
            val localText = localTask.getTextId()?.let { taskTextDao.getTaskTextById(it.toString()) }
            val textDto = localText?.let { com.ruege.mobile.data.network.dto.response.TextDataDto(it.textId.toInt(), it.content, null, null, null) }
            val taskItem = localTask.toTaskItemWithText(textDto)
            taskDetailCache[taskIdInt] = taskItem
            Timber.d("Задание $taskIdInt загружено из локальной БД.")
            emit(Result.Success(taskItem))
            return@flow
        }

        Timber.d("Требуется загрузка с сервера для задания ID $taskIdString.")
        try {
            val response = taskApiService.getTaskDetail(taskIdString, includeText = true)
            if (response.isSuccessful) {
                val taskDetailDto = response.body()
                if (taskDetailDto != null) {
                    val taskItem = taskDetailDto.toEntity().toTaskItemWithText(taskDetailDto.text)
                    taskDetailCache[taskIdInt] = taskItem
                    emit(Result.Success(taskItem))
                } else {
                    emit(Result.Error("Пустой ответ от сервера для задания $taskIdString"))
                }
            } else {
                emit(Result.Error("Ошибка сервера: ${response.code()} для задания $taskIdString"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сети при получении деталей задания ID $taskIdString.")
            emit(Result.Error(e.message.toString()))
        }
    }

    suspend fun getTasksByIds(taskIds: List<Int>): List<TaskEntity> {
        return withContext(Dispatchers.IO) {
            taskDao.getTasksByIds(taskIds)
        }
    }

//    suspend fun checkAnswer(taskId: Int, userAnswer: String): AnswerCheckResult {
//        val task = taskDao.getTaskById(taskId)
//        val isCorrect = task?.answer.equals(userAnswer, ignoreCase = true)
//
//        if (task != null) {
//            task.isSolved = true
//            task.score = if(isCorrect) task.maxPoints else 0
//            task.userAnswer = userAnswer
//            taskDao.insert(task)
//        }
//
//        return AnswerCheckResult(
//            taskId = taskId,
//            isCorrect = isCorrect,
//            correctAnswer = task?.answer ?: "",
//            explanation = task?.explanation,
//            userAnswer = userAnswer,
//            pointsAwarded = if (isCorrect) task?.maxPoints ?: 0 else 0
//        )
//    }

    suspend fun updateTaskProgress(taskEntity: TaskEntity) {
        taskDao.insert(taskEntity)
    }

    fun getTaskTextById(textId: String): Flow<Result<String>> = flow {
        taskTextCache[textId]?.let { cachedText ->
            Timber.d("Текст задания '$textId' взят из кэша памяти.")
            emit(Result.Success(cachedText))
            return@flow
        }

        val textFromDb = withContext(Dispatchers.IO) { taskTextDao.getTaskTextById(textId) }
        if (textFromDb != null) {
            Timber.d("Текст задания '$textId' взят из БД.")
            taskTextCache[textId] = textFromDb.content
            emit(Result.Success(textFromDb.content))
            return@flow
        }

        emit(Result.Loading)
        try {
            Timber.d("Запрос текста задания с ID: $textId с сервера.")
            val response = taskApiService.getTaskTextById(textId)
            if (response.isSuccessful) {
                val taskTextDto = response.body()
                if (taskTextDto != null && taskTextDto.content != null) {
                    Timber.d("Текст задания '$textId' успешно получен, кэшируем в память и сохраняем в БД.")
                    taskTextCache[textId] = taskTextDto.content
                    withContext(Dispatchers.IO) {
                        taskTextDao.insertAll(listOf(TaskTextEntity(textId, taskTextDto.content)))
                    }
                    emit(Result.Success(taskTextDto.content))
                } else {
                    Timber.w("Тело ответа или текст для $textId пустое.")
                    emit(Result.Error("Отсутствует текст в ответе сервера"))
                }
            } else {
                val errorMsg = "Ошибка при загрузке текста задания $textId: ${response.code()} - ${response.message()}"
                Timber.e(errorMsg)
                emit(Result.Error(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Исключение при загрузке текста задания $textId")
            emit(Result.Error(e.message.toString()))
        }
    }

    fun downloadTaskGroup(egeNumber: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            var page = 1
            val allTasks = mutableListOf<TaskDto>()
            while (true) {
                // We'll use a large limit to fetch as many tasks as possible per request
                val response = taskApiService.getTasksByEgeNumberPaginated(egeNumber, pageNumber = page, limit = 100)
                if (response.isSuccessful) {
                    val tasksDto = response.body()?.tasks ?: emptyList()
                    if (tasksDto.isEmpty()) {
                        break // No more tasks to fetch
                    }
                    allTasks.addAll(tasksDto)
                    page++
                } else {
                    throw Exception("Ошибка сервера при загрузке страницы $page для группы $egeNumber: ${response.code()}")
                }
            }

            if (allTasks.isNotEmpty()) {
                val taskEntities = allTasks.map { it.toEntity() }
                val textEntities = allTasks.mapNotNull { it.text?.toEntity() }

                taskDao.insertAll(taskEntities)
                if (textEntities.isNotEmpty()) {
                    taskTextDao.insertAll(textEntities)
                }
                contentDao.updateDownloadStatus("task_group_$egeNumber", true)
            }
            
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при скачивании группы заданий $egeNumber")
            emit(Result.Error(e.message ?: "Неизвестная ошибка при скачивании"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteTaskGroup(egeNumber: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            taskDao.deleteTasksByEgeNumber(egeNumber)
            contentDao.updateDownloadStatus("task_group_$egeNumber", false)
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Ошибка при удалении группы заданий"))
        }
    }.flowOn(Dispatchers.IO)

    private fun TaskDto.toTaskItem(): TaskItem {
        val orderPosition = try {
            this.egeNumber.toInt()
        } catch (e: NumberFormatException) {
            1
        }

        return TaskItem(
            taskId = this.id.toString(),
            title = "Задание ${this.egeNumber}",
            egeTaskNumber = this.egeNumber,
            description = if (this.text != null) "Прочитайте текст и выполните задание" else "",
            content = this.taskText,
            answerType = AnswerType.TEXT,
            maxPoints = 1,
            timeLimit = 0,
            solutions = null,
            correctAnswer = this.solution,
            explanation = this.explanation,
            textId = this.textId,
            orderPosition = orderPosition,
            isSolved = false
        )
    }

    private fun com.ruege.mobile.data.network.dto.response.TextDataDto.toEntity(): TaskTextEntity {
        return TaskTextEntity(
            textId = this.id.toString(),
            content = this.content ?: ""
        )
    }

    private fun TaskEntity.toTaskItemWithText(textDto: com.ruege.mobile.data.network.dto.response.TextDataDto?): TaskItem {
        val contentHtml = this.taskText ?: ""

        val orderPosition = try {
            this.egeNumber.toInt()
        } catch (e: NumberFormatException) {
            1
        }

        return TaskItem(
            taskId = this.id.toString(),
            title = "Задание ${this.egeNumber}",
            egeTaskNumber = this.egeNumber,
            description = if (textDto?.content != null) "Прочитайте текст и выполните задание" else "",
            content = contentHtml,
            answerType = AnswerType.TEXT,
            maxPoints = 1,
            timeLimit = 0,
            solutions = null,
            correctAnswer = this.solution,
            explanation = this.explanation,
            textId = this.getTextId(),
            orderPosition = orderPosition
        )
    }

    private fun TaskEntity.toTaskItem(): TaskItem {
        val orderPosition = try {
            this.egeNumber.toInt()
        } catch (e: NumberFormatException) {
            1
        }

        return TaskItem(
            taskId = this.id.toString(),
            title = "Задание ${this.egeNumber}",
            egeTaskNumber = this.egeNumber,
            description = "",
            content = this.taskText ?: "",
            answerType = AnswerType.TEXT,
            maxPoints = 1,
            timeLimit = 0,
            solutions = null,
            correctAnswer = this.solution,
            explanation = this.explanation,
            textId = this.getTextId(),
            orderPosition = orderPosition,
            isSolved = false
        )
    }

    fun clearTasksCache(categoryId: String) {
        tasksCategoryCache.remove(categoryId)
        pageNumberByCategory.remove(categoryId)
        Timber.d("Кэш для категории $categoryId очищен.")
    }

    fun resetTaskPagination(categoryId: String?) {
        if (categoryId != null) {
            pageNumberByCategory.remove(categoryId.replace("task_group_", ""))
        } else {
            pageNumberByCategory.clear()
        }
        Timber.d("Пагинация сброшена для ${categoryId ?: "всех категорий"}")
    }
}