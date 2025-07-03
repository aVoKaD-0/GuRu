package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.repository.Result
import com.ruege.mobile.model.AnswerCheckResult
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.data.repository.ProgressSyncRepository
import com.ruege.mobile.data.repository.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.utils.Resource
import com.ruege.mobile.data.repository.PracticeStatisticsRepository
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val tasksRepository: TasksRepository,
    private val progressSyncRepository: ProgressSyncRepository,
    private val practiceStatisticsRepository: PracticeStatisticsRepository
) : ViewModel() {

    private val _taskItemsState = MutableLiveData<Resource<List<ContentItem>>>()
    val taskItemsState: LiveData<Resource<List<ContentItem>>> = _taskItemsState

    private val _isAnyTaskSelected = MutableLiveData(false)
    val isAnyTaskSelected: LiveData<Boolean> = _isAnyTaskSelected

    private val _batchDownloadTasksResult = MutableLiveData<Resource<String>>()
    val batchDownloadTasksResult: LiveData<Resource<String>> = _batchDownloadTasksResult

    private val _deleteTaskGroupStatus = MutableLiveData<Resource<Unit>>()
    val deleteTaskGroupStatus: LiveData<Resource<Unit>> = _deleteTaskGroupStatus

    private val _tasks = MutableLiveData<List<TaskItem>?>()
    val tasks: LiveData<List<TaskItem>?> = _tasks

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoadingMoreTasks = MutableLiveData(false)
    val isLoadingMoreTasks: LiveData<Boolean> = _isLoadingMoreTasks

    private val _taskAdditionalText = MutableLiveData<String?>()
    val taskAdditionalText: LiveData<String?> = _taskAdditionalText

    private val _taskAdditionalTextLoading = MutableLiveData<Boolean>()
    val taskAdditionalTextLoading: LiveData<Boolean> = _taskAdditionalTextLoading

    private val _taskAdditionalTextError = MutableLiveData<String?>()
    val taskAdditionalTextError: LiveData<String?> = _taskAdditionalTextError

    private val _answerCheckResultLiveData = MutableLiveData<AnswerCheckResult?>()
    val answerCheckResultLiveData: LiveData<AnswerCheckResult?> = _answerCheckResultLiveData

    private val tasksCategoryCache = mutableMapOf<String, List<TaskItem>>()

    private val taskDetailCache = mutableMapOf<String, TaskItem>()

    private val hasMoreTasksByCategory = mutableMapOf<String, Boolean>()

    private val _taskContent = MutableLiveData<TaskItem?>()
    val taskContent: LiveData<TaskItem?> = _taskContent

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadTaskTopics()
        refreshTaskTopics()
    }

    private suspend fun enrichTasksWithSolvedStatus(tasks: List<TaskItem>, categoryId: String): List<TaskItem> {
        val solvedTaskIds: List<String> = try {
            progressSyncRepository.getSolvedTaskIdsForEgeCategory(categoryId).firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении решенных TaskID для категории $categoryId из ProgressSyncRepository: ${e.message}")
            emptyList()
        }

        Timber.d("enrichTasksWithSolvedStatus: для категории $categoryId получено ${solvedTaskIds.size} решенных ID: $solvedTaskIds")

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

    private fun loadTaskTopics() {
        viewModelScope.launch {
            _taskItemsState.value = Resource.Loading()
            tasksRepository.getTasksTopicsStream().collect { entities ->
                val items = entities.map { ContentItem(it.contentId, it.title, it.description, it.type, it.parentId, it.isDownloaded, false) }
                _taskItemsState.value = Resource.Success(items)
                _isAnyTaskSelected.value = false
            }
        }
    }

    fun refreshTaskTopics() {
        viewModelScope.launch {
            try {
                tasksRepository.refreshTasksTopics()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка обновления: ${e.message}"
                Timber.e(e, "Ошибка обновления списка заданий")
            }
        }
    }

    fun getTasksCategoryCache(): MutableMap<String, List<TaskItem>> {
        return tasksCategoryCache
    }

    fun selectAllTasks(isSelected: Boolean) {
        val currentItems = _taskItemsState.value?.data ?: return
        val updatedItems = currentItems.map { it.copy(isSelected = isSelected) }
        _taskItemsState.value = Resource.Success(updatedItems)
        _isAnyTaskSelected.value = updatedItems.any { it.isSelected }
    }

    fun selectTask(item: ContentItem, isSelected: Boolean) {
        val currentItems = _taskItemsState.value?.data ?: return
        val updatedItems = currentItems.map {
            if (it.contentId == item.contentId) {
                it.copy(isSelected = isSelected)
            } else {
                it
            }
        }
        _taskItemsState.value = Resource.Success(updatedItems)
        _isAnyTaskSelected.value = updatedItems.any { it.isSelected }
    }

    fun downloadSelectedTasks() {
        viewModelScope.launch {
            val selectedItems = _taskItemsState.value?.data?.filter { it.isSelected && !it.isDownloaded }
            if (selectedItems.isNullOrEmpty()) {
                _batchDownloadTasksResult.value = Resource.Success("Нет новых заданий для скачивания.")
                return@launch
            }

            _batchDownloadTasksResult.value = Resource.Loading()
            var successCount = 0
            var errorCount = 0

            for (item in selectedItems) {
                val egeNumber = item.contentId.removePrefix("task_group_")
                tasksRepository.downloadTaskGroup(egeNumber)
                    .onEach { result ->
                        if (result is Result.Success<*>) successCount++
                        else if (result is Result.Error) errorCount++
                    }.collect()
            }
            val message = "Успешно скачано групп: $successCount. Ошибок: $errorCount."
            _batchDownloadTasksResult.value = if (errorCount > 0) Resource.Error(message) else Resource.Success(message)
            selectAllTasks(false)
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
                tasksRepository.getTaskDetail(taskId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val fetchedTask = result.data
                            if (fetchedTask != null) {
                                taskDetailCache[taskId] = fetchedTask
                                processAnswer(fetchedTask, userAnswer)
                            } else {
                                Timber.e("Ошибка при загрузке деталей задания $taskId для проверки ответа: задание не найдено.")
                                _errorMessage.value = "Ошибка получения данных для проверки ответа."
                                _isLoading.value = false
                            }
                        }
                        is Result.Error -> {
                            Timber.e(result.message, "Ошибка при загрузке деталей задания $taskId для проверки ответа.")
                            _errorMessage.value = "Ошибка получения данных для проверки ответа."
                            _isLoading.value = false
                        }
                        is Result.Loading -> {  }
                    }
                }
            } else {
                processAnswer(taskItem, userAnswer)
            }
        }
    }


    fun requestTaskTextForCurrentTask(taskId: Int) {
        viewModelScope.launch {
            _taskAdditionalTextLoading.value = true
            _taskAdditionalTextError.value = null
            tasksRepository.getTaskTextById(taskId.toString()).collect { result ->
                when(result) {
                    is Result.Success -> _taskAdditionalText.postValue(result.data)
                    is Result.Error -> _taskAdditionalTextError.postValue(result.message)
                    is Result.Loading -> {}
                }
                _taskAdditionalTextLoading.postValue(false)
            }
        }
    }


    fun deleteDownloadedTaskGroup(egeNumber: String) {
        viewModelScope.launch {
            tasksRepository.deleteTaskGroup(egeNumber).collect { result ->
                when(result) {
                    is Result.Loading -> _deleteTaskGroupStatus.postValue(Resource.Loading())
                    is Result.Success -> _deleteTaskGroupStatus.postValue(Resource.Success(Unit))
                    is Result.Error -> _deleteTaskGroupStatus.postValue(Resource.Error(result.message ?: "Unknown error"))
                }
            }
        }
    }

    fun loadTasksByCategory(egeNumber: String) {
        viewModelScope.launch {
            tasksRepository.resetTaskPagination(egeNumber)
            tasksRepository.getTasksByCategory(egeNumber).collect { result ->
                when(result) {
                    is Result.Success -> {
                        val page = result.data ?: return@collect
                        val enrichedTasks = enrichTasksWithSolvedStatus(page.tasks, egeNumber)
                        _tasks.postValue(enrichedTasks)
                        hasMoreTasksByCategory[egeNumber] = page.hasMore
                        _errorMessage.postValue(null)
                    }
                    is Result.Error -> _errorMessage.postValue(result.message)
                    is Result.Loading -> {}
                }
            }
        }
    }

    fun loadMoreTasksByCategory(egeNumber: String) {
        viewModelScope.launch {
            if (_isLoadingMoreTasks.value == true || hasMoreTasksByCategory[egeNumber] == false) return@launch

            _isLoadingMoreTasks.postValue(true)
            tasksRepository.getTasksByCategory(egeNumber).collect { result ->
                when(result) {
                    is Result.Success -> {
                        val page = result.data ?: return@collect
                        if (page.tasks.isNotEmpty()) {
                            val enrichedNewTasks = enrichTasksWithSolvedStatus(page.tasks, egeNumber)
                            val currentTasks = _tasks.value ?: emptyList()
                            _tasks.postValue(currentTasks + enrichedNewTasks)
                            hasMoreTasksByCategory[egeNumber] = page.hasMore
                        } else {
                            hasMoreTasksByCategory[egeNumber] = false
                        }

                    }
                    is Result.Error -> _errorMessage.postValue(result.message)
                    is Result.Loading -> {}
                }
                _isLoadingMoreTasks.postValue(false)
            }

        }
    }

    fun hasMoreTasksToLoad(egeNumber: String): Boolean {
        return hasMoreTasksByCategory.getOrDefault(egeNumber, true)
    }

    private fun processAnswer(task: TaskItem, userAnswer: String) {
        val correctAnswerString = task.correctAnswer?: ""
        val explanationString = task.explanation?: "Объяснение отсутствует."

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

    fun clearAdditionalTextState() {
        _taskAdditionalText.value = null
        _taskAdditionalTextError.value = null
        _taskAdditionalTextLoading.value = false
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

    fun clearAllDownloadedTasks() {
        viewModelScope.launch {
            try {
                tasksRepository.clearAllDownloadedTasks()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear all downloaded tasks")
            }
        }
    }
}