    package com.ruege.mobile.ui.viewmodel

    import android.util.Log
    import androidx.lifecycle.LiveData
    import androidx.lifecycle.MutableLiveData
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.asLiveData
    import androidx.lifecycle.viewModelScope
    import com.ruege.mobile.data.local.entity.ContentEntity
    import com.ruege.mobile.data.local.entity.TaskEntity
    import com.ruege.mobile.data.network.dto.response.TheoryContentDto
    import com.ruege.mobile.data.network.dto.response.EssayContentDto
    import com.ruege.mobile.data.repository.ContentRepository
    import com.ruege.mobile.data.repository.Result
    import com.ruege.mobile.model.AnswerCheckResult
    import com.ruege.mobile.model.TaskItem
    import com.ruege.mobile.data.local.dao.TaskDao
    import com.ruege.mobile.data.local.dao.CategoryDao
    import com.ruege.mobile.model.Solution
    import com.ruege.mobile.data.repository.ProgressSyncRepository
    import com.ruege.mobile.data.repository.PracticeStatisticsRepository
    import dagger.hilt.android.lifecycle.HiltViewModel
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.catch
    import kotlinx.coroutines.flow.firstOrNull
    import kotlinx.coroutines.flow.map
    import kotlinx.coroutines.flow.onStart
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import timber.log.Timber
    import javax.inject.Inject
    import kotlinx.coroutines.flow.collect

    /**
     * ViewModel для управления основным контентом (теория, задания и т.д.)
     */
    @HiltViewModel
    class ContentViewModel @Inject constructor(
        private val contentRepository: ContentRepository,
        private val taskDao: TaskDao,
        private val categoryDao: CategoryDao,
        private val progressSyncRepository: ProgressSyncRepository,
        private val practiceStatisticsRepository: PracticeStatisticsRepository
    ) : ViewModel() {

        private val TAG = "ContentViewModel"

        /**
         * LiveData со списком тем теории.
         */
        val theoryTopicsLiveData: LiveData<List<ContentEntity>> = contentRepository
            .getTheoryTopicsStream()
            .asLiveData()

        /**
         * LiveData со списком тем сочинений.
         */
        val essayTopicsLiveData: LiveData<List<ContentEntity>> = contentRepository
            .getEssayTopicsStream()
            .asLiveData()

        /**
         * LiveData со списком заданий.
         */
        @get:JvmName("getTasksTopicsLiveData")
        val tasksTopicsLiveData: LiveData<List<ContentEntity>> = contentRepository
            .getTasksTopicsStream()
            .asLiveData()

        private val _theoryContent = MutableLiveData<TheoryContentDto?>()
        val theoryContent: LiveData<TheoryContentDto?> = _theoryContent

        private val _essayContent = MutableLiveData<EssayContentDto?>()
        val essayContent: LiveData<EssayContentDto?> = _essayContent

        private val _taskContent = MutableLiveData<TaskItem?>()
        @get:JvmName("getTaskContent")
        val taskContent: LiveData<TaskItem?> = _taskContent

        private val _tasks = MutableLiveData<List<TaskItem>?>(emptyList())
        @get:JvmName("getTasks")
        val tasks: MutableLiveData<List<TaskItem>?> = _tasks

        private val _answerCheckResultLiveData = MutableLiveData<AnswerCheckResult?>()
        val answerCheckResultLiveData: LiveData<AnswerCheckResult?> = _answerCheckResultLiveData

        private val _isLoading = MutableLiveData<Boolean>(false)
        @get:JvmName("getIsLoading")
        val isLoading: LiveData<Boolean> = _isLoading

        private val _errorMessage = MutableLiveData<String?>()
        @get:JvmName("getErrorMessage")
        val errorMessage: LiveData<String?> = _errorMessage

        private var spentTime = 0
        
        private var taskTimerRunning = false
        
        private val tasksCategoryCache = mutableMapOf<String, List<TaskItem>>()
        
        private val tasksCacheTimestamps = mutableMapOf<String, Long>()
        
        private val CACHE_LIFETIME_MS = 15 * 60 * 1000L

        private val _isLoadingTasks = mutableMapOf<String, Boolean>()

        private val _isLoadingTaskDetails = mutableMapOf<String, Boolean>()
        
        private val taskDetailCache = mutableMapOf<String, TaskItem>()

        private val _isLoadingMoreTasks = MutableLiveData<Boolean>(false)
        @get:JvmName("getIsLoadingMoreTasks")
        val isLoadingMoreTasks: LiveData<Boolean> = _isLoadingMoreTasks

        private val _hasMoreTasksToLoad = MutableLiveData<Boolean>(true)
        val hasMoreTasksToLoad: LiveData<Boolean> = _hasMoreTasksToLoad

        private val _taskAdditionalText = MutableLiveData<String?>()
        val taskAdditionalText: LiveData<String?> = _taskAdditionalText

        private val _taskAdditionalTextLoading = MutableLiveData<Boolean>()
        val taskAdditionalTextLoading: LiveData<Boolean> = _taskAdditionalTextLoading

        private val _taskAdditionalTextError = MutableLiveData<String?>()
        val taskAdditionalTextError: LiveData<String?> = _taskAdditionalTextError

        private val taskTextCache = mutableMapOf<String, String>()

        private val taskDetailTextIdCache = mutableMapOf<String, String?>()

        init {
            loadInitialContent()
        }

        /**
         * Загружает начальные данные контента (теория и задания) при запуске.
         */
        fun loadInitialContent() {
            viewModelScope.launch {
                Log.d(TAG, "Loading initial content")
                Timber.d("Начинаем загрузку начальных данных")
                try {
                    _isLoading.value = true
                    Timber.d("Установлен статус загрузки: true")
                    
                    Timber.d("Загружаем темы теории...")
                    contentRepository.refreshTheoryTopics()
                    Timber.d("Темы теории загружены успешно")
                    
                    Timber.d("Загружаем темы сочинений...")
                    contentRepository.refreshEssayTopics()
                    Timber.d("Темы сочинений загружены успешно")
                    
                    Timber.d("Загружаем группы заданий...")
                    contentRepository.refreshTasksTopics()
                    Timber.d("Группы заданий загружены успешно")
                    
                    Timber.d("Устанавливаем статус загрузки: false")
                    _isLoading.value = false
                    
                    delay(3000) 
                    normalizeAllTaskCounts()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading initial content: ${e.message}", e)
                    Timber.e(e, "Ошибка загрузки начальных данных: %s", e.message)
                    _errorMessage.value = "Ошибка загрузки контента: ${e.message}"
                    Timber.d("Устанавливаем статус загрузки: false из-за ошибки")
                    _isLoading.value = false
                }
            }
        }
        
        /**
         * Загружает только темы теории.
         */
        fun loadTheoryTopicsOnly() {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Loading only theory topics")
                    _isLoading.value = true
                    
                    contentRepository.refreshTheoryTopics()
                    
                    _isLoading.value = false
                    Log.d(TAG, "Theory topics loaded successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading theory topics: ${e.message}", e)
                    _errorMessage.value = "Ошибка загрузки тем теории: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
        
        /**
         * Загружает только группы заданий.
         */
        fun loadTasksTopicsOnly() {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Loading only tasks topics")
                    _isLoading.value = true
                    
                    contentRepository.refreshTasksTopics()
                    
                    _isLoading.value = false
                    Log.d(TAG, "Tasks topics loaded successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading tasks topics: ${e.message}", e)
                    _errorMessage.value = "Ошибка загрузки групп заданий: ${e.message}"
                    _isLoading.value = false
                }
            }
        }

        /**
         * Загружает содержимое теории по ID.
         * @param contentId ID теории
         */
        fun loadTheoryContent(contentId: String) {
            viewModelScope.launch {
                _isLoading.value = true
                _theoryContent.value = null 
                _errorMessage.value = null  
                Timber.d("Загрузка HTML контента для теории: $contentId")

                try {
                    val theoryDto = contentRepository.getTheoryContentById(contentId)
                    _isLoading.value = false

                    if (theoryDto != null) {
                        if (theoryDto.content.isNotEmpty()) {
                            _theoryContent.value = theoryDto
                            Timber.d("HTML контент для $contentId (теория) успешно загружен.")
                        } else {
                            _theoryContent.value = null
                            _errorMessage.value = "Содержимое теории отсутствует."
                            Timber.w("HTML контент для $contentId (теория) пуст.")
                        }
                    } else {
                        _errorMessage.value = "Не удалось загрузить теорию (ответ null)."
                        Timber.w("Не удалось загрузить теорию для $contentId, DTO is null.")
                        _theoryContent.value = null
                    }
                } catch (e: Exception) {
                    _isLoading.value = false
                    Timber.e(e, "Ошибка загрузки HTML контента для $contentId (теория)")
                    _errorMessage.value = "Ошибка загрузки теории: ${e.message ?: "Неизвестная ошибка"}"
                    _theoryContent.value = null
                }
            }
        }

        /**
         * Загружает содержимое сочинения по ID.
         * @param contentId ID сочинения
         */
        fun loadEssayContent(contentId: String) {
            viewModelScope.launch {
                _isLoading.value = true
                _essayContent.value = null 
                _errorMessage.value = null
                Timber.d("Загрузка HTML контента для сочинения: $contentId")

                try {
                    val essayDto = contentRepository.getEssayContentById(contentId)
                    _isLoading.value = false

                    if (essayDto != null) {
                        if (essayDto.content.isNotEmpty()) {
                            _essayContent.value = essayDto
                            Timber.d("HTML контент для $contentId (сочинение) успешно загружен.")
                        } else {
                            _essayContent.value = null
                            _errorMessage.value = "Содержимое сочинения отсутствует."
                            Timber.w("HTML контент для $contentId (сочинение) пуст.")
                        }
                    } else {
                        _errorMessage.value = "Не удалось загрузить сочинение (ответ null)."
                        Timber.w("Не удалось загрузить сочинение для $contentId, DTO is null.")
                        _essayContent.value = null
                    }
                } catch (e: Exception) {
                    _isLoading.value = false
                    Timber.e(e, "Ошибка загрузки HTML контента для $contentId (сочинение)")
                    _errorMessage.value = "Ошибка загрузки сочинения: ${e.message ?: "Неизвестная ошибка"}"
                    _essayContent.value = null
                }
            }
        }

        /**
         * Загружает задания по указанной категории.
         * @param categoryId - ID категории задания, например номер ЕГЭ задания.
         */
        fun loadTasksByCategory(categoryId: String) {
            if (_isLoadingTasks[categoryId] == true) {
                Log.d(TAG, "Загрузка заданий для категории $categoryId уже в процессе. Пропускаем запрос.")
                return
            }
            
            val cachedTasks = tasksCategoryCache[categoryId]
            val cachedTimestamp = tasksCacheTimestamps[categoryId] ?: 0L
            val currentTime = System.currentTimeMillis()
            
            if (cachedTasks != null && !cachedTasks.isEmpty() && 
                (currentTime - cachedTimestamp < CACHE_LIFETIME_MS)) {
                Log.d(TAG, "Используем кэшированные задания для категории $categoryId. Найдено ${cachedTasks.size} заданий.")
                viewModelScope.launch {
                    val enrichedCachedTasks = enrichTasksWithSolvedStatus(cachedTasks, categoryId)
                    _tasks.value = enrichedCachedTasks
                }
                return
            }
            
            _isLoadingTasks[categoryId] = true
            
            contentRepository.resetTaskPagination(categoryId)
            _hasMoreTasksToLoad.value = true
            
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Загружаем задания для категории $categoryId")
                    _isLoading.value = true
                    _errorMessage.value = null
                    
                    contentRepository.getTasksByCategory(categoryId).collect { result ->
                        _isLoading.value = false
                        _isLoadingTasks[categoryId] = false
                        
                        when (result) {
                            is Result.Success -> {
                                var tasks = result.data
                                Log.d(TAG, "Получено ${tasks.size} заданий для категории $categoryId")

                                tasks = enrichTasksWithSolvedStatus(tasks, categoryId)
                                
                                tasksCategoryCache[categoryId] = tasks
                                tasksCacheTimestamps[categoryId] = System.currentTimeMillis()
                                
                                _tasks.value = tasks
                                
                                _hasMoreTasksToLoad.value = contentRepository.hasMoreTasksToLoad(categoryId)
                            }
                            is Result.Failure -> {
                                val error = result.exception
                                Log.e(TAG, "Ошибка при загрузке заданий для категории $categoryId", error)
                                _errorMessage.value = "Ошибка загрузки заданий: ${error.message}"
                            }
                            is Result.Loading -> {
                                 _isLoading.value = true 
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Исключение при загрузке заданий для категории $categoryId", e)
                    _isLoading.value = false
                    _isLoadingTasks[categoryId] = false
                    _errorMessage.value = "Исключение при загрузке заданий: ${e.message}"
                }
            }
        }

        private suspend fun enrichTasksWithSolvedStatus(tasks: List<TaskItem>, categoryId: String): List<TaskItem> {
            val solvedTaskIds: List<String> = try {
                progressSyncRepository.getSolvedTaskIdsForEgeCategory(categoryId).firstOrNull() ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении решенных TaskID для категории $categoryId из ProgressSyncRepository: ${e.message}", e)
                emptyList() 
            }

            Log.d(TAG, "enrichTasksWithSolvedStatus: для категории $categoryId получено ${solvedTaskIds.size} решенных ID: $solvedTaskIds")

            if (solvedTaskIds.isEmpty()) {
                return tasks.map { it.copy(isSolved = false) }
            }

            return tasks.map { task ->
                if (solvedTaskIds.contains(task.taskId)) {
                    task.copy(isSolved = true)
                } else {
                    task.copy(isSolved = false)
                }
            }
        }

        /**
         * Загружает детальную информацию о задании.
         */
        fun loadTaskDetail(id: String) {
            Timber.d("Загрузка задания с ID: $id")
            
            val formattedId = if (id.startsWith("task_group_")) {
                id.replace("task_group_", "") 
            } else {
                id
            }
            
            if (_isLoadingTaskDetails[formattedId] == true) {
                Timber.d("Загрузка задания с ID: $formattedId уже выполняется, пропускаем повторный запрос")
                return
            }
            
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    _isLoadingTaskDetails[formattedId] = true
                    _errorMessage.value = null 
                    _taskContent.value = null 
                    startTaskTimer()
                    
                    Timber.d("Запрос задания по ID: $formattedId")
                    
                    var taskToLoadTextFor: TaskItem? = null

                    val taskFromCurrentList = _tasks.value?.find { it.taskId == formattedId }
                    if (taskFromCurrentList != null) {
                        Timber.d("Задание с ID: $formattedId найдено в текущем загруженном списке")
                        _taskContent.value = taskFromCurrentList
                        taskToLoadTextFor = taskFromCurrentList
                    }
                    
                    if (taskToLoadTextFor == null) {
                        for ((_, cachedTasks) in tasksCategoryCache) {
                            val taskFromCache = cachedTasks.find { it.taskId == formattedId }
                            if (taskFromCache != null) {
                                Timber.d("Задание с ID: $formattedId найдено в кеше категорий")
                                _taskContent.value = taskFromCache
                                taskToLoadTextFor = taskFromCache
                                break 
                            }
                        }
                    }
                    
                    if (taskToLoadTextFor == null) {
                        val cachedTaskDetail = taskDetailCache[formattedId]
                        if (cachedTaskDetail != null) {
                            Timber.d("Задание с ID: $formattedId найдено в кеше деталей")
                            _taskContent.value = cachedTaskDetail
                            taskToLoadTextFor = cachedTaskDetail
                        }
                    }

                    if (taskToLoadTextFor != null) {
                        _isLoading.value = false
                        _isLoadingTaskDetails[formattedId] = false
                        val task = taskToLoadTextFor 
                        val currentTextId = task.textId?.toString()
                        if (!currentTextId.isNullOrEmpty()) {
                            Timber.d("Загрузка дополнительного текста для задания ${task.taskId} с textId $currentTextId (из кеша/списка)")
                            loadTaskTextByTextId(currentTextId)
                        } else {
                            Timber.d("textId отсутствует для задания ${task.taskId} (из кеша/списка), дополнительный текст не запрашиваем.")
                            _taskAdditionalText.value = null
                            _taskAdditionalTextLoading.value = false
                            _taskAdditionalTextError.value = null
                        }
                        return@launch
                    }
                    
                    contentRepository.getTaskDetail(formattedId).collect { result ->
                        
                        when (result) {
                            is Result.Success -> {
                                val task = result.data
                                Timber.d("Задание загружено из репозитория: $task")
                                _taskContent.value = task
                                taskDetailCache[formattedId] = task

                                val currentTextId = task.textId?.toString() 
                                if (!currentTextId.isNullOrEmpty()) {
                                    Timber.d("Загрузка дополнительного текста для задания ${task.taskId} с textId $currentTextId (из репозитория)")
                                    loadTaskTextByTextId(currentTextId) 
                                } else {
                                    Timber.d("textId отсутствует для задания ${task.taskId} (из репозитория), дополнительный текст не запрашиваем.")
                                    _taskAdditionalText.value = null
                                    _taskAdditionalTextLoading.value = false
                                    _taskAdditionalTextError.value = null 
                                }
                                _isLoading.value = false 
                                _isLoadingTaskDetails[formattedId] = false 
                            }
                            is Result.Failure -> {
                                val e = result.exception 
                                Timber.e(e, "Ошибка при загрузке задания с ID: $formattedId")
                                if (e?.message == ContentRepository.NO_DATA_AND_NETWORK_ISSUE_FLAG) {
                                    _errorMessage.value = "Не удалось загрузить задание. Проверьте подключение к интернету или попробуйте позже. Если проблема останется, отпишитесь в поддержку :)"
                                } else {
                                    _errorMessage.value = e?.message ?: "Произошла ошибка при загрузке задания"
                                }
                                _isLoading.value = false 
                                _isLoadingTaskDetails[formattedId] = false 
                            }
                            is Result.Loading -> {
                                 _isLoading.value = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Исключение при загрузке задания с ID: $formattedId: ${e.javaClass.simpleName}")
                    _isLoading.value = false
                    _isLoadingTaskDetails[formattedId] = false
                    _errorMessage.value = e.message ?: "Произошла неизвестная ошибка при подготовке к загрузке задания"
                }
            }
        }

        /**
         * Отправляет ответ пользователя на проверку.
         * @param taskId ID задания
         * @param userAnswer Ответ пользователя
         */
        fun checkAnswer(taskId: String, userAnswer: String) {
            viewModelScope.launch {
                Timber.d("Проверка ответа для задания $taskId: '$userAnswer'")
                _isLoading.value = true
                _errorMessage.value = null
                _answerCheckResultLiveData.value = null 

                var taskItem: TaskItem? = _tasks.value?.find { it.taskId == taskId }
                if (taskItem == null) {
                    taskItem = taskDetailCache[taskId]
                }

                if (taskItem == null) {
                    contentRepository.getTaskDetail(taskId).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val fetchedTask = result.data
                                taskDetailCache[taskId] = fetchedTask 
                                processAnswer(fetchedTask, userAnswer)
                            }
                            is Result.Failure -> {
                                Timber.e(result.exception, "Ошибка при загрузке деталей задания $taskId для проверки ответа.")
                                _errorMessage.value = "Ошибка получения данных для проверки ответа."
                    _isLoading.value = false
                            }
                            Result.Loading -> {  }
                        }
                    }
                } else {
                    processAnswer(taskItem, userAnswer)
                }
            }
        }

        private fun processAnswer(task: TaskItem, userAnswer: String) {
            val correctAnswerString = task.correctAnswer ?: ""
            val explanationString = task.explanation ?: "Объяснение отсутствует."

                if (correctAnswerString.isEmpty()) {
                Timber.e("Невозможно проверить ответ: правильный ответ не определен для задания ${task.taskId}")
                    _errorMessage.value = "Ошибка: Правильный ответ для этого задания не найден."
                    _isLoading.value = false
                return
                }

                val isCorrect = userAnswer.equals(correctAnswerString, ignoreCase = true) 
            val points = if (isCorrect) task.maxPoints else 0

                val result = AnswerCheckResult(
                taskId = task.taskId,
                    isCorrect = isCorrect,
                    explanation = explanationString,
                    correctAnswer = correctAnswerString, 
                    userAnswer = userAnswer,
                pointsAwarded = points
                )
                
            Timber.d("Результат проверки для задания ${task.taskId}: ${result.isCorrect}, баллы: ${result.pointsAwarded}")
            _answerCheckResultLiveData.postValue(result)
            _isLoading.value = false

            viewModelScope.launch {
                practiceStatisticsRepository.recordAttempt(task, isCorrect, System.currentTimeMillis())
            }
        }

        fun clearAnswerResult() {
            _answerCheckResultLiveData.value = null 
        }

        /**
         * Запускает таймер для отслеживания времени выполнения задания.
         */
        private fun startTaskTimer() {
            spentTime = 0
            taskTimerRunning = true
            viewModelScope.launch {
                while (taskTimerRunning) {
                    kotlinx.coroutines.delay(1000)
                    spentTime++
                }
            }
        }

        /**
         * Останавливает таймер.
         */
        private fun stopTaskTimer() {
            taskTimerRunning = false
        }

        /**
         * Очищает только текущее активное содержимое без затрагивания кеша и списка заданий.
         * Вызывается при скрытии панели с деталями задания/теории.
         */
        fun clearContent() {
            _theoryContent.value = null
            _essayContent.value = null 
            
            _taskContent.value = null
            
            _errorMessage.value = null
            
            if (taskTimerRunning) {
                stopTaskTimer()
            }
            
            
            Timber.d("Очищено только активное содержимое без затрагивания кеша и списка заданий")
        }
        
        /**
         * Очищает кэш заданий для указанной категории или всех категорий.
         */
        fun clearTaskCache(categoryId: String? = null) {
            if (categoryId != null) {
                tasksCategoryCache.remove(categoryId)
                tasksCacheTimestamps.remove(categoryId) 
                Timber.d("Кэш заданий для категории $categoryId очищен")
            } else {
                tasksCategoryCache.clear()
                tasksCacheTimestamps.clear()
                Timber.d("Кэш всех заданий очищен")
            }
        }

        /**
         * Временный метод для отладки проблемы с отсутствием правильных ответов
         * Выводит информацию о том, какое значение имеет поле correctAnswer 
         * в задании с указанным ID
         */
        fun debugTaskSolution(taskId: String) {
            viewModelScope.launch {
                try {
                    Timber.d("DEBUG: Запускаю отладку для задания с ID=$taskId")
                    
                    val currentTask = _taskContent.value
                    if (currentTask != null) {
                        Timber.d("DEBUG Current _taskContent: ID=${currentTask.taskId}, correctAnswer=${currentTask.correctAnswer}, explanation=${currentTask.explanation}")
                    } else {
                        Timber.d("DEBUG: Текущий _taskContent равен null")
                    }
                    
                    contentRepository.getTaskDetail(taskId).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val taskItem = result.data
                                Timber.d("DEBUG getTaskDetail: ID=${taskItem.taskId}, correctAnswer=${taskItem.correctAnswer}, explanation=${taskItem.explanation}")
                            }
                            is Result.Failure -> {
                                val error = result.exception
                                Timber.e(error, "DEBUG: Ошибка при получении задания из репозитория для ID=$taskId")
                            }
                            is Result.Loading -> {
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "DEBUG: Ошибка при отладке задания с ID=$taskId")
                }
            }
        }

        /**
         * Очищает кэш деталей заданий.
         * Вызывается при скрытии панели с деталями задания или перед загрузкой новых заданий.
         */
        fun clearTaskDetailCache() {
            taskDetailCache.clear()
            Timber.d("Кэш деталей заданий очищен")
        }
        
        /**
         * Загружает следующую страницу заданий для указанной категории.
         * Этот метод должен вызываться, когда пользователь прокручивает список до конца.
         */
        fun loadMoreTasksByCategory(categoryId: String) {
            if (_isLoadingMoreTasks.value == true || _isLoadingTasks[categoryId] == true) {
                Timber.d("Пропускаем загрузку дополнительных заданий - уже идет загрузка")
                return
            }
            
            if (!contentRepository.hasMoreTasksToLoad(categoryId)) {
                Timber.d("Пропускаем загрузку дополнительных заданий - нет дополнительных страниц")
                _hasMoreTasksToLoad.value = false
                return
            }
            
            viewModelScope.launch {
                try {
                    Timber.d("Загрузка следующей страницы заданий для категории $categoryId")
                    _isLoadingMoreTasks.value = true
                    
                    contentRepository.loadMoreTasksByCategory(categoryId).collect { result ->
                        _isLoadingMoreTasks.value = false
                        
                        when (result) {
                            is Result.Success -> {
                                var additionalTasks = result.data
                                Timber.d("Получено ${additionalTasks.size} дополнительных заданий для категории $categoryId")
                                
                                if (additionalTasks.isNotEmpty()) {
                                    additionalTasks = enrichTasksWithSolvedStatus(additionalTasks, categoryId)

                                    val currentTasks = _tasks.value ?: emptyList()
                                    val updatedTasks = currentTasks + additionalTasks
                                    
                                    tasksCategoryCache[categoryId] = updatedTasks
                                    tasksCacheTimestamps[categoryId] = System.currentTimeMillis()
                                    
                                    _tasks.value = updatedTasks
                                    
                                    Timber.d("Список заданий обновлен, теперь содержит ${updatedTasks.size} заданий")
                                } else {
                                    Timber.d("Получен пустой список дополнительных заданий")
                                    _hasMoreTasksToLoad.value = false
                                }
                            }
                            is Result.Failure -> {
                                val throwable = result.exception
                                Timber.e(throwable, "Ошибка при загрузке дополнительных заданий для категории $categoryId")
                                _errorMessage.value = "Не удалось загрузить дополнительные задания: ${throwable?.message ?: "неизвестная ошибка"}"
                            }
                            is Result.Loading -> {
                                 _isLoadingMoreTasks.value = true 
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Исключение при загрузке дополнительных заданий для категории $categoryId")
                    _isLoadingMoreTasks.value = false
                    _errorMessage.value = "Произошла ошибка при загрузке дополнительных заданий: ${e.message ?: "неизвестная ошибка"}"
                }
            }
        }
        
        /**
         * Проверяет, есть ли еще задания для загрузки для указанной категории
         */
        fun hasMoreTasksToLoad(categoryId: String): Boolean {
            val currentTasks = _tasks.value
            
            if (currentTasks == null || currentTasks.isEmpty()) {
                Timber.d("hasMoreTasksToLoad: Нет текущих заданий для категории $categoryId, возвращаем true")
                return true
            }
            
            if (currentTasks.size >= 50) {
                Timber.d("hasMoreTasksToLoad: Уже загружено ${currentTasks.size} заданий для категории $categoryId, возможно больше нет")
            }
            
            val hasMore = contentRepository.hasMoreTasksToLoad(categoryId)
            Timber.d("hasMoreTasksToLoad: Категория $categoryId, результат: $hasMore")
            return hasMore
        }
        
        /**
         * Проверяет, загружаются ли в данный момент дополнительные задания
         */
        fun isLoadingMoreTasks(categoryId: String): Boolean {
            val isLoading = _isLoadingMoreTasks.value == true
            Timber.d("isLoadingMoreTasks: Категория $categoryId, загрузка: $isLoading")
            return isLoading
        }
        
        /**
         * Сбрасывает информацию о пагинации для указанной категории.
         * Используется как аварийный вариант при проблемах с пагинацией.
         */
        fun resetTaskPagination(categoryId: String? = null) {
            contentRepository.resetTaskPagination(categoryId)
            _hasMoreTasksToLoad.value = true
            Timber.d("Сброшена информация о пагинации ${if (categoryId != null) "для категории $categoryId" else "для всех категорий"}")
        }

        /**
         * Принудительная синхронизация с сервером для перезагрузки всех данных.
         * Используется при проблемах с данными или когда нужно полностью обновить кеш.
         */
        fun forceSyncWithServer() {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Starting forced sync with server...")
                    _isLoading.value = true
                    _errorMessage.value = null
                    
                    tasksCategoryCache.clear()
                    tasksCacheTimestamps.clear()
                    
                    contentRepository.resetTaskPagination() 
                    
                    contentRepository.refreshTasksTopics()
                    
                    Log.d(TAG, "Forced sync completed successfully")
                    _isLoading.value = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error during forced sync: ${e.message}", e)
                    _errorMessage.value = "Ошибка синхронизации: ${e.message}"
                    _isLoading.value = false
                }
            }
        }

        /**
         * Нормализует количество заданий во всех категориях.
         * Это полезно при первом запуске или если произошла ошибка в отображении количества заданий.
         */
        fun normalizeAllTaskCounts() {
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    contentRepository.normalizeAllTaskCounts()
                } catch (e: Exception) {
                    _errorMessage.value = "Ошибка при нормализации количества заданий: ${e.message}"
                    Timber.e(e, "Ошибка при нормализации количества заданий")
                } finally {
                    _isLoading.value = false
                }
            }
        }
        
        /**
         * Нормализует количество заданий для указанной категории.
         * @param categoryId ID категории заданий EGE (от 1 до 30)
         */
        fun normalizeTaskCount(categoryId: Int) {
            viewModelScope.launch {
                try {
                    val categoryIdString: String = categoryId.toString()
                    contentRepository.normalizeTaskCount(categoryIdString)
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при нормализации количества заданий для категории $categoryId")
                }
            }
        }

        /**
         * Загружает или получает из кеша дополнительный текст для задания.
         * Сначала проверяет, известен ли textId. Если нет, загружает детали задания.
         * @param taskId ID основного задания
         */
        fun requestTaskTextForCurrentTask(taskId: String) {
            viewModelScope.launch {
                _taskAdditionalTextLoading.value = true
                _taskAdditionalTextError.value = null
                _taskAdditionalText.value = null 

                Timber.d("Запрос дополнительного текста для задания $taskId")

                var textIdToLoad: String? = taskDetailTextIdCache[taskId]

                if (textIdToLoad != null) {
                    Timber.d("Найден textId ($textIdToLoad) в кэше taskDetailTextIdCache для taskId $taskId.")
                } else {
                    val taskFromList = _tasks.value?.find { it.taskId == taskId }
                    textIdToLoad = taskFromList?.textId?.toString()
                    if (textIdToLoad != null) {
                        Timber.d("Найден textId ($textIdToLoad) в общем списке задач для taskId $taskId.")
                    }
                }

                if (textIdToLoad == null) {
                    Timber.d("textId для $taskId не найден ни в одном кэше, загружаем детали задания...")
                    contentRepository.getTaskDetail(taskId).collect { detailResult: Result<TaskItem> ->
                        when (detailResult) {
                            is Result.Success -> {
                                val taskDetailItem = detailResult.data
                                textIdToLoad = taskDetailItem.textId?.toString()
                                if (textIdToLoad != null) {
                                    taskDetailTextIdCache[taskId] = textIdToLoad!! 
                                    Timber.d("Загружен textId ($textIdToLoad) из деталей задания $taskId. Загружаем текст...")
                                    loadAndCacheTaskText(textIdToLoad!!) 
                                } else {
                                    Timber.w("textId отсутствует в деталях задания $taskId.")
                                    _taskAdditionalTextError.value = "Дополнительный текст для этого задания недоступен (нет textId в деталях)."
                                    _taskAdditionalTextLoading.value = false
                                }
                            }
                            is Result.Failure -> {
                                Timber.e(detailResult.exception, "Ошибка при загрузке деталей задания $taskId для получения textId.")
                                _taskAdditionalTextError.value = "Не удалось получить информацию для дополнительного текста."
                                _taskAdditionalTextLoading.value = false
                            }
                            Result.Loading -> {  }
                        }
                    }
                } else {
                    loadAndCacheTaskText(textIdToLoad!!) 
                }
            }
        }

        /**
         * Загружает или получает из кеша дополнительный текст для задания по известному textId.
         * @param textId ID текста для загрузки. Не должен быть null или пустым.
         */
        private fun loadTaskTextByTextId(textId: String) {
            viewModelScope.launch {
                if (textId.isEmpty()) {
                    Timber.w("Попытка загрузить текст с пустым textId.")
                    _taskAdditionalTextError.value = "Невозможно загрузить текст: ID текста отсутствует."
                    _taskAdditionalTextLoading.value = false
                    _taskAdditionalText.value = null
                    return@launch
                }

                _taskAdditionalTextLoading.value = true
                _taskAdditionalTextError.value = null
                _taskAdditionalText.value = null 

                Timber.d("Запрос дополнительного текста по textId: $textId")
                loadAndCacheTaskText(textId)
            }
        }

        private suspend fun loadAndCacheTaskText(textId: String) {
            if (taskTextCache.containsKey(textId)) {
                _taskAdditionalText.value = taskTextCache[textId]
                _taskAdditionalTextLoading.value = false
                Timber.d("Дополнительный текст для textId $textId загружен из кэша.")
                return
            }

            Timber.d("Загрузка дополнительного текста с сервера для textId $textId")
            contentRepository.getTaskTextById(textId).collect { textResult: Result<String> ->
                when (textResult) {
                    is Result.Success -> {
                        val textContent = textResult.data
                        taskTextCache[textId] = textContent
                        _taskAdditionalText.value = textContent
                        Timber.d("Дополнительный текст для textId $textId успешно загружен и закэширован.")
                    }
                    is Result.Failure -> {
                        Timber.e(textResult.exception, "Ошибка загрузки дополнительного текста для textId $textId.")
                        _taskAdditionalTextError.value = "Ошибка загрузки текста: ${textResult.exception?.message}"
                    }
                    Result.Loading -> {  }
                }
                _taskAdditionalTextLoading.value = false 
            }
        }

        /**
         * Очищает состояние LiveData, связанное с дополнительным текстом задания.
         */
        fun clearAdditionalTextState() {
            _taskAdditionalText.value = null
            _taskAdditionalTextLoading.value = false
            _taskAdditionalTextError.value = null
            Timber.d("Состояние дополнительного текста задания очищено.")
        }

        /**
         * Обновляет прогресс пользователя для решенного задания.
         * @param task Решенное задание
         * @param taskGroupId ID группы заданий (например, номер ЕГЭ)
         */
        fun updateUserProgressForTask(task: TaskItem, taskGroupId: String) {
            viewModelScope.launch {
                try {
                    Timber.d("Обновление прогресса для задания ${task.taskId} в группе $taskGroupId. Задание решено: ${task.isSolved}, корректно: ${task.isCorrect}")
                    val formattedGroupId = if (taskGroupId.startsWith("task_group_")) taskGroupId else "task_group_$taskGroupId"

                    progressSyncRepository.addSolvedTask(
                        taskGroupId = formattedGroupId,
                        solvedTaskId = task.taskId,
                        syncImmediately = task.isCorrect ?: false 
                    )
                    Timber.d("Прогресс для задания ${task.taskId} (группа $formattedGroupId) успешно обновлен.")
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при обновлении прогресса для задания ${task.taskId} в группе $taskGroupId")
                }
            }
        }
    }