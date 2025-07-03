package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.repository.TasksRepository
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.ruege.mobile.data.repository.EssayRepository
import com.ruege.mobile.data.local.entity.UserEssayEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.ruege.mobile.data.repository.Result
import com.ruege.mobile.data.network.dto.response.EssayCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import androidx.lifecycle.asLiveData

@HiltViewModel
class EssayViewModel @Inject constructor(
    private val tasksRepository: TasksRepository,
    private val essayRepository: EssayRepository
) : ViewModel() {

    private val _essayTasks = MutableLiveData<Resource<List<TaskItem>>>()
    val essayTasks: LiveData<Resource<List<TaskItem>>> = _essayTasks

    private val _essayCheckState = MutableLiveData<Resource<EssayCheckResult>?>()
    val essayCheckState: LiveData<Resource<EssayCheckResult>?> = _essayCheckState

    private val _additionalTextState = MutableLiveData<Resource<String>?>()
    val additionalTextState: LiveData<Resource<String>?> = _additionalTextState

    private val _currentTaskId = MutableStateFlow<String?>(null)
    val savedEssay: LiveData<UserEssayEntity?> = _currentTaskId.flatMapLatest { taskId ->
        if (taskId == null) {
            flowOf(null)
        } else {
            essayRepository.getSavedEssay(taskId)
        }
    }.asLiveData()

    private var autoSaveJob: Job? = null
    private var pollingJob: Job? = null

    init {
        loadEssayTasks()
    }

    private fun loadEssayTasks() {
        viewModelScope.launch {
            _essayTasks.value = Resource.Loading()
            tasksRepository.getTasksByCategory("27").collect { result ->
                when (result) {
                    is Result.Success -> {
                        val tasks = result.data?.tasks ?: emptyList()
                        val processedTasks = tasks.map {
                            it.copy(title = extractProblem(it.content) ?: it.title)
                        }
                        _essayTasks.value = Resource.Success(processedTasks)
                    }
                    is Result.Error -> {
                        _essayTasks.value = Resource.Error(result.message ?: "Unknown error")
                    }
                    is Result.Loading -> {
                        _essayTasks.value = Resource.Loading()
                    }
                }
            }
        }
    }

    private fun extractProblem(content: String?): String? {
        if (content == null) return null
        val startIndex = content.indexOf("«")
        val endIndex = content.indexOf("»", startIndex)
        return if (startIndex != -1 && endIndex != -1) {
            content.substring(startIndex + 1, endIndex)
        } else {
            null
        }
    }

    fun checkEssay(taskId: String, textId: Int?, essayContent: String, title: String) {
        val taskIdInt = taskId.toIntOrNull()
        if (taskIdInt == null || textId == null) {
            _essayCheckState.value = Resource.Error(message = "Неверный ID задания или текста", data = EssayCheckResult("error", null, essayContent, "Неверный ID задания или текста"))
            return
        }

        viewModelScope.launch {
            _essayCheckState.value = Resource.Loading()
            val result = essayRepository.checkEssay(
                essayContent = essayContent,
                taskId = taskIdInt,
                textId = textId
            )
            when (result) {
                is Result.Success -> {
                    val checkId = result.data!!
                    essayRepository.saveCheckId(taskId, checkId)
                    _essayCheckState.value = Resource.Loading(EssayCheckResult("processing", null, essayContent, "Сочинение принято в работу."))
                    startPolling(checkId, taskId, title, essayContent)
                }
                is Result.Error -> {
                    _essayCheckState.value = Resource.Error(message = result.message ?: "Unknown error", data = EssayCheckResult("error", null, essayContent, result.message ?: "Unknown error"))
                }
                else -> {}
            }
        }
    }

    private fun startPolling(checkId: String, taskId: String, title: String, essayContent: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            Timber.d("Polling started for checkId: $checkId")
            while (true) {
                val result = essayRepository.getEssayCheckResult(checkId)
                if (result is Result.Success) {
                    val checkResult = result.data!!
                    when (checkResult.status) {
                        "completed" -> {
                            _essayCheckState.postValue(Resource.Success(checkResult))
                            essayRepository.saveCompletedEssay(taskId, title, checkResult.essayContent ?: essayContent, checkResult.result!!)
                            pollingJob?.cancel()
                            break
                        }
                        "error" -> {
                            _essayCheckState.postValue(Resource.Error(data = checkResult, message = checkResult.detail?: "Не известная ошибка"))
                            essayRepository.saveCheckId(taskId, "")
                            pollingJob?.cancel()
                            break
                        }
                        "processing" -> {
                            _essayCheckState.postValue(Resource.Loading(checkResult))
                        }
                    }
                } else if (result is Result.Error) {
                    _essayCheckState.postValue(Resource.Error(message = result.message, data = EssayCheckResult("error", null, essayContent, result.message)))
                    pollingJob?.cancel()
                    break
                }
                delay(30000)
            }
        }
    }

    fun pollExistingCheck(checkId: String, taskId: String, title: String, essayContent: String) {
        Timber.d("pollExistingCheck called for checkId: $checkId")
        _essayCheckState.value = Resource.Loading(EssayCheckResult("processing", null, essayContent, "Проверяем статус предыдущей проверки..."))
        startPolling(checkId, taskId, title, essayContent)
    }

    fun loadAdditionalText(textId: Int) {
        viewModelScope.launch {
            _additionalTextState.value = Resource.Loading()
            tasksRepository.getTaskTextById(textId.toString()).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _additionalTextState.value = Resource.Success(result.data ?: "")
                    }
                    is Result.Error -> {
                        _additionalTextState.value = Resource.Error(result.message ?: "Unknown error")
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadSavedEssay(taskId: String) {
        Timber.d("loadSavedEssay called. Setting _currentTaskId to: $taskId")
        _currentTaskId.value = taskId
    }

    fun saveEssayContent(taskId: String, content: String) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            essayRepository.saveEssayContent(taskId, content)
        }
    }

    fun clearEssay(taskId: String) {
        viewModelScope.launch {
            essayRepository.clearEssay(taskId)
            _essayCheckState.value = null
            pollingJob?.cancel()
        }
    }

    fun clearCheckState() {
        _essayCheckState.value = null
        pollingJob?.cancel()
    }

    fun clearAdditionalTextState() {
        _additionalTextState.value = null
    }

    fun clearSavedEssay() {
        Timber.d("clearSavedEssay called. Setting _currentTaskId to null.")
        _currentTaskId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared called in EssayViewModel.")
        pollingJob?.cancel()
        autoSaveJob?.cancel()
    }
} 