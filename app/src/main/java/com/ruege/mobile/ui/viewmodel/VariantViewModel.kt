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
import com.ruege.mobile.utilss.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.ruege.mobile.data.network.dto.UserAnswerPayloadDto
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect

@HiltViewModel
class VariantViewModel @Inject constructor(
    private val variantRepository: VariantRepository
) : ViewModel() {

    private val _variantsState = MutableStateFlow<Resource<List<VariantEntity>>>(Resource.Loading())
    val variantsState: StateFlow<Resource<List<VariantEntity>>> = _variantsState.asStateFlow()
    val variantsLiveData: LiveData<Resource<List<VariantEntity>>> = _variantsState.asLiveData()

    private val _variantDetailsState = MutableStateFlow<Resource<VariantEntity>>(Resource.Loading())
    val variantDetailsState: StateFlow<Resource<VariantEntity>> = _variantDetailsState.asStateFlow()
    val variantDetailsLiveData: LiveData<Resource<VariantEntity>> = _variantDetailsState.asLiveData()

    private val _sharedTextsState = MutableStateFlow<Resource<List<VariantSharedTextEntity>>>(Resource.Success(emptyList()))
    val sharedTextsState: StateFlow<Resource<List<VariantSharedTextEntity>>> = _sharedTextsState.asStateFlow()
    val sharedTextsLiveData: LiveData<Resource<List<VariantSharedTextEntity>>> = _sharedTextsState.asLiveData()

    private val _tasksState = MutableStateFlow<Resource<List<VariantTaskEntity>>>(Resource.Success(emptyList()))
    val tasksState: StateFlow<Resource<List<VariantTaskEntity>>> = _tasksState.asStateFlow()
    val tasksLiveData: LiveData<Resource<List<VariantTaskEntity>>> = _tasksState.asLiveData()

    private val _userAnswersForCurrentVariantLiveData = MutableLiveData<Resource<Map<Int, UserVariantTaskAnswerEntity>>>(Resource.Loading())
    val userAnswersForCurrentVariantLiveData: LiveData<Resource<Map<Int, UserVariantTaskAnswerEntity>>> = _userAnswersForCurrentVariantLiveData

    private val _variantCheckedState = MutableStateFlow<Boolean>(false)
    val variantCheckedState: StateFlow<Boolean> = _variantCheckedState.asStateFlow()

    private val _selectedVariants = MutableStateFlow<Set<Int>>(emptySet())
    val selectedVariants: StateFlow<Set<Int>> = _selectedVariants.asStateFlow()

    init {
        fetchVariants()
    }

    fun fetchVariants() {
        viewModelScope.launch {
            variantRepository.getVariants().collect { resource ->
                val currentState = _variantsState.value
                if (currentState is Resource.Loading && resource is Resource.Success && resource.data.isNullOrEmpty()) {
                    return@collect
                }
                _variantsState.value = resource
            }
        }
        viewModelScope.launch {
            try {
                variantRepository.fetchVariantsFromServer()
            } catch (e: Exception) {
                Log.e("ViewModel", "Error fetching variants", e)
                if (_variantsState.value !is Resource.Success || _variantsState.value?.data.isNullOrEmpty()) {
                    _variantsState.value = Resource.Error("Ошибка загрузки вариантов: ${e.message}", null)
                }
            }
        }
    }

    fun toggleVariantSelection(variantId: Int, isSelected: Boolean) {
        _variantsState.update { currentState ->
            if (currentState is Resource.Success) {
                val updatedList = currentState.data?.map { variant ->
                    if (variant.variantId == variantId) {
                        variant.copy(isSelected = isSelected)
                    } else {
                        variant
                    }
                }
                Resource.Success(updatedList ?: emptyList())
            } else {
                currentState
            }
        }
    }

    fun selectAllVariants(selectAll: Boolean) {
        _variantsState.update { currentState ->
            if (currentState is Resource.Success) {
                val updatedList = currentState.data?.map { it.copy(isSelected = selectAll) }
                Resource.Success(updatedList ?: emptyList())
            } else {
                currentState
            }
        }
    }

    fun downloadSelectedVariants() {
        viewModelScope.launch {
            val selectedVariantIds = _variantsState.value.data
                ?.filter { it.isSelected && !it.isDownloaded }
                ?.map { it.variantId } ?: emptyList()

            if (selectedVariantIds.isEmpty()) {
                Log.d("ViewModel", "Нет выбранных вариантов для скачивания.")
                return@launch
            }

            Log.d("ViewModel", "Скачивание вариантов: $selectedVariantIds")
            selectedVariantIds.forEach { variantId ->
                variantRepository.fetchAndSaveVariantDetails(variantId)
                variantRepository.updateVariantDownloadedStatus(variantId, true)
            }
            selectAllVariants(false)
        }
    }

    fun consumeVariantDetails() {
        _variantDetailsState.value = Resource.Loading()
        Log.d("ViewModel", "consumeVariantDetails: Variant details state has been reset.")
    }

    fun fetchVariantDetails(variantId: Int) {
        viewModelScope.launch {
            Log.d("ViewModel", "fetchVariantDetails($variantId) - START (Local DB logic)")
            _variantDetailsState.value = Resource.Loading()
            _sharedTextsState.value = Resource.Loading()
            _tasksState.value = Resource.Loading()
            _userAnswersForCurrentVariantLiveData.value = Resource.Loading()
            _variantCheckedState.value = false
            Log.d("ViewModel", "fetchVariantDetails($variantId) - States set to Loading/False")
    
            try {
                Log.d("ViewModel", "Загрузка варианта $variantId и связанных данных из локальной БД.")
    
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
                    .onEach { tasks -> _tasksState.value = Resource.Success(tasks) }
                    .launchIn(viewModelScope)
    
                val userAnswersMap = variantRepository.getUserAnswersForVariant(variantId)
                _userAnswersForCurrentVariantLiveData.value = Resource.Success(userAnswersMap)
                Log.d("ViewModel", "fetchVariantDetails($variantId) - Загружено ${userAnswersMap.size} ответов пользователя.")
    
            } catch (e: Exception) {
                val errorMsg = "Ошибка при загрузке деталей варианта из БД: ${e.message}"
                Log.e("ViewModel", "fetchVariantDetails($variantId) - CATCH: $errorMsg", e)
                _variantDetailsState.value = Resource.Error(errorMsg, null)
                _sharedTextsState.value = Resource.Error(errorMsg, null)
                _tasksState.value = Resource.Error(errorMsg, null)
                _userAnswersForCurrentVariantLiveData.value = Resource.Error(errorMsg, null)
            }
            Log.d("ViewModel", "fetchVariantDetails($variantId) - END")
        }
    }

    fun updateVariantLastAccessedTime(variantId: Int) {
        viewModelScope.launch {
            variantRepository.updateVariantLastAccessedTime(variantId)
        }
    }

    fun updateVariantTimer(variantId: Int, timeInMillis: Long) {
        viewModelScope.launch {
            Log.d("ViewModel", "Updating timer for variant $variantId to $timeInMillis ms")
            variantRepository.updateVariantTimer(variantId, timeInMillis)
        }
    }

    fun setVariantDownloadedStatus(variantId: Int, isDownloaded: Boolean) {
        viewModelScope.launch {
            variantRepository.updateVariantDownloadedStatus(variantId, isDownloaded)
        }
    }
    
    /**
     * Сохраняет ответ пользователя для указанного задания в БД и сразу проверяет его.
     */
    fun saveUserAnswerAndCheck(variantId: Int, taskId: Int, answer: String) {
        viewModelScope.launch {
            try {
                Log.d("ViewModel", "Сохранение и проверка ответа для variantId=$variantId, taskId=$taskId, ответ: '$answer'")
                val savedAnswerEntity = variantRepository.saveUserAnswer(variantId, taskId, answer)

                if (savedAnswerEntity == null) {
                    Log.e("ViewModel", "Не удалось сохранить или получить ответ для taskId=$taskId после сохранения.")
                    return@launch
                }

                val taskEntity = variantRepository.getTask(taskId)
                if (taskEntity == null) {
                    Log.e("ViewModel", "Не удалось получить данные задания (taskEntity) для taskId=$taskId для проверки.")
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
                Log.d("ViewModel", "Результат проверки для taskId=$taskId обновлен: isCorrect=$isCorrect, points=$pointsAwarded")

                val checkedAnswerEntity = savedAnswerEntity.copy(isSubmissionCorrect = isCorrect, pointsAwarded = pointsAwarded)
                updateUserAnswersLiveData(variantId, checkedAnswerEntity)

            } catch (e: Exception) {
                Log.e("ViewModel", "Ошибка сохранения и проверки ответа для taskId=$taskId: ${e.message}", e)
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
                Log.d("ViewModel", "LiveData ответов обновлена для taskId=${answerEntity.variantTaskId}")
            } ?: run {
                _userAnswersForCurrentVariantLiveData.value = Resource.Success(mapOf(answerEntity.variantTaskId to answerEntity))
                Log.d("ViewModel", "LiveData ответов создана с первым элементом для taskId=${answerEntity.variantTaskId}")
            }
        } else {
            Log.w("ViewModel", "LiveData ответов не в состоянии Success, не удалось обновить для taskId=${answerEntity.variantTaskId}")
        }
    }
    
    @Deprecated("Use saveUserAnswerAndCheck for immediate checking", ReplaceWith("saveUserAnswerAndCheck(variantId, taskId, answer)"))
    fun saveUserAnswer_OLD(variantId: Int, taskId: Int, answer: String) {
        viewModelScope.launch {
            try {
                variantRepository.saveUserAnswer(variantId, taskId, answer)
                 Log.d("ViewModel", "Ответ для variantId=$variantId, taskId=$taskId сохранен (OLD METHOD): '$answer'")
            } catch (e: Exception) {
                Log.e("ViewModel", "Ошибка сохранения ответа для taskId=$taskId (OLD METHOD): ${e.message}", e)
            }
        }
    }

    /**
     * Проверяет ответы пользователя для указанного варианта.
     */
    fun checkVariantAnswers(variantId: Int) {
        viewModelScope.launch {
            Log.d("ViewModel", "checkVariantAnswers для варианта $variantId вызван. Устанавливаем _variantCheckedState = true.")
            _variantCheckedState.value = true
        }
    }

    fun synchronizeAnswersWithServer(variantId: Int) {
        viewModelScope.launch {
            Log.d("ViewModel", "Начало синхронизации ответов для варианта $variantId с сервером.")
            val userAnswersResource = _userAnswersForCurrentVariantLiveData.value

            val answersToSync: List<UserVariantTaskAnswerEntity>? = if (userAnswersResource is Resource.Success) {
                userAnswersResource.data?.values?.toList()
            } else {
                try {
                    Log.d("ViewModel", "LiveData не содержит ответов, загрузка из репозитория для синхронизации.")
                    variantRepository.getUserAnswersForVariantList(variantId)
                } catch (e: Exception) {
                    Log.e("ViewModel", "Ошибка получения ответов из репозитория для синхронизации: ${e.message}", e)
                    return@launch
                }
            }

            if (answersToSync.isNullOrEmpty()) {
                Log.i("ViewModel", "Нет ответов для синхронизации для варианта $variantId.")
                return@launch
            }

            val payload = answersToSync.mapNotNull { answerEntity ->
                if (answerEntity.userSubmittedAnswer != null && answerEntity.isSubmissionCorrect != null) {
                    UserAnswerPayloadDto(
                        variantTaskId = answerEntity.variantTaskId,
                        userSubmittedAnswer = answerEntity.userSubmittedAnswer!!,
                        isCorrect = answerEntity.isSubmissionCorrect!!
                    )
                } else {
                    Log.w("ViewModel", "Пропуск ответа для taskId ${answerEntity.variantTaskId} при синхронизации: отсутствует текст или результат проверки.")
                    null
                }
            }

            if (payload.isEmpty()) {
                Log.i("ViewModel", "Нет корректно сформированных данных для отправки на сервер (variantId: $variantId).")
                return@launch
            }

            try {
                val syncResult = variantRepository.submitUserAnswers(variantId, payload)
                if (syncResult is Resource.Success) {
                    Log.i("ViewModel", "Ответы для варианта $variantId успешно синхронизированы с сервером. Ответ: ${syncResult.data}")
                    Log.i("ViewModel", "Синхронизация успешна для варианта $variantId. Вызов clearAnswersForCompletedVariant.")
                    clearAnswersForCompletedVariant(variantId)

                } else if (syncResult is Resource.Error) {
                    Log.e("ViewModel", "Ошибка синхронизации ответов для варианта $variantId: ${syncResult.message}")
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Исключение во время синхронизации ответов для варианта $variantId: ${e.message}", e)
            }
        }
    }

    /**
     * Очищает все ответы пользователя и сбрасывает таймер для указанного варианта из локальной базы данных.
     * Вызывается после завершения варианта и закрытия соответствующего UI.
     */
    fun clearAnswersForCompletedVariant(variantId: Int) {
        viewModelScope.launch {
            try {
                Log.i("ViewModel", "clearAnswersForCompletedVariant CALLED for variantId: $variantId")
                variantRepository.clearUserAnswersForVariant(variantId)
                Log.i("ViewModel", "Successfully CALLED variantRepository.clearUserAnswersForVariant for variantId: $variantId")

                val defaultTimerDuration = (3 * 60 * 60 * 1000) + (55 * 60 * 1000).toLong()
                variantRepository.updateVariantTimer(variantId, defaultTimerDuration)
                Log.i("ViewModel", "Successfully reset timer for variantId: $variantId to default.")

            } catch (e: Exception) {
                Log.e("ViewModel", "ERROR in clearAnswersForCompletedVariant for variantId $variantId: ${e.message}", e)
            }
        }
    }
} 