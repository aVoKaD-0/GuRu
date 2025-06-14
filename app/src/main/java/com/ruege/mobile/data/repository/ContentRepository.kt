package com.ruege.mobile.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.TaskDao
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.data.network.api.TaskApiService
import com.ruege.mobile.data.network.api.TheoryApiService
import com.ruege.mobile.data.network.api.EssayApiService
import com.ruege.mobile.data.local.entity.TaskEntity
import com.ruege.mobile.data.network.dto.response.TaskDto
import com.ruege.mobile.data.network.dto.response.TheoryContentDto
import com.ruege.mobile.data.network.dto.response.EssayContentDto
import com.ruege.mobile.model.AnswerCheckResult
import com.ruege.mobile.model.AnswerType
import com.ruege.mobile.model.Solution
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.data.network.dto.request.AnswerRequest
import com.ruege.mobile.data.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.first
import java.util.NoSuchElementException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.data.local.dao.DownloadedTheoryDao
import com.ruege.mobile.data.local.entity.DownloadedTheoryEntity
import kotlinx.coroutines.flow.map
import com.ruege.mobile.data.network.dto.response.TheorySummaryDto
import kotlinx.coroutines.flow.flowOn
import com.ruege.mobile.data.local.dao.TaskTextDao
import com.ruege.mobile.data.local.entity.TaskTextEntity

