package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.ruege.mobile.data.local.entity.UserVariantTaskAnswerEntity
import com.ruege.mobile.data.local.entity.VariantEntity
import com.ruege.mobile.data.local.entity.VariantSharedTextEntity
import com.ruege.mobile.data.local.entity.VariantTaskEntity
import com.ruege.mobile.data.repository.VariantRepository
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.ruege.mobile.data.repository.EssayRepository
import com.ruege.mobile.data.network.dto.response.EssayCheckResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import com.ruege.mobile.data.repository.Result
import timber.log.Timber

@HiltViewModel
class VariantViewModel @Inject constructor(
    private val variantRepository: VariantRepository,
    private val essayRepository: EssayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<List<VariantEntity>>>(Resource.Loading())
    val variantsState: StateFlow<Resource<List<VariantEntity>>> = _uiState.asStateFlow()

    private val localVariantsState = MutableStateFlow<List<VariantEntity>>(emptyList())

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadEvent = MutableSharedFlow<Resource<String>>()
    val downloadEvent: SharedFlow<Resource<String>> = _downloadEvent.asSharedFlow()

    private val _variantDetailsState = MutableStateFlow<Resource<VariantEntity>>(Resource.Loading())
    val variantDetailsLiveData: LiveData<Resource<VariantEntity>> = _variantDetailsState.asLiveData()

    private val _sharedTextsState = MutableStateFlow<Resource<List<VariantSharedTextEntity>>>(Resource.Success(emptyList()))
    val sharedTextsLiveData: LiveData<Resource<List<VariantSharedTextEntity>>> = _sharedTextsState.asLiveData()

    private val _tasksState = MutableStateFlow<Resource<List<VariantTaskEntity>>>(Resource.Success(emptyList()))
    val tasksLiveData: LiveData<Resource<List<VariantTaskEntity>>> = _tasksState.asLiveData()

    private val _userAnswersForCurrentVariantLiveData = MutableLiveData<Resource<Map<Int, UserVariantTaskAnswerEntity>>>(Resource.Loading())
    val userAnswersForCurrentVariantLiveData: LiveData<Resource<Map<Int, UserVariantTaskAnswerEntity>>> = _userAnswersForCurrentVariantLiveData

    private val _variantCheckedState = MutableStateFlow<Boolean>(false)
    val variantCheckedState: StateFlow<Boolean> = _variantCheckedState.asStateFlow()

    private val _selectedVariants = MutableStateFlow<Set<Int>>(emptySet())

    private val _essayCheckState = MutableStateFlow<Resource<EssayCheckResult>?>(null)
    val essayCheckState: StateFlow<Resource<EssayCheckResult>?> = _essayCheckState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            variantRepository.getVariants()
                .combine(localVariantsState) { resource, localState ->
                    when (resource) {
                        is Resource.Success -> {
                            val dbVariants = resource.data ?: emptyList()
                            val localStateMap = localState.associateBy { it.variantId }
                            val mergedList = dbVariants.map { dbVariant ->
                                dbVariant.copy(isSelected = localStateMap[dbVariant.variantId]?.isSelected ?: false)
                            }
                            localVariantsState.value = mergedList
                            Resource.Success(mergedList)
                        }
                        is Resource.Error -> {
                            if (localVariantsState.value.isEmpty()) resource else Resource.Success(localVariantsState.value)
                        }
                        is Resource.Loading -> {
                            if (localVariantsState.value.isEmpty()) Resource.Loading() else Resource.Success(localVariantsState.value)
                        }
                        else -> {
                            resource
                        }
                    }
                }
                .collect { combinedResource ->
                    _uiState.value = combinedResource
                }
        }
        fetchVariants()
    }

    private fun fetchVariants() {
        viewModelScope.launch {
            try {
                variantRepository.fetchVariantsFromServer()
                if (_uiState.value is Resource.Loading) {
                    _uiState.value = Resource.Success(emptyList())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching variants")
                if (_uiState.value.data.isNullOrEmpty()) {
                    _uiState.value = Resource.Error("Ошибка загрузки вариантов: ${e.message}", null)
                }
            }
        }
    }

    fun toggleVariantSelection(variantId: Int, isSelected: Boolean) {
        localVariantsState.update { currentList ->
            currentList.map { variant ->
                if (variant.variantId == variantId) {
                    variant.copy(isSelected = isSelected)
                } else {
                    variant
                }
            }
        }
    }

    fun selectAllVariants(selectAll: Boolean) {
        localVariantsState.update { currentList ->
            currentList.map { it.copy(isSelected = selectAll) }
        }
    }

    fun downloadSelectedVariants() {
        viewModelScope.launch {
            val selectedVariantIds = localVariantsState.value
                .filter { it.isSelected && !it.isDownloaded }
                .map { it.variantId }
                
            if (selectedVariantIds.isEmpty()) {
                Timber.d("Нет выбранных вариантов для скачивания.")
                return@launch
            }

            _isDownloading.value = true
            var successfulDownloads = 0
            var lastError: String? = null

            Timber.d("Скачивание вариантов: $selectedVariantIds")
            selectedVariantIds.forEach { variantId ->
                val result = variantRepository.fetchAndSaveVariantDetails(variantId)
                if (result is Resource.Success) {
                    variantRepository.updateVariantDownloadedStatus(variantId, true)
                    successfulDownloads++
                } else {
                    lastError = result.message ?: "Неизвестная ошибка"
                }
            }
            
            _isDownloading.value = false
            selectAllVariants(false)

            if (lastError != null) {
                val failedCount = selectedVariantIds.size - successfulDownloads
                _downloadEvent.emit(Resource.Error("Не удалось скачать $failedCount из ${selectedVariantIds.size} вариантов. Ошибка: $lastError"))
            } else {
                _downloadEvent.emit(Resource.Success("Успешно скачано $successfulDownloads вариантов."))
            }
        }
    }

    fun deleteDownloadedVariant(variantId: Int) {
        viewModelScope.launch {
            val result = variantRepository.deleteDownloadedVariant(variantId)
            if (result is Resource.Success) {
                _downloadEvent.emit(Resource.Success("Вариант успешно удален."))
            } else if (result is Resource.Error) {
                _downloadEvent.emit(Resource.Error(result.message ?: "Не удалось удалить вариант."))
            }
        }
    }

    fun consumeVariantDetails() {
        _variantDetailsState.value = Resource.Loading()
        Timber.d("consumeVariantDetails: Variant details state has been reset.")
    }

    fun fetchVariantDetails(variantId: Int) {
        viewModelScope.launch {
            Timber.d("fetchVariantDetails($variantId) - START (Local DB logic)")
            _variantDetailsState.value = Resource.Loading()
            _sharedTextsState.value = Resource.Loading()
            _tasksState.value = Resource.Loading()
            _userAnswersForCurrentVariantLiveData.value = Resource.Loading()
            _variantCheckedState.value = false
            Timber.d("fetchVariantDetails($variantId) - States set to Loading/False")
    
            try {
                Timber.d("Загрузка варианта $variantId и связанных данных из локальной БД.")
    
                variantRepository.getVariantDetails(variantId)
                    .onEach { variant ->
                        if (variant != null) {
                            _variantDetailsState.value = Resource.Success(variant)
                        } else {
                            _variantDetailsState.value = Resource.Error("Вариант с ID $variantId не найден в БД", null)
                        }
                    }
                    .launchIn(viewModelScope)
    
                variantRepository.getSharedTextsForVariant(variantId)
                    .onEach { texts -> _sharedTextsState.value = Resource.Success(texts) }
                    .launchIn(viewModelScope)
    
                variantRepository.getTasksForVariant(variantId)
                    .onEach { tasks -> 
                        _tasksState.value = Resource.Success(tasks) 
                        tasks.find { it.egeNumber == "27" && it.checkId != null }?.let { task ->
                            pollExistingCheckForVariantEssay(task.checkId!!, task.variantTaskId)
                        }
                    }
                    .launchIn(viewModelScope)
    
                val userAnswersMap = variantRepository.getUserAnswersForVariant(variantId)
                _userAnswersForCurrentVariantLiveData.value = Resource.Success(userAnswersMap)
                Timber.d("fetchVariantDetails($variantId) - Загружено ${userAnswersMap.size} ответов пользователя.")
    
            } catch (e: Exception) {
                val errorMsg = "Ошибка при загрузке деталей варианта из БД: ${e.message}"
                Timber.e(e, "fetchVariantDetails($variantId) - CATCH: $errorMsg")
                _variantDetailsState.value = Resource.Error(errorMsg, null)
                _sharedTextsState.value = Resource.Error(errorMsg, null)
                _tasksState.value = Resource.Error(errorMsg, null)
                _userAnswersForCurrentVariantLiveData.value = Resource.Error(errorMsg, null)
            }
            Timber.d("fetchVariantDetails($variantId) - END")
        }
    }

    fun updateVariantLastAccessedTime(variantId: Int) {
        viewModelScope.launch {
            variantRepository.updateVariantLastAccessedTime(variantId)
        }
    }

    fun updateVariantTimer(variantId: Int, timeInMillis: Long) {
        viewModelScope.launch {
            Timber.d("Updating timer for variant $variantId to $timeInMillis ms")
            variantRepository.updateVariantTimer(variantId, timeInMillis)
        }
    }
    
    /**
     * Сохраняет ответ пользователя для указанного задания в БД и сразу проверяет его.
     */
    fun saveUserAnswerAndCheck(variantId: Int, taskId: Int, answer: String) {
        viewModelScope.launch {
            try {
                Timber.d("Сохранение и проверка ответа для variantId=$variantId, taskId=$taskId, ответ: '$answer'")
                val savedAnswerEntity = variantRepository.saveUserAnswer(variantId, taskId, answer)

                if (savedAnswerEntity == null) {
                    Timber.e("Не удалось сохранить или получить ответ для taskId=$taskId после сохранения.")
                    return@launch
                }

                val taskEntity = variantRepository.getTask(taskId)
                if (taskEntity == null) {
                    Timber.e("Не удалось получить данные задания (taskEntity) для taskId=$taskId для проверки.")
                    updateUserAnswersLiveData(variantId, savedAnswerEntity)
                    return@launch
                }

                val userAnswerText = savedAnswerEntity.userSubmittedAnswer?.trim()
                val correctAnswerText = taskEntity.solutionText?.trim()
                var isCorrect = false
                var pointsAwarded = 0

                if (!userAnswerText.isNullOrEmpty() && !correctAnswerText.isNullOrEmpty()) {
                    isCorrect = userAnswerText.equals(correctAnswerText, ignoreCase = true)
                    if (isCorrect) {
                        pointsAwarded = taskEntity.maxPoints ?: 0
                    }
                } else if (userAnswerText.isNullOrEmpty() && correctAnswerText.isNullOrEmpty()) {
                }

                variantRepository.updateUserAnswerResult(taskId, isCorrect, pointsAwarded)
                Timber.d("Результат проверки для taskId=$taskId обновлен: isCorrect=$isCorrect, points=$pointsAwarded")

                val checkedAnswerEntity = savedAnswerEntity.copy(isSubmissionCorrect = isCorrect, pointsAwarded = pointsAwarded)
                updateUserAnswersLiveData(variantId, checkedAnswerEntity)

            } catch (e: Exception) {
                Timber.e(e, "Ошибка сохранения и проверки ответа для taskId=$taskId: ${e.message}")
            }
        }
    }

    /**
     * Вспомогательный метод для обновления _userAnswersForCurrentVariantLiveData
     */
    private fun updateUserAnswersLiveData(variantId: Int, answerEntity: UserVariantTaskAnswerEntity) {
        val currentAnswersResource = _userAnswersForCurrentVariantLiveData.value
        if (currentAnswersResource is Resource.Success) {
            currentAnswersResource.data?.let { currentMap ->
                val updatedMap = currentMap.toMutableMap()
                updatedMap[answerEntity.variantTaskId] = answerEntity
                _userAnswersForCurrentVariantLiveData.value = Resource.Success(updatedMap)
                Timber.d("LiveData ответов обновлена для taskId=${answerEntity.variantTaskId}")
            } ?: run {
                _userAnswersForCurrentVariantLiveData.value = Resource.Success(mapOf(answerEntity.variantTaskId to answerEntity))
                Timber.d("LiveData ответов создана с первым элементом для taskId=${answerEntity.variantTaskId}")
            }
        } else {
            Timber.w("LiveData ответов не в состоянии Success, не удалось обновить для taskId=${answerEntity.variantTaskId}")
        }
    }
    
    fun checkVariantEssay(variantTaskId: Int, textId: Int?, text: String) {
        viewModelScope.launch {
            _essayCheckState.value = Resource.Loading()
            val result = essayRepository.checkEssay(
                essayContent = text,
                textId = textId,
                variantTaskId = variantTaskId
            )

            when(result) {
                is Result.Success -> {
                    val checkId = result.data
                    variantRepository.saveCheckIdForVariantTask(variantTaskId, checkId)
                    _essayCheckState.value = Resource.Loading(EssayCheckResult("processing", null, text, "Сочинение принято в работу."))
                    startPollingForVariantEssay(checkId, variantTaskId)
                }
                is Result.Error -> {
                    _essayCheckState.value = Resource.Error(
                        message = result.message ?: "Unknown error", 
                        data = EssayCheckResult("error", null, text, result.message ?: "Unknown error")
                    )
                }
                else -> {}
            }
        }
    }

    private fun startPollingForVariantEssay(checkId: String, variantTaskId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            Timber.d("Polling started for checkId: $checkId")
            while (true) {
                val result = essayRepository.getEssayCheckResult(checkId)
                if (result is Result.Success) {
                    val checkResult = result.data
                    when (checkResult.status) {
                        "completed" -> {
                            _essayCheckState.value = Resource.Success(checkResult)
                            variantRepository.saveEssayCheckResult(variantTaskId, checkResult.result!!)
                            pollingJob?.cancel()
                            break
                        }
                        "error" -> {
                            _essayCheckState.value = Resource.Error(data = checkResult, message = checkResult.detail?: "Не известная ошибка")
                            variantRepository.saveCheckIdForVariantTask(variantTaskId, "")
                            pollingJob?.cancel()
                            break
                        }
                        "processing" -> {
                            _essayCheckState.value = Resource.Loading(checkResult)
                        }
                    }
                } else if (result is Result.Error) {
                    _essayCheckState.value = Resource.Error(message = result.message, data = EssayCheckResult("error", null, null, result.message))
                    pollingJob?.cancel()
                    break
                }
                delay(30000)
            }
        }
    }

    fun pollExistingCheckForVariantEssay(checkId: String, variantTaskId: Int) {
        Timber.d("pollExistingCheckForVariantEssay called for checkId: $checkId")
        _essayCheckState.value = Resource.Loading(EssayCheckResult("processing", null, null, "Проверяем статус предыдущей проверки..."))
        startPollingForVariantEssay(checkId, variantTaskId)
    }

    fun clearEssayCheckState() {
        _essayCheckState.value = null
        pollingJob?.cancel()
    }

    @Deprecated("Use saveUserAnswerAndCheck for immediate checking", ReplaceWith("saveUserAnswerAndCheck(variantId, taskId, answer)"))
    fun saveUserAnswer_OLD(variantId: Int, taskId: Int, answer: String) {
        viewModelScope.launch {
            try {
                variantRepository.saveUserAnswer(variantId, taskId, answer)
                 Timber.d("Ответ для variantId=$variantId, taskId=$taskId сохранен (OLD METHOD): '$answer'")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка сохранения ответа для taskId=$taskId (OLD METHOD): ${e.message}")
            }
        }
    }

    /**
     * Проверяет ответы пользователя для указанного варианта.
     */
    fun checkVariantAnswers(variantId: Int) {
        viewModelScope.launch {
            Timber.d("checkVariantAnswers для варианта $variantId вызван. Устанавливаем _variantCheckedState = true.")
            _variantCheckedState.value = true
        }
    }

    /**
     * Очищает все ответы пользователя и сбрасывает таймер для указанного варианта из локальной базы данных.
     * Вызывается после завершения варианта и закрытия соответствующего UI.
     */
    fun clearAnswersForCompletedVariant(variantId: Int) {
        viewModelScope.launch {
            try {
                Timber.i("clearAnswersForCompletedVariant CALLED for variantId: $variantId")
                variantRepository.clearUserAnswersForVariant(variantId)
                Timber.i("Successfully CALLED variantRepository.clearUserAnswersForVariant for variantId: $variantId")

                val defaultTimerDuration = (3 * 60 * 60 * 1000) + (55 * 60 * 1000).toLong()
                variantRepository.updateVariantTimer(variantId, defaultTimerDuration)
                Timber.i("Successfully reset timer for variantId: $variantId to default.")

            } catch (e: Exception) {
                Timber.e(e, "ERROR in clearAnswersForCompletedVariant for variantId $variantId: ${e.message}")
            }
        }
    }

    fun clearAllDownloadedVariants() {
        viewModelScope.launch {
            try {
                variantRepository.clearAllDownloadedVariants()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear all variants")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
} 