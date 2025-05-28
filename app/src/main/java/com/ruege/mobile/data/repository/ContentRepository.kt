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

@Singleton
class ContentRepository @Inject constructor(
    private val contentDao: ContentDao,
    private val taskDao: TaskDao,
    private val theoryApiService: TheoryApiService,
    private val taskApiService: TaskApiService,
    private val essayApiService: EssayApiService,
    private val externalScope: CoroutineScope
) {

    private val TAG = "ContentRepository"
    
    // StateFlow для отслеживания загрузки контента
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
        started = SharingStarted.Lazily,
        initialValue = false
    )
    
    // Срок действия кэша в миллисекундах (24 часа)
    private val CACHE_EXPIRATION_TIME = 24 * 60 * 60 * 1000L
    
    // Список категорий заданий, которые уже были обновлены в текущей сессии
    private val updatedTaskCategories = mutableSetOf<String>()
    
    // Кэш заданий по категориям для избежания повторных запросов
    private val tasksCategoryCache = mutableMapOf<String, List<TaskItem>>()
    
    // Время последней загрузки заданий каждой категории (в миллисекундах)
    private val tasksCacheTimestamps = mutableMapOf<String, Long>()

    companion object {
        const val NO_DATA_AND_NETWORK_ISSUE_FLAG = "NO_DATA_AND_NETWORK_ISSUE"
    }

    /**
     * Получает поток списка тем теории из локальной БД.
     */
    fun getTheoryTopicsStream(): Flow<List<ContentEntity>> {
        Log.d(TAG, "Getting theory topics stream from DAO")
        // Используем LiveData.asFlow() для преобразования
        return contentDao.getContentsByType("theory").asFlow()
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
        Timber.d("Getting tasks topics stream from DAO")
        // Используем LiveData.asFlow() для преобразования
        return contentDao.getContentsByType("task_group").asFlow()
    }

    /**
     * Запрашивает список тем теории с сервера и сохраняет в БД.
     * Добавляет новые элементы и обновляет существующие.
     */
    suspend fun refreshTheoryTopics() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing theory topics from network...")
                val response = theoryApiService.getAllTheory()

                if (response.isSuccessful && response.body() != null) {
                    val theorySummaries = response.body()!!
                    
                    // Получаем список уже существующих элементов ТЕОРИИ из БД
                    val existingContentIds = contentDao.getContentsByTypeSync("theory").map { it.contentId }.toSet()
                    
                    // Преобразуем все записи в ContentEntity
                    val allContentEntities = theorySummaries.map { dto -> 
                        val entity = dto.toContentEntity()
                        // Устанавливаем признак загруженности
                        entity.setDownloaded(true)
                        entity
                    }
                    
                    // Разделяем на новые и существующие записи
                    val newContentEntities = allContentEntities.filter { !existingContentIds.contains(it.contentId) }
                    val existingContentEntities = allContentEntities.filter { existingContentIds.contains(it.contentId) }
                    
                    // Обновляем существующие записи
                    if (existingContentEntities.isNotEmpty()) {
                        Log.d(TAG, "Updating ${existingContentEntities.size} existing theory topics in database")
                        contentDao.updateAll(existingContentEntities)
                        Log.d(TAG, "Successfully updated existing theory topics")
                    }
                    
                    // Добавляем новые записи
                    if (newContentEntities.isNotEmpty()) {
                        Log.d(TAG, "Adding ${newContentEntities.size} new theory topics to database")
                        contentDao.insertAll(newContentEntities)
                        Log.d(TAG, "Successfully added new theory topics")
                    }
                    
                    Log.d(TAG, "Successfully refreshed theory topics from network: ${existingContentEntities.size} updated, ${newContentEntities.size} added")
                    _theoryContentLoaded.value = true
                } else {
                    Log.w(TAG, "Failed to refresh theory topics. Code: ${response.code()}")
                    // Можно добавить обработку ошибок, например, показать сообщение пользователю
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing theory topics", e)
                // Обработка исключений (например, нет сети)
            }
        }
    }

    /**
     * Запрашивает список тем сочинений с сервера и сохраняет в БД.
     */
    suspend fun refreshEssayTopics() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing essay topics from network...")
                val response = essayApiService.getAllEssayTopics()

                if (response.isSuccessful && response.body() != null) {
                    val essaySummaries = response.body()!!
                    
                    // Получаем существующие ID сочинений
                    val existingContentIds = contentDao.getContentsByTypeSync("essay").map { it.contentId }.toSet()
                    
                    val allContentEntities = essaySummaries.map { dto -> 
                        val entity = dto.toContentEntity()
                        entity.setDownloaded(true)
                        entity
                    }
                    
                    val newContentEntities = allContentEntities.filter { !existingContentIds.contains(it.contentId) }
                    val existingContentEntities = allContentEntities.filter { existingContentIds.contains(it.contentId) }
                    
                    if (existingContentEntities.isNotEmpty()) {
                        Log.d(TAG, "Updating ${existingContentEntities.size} existing essay topics in database")
                        contentDao.updateAll(existingContentEntities)
                        Log.d(TAG, "Successfully updated existing essay topics")
                    }
                    
                    if (newContentEntities.isNotEmpty()) {
                        Log.d(TAG, "Adding ${newContentEntities.size} new essay topics to database")
                        contentDao.insertAll(newContentEntities)
                        Log.d(TAG, "Successfully added new essay topics")
                    }
                    
                    Log.d(TAG, "Successfully refreshed essay topics from network: ${existingContentEntities.size} updated, ${newContentEntities.size} added")
                    _essayContentLoaded.value = true
                } else {
                    Log.w(TAG, "Failed to refresh essay topics. Code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing essay topics", e)
            }
        }
    }

    /**
     * Запрашивает список заданий с сервера и сохраняет в БД.
     * Добавляет новые элементы и обновляет существующие.
     */
    suspend fun refreshTasksTopics() {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Refreshing tasks topics from network...")
                
                // Получаем группы заданий ЕГЭ
                val response = taskApiService.getAllTasks()
                
                if (response.isSuccessful && response.body() != null) {
                    val taskGroups = response.body()!!
                    val newTasksEntities = mutableListOf<ContentEntity>()
                    val existingTasksEntities = mutableListOf<ContentEntity>()
                    
                    // Получаем список уже существующих элементов из БД
                    val existingContentIds = contentDao.getAllProgressContentIds().toSet()
                    
                    // Создаем ContentEntity для каждой группы заданий
                    for (group in taskGroups) {
                        val egeNumber = group["ege_number"] as? String ?: continue
                        val title = group["title"] as? String ?: "Задание $egeNumber"
                        val countValue = when (val count = group["count"]) {
                            is Double -> count.toInt()
                            is Int -> count
                            is String -> count.toIntOrNull() ?: 0
                            else -> 0
                        }
                        
                        // Используем egeNumber как contentId, чтобы избежать дублирования
                        val contentId = "task_group_$egeNumber"
                        
                        // Создаем ContentEntity для группы заданий
                        val entity = ContentEntity().apply { 
                            setContentId(contentId)
                            setTitle(title)
                            setType("task_group")
                            
                            // Используем номер задания ЕГЭ для порядка, преобразуя его в число
                            val orderPosition = try {
                                egeNumber.toInt()
                            } catch (e: NumberFormatException) {
                                1 // По умолчанию, если не удалось преобразовать
                            }
                            setOrderPosition(orderPosition)
                            
                            // Устанавливаем описание с количеством заданий
                            setDescription("$countValue заданий")
                            
                            // Устанавливаем признак загруженности
                            setDownloaded(true)
                        }
                        
                        // Проверяем, существует ли уже такой элемент
                        if (existingContentIds.contains(contentId)) {
                            existingTasksEntities.add(entity)
                        } else {
                            newTasksEntities.add(entity)
                        }
                    }
                    
                    // Обновляем существующие записи
                    if (existingTasksEntities.isNotEmpty()) {
                        Timber.d("Updating ${existingTasksEntities.size} existing task groups in database")
                        contentDao.updateAll(existingTasksEntities)
                        Timber.d("Successfully updated existing task groups")
                    }
                    
                    // Добавляем новые записи
                    if (newTasksEntities.isNotEmpty()) {
                        Timber.d("Adding ${newTasksEntities.size} new task groups to database")
                        contentDao.insertAll(newTasksEntities)
                        Timber.d("Successfully added new task groups")
                    }
                    
                    Timber.d("Successfully refreshed task groups from network: ${existingTasksEntities.size} updated, ${newTasksEntities.size} added")
                    _tasksContentLoaded.value = true

                    // После успешной загрузки всех групп заданий, нормализуем количество
                    normalizeAllTaskCounts()
                } else {
                    Timber.w("Failed to refresh task group topics. Code: ${response.code()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing task topics")
            }
        }
    }

    /**
     * Получает содержимое конкретной теории по ID.
     * В этой версии используем сетевой запрос без кэширования.
     * @param contentId ID теории
     * @return Объект TheoryContentDto с содержимым или null в случае ошибки
     */
    suspend fun getTheoryContentById(contentId: String): TheoryContentDto? {
        return withContext(Dispatchers.IO) {
            try {
                // Запрашиваем с сервера
                Log.d(TAG, "Getting theory content from network for ID: $contentId")
                val response = theoryApiService.getTheoryContent(contentId)
                
                if (response.isSuccessful && response.body() != null) {
                    val theoryContent = response.body()!!
                    Log.d(TAG, "Successfully loaded theory content from network")
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
     * @param contentId ID сочинения
     * @return Объект EssayContentDto с содержимым или null в случае ошибки
     */
    suspend fun getEssayContentById(contentId: String): EssayContentDto? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting essay content from network for ID: $contentId")
                val response = essayApiService.getEssayContent(contentId)
                
                if (response.isSuccessful && response.body() != null) {
                    val essayContent = response.body()!!
                    Log.d(TAG, "Successfully loaded essay content from network")
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
     * При первом обращении к категории в рамках сессии всегда запрашивает обновление с сервера.
     * Для последующих обращений сначала проверяет локальную БД, затем сервер.
     */
    fun getTasksByCategory(categoryId: String, page: Int = 1, pageSize: Int = 20): Flow<Result<List<TaskItem>>> = flow {
        // Извлекаем чистый ID категории (например, "1" из "task_group_1")
        // Эта логика уже должна быть здесь или в вызывающем коде (ViewModel)
        // Убедимся, что работаем с чистым ID для API
        val actualCategoryId = categoryId.replace("task_group_", "")
        Timber.d("Запрос заданий для категории: $categoryId (actual ID for API: $actualCategoryId), страница: $page")

        // Проверка, является ли actualCategoryId числом
        if (!actualCategoryId.all { it.isDigit() }) {
            Timber.e("Некорректный actualCategoryId для API: $actualCategoryId. Ожидался числовой ID.")
            emit(Result.Failure(IllegalArgumentException("Некорректный ID категории для API: $actualCategoryId")))
            return@flow
        }
        
        var tasksFromDb: List<com.ruege.mobile.data.local.entity.TaskEntity>? = null
        val isFirstAccess = !updatedTaskCategories.contains(categoryId) // Проверяем для categoryId (e.g., "task_group_1")
        var shouldFetchFromServer = isFirstAccess 
        Timber.d("Для категории $categoryId: isFirstAccess = $isFirstAccess, начальное shouldFetchFromServer = $shouldFetchFromServer")
        
        // Сначала пытаемся загрузить из локальной БД, если это первая страница
        if (page == 1) { // Логично проверять БД только для первой страницы
            try {
                tasksFromDb = withContext(Dispatchers.IO) { 
                    taskDao.getTasksByEgeNumberSync(actualCategoryId)
                }
                Timber.d("Прочитано из БД для EGE $actualCategoryId: ${tasksFromDb?.size ?: "null"} заданий.")
                
                // Если локальная БД пуста, нужно загрузить с сервера в любом случае
                if (tasksFromDb.isNullOrEmpty()) {
                    shouldFetchFromServer = true
                    Timber.d("Локальных данных нет, shouldFetchFromServer = true")
                } else if (!isFirstAccess) {
                    // Локальные данные есть, и это не первый доступ, можем отдать и не ходить на сервер (если не хотим принудительного обновления)
                    // В текущей логике, если !isFirstAccess, то shouldFetchFromServer уже false.
                    // Можно добавить флаг принудительного обновления, если потребуется.
                    Timber.d("Локальные данные есть, не первый доступ.")
                }
            } catch (e: Exception) {
                Timber.w(e, "Ошибка при доступе к БД для EGE $actualCategoryId. Загружаем с сервера.")
                shouldFetchFromServer = true
            }
        } else {
            // Для последующих страниц всегда идем на сервер
            shouldFetchFromServer = true
            Timber.d("Запрос не первой страницы ($page), shouldFetchFromServer = true")
        }

        // Шаг 2: Сначала возвращаем локальные данные, если они есть и НЕ нужно идти на сервер
        // (Эта логика была немного запутана, упрощаем)
        if (!shouldFetchFromServer && tasksFromDb != null && !tasksFromDb.isEmpty()) {
            Timber.d("Используем только локальные данные для $categoryId (не первая страница или не первый доступ с данными в кеше)")
            val taskItems = tasksFromDb.map { it.toTaskItem() }
            emit(Result.Success(taskItems))
            return@flow 
        }

        // Шаг 3: Загружаем данные с сервера (при первом доступе или если локальных данных нет, или не первая страница)
        if (shouldFetchFromServer) {
            try {
                Timber.d("Загрузка заданий для EGE $actualCategoryId с сервера.")
                val response = taskApiService.getTasksByEgeNumberPaginated(actualCategoryId, limit = pageSize, pageNumber = page)
                if (response.isSuccessful) {
                    val taskDtos = response.body() ?: emptyList()
                    
                    if (taskDtos.isNotEmpty()) {
                        // Преобразуем DTO в сущности для сохранения в БД
                        val taskEntities = taskDtos.map { it.toEntity() }
                        
                        // Сохраняем в базу данных
                        withContext(Dispatchers.IO) {
                            try {
                                Timber.d("Сохранение ${taskEntities.size} заданий в БД для EGE $actualCategoryId...")
                                
                                // Если это первый доступ, удаляем старые данные перед вставкой новых
                                if (shouldFetchFromServer && tasksFromDb != null && tasksFromDb.isNotEmpty()) {
                                    Timber.d("Удаление старых заданий для EGE $actualCategoryId...")
                                    taskDao.deleteByEgeNumber(actualCategoryId)
                                }
                                
                                // Вставляем новые данные
                                taskDao.insertAll(taskEntities)
                                
                                // Помечаем категорию как обновленную в текущей сессии
                                updatedTaskCategories.add(categoryId)
                                
                                // Обновляем соответствующую запись ContentEntity для отображения актуального количества заданий
                                if (shouldFetchFromServer) {
                                    try {
                                        val contentId = "task_group_$actualCategoryId"
                                        val contentEntity = contentDao.getContentByIdSync(contentId)
                                        if (contentEntity != null) {
                                            Timber.d("[COUNT_DEBUG] Существующее описание для группы $actualCategoryId: ${contentEntity.getDescription()}")
                                            
                                            // Проверяем, содержит ли описание информацию о количестве заданий
                                            val currentDescription = contentEntity.getDescription() ?: ""
                                            val hasTaskCount = currentDescription.contains("заданий") || currentDescription.contains("задание")
                                            
                                            if (hasTaskCount) {
                                                // Если описание уже содержит информацию о количестве - сохраняем его
                                                Timber.d("[COUNT_DEBUG] Сохраняем существующее описание о количестве заданий: $currentDescription")
                                                contentDao.updateDownloadStatus(contentId, true)
                                            } else {
                                                // Если описание не содержит информацию о количестве - обновляем его
                                                Timber.d("[COUNT_DEBUG] Описание не содержит информацию о количестве заданий, устанавливаем новое: ${taskEntities.size} заданий")
                                                contentDao.updateDownloadStatusAndDescription(contentId, true, "${taskEntities.size} заданий")
                                            }
                                            Timber.d("[COUNT_DEBUG] Обновлена запись ContentEntity для группы $actualCategoryId")
                                        } else {
                                            // Если запись ContentEntity не найдена, создаем новую
                                            Timber.d("[COUNT_DEBUG] Создаем новую запись ContentEntity для группы $actualCategoryId: ${taskEntities.size} заданий")
                                            val newContentEntity = ContentEntity().apply {
                                                setContentId("task_group_$actualCategoryId")
                                                setTitle("Задание $actualCategoryId")
                                                setType("task_group")
                                                setOrderPosition(actualCategoryId.toIntOrNull() ?: 0)
                                                setDescription("${taskEntities.size} заданий")
                                                setDownloaded(true)
                                            }
                                            contentDao.insert(newContentEntity)
                                            Timber.d("[COUNT_DEBUG] Создана новая запись ContentEntity для группы $actualCategoryId: ${taskEntities.size} заданий")
                                        }
                                    } catch (contentException: Exception) {
                                        Timber.e(contentException, "[COUNT_DEBUG] Ошибка при обновлении ContentEntity для группы $actualCategoryId")
                                    }
                                } else {
                                    Timber.d("[COUNT_DEBUG] Пропускаем обновление описания ContentEntity для $actualCategoryId - не первый доступ")
                                }
                                
                                Timber.d("Успешно сохранено ${taskEntities.size} заданий, категория $categoryId отмечена как обновленная")
                            } catch (dbException: Exception) {
                                Timber.e(dbException, "!!! Ошибка БД при сохранении заданий для EGE $actualCategoryId")
                            }
                        }
                        
                        // Преобразуем DTO в TaskItem для отображения
                        val finalTaskItems = taskDtos.map { taskDto ->
                            TaskItem(
                                taskId = taskDto.id.toString(),
                                title = "Задание ${taskDto.egeNumber}",
                                egeTaskNumber = taskDto.egeNumber,
                                description = taskDto.taskText ?: "",
                                content = taskDto.taskText ?: "",
                                answerType = AnswerType.TEXT, 
                                maxPoints = 1, 
                                timeLimit = 0, 
                                solutions = null,
                                correctAnswer = taskDto.solution,
                                explanation = taskDto.explanation,
                                textId = taskDto.textId
                            )
                        }
                        
                        // Отправляем результат
                        emit(Result.Success(finalTaskItems))
                        Timber.d("Успешно загружено и смаплено ${finalTaskItems.size} заданий с сервера.")
                        
                        // Обновляем кэш категорий
                        tasksCategoryCache[categoryId] = finalTaskItems
                        tasksCacheTimestamps[categoryId] = System.currentTimeMillis()
                    } else if (tasksFromDb != null && tasksFromDb.isNotEmpty()) {
                        // Если с сервера пришел пустой список, но у нас есть локальные данные - используем их
                        Timber.d("Сервер вернул пустой список заданий, используем ${tasksFromDb.size} локальных заданий")
                        val taskItems = tasksFromDb.map { it.toTaskItem() }
                        emit(Result.Success(taskItems))
                    } else {
                        // И с сервера пусто, и локально ничего нет
                        Timber.w("Сервер вернул пустой список заданий, и локальных данных нет.")
                        emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
                    }
                } else {
                    // Ошибка запроса к серверу
                    Timber.w("Не удалось загрузить задания с сервера (Код: ${response.code()}).")
                    
                    // Если есть локальные данные - используем их
                    if (tasksFromDb != null && tasksFromDb.isNotEmpty()) {
                        Timber.d("Используем ${tasksFromDb.size} локальных заданий из-за ошибки сервера")
                        val taskItems = tasksFromDb.map { it.toTaskItem() }
                        emit(Result.Success(taskItems))
                    } else {
                        // Нет ни локальных данных, ни данных с сервера
                        emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
                    }
                }
            } catch (e: Exception) { 
                Timber.e(e, "Ошибка сети при получении заданий с сервера.")
                
                // Если есть локальные данные - используем их в случае сетевой ошибки
                if (tasksFromDb != null && tasksFromDb.isNotEmpty()) {
                    Timber.d("Используем ${tasksFromDb.size} локальных заданий из-за ошибки сети")
                    val taskItems = tasksFromDb.map { it.toTaskItem() }
                    emit(Result.Success(taskItems))
                } else {
                    emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
                }
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
        var dbFetchAttempted = false
        var taskEntity: com.ruege.mobile.data.local.entity.TaskEntity? = null
        var taskIdInt = -1
        
        // Проверяем, является ли это первое обращение к заданию в текущей сессии
        try {
            // Проверяем, содержит ли ID задания только цифры
            val isNumeric = taskIdString.all { it.isDigit() }
            if (!isNumeric) {
                Timber.e("ID задания не числовой: $taskIdString - требуется только числовой ID")
                emit(Result.Failure(IllegalArgumentException("ID задания должен быть числовым: $taskIdString")))
                return@flow
            }
            
            taskIdInt = taskIdString.toInt()
            Timber.d("ID задания преобразован в число: $taskIdInt")
        } catch (e: NumberFormatException) {
            Timber.e(e, "Неверный формат ID задания: $taskIdString")
            emit(Result.Failure(IllegalArgumentException("Неверный формат ID задания: $taskIdString")))
            return@flow
        }
        
        // Создаем уникальный ключ для этого задания (отличный от ключа категории)
        val taskKey = -taskIdInt // Используем отрицательное значение, чтобы отличать от ID категорий
        val isFirstAccess = !updatedTaskCategories.contains<String>(taskKey.toString())
        Timber.d("Запрос детального задания ID: $taskIdString, первое обращение: $isFirstAccess")
        
        var shouldFetchFromServer = isFirstAccess

        // Шаг 1: Загружаем данные из локальной БД
        try {
            dbFetchAttempted = true
            taskEntity = withContext(Dispatchers.IO) { 
                taskDao.getTaskByIdSync(taskIdInt)
            }
            Timber.d("Прочитано из БД для ID $taskIdInt: ${if(taskEntity != null) "Найдено" else "null"}.")
            
            // Если локальная БД пуста, нужно загрузить с сервера в любом случае
            if (taskEntity == null) {
                shouldFetchFromServer = true
            }
        } catch (e: Exception) {
            Timber.w(e, "Ошибка при доступе к БД для задания ID $taskIdInt. Загружаем с сервера.")
            shouldFetchFromServer = true
        }

        // Шаг 2: Сначала возвращаем локальные данные, если они есть и это не первый доступ
        if (!isFirstAccess && taskEntity != null) {
            Timber.d("Задание ID: $taskIdInt найдено в локальной БД. Отдаем результат.")
            val taskItem = taskEntity.toTaskItem() 
            emit(Result.Success(taskItem))
            if (!shouldFetchFromServer) {
                Timber.d("Используем только локальные данные, задание $taskIdInt уже было обновлено в этой сессии")
                return@flow // Не первый доступ, есть локальные данные - выходим
            }
        }

        // Шаг 3: Загружаем данные с сервера (при первом доступе или если локальных данных нет)
        if (shouldFetchFromServer) {
            try {
                Timber.d("Загрузка деталей задания ID: $taskIdString с сервера.")
                val response = taskApiService.getTaskDetail(taskIdString, includeText = true)
                if (response.isSuccessful) {
                    val taskDetailDto = response.body()
                    if (taskDetailDto != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                Timber.d("Сохранение задания ID: ${taskDetailDto.id} в БД...")
                                
                                // Сохраняем задание в БД используя insert
                                taskDao.insert(taskDetailDto.toEntity())
                                
                                // Отмечаем задание как обновленное в этой сессии
                                updatedTaskCategories.add(taskKey.toString())
                                
                                // ВАЖНО: НЕ обновляем ContentEntity для группы заданий здесь,
                                // чтобы избежать изменения количества заданий при просмотре деталей задания
                                Timber.d("[COUNT_DEBUG] getTaskDetail: НЕ обновляем ContentEntity для задания ID: ${taskDetailDto.id}, EGE: ${taskDetailDto.egeNumber}")
                                
                                // Проверка и логирование текущего статуса ContentEntity
                                if (taskDetailDto.egeNumber != null) {
                                    val contentId = "task_group_${taskDetailDto.egeNumber}"
                                    val contentEntity = contentDao.getContentByIdSync(contentId)
                                    Timber.d("[COUNT_DEBUG] getTaskDetail: текущее состояние ContentEntity для группы ${taskDetailDto.egeNumber}: " +
                                             "description = ${contentEntity?.getDescription() ?: "null"}, " +
                                             "downloaded = ${contentEntity?.isDownloaded() ?: false}")
                                }
                                
                                Timber.d("Успешно сохранено задание ID: ${taskDetailDto.id}, отмечено как обновленное.")
                                // TODO: Сохранить taskDetailDto.text в TextDao
                            } catch (dbException: Exception) {
                                Timber.e(dbException, "!!! Ошибка БД при сохранении задания ID: ${taskDetailDto.id}")
                            }
                        }
                        
                        // Формируем HTML с текстом, если он есть
                        val textContent = taskDetailDto.text?.content
                        val textAuthor = taskDetailDto.text?.author
                        val contentHtml = if (textContent != null) {
                            val authorHtml = if (textAuthor != null) "<p class='author'>— $textAuthor</p>" else ""
                            """
                            <div class="task-text">
                                $textContent
                                $authorHtml
                            </div>
                            <div class="task-question">
                                ${taskDetailDto.taskText ?: ""}
                            </div>
                            """.trimIndent()
                        } else {
                            taskDetailDto.taskText ?: ""
                        }
                        
                        // Создаем объект TaskItem для отображения
                        val taskItemFromServer = TaskItem(
                            taskId = taskDetailDto.id.toString(),
                            title = "Задание ${taskDetailDto.egeNumber ?: taskIdString}",
                            egeTaskNumber = taskDetailDto.egeNumber,
                            description = if (textContent != null) "Прочитайте текст и выполните задание" else "",
                            content = contentHtml,
                            answerType = AnswerType.TEXT, 
                            maxPoints = 1, 
                            timeLimit = 0, 
                            solutions = null,
                            correctAnswer = taskDetailDto.solution,
                            explanation = taskDetailDto.explanation
                        )
                        
                        // Отправляем результат
                        emit(Result.Success(taskItemFromServer))
                        Timber.d("Успешно загружено и смаплено задание ID: $taskIdString с сервера.")
                    } else {
                        Timber.w("Тело ответа для деталей задания ID: $taskIdString пусто.")
                        
                        // Если у нас все еще есть локальные данные - используем их
                        if (taskEntity != null) {
                            Timber.d("Используем локальное задание из-за пустого ответа сервера.")
                            val taskItem = taskEntity.toTaskItem()
                            emit(Result.Success(taskItem))
                        } else {
                            emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
                        }
                    }
                } else {
                    Timber.w("Не удалось загрузить детали задания с сервера (Код: ${response.code()}).")
                    
                    // Если у нас есть локальные данные - используем их в случае ошибки
                    if (taskEntity != null) {
                        Timber.d("Используем локальное задание из-за ошибки сервера.")
                        val taskItem = taskEntity.toTaskItem()
                        emit(Result.Success(taskItem))
                    } else {
                        emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
                    }
                }
            } catch (e: Exception) { 
                Timber.e(e, "Ошибка сети при получении деталей задания с сервера.")
                
                // Если у нас есть локальные данные - используем их в случае сетевой ошибки
                if (taskEntity != null) {
                    Timber.d("Используем локальное задание из-за ошибки сети.")
                    val taskItem = taskEntity.toTaskItem()
                    emit(Result.Success(taskItem))
                } else {
                    emit(Result.Failure(Exception(NO_DATA_AND_NETWORK_ISSUE_FLAG)))
                }
            }
        }
    }

    /**
     * Получает содержимое конкретного элемента контента (например, текст теории).
     * Сначала проверяет локальную БД, затем запрашивает с сервера.
     */
    suspend fun getContentDetails(contentId: String): ContentEntity? {
        return withContext(Dispatchers.IO) {
            var content = contentDao.getContentByIdSync(contentId)
            // TODO: Добавить логику запроса с сервера, если content == null или нужно обновить
            // if (content == null || needsUpdate(content)) { 
            //    content = fetchAndSaveContentDetails(contentId)
            // }
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
    
    // TODO: Метод fetchAndSaveContentDetails(contentId) для загрузки 
    //       и сохранения полного содержимого элемента (теории, задания и т.д.)

    // ----- Новый метод для проверки ответа -----
    /**
     * Отправляет ответ пользователя на сервер для проверки.
     * @param taskId ID задания
     * @param userAnswer Ответ пользователя
     * @return Результат проверки ответа
     * @throws Exception если произошла ошибка сети или API
     */
    /*
    suspend fun checkAnswer(taskId: Int, userAnswer: String): AnswerCheckResult {
        Timber.d("Отправка ответа '$userAnswer' для задания $taskId на сервер")
        // Создаем объект запроса с правильным полем
        val request = AnswerRequest(userAnswer = userAnswer)
        // Выполняем API вызов с правильными параметрами
        val response: AnswerCheckResult = taskApiService.checkAnswer(taskId, request)

        // Теперь response - это уже объект AnswerCheckResult
        Timber.d("Получен ответ от сервера для задания $taskId: isCorrect=${response.isCorrect}")

        // TODO: Сохранить попытку ответа локально или на сервере через API, если нужно

        // Возвращаем полученный результат
        return response
    }
    */
    // ------------------------------------------
    
    // Переменные для пагинации
    private val currentPageMap = mutableMapOf<String, Int>()
    private val hasMoreItemsMap = mutableMapOf<String, Boolean>()
    private val pageSize = 20 // Размер страницы по умолчанию
    
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
        // Если это числовой egeNumber, выполняем проверку диапазона
        val numericCategory = categoryId.toIntOrNull()
        if (numericCategory != null && (numericCategory <= 0 || numericCategory > 30)) {
            // Номера EGE заданий должны быть от 1 до 30 максимум
            Timber.e("Категория $categoryId выходит за рамки допустимых номеров EGE задания (1-30). Вероятно, передан ID задания вместо номера категории.")
            emit(Result.Failure(Exception("Неверный ID категории: $categoryId. Ожидается номер EGE задания от 1 до 30.")))
            return@flow
        }
        
        // Получаем текущую страницу или устанавливаем 2, если это первая загрузка дополнительных заданий
        val currentPage = currentPageMap[categoryId]?.plus(1) ?: 2
        val egeNumber = categoryId
        
        // Вычисляем skip на основе текущей страницы и размера страницы
        val skip = (currentPage - 1) * pageSize
        
        Timber.d("Загрузка дополнительных заданий для категории $categoryId, страница $currentPage (skip=$skip, limit=$pageSize)")
        
        try {
            // Запрашиваем дополнительные задания с сервера с указанием страницы
            val response = taskApiService.getTasksByEgeNumberPaginated(egeNumber, limit = pageSize, pageNumber = currentPage)
            
            if (response.isSuccessful) {
                val taskDtos = response.body() ?: emptyList()
                
                // Проверяем, есть ли еще страницы
                val hasMoreItems = taskDtos.size >= pageSize
                hasMoreItemsMap[categoryId] = hasMoreItems
                
                if (taskDtos.isNotEmpty()) {
                    // Сохраняем номер текущей страницы
                    currentPageMap[categoryId] = currentPage
                    
                    // Преобразуем DTO в сущности для сохранения в БД
                    val taskEntities = taskDtos.map { it.toEntity() }
                    
                    // Сохраняем в базу данных без обновления описания ContentEntity
                    withContext(Dispatchers.IO) {
                        try {
                            Timber.d("Сохранение ${taskEntities.size} дополнительных заданий в БД для EGE $egeNumber...")
                            
                            // Используем обычный insertAll, как в методе getTasksByCategory
                            taskDao.insertAll(taskEntities)
                            
                            // ВАЖНО: Для загрузки дополнительных заданий НЕ обновляем описание ContentEntity, 
                            // чтобы избежать сброса UI и перерисовки с неверным количеством
                            
                            // Обновляем только статус загрузки, не меняя описание с количеством
                            val contentId = "task_group_$egeNumber"
                            Timber.d("[COUNT_DEBUG] loadMoreTasksByCategory: получен contentId: $contentId. Обновляем только статус загрузки без изменения описания")
                            
                            // Получаем текущий контент перед обновлением для проверки
                            val contentBefore = contentDao.getContentByIdSync(contentId)
                            val currentDescription = contentBefore?.getDescription() ?: ""
                            Timber.d("[COUNT_DEBUG] loadMoreTasksByCategory: текущее description: $currentDescription")
                            
                            // Обновляем только статус загрузки, не трогая description
                            contentDao.updateDownloadStatus(contentId, true)
                            
                            // Если описание было пустым, обновляем его с количеством всех заданий 
                            // (старых + новых, которые мы только что загрузили)
                            if (currentDescription.isEmpty() || (!currentDescription.contains("заданий") && !currentDescription.contains("задание"))) {
                                // Получаем полное количество заданий для этой категории
                                val allTasksCount = withContext(Dispatchers.IO) {
                                    taskDao.getTasksByEgeNumberSync(egeNumber).size + taskEntities.size
                                }
                                Timber.d("[COUNT_DEBUG] loadMoreTasksByCategory: нет существующего количества заданий, устанавливаем новое: $allTasksCount заданий")
                                contentDao.updateDescription(contentId, "$allTasksCount заданий")
                            }
                            
                            // Проверяем после обновления
                            val contentAfter = contentDao.getContentByIdSync(contentId)
                            Timber.d("[COUNT_DEBUG] loadMoreTasksByCategory: description после обновления: ${contentAfter?.getDescription() ?: "null"}")
                            
                            Timber.d("Успешно сохранено ${taskEntities.size} дополнительных заданий для EGE $egeNumber")
                        } catch (dbException: Exception) {
                            Timber.e(dbException, "!!! Ошибка БД при сохранении дополнительных заданий для EGE $egeNumber")
                        }
                    }
                    
                    // Преобразуем DTO в TaskItem для отображения
                    val additionalTaskItems = taskDtos.map { taskDto ->
                        TaskItem(
                            taskId = taskDto.id.toString(),
                            title = "Задание ${taskDto.egeNumber}",
                            egeTaskNumber = taskDto.egeNumber,
                            description = taskDto.taskText ?: "",
                            content = taskDto.taskText ?: "",
                            answerType = AnswerType.TEXT, 
                            maxPoints = 1, 
                            timeLimit = 0, 
                            solutions = null,
                            correctAnswer = taskDto.solution,
                            explanation = taskDto.explanation
                        )
                    }
                    
                    // Отправляем результат
                    emit(Result.Success(additionalTaskItems))
                    Timber.d("Успешно загружено и смаплено ${additionalTaskItems.size} дополнительных заданий с сервера, страница $currentPage")
                } else {
                    // Пустой список заданий - больше страниц нет
                    hasMoreItemsMap[categoryId] = false
                    emit(Result.Success(emptyList()))
                    Timber.d("Сервер вернул пустой список дополнительных заданий, страница $currentPage. Больше заданий нет.")
                }
            } else {
                // Ошибка запроса к серверу
                Timber.w("Не удалось загрузить дополнительные задания с сервера (Код: ${response.code()}).")
                emit(Result.Failure(Exception("Ошибка сервера: ${response.code()}")))
            }
        } catch (e: Exception) { 
            Timber.e(e, "Ошибка сети при получении дополнительных заданий с сервера.")
            emit(Result.Failure(e))
        }
    }

    /**
     * Нормализация количества заданий - проверяет актуальное количество в БД и обновляет описание,
     * но только если текущее описание не содержит информацию о количестве.
     * @param categoryId номер категории EGE (от 1 до 30)
     */
    suspend fun normalizeTaskCount(categoryId: String) {
        val egeNumber = categoryId
        val contentId = "task_group_$egeNumber"
        
        Timber.d("[COUNT_DEBUG] Начало нормализации количества заданий для категории $categoryId")
        
        withContext(Dispatchers.IO) {
            try {
                // Проверяем текущее состояние ContentEntity
                val contentEntity = contentDao.getContentByIdSync(contentId)
                
                if (contentEntity == null) {
                    Timber.d("[COUNT_DEBUG] ContentEntity не найден для категории $categoryId, нечего нормализовать")
                    return@withContext
                }
                
                val currentDescription = contentEntity.getDescription() ?: ""
                Timber.d("[COUNT_DEBUG] Текущее описание: $currentDescription")
                
                // Проверяем, содержит ли описание информацию о количестве
                val hasTaskCount = currentDescription.contains("заданий") || currentDescription.contains("задание")
                
                if (!hasTaskCount) {
                    // Получаем актуальное количество заданий из БД
                    val taskEntities = taskDao.getTasksByEgeNumberSync(egeNumber)
                    val taskCount = taskEntities.size
                    
                    if (taskCount > 0) {
                        // Обновляем только если есть задания
                        Timber.d("[COUNT_DEBUG] Обновляем описание для $contentId на: $taskCount заданий")
                        contentDao.updateDescription(contentId, "$taskCount заданий")
                    } else {
                        Timber.d("[COUNT_DEBUG] В БД нет заданий для категории $categoryId, оставляем описание без изменений")
                    }
                } else {
                    Timber.d("[COUNT_DEBUG] Описание уже содержит информацию о количестве заданий: $currentDescription, пропускаем")
                }
            } catch (e: Exception) {
                Timber.e(e, "[COUNT_DEBUG] Ошибка при нормализации количества заданий для категории $categoryId")
            }
        }
    }
    
    /**
     * Нормализация количества заданий для всех категорий - проходит по всем категориям EGE и
     * обновляет описание с количеством, если оно не содержит информацию о задачах.
     */
    suspend fun normalizeAllTaskCounts() {
        Timber.d("[COUNT_DEBUG] Запуск нормализации количества заданий для всех категорий")
        
        // Обрабатываем все категории EGE от 1 до 30
        for (categoryId in 1..30) {
            normalizeTaskCount(categoryId.toString())
        }
        
        Timber.d("[COUNT_DEBUG] Нормализация количества заданий для всех категорий завершена")
    }

    suspend fun getCachedTaskSolution(taskId: String): Solution? {
        // return taskDao.getTaskSolution(taskId) // Закомментировано из-за Unresolved reference: getTaskSolution
        return null // Временная заглушка
    }

    /**
     * Получает текст задания по его ID с сервера.
     */
    fun getTaskTextById(textId: String): Flow<Result<String>> = flow {
        emit(Result.Loading) // Сообщаем о начале загрузки
        try {
            Timber.d("Запрос текста задания с ID: $textId")
            val response = taskApiService.getTaskTextById(textId)
            if (response.isSuccessful) {
                val taskTextDto = response.body()
                if (taskTextDto != null && taskTextDto.content != null) {
                    Timber.d("Текст задания '$textId' успешно получен: ${taskTextDto.content.take(50)}...")
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
}