@Singleton
class ContentRepository @Inject constructor(
    private val contentDao: ContentDao,
    private val taskDao: TaskDao,
    private val theoryApiService: TheoryApiService,
    private val taskApiService: TaskApiService,
    private val essayApiService: EssayApiService,
    private val downloadedTheoryDao: DownloadedTheoryDao,
    private val taskTextDao: TaskTextDao,
    private val externalScope: CoroutineScope
) {

    private val TAG = "ContentRepository"
    
    private val theoryTopicsCache = MutableStateFlow<List<TheorySummaryDto>>(emptyList())
    private val taskTopicsCache = MutableStateFlow<List<ContentEntity>>(emptyList())
    
    private val _theoryContentLoaded = MutableStateFlow(false)
    private val _essayContentLoaded = MutableStateFlow(false)
    private val _tasksContentLoaded = MutableStateFlow(false)

    val initialContentLoaded: StateFlow<Boolean> = combine(
        _theoryContentLoaded,
        _essayContentLoaded,
        _tasksContentLoaded
    ) { theoryLoaded, essayLoaded, tasksLoaded ->
        theoryLoaded && essayLoaded && tasksLoaded
    }.stateIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    private val theoryContentCache = mutableMapOf<String, TheoryContentDto>()
    private val essayContentCache = mutableMapOf<String, EssayContentDto>()
    private val taskTextCache = mutableMapOf<String, String>()
    
    private val CACHE_EXPIRATION_TIME = 24 * 60 * 60 * 1000L
    
    private val updatedTaskCategories = mutableSetOf<String>()
    
    private val tasksCategoryCache = mutableMapOf<String, List<TaskItem>>()
    
    private val tasksCacheTimestamps = mutableMapOf<String, Long>()

    private val taskDetailCache = mutableMapOf<Int, TaskItem>()

    companion object {
        const val NO_DATA_AND_NETWORK_ISSUE_FLAG = "NO_DATA_AND_NETWORK_ISSUE"
    }

    private fun logCacheContents(categoryId: String) {
        val cachedTasks = tasksCategoryCache[categoryId]
        if (cachedTasks != null) {
            Timber.d("--- Проверка кэша для категории '$categoryId' ---")
            cachedTasks.forEachIndexed { index, task ->
                val contentPreview = task.content.take(80).replace("\n", " ")
                Timber.d("  #${index + 1}: TaskId=${task.taskId}, TextId=${task.textId}, IsSolved=${task.isSolved}, Content='${contentPreview}...'")
            }
            Timber.d("--- Конец проверки кэша для категории '$categoryId' ---")
        } else {
            Timber.d("Кеш для категории '$categoryId' пуст.")
        }
    }

    /**
     * Получает поток списка тем теории из локальной БД.
     */
    fun getTheoryTopicsStream(): Flow<List<ContentEntity>> {
        Log.d(TAG, "Getting theory topics stream from cache")
        return theoryTopicsCache.combine(downloadedTheoryDao.getAllIdsAsFlow()) { dtos, downloadedIds ->
            val downloadedIdsSet = downloadedIds.toSet()
            dtos.map { dto ->
                dto.toContentEntity().apply {
                    this.isDownloaded = downloadedIdsSet.contains(this.contentId)
                }
            }
        }
    }

    /**
     * Получает поток списка тем сочинений из локальной БД.
     */
    fun getEssayTopicsStream(): Flow<List<ContentEntity>> {
        Log.d(TAG, "Getting essay topics stream from DAO")
        return contentDao.getContentsByType("essay").asFlow()
    }

    /**
     * Получает поток списка заданий из локальной БД.
     */
    fun getTasksTopicsStream(): Flow<List<ContentEntity>> {
        Timber.d("Getting tasks topics stream from cache and combining with DB status")
        return taskTopicsCache.combine(taskDao.getDownloadedEgeNumbersStream()) { cachedGroups, downloadedEgeNumbers ->
            val downloadedSet = downloadedEgeNumbers.toSet()
            cachedGroups.map { group ->
                val egeNumber = group.contentId.removePrefix("task_group_")
                group.apply {
                    this.isDownloaded = downloadedSet.contains(egeNumber)
                }
            }
        }
    }

    /**
     * Запрашивает список тем теории с сервера и сохраняет в БД.
     * Добавляет новые элементы и обновляет существующие.
     * Загрузка происходит только если в БД нет данных по теории.
     */
    suspend fun refreshTheoryTopics() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing theory topics from network...")
                val response = theoryApiService.getAllTheory()

                if (response.isSuccessful && response.body() != null) {
                    val theorySummaries = response.body()!!
                    theoryTopicsCache.value = theorySummaries
                    Log.d(TAG, "Successfully refreshed and cached ${theorySummaries.size} theory topics from network")
                } else {
                    Log.w(TAG, "Failed to refresh theory topics. Code: ${response.code()}")
                    if (theoryTopicsCache.value.isNotEmpty()) {
                        Log.d(TAG, "Using existing theory topics from cache (${theoryTopicsCache.value.size} items)")
                    } else {
                        Log.w(TAG, "No existing theory topics in cache")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing theory topics", e)
                if (theoryTopicsCache.value.isNotEmpty()) {
                    Log.d(TAG, "Using existing theory topics from cache after error (${theoryTopicsCache.value.size} items)")
                } else {
                    Log.w(TAG, "No existing theory topics in cache after error")
                }
            } finally {
                _theoryContentLoaded.value = true
            }
        }
    }

    /**
     * Запрашивает список тем сочинений с сервера и сохраняет в БД.
     * Добавляет новые элементы и обновляет существующие.
     * Загрузка происходит только если в БД нет данных по сочинениям.
     */
    suspend fun refreshEssayTopics() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing essay topics from network...")
                val response = essayApiService.getAllEssayTopics()

                if (response.isSuccessful && response.body() != null) {
                    val essaySummaries = response.body()!!
                    
                    val allContentEntities = essaySummaries.map { dto -> 
                        val entity = dto.toContentEntity()
                        entity.setDownloaded(false)
                        entity
                    }
                    
                    contentDao.insertAll(allContentEntities)
                    Log.d(TAG, "Successfully refreshed and saved ${allContentEntities.size} essay topics from network to DB")
                } else {
                    Log.w(TAG, "Failed to refresh essay topics. Code: ${response.code()}")
                    val existingEssays = contentDao.getContentsByTypeSync("essay")
                    if (existingEssays.isNotEmpty()) {
                        Log.d(TAG, "Using existing essay topics from DB (${existingEssays.size} items)")
                    } else {
                        Log.w(TAG, "No existing essay topics in DB")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing essay topics", e)
                val existingEssays = contentDao.getContentsByTypeSync("essay")
                if (existingEssays.isNotEmpty()) {
                    Log.d(TAG, "Using existing essay topics from DB after error (${existingEssays.size} items)")
                } else {
                    Log.w(TAG, "No existing essay topics in DB after error")
                }
            } finally {
                _essayContentLoaded.value = true
            }
        }
    }

    /**
     * Запрашивает список заданий с сервера и сохраняет в БД.
     * Добавляет новые элементы и обновляет существующие.
     * Загрузка происходит только если в БД нет групп заданий.
     */
    suspend fun refreshTasksTopics() {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Refreshing tasks topics from network...")

                val response = taskApiService.getAllTasks()

                if (response.isSuccessful && response.body() != null) {
                    val taskGroups = response.body()!!
                    val newTasksEntities = mutableListOf<ContentEntity>()

                    for (group in taskGroups) {
                        val egeNumber = group["ege_number"] as? String ?: continue
                        val title = group["title"] as? String ?: "Задание $egeNumber"
                        val countValue = when (val count = group["count"]) {
                            is Double -> count.toInt()
                            is Int -> count
                            is String -> count.toIntOrNull() ?: 0
                            else -> 0
                        }

                        val contentId = "task_group_$egeNumber"

                        val downloadedCount = taskDao.getTaskCountByEgeNumberSync(egeNumber)
                        val isDownloaded = downloadedCount > 0

                        val entity = ContentEntity().apply {
                            setContentId(contentId)
                            setTitle(title)
                            setType("task_group")

                            val orderPosition = try {
                                egeNumber.toInt()
                            } catch (e: NumberFormatException) {
                                1
                            }
                            setOrderPosition(orderPosition)

                            setDescription("$countValue заданий")

                            setDownloaded(isDownloaded)
                        }

                        newTasksEntities.add(entity)
                    }

                    taskTopicsCache.value = newTasksEntities
                    Timber.d("Successfully refreshed and cached ${newTasksEntities.size} task groups from network")
                } else {
                    Timber.w("Failed to refresh task group topics. Code: ${response.code()}")
                    if (taskTopicsCache.value.isNotEmpty()) {
                        Timber.d("Using existing task groups from cache (${taskTopicsCache.value.size} items)")
                    } else {
                        Timber.w("No existing task groups in cache")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing task topics")
                if (taskTopicsCache.value.isNotEmpty()) {
                    Timber.d("Using existing task groups from cache after error (${taskTopicsCache.value.size} items)")
                } else {
                    Timber.w("No existing task groups in cache after error")
                }
            } finally {
                _tasksContentLoaded.value = true
            }
        }
    }

    /**
     * Получает содержимое конкретной теории по ID.
     * Сначала проверяет кэш в памяти, затем запрашивает с сервера.
     * @param contentId ID теории
     * @return Объект TheoryContentDto с содержимым или null в случае ошибки
     */
    suspend fun getTheoryContentById(contentId: String): TheoryContentDto? {
        if (theoryContentCache.containsKey(contentId)) {
            Log.d(TAG, "Getting theory content from memory cache for ID: $contentId")
            return theoryContentCache[contentId]
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting theory content from network for ID: $contentId")
                val response = theoryApiService.getTheoryContent(contentId)
                
                if (response.isSuccessful && response.body() != null) {
                    val theoryContent = response.body()!!
                    Log.d(TAG, "Successfully loaded theory content from network, caching result.")
                    theoryContentCache[contentId] = theoryContent
                    return@withContext theoryContent
                } else {
                    Log.w(TAG, "Failed to load theory content from network. Code: ${response.code()}")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading theory content", e)
                return@withContext null
            }
        }
    }

    /**
     * Получает содержимое конкретного сочинения по ID.
     * Сначала проверяет кэш в памяти, затем запрашивает с сервера.
     * @param contentId ID сочинения
     * @return Объект EssayContentDto с содержимым или null в случае ошибки
     */
    suspend fun getEssayContentById(contentId: String): EssayContentDto? {
        if (essayContentCache.containsKey(contentId)) {
            Log.d(TAG, "Getting essay content from memory cache for ID: $contentId")
            return essayContentCache[contentId]
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting essay content from network for ID: $contentId")
                val response = essayApiService.getEssayContent(contentId)
                
                if (response.isSuccessful && response.body() != null) {
                    val essayContent = response.body()!!
                    Log.d(TAG, "Successfully loaded essay content from network, caching result.")
                    essayContentCache[contentId] = essayContent
                    return@withContext essayContent
                } else {
                    Log.w(TAG, "Failed to load essay content from network. Code: ${response.code()}")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading essay content", e)
                return@withContext null
            }
        }
    }

    /**
     * Получает список заданий по категории ЕГЭ.
     * Реализует простую стратегию кэширования на время сессии: при первом обращении к категории
     * данные обновляются с сервера, при последующих - берутся из локальной БД.
     * Для пагинации (page > 1) всегда идет запрос на сервер.
     */
    fun getTasksByCategory(categoryId: String, page: Int = 1, pageSize: Int = 20): Flow<Result<List<TaskItem>>> = flow {
        val actualCategoryId = categoryId.replace("task_group_", "")
        Timber.d("Запрос заданий для категории: $categoryId (ID для API: $actualCategoryId), страница: $page")

        // Проверяем кеш по обоим возможным ключам - с префиксом и без
        val cachedTasks = tasksCategoryCache[categoryId] ?: tasksCategoryCache[actualCategoryId]
        if (page == 1 && cachedTasks != null) {
            Timber.d("Категория $categoryId найдена в кэше. Возвращаем ${cachedTasks.size} заданий.")
            emit(Result.Success(cachedTasks))
            return@flow
        }

        if (page == 1) {
            val localTasks = withContext(Dispatchers.IO) { taskDao.getTasksByEgeNumberSync(actualCategoryId) }
            if (localTasks.isNotEmpty()) {
                Timber.d("Найдено ${localTasks.size} скачанных заданий в БД для категории $actualCategoryId. Пагинация для этой категории будет отключена.")
                val taskItems = localTasks.map { it.toTaskItem() }.sortedBy { it.orderPosition }
                
                // Сохраняем в кеш с полным ID (с префиксом)
                tasksCategoryCache[categoryId] = taskItems
                hasMoreItemsMap[categoryId] = false
                emit(Result.Success(taskItems))
                return@flow
            }
        }
        
        if (!actualCategoryId.all { it.isDigit() }) {
            Timber.e("Некорректный ID категории для API: $actualCategoryId")
            emit(Result.Failure(IllegalArgumentException("Некорректный ID категории для API: $actualCategoryId")))
            return@flow
        }
        
        Timber.d("Требуется загрузка с сервера для категории $categoryId. page=$page")
        try {
            val response = taskApiService.getTasksByEgeNumberPaginated(actualCategoryId, limit = pageSize, pageNumber = page)
            if (response.isSuccessful) {
                val responseBody = response.body()
                val taskDtos = responseBody?.tasks ?: emptyList()
                Timber.d("С сервера для EGE $actualCategoryId получено ${taskDtos.size} заданий.")

                if (page == 1) {
                    hasMoreItemsMap[categoryId] = taskDtos.size >= pageSize
                }

                if (taskDtos.isNotEmpty()) {
                    val taskItems = taskDtos.map { it.toTaskItem() }.sortedBy { it.orderPosition }
                    
                    if (page == 1) {
                        // Сохраняем в кеш с полным ID (с префиксом)
                        tasksCategoryCache[categoryId] = taskItems
                    }
                    
                    emit(Result.Success(taskItems))
                } else {
                    val localTasks = withContext(Dispatchers.IO) { taskDao.getTasksByEgeNumberSync(actualCategoryId) }
                    if (page == 1 && localTasks.isEmpty()) {
                         Timber.w("Сервер вернул пустой список, и локальных данных нет для $categoryId.")
                         emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
                    } else {
                         emit(Result.Success(emptyList()))
                    }
                }
            } else {
                Timber.w("Ошибка сервера при загрузке заданий для $categoryId: ${response.code()}. Используем локальные данные, если есть.")
                val localTasks = withContext(Dispatchers.IO) { taskDao.getTasksByEgeNumberSync(actualCategoryId) }
                if (localTasks.isNotEmpty() && page == 1) {
                    emit(Result.Success(localTasks.map { it.toTaskItem() }))
                } else {
                    emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сети при загрузке заданий для $categoryId. Используем локальные данные, если есть.")
            val localTasks = withContext(Dispatchers.IO) { taskDao.getTasksByEgeNumberSync(actualCategoryId) }
            if (localTasks.isNotEmpty() && page == 1) {
                emit(Result.Success(localTasks.map { it.toTaskItem() }))
            } else {
                emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
            }
        }
    }

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
            emit(Result.Failure(IllegalArgumentException("Неверный формат ID задания: $taskIdString")))
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
                    emit(Result.Failure(Exception("Пустой ответ от сервера для задания $taskIdString")))
                }
            } else {
                emit(Result.Failure(Exception("Ошибка сервера: ${response.code()} для задания $taskIdString")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сети при получении деталей задания ID $taskIdString.")
            emit(Result.Failure(e))
        }
    }

    /**
     * Получает содержимое конкретного элемента контента (например, текст теории).
     * Сначала проверяет локальную БД, затем запрашивает с сервера.
     */
    suspend fun getContentDetails(contentId: String): ContentEntity? {
        return withContext(Dispatchers.IO) {
            var content = contentDao.getContentByIdSync(contentId)
            content
        }
    }
    
    /**
     * Определяет тип ответа на основе вариантов ответов.
     */
    private fun determineAnswerType(options: List<com.ruege.mobile.data.network.dto.response.TaskOptionDto>?): AnswerType {
        return when {
            options == null -> AnswerType.TEXT
            options.count { it.isCorrect } > 1 -> AnswerType.MULTIPLE_CHOICE
            else -> AnswerType.SINGLE_CHOICE
        }
    }

    /**
     * Преобразует строковое представление типа задания в перечисление AnswerType.
     */
    private fun convertStringToAnswerType(type: String): AnswerType {
        return when (type.uppercase()) {
            "TEXT" -> AnswerType.TEXT
            "SINGLE_CHOICE" -> AnswerType.SINGLE_CHOICE
            "MULTIPLE_CHOICE" -> AnswerType.MULTIPLE_CHOICE
            "NUMBER" -> AnswerType.NUMBER
            else -> AnswerType.TEXT
        }
    }
    
    /*
    suspend fun checkAnswer(taskId: Int, userAnswer: String): AnswerCheckResult {
        Timber.d("Отправка ответа '$userAnswer' для задания $taskId на сервер")
        val request = AnswerRequest(userAnswer = userAnswer)
        val response: AnswerCheckResult = taskApiService.checkAnswer(taskId, request)

        Timber.d("Получен ответ от сервера для задания $taskId: isCorrect=${response.isCorrect}")
        return response
    }
    */
    
    private val currentPageMap = mutableMapOf<String, Int>()
    private val hasMoreItemsMap = mutableMapOf<String, Boolean>()
    private val pageSize = 20
    
    /**
     * Проверяет, есть ли еще задания для загрузки для указанной категории
     */
    fun hasMoreTasksToLoad(categoryId: String): Boolean {
        return hasMoreItemsMap[categoryId] ?: true
    }
    
    /**
     * Сбрасывает информацию о пагинации для указанной категории или всех категорий.
     */
    fun resetTaskPagination(categoryId: String? = null) {
        if (categoryId != null) {
            currentPageMap[categoryId] = 1
            hasMoreItemsMap[categoryId] = true
            Timber.d("Сброшена информация о пагинации для категории $categoryId")
        } else {
            currentPageMap.clear()
            hasMoreItemsMap.clear()
            Timber.d("Сброшена информация о пагинации для всех категорий")
        }
    }
    
    /**
     * Загружает следующую страницу заданий для указанной категории.
     */
    fun loadMoreTasksByCategory(categoryId: String): Flow<Result<List<TaskItem>>> = flow {
        val egeNumber = categoryId.replace("task_group_", "")
        val currentPage = currentPageMap.getOrPut(categoryId) { 1 } + 1
        
        Timber.d("Загрузка дополнительных заданий для категории $categoryId (ID для API: $egeNumber), страница $currentPage")
        
        try {
            val response = taskApiService.getTasksByEgeNumberPaginated(
                egeNumber = egeNumber,
                limit = pageSize,
                pageNumber = currentPage,
                includeText = false
            )
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                val taskDtos = responseBody?.tasks ?: emptyList()
                
                hasMoreItemsMap[categoryId] = taskDtos.size >= pageSize
                
                if (taskDtos.isNotEmpty()) {
                    currentPageMap[categoryId] = currentPage
                    
                    val additionalTaskItems = taskDtos.map { it.toTaskItem() }
                    
                    // Используем полный ID категории с префиксом для кеширования
                    val existingTasks = tasksCategoryCache[categoryId] ?: emptyList()
                    tasksCategoryCache[categoryId] = (existingTasks + additionalTaskItems).sortedBy { it.orderPosition }
                    
                    Timber.d("Успешно загружено и добавлено в кэш ${additionalTaskItems.size} заданий для категории $categoryId. Новый размер кэша: ${tasksCategoryCache[categoryId]?.size}")
                    
                    emit(Result.Success(tasksCategoryCache[categoryId]!!))
                } else {
                    hasMoreItemsMap[categoryId] = false
                    emit(Result.Success(tasksCategoryCache[categoryId] ?: emptyList()))
                    Timber.d("Сервер вернул пустой список. Больше заданий для категории $categoryId нет.")
                }
            } else {
                Timber.w("Не удалось загрузить дополнительные задания с сервера для категории $categoryId (Код: ${response.code()}).")
                emit(Result.Failure(Exception("Ошибка сервера: ${response.code()}")))
            }
        } catch (e: Exception) { 
            Timber.e(e, "Ошибка сети при получении дополнительных заданий для категории $categoryId.")
            emit(Result.Failure(e))
        }
    }

    /**
     * Получает текст задания по его ID с сервера.
     * Сначала проверяет кэш в памяти, затем запрашивает с сервера.
     */
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
                    emit(Result.Failure(Exception("Отсутствует текст в ответе сервера")))
                }
            } else {
                val errorMsg = "Ошибка при загрузке текста задания $textId: ${response.code()} - ${response.message()}"
                Timber.e(errorMsg)
                emit(Result.Failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Исключение при загрузке текста задания $textId")
            emit(Result.Failure(e))
        }
    }

    suspend fun getTasksByIds(taskIds: List<Int>): List<TaskEntity> = withContext(Dispatchers.IO) {
        taskDao.getTasksByIds(taskIds)
    }

    private fun TaskDto.toTaskItem(): TaskItem {
        // Получаем порядковый номер из номера задания ЕГЭ
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
            content = this.taskText ?: "",
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

    private fun TaskEntity.toTaskItem(): TaskItem {
        // Получаем порядковый номер из номера задания ЕГЭ
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

    private fun TaskEntity.toTaskItemWithText(textDto: com.ruege.mobile.data.network.dto.response.TextDataDto?): TaskItem {
        val contentHtml = this.taskText ?: ""
        
        // Получаем порядковый номер из номера задания ЕГЭ
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

    suspend fun downloadTheory(contentId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            val theoryDto = getTheoryContentById(contentId)
            if (theoryDto != null) {
                val downloadedEntity = DownloadedTheoryEntity(
                    theoryDto.id.toString(),
                    theoryDto.title,
                    theoryDto.content,
                    System.currentTimeMillis()
                )
                downloadedTheoryDao.insert(downloadedEntity)

                val contentEntity = ContentEntity.createForKotlin(
                    theoryDto.id.toString(),
                    theoryDto.title,
                    "", 
                    "theory",
                    null, 
                    true, 
                    false, 
                    theoryDto.egeNumber
                )
                contentDao.insert(contentEntity)

                emit(Result.Success(Unit))
            } else {
                emit(Result.Failure(Exception("Не удалось загрузить теорию для скачивания.")))
            }
        } catch (e: Exception) {
            emit(Result.Failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getDownloadedTheory(contentId: String): LiveData<DownloadedTheoryEntity> {
        return downloadedTheoryDao.getDownloadedTheoryById(contentId)
    }

    suspend fun deleteDownloadedTheory(contentId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            downloadedTheoryDao.deleteById(contentId)
            contentDao.deleteById(contentId)
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun downloadTaskGroup(egeNumber: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            Timber.d("Starting download for task group egeNumber: $egeNumber")
            val response = taskApiService.getTasksByEgeNumberPaginated(
                egeNumber = egeNumber,
                limit = 1000, 
                pageNumber = 1,
                includeText = true
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                val taskDtos = responseBody?.tasks ?: emptyList()
                val textDtos = responseBody?.texts ?: emptyList()

                if (taskDtos.isNotEmpty()) {
                    val taskEntities = taskDtos.map { it.toEntity() }
                    taskDao.insertAll(taskEntities)

                    if (textDtos.isNotEmpty()) {
                        val textEntities = textDtos.map { TaskTextEntity(it.id.toString(), it.content ?: "") }
                        taskTextDao.insertAll(textEntities)
                    }
                    Timber.d("Successfully downloaded and saved ${taskDtos.size} tasks for egeNumber $egeNumber.")
                    
                    val contentId = "task_group_$egeNumber"
                    val title = "Задание $egeNumber"
                    val description = "${taskDtos.size} заданий"
                    
                    // Преобразуем номер задания в позицию для сортировки
                    val orderPosition = try {
                        egeNumber.toInt()
                    } catch (e: NumberFormatException) {
                        1
                    }
                    
                    val contentEntity = ContentEntity.createForKotlin(
                        contentId,
                        title,
                        description,
                        "task_group",
                        null,
                        true,  // Устанавливаем флаг isDownloaded
                        false,
                        egeNumber.toInt()
                    )
                    // Устанавливаем порядковый номер для сортировки
                    contentEntity.setOrderPosition(orderPosition)
                    
                    contentDao.insert(contentEntity)
                    Timber.d("Updated content table for task group $egeNumber marking as downloaded")
                    
                    emit(Result.Success(Unit))
                } else {
                    emit(Result.Failure(Exception("No tasks found for group $egeNumber to download.")))
                }
            } else {
                emit(Result.Failure(Exception("Failed to fetch tasks for group $egeNumber. Code: ${response.code()}")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading task group $egeNumber")
            emit(Result.Failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteDownloadedTaskGroup(egeNumber: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            val tasksInGroup = taskDao.getTasksByEgeNumberSync(egeNumber)
            val textIdsToDelete = tasksInGroup.mapNotNull { it.getTextId() }.distinct().map { it.toString() }

            taskDao.deleteTasksByEgeNumber(egeNumber)
            Timber.d("Deleted all tasks for egeNumber $egeNumber.")

            if (textIdsToDelete.isNotEmpty()) {
                taskTextDao.deleteByIds(textIdsToDelete)
                Timber.d("Deleted ${textIdsToDelete.size} associated texts for egeNumber $egeNumber.")
            }

            emit(Result.Success(Unit))
        } catch(e: Exception) {
            Timber.e(e, "Error deleting task group $egeNumber")
            emit(Result.Failure(e))
        }
    }.flowOn(Dispatchers.IO)
}