package com.ruege.mobile.viewmodel

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
import android.util.Log
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.ruege.mobile.data.network.dto.UserAnswerPayloadDto

@HiltViewModel
class VariantViewModel @Inject constructor(
    private val variantRepository: VariantRepository
) : ViewModel() {

    private val _variantsState = MutableStateFlow<Resource<List<VariantEntity>>>(Resource.Loading()) // Начальное состояние - загрузка
    val variantsState: StateFlow<Resource<List<VariantEntity>>> = _variantsState.asStateFlow()
    // Добавляем LiveData для использования из MainActivity (Java)
    val variantsLiveData: LiveData<Resource<List<VariantEntity>>> = _variantsState.asLiveData()

    // LiveData для деталей одного варианта
    private val _variantDetailsState = MutableStateFlow<Resource<VariantEntity>>(Resource.Loading()) // Начальное состояние - загрузка
    val variantDetailsState: StateFlow<Resource<VariantEntity>> = _variantDetailsState.asStateFlow()
    val variantDetailsLiveData: LiveData<Resource<VariantEntity>> = _variantDetailsState.asLiveData()

    // StateFlows для списков связанных сущностей
    private val _sharedTextsState = MutableStateFlow<Resource<List<VariantSharedTextEntity>>>(Resource.Success(emptyList()))
    val sharedTextsState: StateFlow<Resource<List<VariantSharedTextEntity>>> = _sharedTextsState.asStateFlow()
    val sharedTextsLiveData: LiveData<Resource<List<VariantSharedTextEntity>>> = _sharedTextsState.asLiveData()

    private val _tasksState = MutableStateFlow<Resource<List<VariantTaskEntity>>>(Resource.Success(emptyList()))
    val tasksState: StateFlow<Resource<List<VariantTaskEntity>>> = _tasksState.asStateFlow()
    val tasksLiveData: LiveData<Resource<List<VariantTaskEntity>>> = _tasksState.asLiveData()

    // LiveData для хранения загруженных ответов пользователя для ТЕКУЩЕГО варианта.
    // Ключ - ID задания (variantTaskId), Значение - сам ответ (UserVariantTaskAnswerEntity).
    private val _userAnswersForCurrentVariantLiveData = MutableLiveData<Resource<Map<Int, UserVariantTaskAnswerEntity>>>(Resource.Loading())
    val userAnswersForCurrentVariantLiveData: LiveData<Resource<Map<Int, UserVariantTaskAnswerEntity>>> = _userAnswersForCurrentVariantLiveData

    // StateFlow для отслеживания состояния проверки варианта
    private val _variantCheckedState = MutableStateFlow<Boolean>(false)
    val variantCheckedState: StateFlow<Boolean> = _variantCheckedState.asStateFlow()

    // Если нужны опции для каждой задачи отдельно, это будет сложнее и, возможно, потребует
    // списка StateFlow или другой структуры данных.
    // Пока не добавляем опции напрямую в ViewModel, их можно будет получать по мере необходимости
    // для конкретной задачи уже в UI или через дополнительный метод во ViewModel.

    init {
        fetchVariants()
    }

    fun fetchVariants() {
        variantRepository.getVariants()
            .onEach { resource: Resource<List<VariantEntity>> ->
                _variantsState.value = resource
            }
            .launchIn(viewModelScope) // Запускаем Flow в viewModelScope
    }

    // Метод для загрузки деталей конкретного варианта
    fun fetchVariantDetails(variantId: Int) {
        viewModelScope.launch {
            Log.d("ViewModel", "fetchVariantDetails($variantId) - START") // Лог старта
            _variantDetailsState.value = Resource.Loading()
            _sharedTextsState.value = Resource.Loading() // Также устанавливаем загрузку для связанных данных
            _tasksState.value = Resource.Loading()
            _userAnswersForCurrentVariantLiveData.value = Resource.Loading() // <--- Сброс и загрузка ответов
            _variantCheckedState.value = false // Сбрасываем состояние проверки при загрузке нового варианта
            Log.d("ViewModel", "fetchVariantDetails($variantId) - States set to Loading/False")

            try {
                // Сначала очистим предыдущие ответы при загрузке нового варианта (опционально, но часто желательно)
                // variantRepository.clearUserAnswersForVariant(variantId) // Если нужно очищать при каждом открытии
                // Если не нужно очищать, а только догружать/обновлять, эту строку можно убрать.
                // Для текущей задачи "вставлять прошлые ответы" - очищать НЕ НУЖНО.
                Log.d("ViewModel", "fetchVariantDetails($variantId) - Calling repository.fetchAndSaveVariantDetails...")
                val resource = variantRepository.fetchAndSaveVariantDetails(variantId)
                Log.d("ViewModel", "fetchVariantDetails($variantId) - Repository returned: $resource")
                _variantDetailsState.value = resource

                if (resource is Resource.Success && resource.data != null) {
                    val currentVariantId = resource.data.variantId
                    Log.d("ViewModel", "fetchVariantDetails($variantId) - Variant details SUCCESS. currentVariantId=$currentVariantId. Loading shared texts...")
                    // Загружаем общие тексты
                    variantRepository.getSharedTextsForVariant(currentVariantId)
                        .onEach { texts -> 
                            _sharedTextsState.value = Resource.Success(texts)
                            Log.d("ViewModel", "fetchVariantDetails($variantId) - Shared texts loaded: ${texts.size}")
                        }
                        .launchIn(viewModelScope)

                    // Загружаем задания
                    Log.d("ViewModel", "fetchVariantDetails($variantId) - Loading tasks...")
                    variantRepository.getTasksForVariant(currentVariantId)
                        .onEach { tasks -> 
                            _tasksState.value = Resource.Success(tasks)
                            Log.d("ViewModel", "fetchVariantDetails($variantId) - Tasks loaded: ${tasks.size}")
                        }
                        .launchIn(viewModelScope)

                    // Загружаем сохраненные ответы пользователя для этого варианта
                    Log.d("ViewModel", "fetchVariantDetails($variantId) - Loading user answers...")
                    try {
                        val userAnswersMap = variantRepository.getUserAnswersForVariant(currentVariantId)
                        _userAnswersForCurrentVariantLiveData.value = Resource.Success(userAnswersMap)
                        Log.d("ViewModel", "fetchVariantDetails($variantId) - User answers loaded: ${userAnswersMap.size} answers for variant $currentVariantId")
                    } catch (e: Exception) {
                        val errorMsg = "Ошибка загрузки ответов пользователя для варианта $currentVariantId: ${e.message}"
                        Log.e("ViewModel", errorMsg, e)
                        _userAnswersForCurrentVariantLiveData.value = Resource.Error(errorMsg, null)
                    }

                } else if (resource is Resource.Error) {
                    val errorMsg = resource.message ?: "Неизвестная ошибка"
                    Log.e("ViewModel", "fetchVariantDetails($variantId) - Variant details ERROR: $errorMsg")
                    _sharedTextsState.value = Resource.Error("Ошибка загрузки текстов: $errorMsg", null)
                    _tasksState.value = Resource.Error("Ошибка загрузки заданий: $errorMsg", null)
                    _userAnswersForCurrentVariantLiveData.value = Resource.Error("Ошибка загрузки ответов: $errorMsg", null)
                }
            } catch (e: Exception) {
                val errorMsg = "Ошибка при загрузке деталей варианта: ${e.message}"
                Log.e("ViewModel", "fetchVariantDetails($variantId) - Outer CATCH: $errorMsg", e)
                _variantDetailsState.value = Resource.Error(errorMsg, null)
                _sharedTextsState.value = Resource.Error(errorMsg, null)
                _tasksState.value = Resource.Error(errorMsg, null)
                _userAnswersForCurrentVariantLiveData.value = Resource.Error(errorMsg, null)
            }
            Log.d("ViewModel", "fetchVariantDetails($variantId) - END") // Лог конца
        }
    }

    // Метод для получения опций для конкретного задания (если нужно)
    // fun getOptionsForTask(taskId: Int): Flow<List<VariantTaskOptionEntity>> {
    //     return variantRepository.getOptionsForTask(taskId)
    // }

    // Метод для обновления времени последнего доступа к варианту
    fun updateVariantLastAccessedTime(variantId: Int) {
        viewModelScope.launch {
            variantRepository.updateVariantLastAccessedTime(variantId)
        }
    }

    // Метод для обновления статуса загрузки варианта
    // (например, если пользователь скачивает вариант для офлайн-доступа)
    fun setVariantDownloadedStatus(variantId: Int, isDownloaded: Boolean) {
        viewModelScope.launch {
            variantRepository.updateVariantDownloadedStatus(variantId, isDownloaded)
        }
    }
    
    // Можно добавить и другие методы, если они понадобятся для UI
    // например, получение только загруженных вариантов, если это нужно отдельно от основного списка
    // private val _downloadedVariantsState = MutableStateFlow<List<VariantEntity>>(emptyList())
    // val downloadedVariantsState: StateFlow<List<VariantEntity>> = _downloadedVariantsState.asStateFlow()

    // fun fetchDownloadedVariants() {
    //     variantRepository.getDownloadedVariants()
    //         .onEach { variants ->
    //             _downloadedVariantsState.value = variants
    //         }
    //         .launchIn(viewModelScope)
    // }

    /**
     * Сохраняет ответ пользователя для указанного задания в БД и сразу проверяет его.
     */
    fun saveUserAnswerAndCheck(variantId: Int, taskId: Int, answer: String) {
        viewModelScope.launch {
            try {
                Log.d("ViewModel", "Сохранение и проверка ответа для variantId=$variantId, taskId=$taskId, ответ: '$answer'")
                // 1. Сохраняем ответ и получаем обновленную сущность
                val savedAnswerEntity = variantRepository.saveUserAnswer(variantId, taskId, answer)

                if (savedAnswerEntity == null) {
                    Log.e("ViewModel", "Не удалось сохранить или получить ответ для taskId=$taskId после сохранения.")
                    // Можно добавить обработку ошибки, например, показать Toast
                    return@launch
                }

                // 2. Получаем данные задания для проверки (правильный ответ, баллы)
                val taskEntity = variantRepository.getTask(taskId)
                if (taskEntity == null) {
                    Log.e("ViewModel", "Не удалось получить данные задания (taskEntity) для taskId=$taskId для проверки.")
                    // Возможно, стоит обновить LiveData с сохраненным, но не проверенным ответом
                    updateUserAnswersLiveData(variantId, savedAnswerEntity)
                    return@launch
                }

                // 3. Логика проверки ответа
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
                    // Опционально: считаем пустой ответ на пустое решение правильным
                    // isCorrect = true
                    // pointsAwarded = taskEntity.maxPoints ?: 0 
                }

                // 4. Обновляем результат проверки в БД
                variantRepository.updateUserAnswerResult(taskId, isCorrect, pointsAwarded)
                Log.d("ViewModel", "Результат проверки для taskId=$taskId обновлен: isCorrect=$isCorrect, points=$pointsAwarded")

                // 5. Обновляем LiveData с проверенным ответом
                val checkedAnswerEntity = savedAnswerEntity.copy(isSubmissionCorrect = isCorrect, pointsAwarded = pointsAwarded)
                updateUserAnswersLiveData(variantId, checkedAnswerEntity)

            } catch (e: Exception) {
                Log.e("ViewModel", "Ошибка сохранения и проверки ответа для taskId=$taskId: ${e.message}", e)
                // Здесь можно обработать ошибку, например, показать Toast
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
            // Если LiveData еще не была успешно загружена, можно попробовать загрузить все ответы заново
            // или создать новый Success Resource с этим единственным ответом.
            // Пока просто логируем, но в идеале нужно обработать этот случай.
            Log.w("ViewModel", "LiveData ответов не в состоянии Success, не удалось обновить для taskId=${answerEntity.variantTaskId}")
            // Как вариант, если нужно обязательно показать этот ответ:
            // _userAnswersForCurrentVariantLiveData.value = Resource.Success(mapOf(answerEntity.variantTaskId to answerEntity))
            // Но это сотрет другие ответы, если они были. Лучше перезапросить все:
            // fetchUserAnswersForVariant(variantId)
        }
    }
    
    // Старый метод saveUserAnswer - переименовать или удалить. Пока переименую, чтобы сохранить вызовы.
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
     * // TODO: Этот метод теперь может быть не нужен, или его логика должна измениться,
     * // так как проверка происходит при сохранении каждого ответа.
     * // Возможно, он будет отвечать за установку _variantCheckedState или другие общие действия по завершению.
     */
    fun checkVariantAnswers(variantId: Int) {
        viewModelScope.launch {
            Log.d("ViewModel", "checkVariantAnswers для варианта $variantId вызван. Устанавливаем _variantCheckedState = true.")
            _variantCheckedState.value = true
            // Можно оставить логирование из предыдущей версии, если оно полезно для отладки,
            // но сама логика принятия решения о `_variantCheckedState` упрощается.
        }
    }

    fun synchronizeAnswersWithServer(variantId: Int) {
        viewModelScope.launch {
            Log.d("ViewModel", "Начало синхронизации ответов для варианта $variantId с сервером.")
            val userAnswersResource = _userAnswersForCurrentVariantLiveData.value

            val answersToSync: List<UserVariantTaskAnswerEntity>? = if (userAnswersResource is Resource.Success) {
                userAnswersResource.data?.values?.toList()
            } else {
                // Если LiveData не содержит успешных данных, попробуем загрузить из БД напрямую
                try {
                    Log.d("ViewModel", "LiveData не содержит ответов, загрузка из репозитория для синхронизации.")
                    variantRepository.getUserAnswersForVariantList(variantId)
                } catch (e: Exception) {
                    Log.e("ViewModel", "Ошибка получения ответов из репозитория для синхронизации: ${e.message}", e)
                    // Можно показать ошибку пользователю или просто прервать синхронизацию
                    // _syncStatusLiveData.value = Resource.Error("Не удалось получить ответы для синхронизации")
                    return@launch
                }
            }

            if (answersToSync.isNullOrEmpty()) {
                Log.i("ViewModel", "Нет ответов для синхронизации для варианта $variantId.")
                // _syncStatusLiveData.value = Resource.Success(emptyList()) // Успех, но ничего не отправлено
                return@launch
            }

            val payload = answersToSync.mapNotNull { answerEntity ->
                // Включаем только ответы, где есть сам текст ответа
                // и результат проверки (isSubmissionCorrect не null)
                if (answerEntity.userSubmittedAnswer != null && answerEntity.isSubmissionCorrect != null) {
                    UserAnswerPayloadDto(
                        variantTaskId = answerEntity.variantTaskId,
                        userSubmittedAnswer = answerEntity.userSubmittedAnswer!!, // Уже проверили на null
                        isCorrect = answerEntity.isSubmissionCorrect!!      // Уже проверили на null
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

            // _syncStatusLiveData.value = Resource.Loading() // Показать индикатор загрузки синхронизации, если есть
            try {
                val syncResult = variantRepository.submitUserAnswers(variantId, payload)
                if (syncResult is Resource.Success) {
                    Log.i("ViewModel", "Ответы для варианта $variantId успешно синхронизированы с сервером. Ответ: ${syncResult.data}")
                    // _syncStatusLiveData.value = syncResult // Обновить статус синхронизации
                    // Можно добавить логику для обработки ответа сервера, если это необходимо

                    // !!! ВЫЗЫВАЕМ ОЧИСТКУ ОТВЕТОВ ЗДЕСЬ !!!
                    Log.i("ViewModel", "Синхронизация успешна для варианта $variantId. Вызов clearAnswersForCompletedVariant.")
                    clearAnswersForCompletedVariant(variantId)

                } else if (syncResult is Resource.Error) {
                    Log.e("ViewModel", "Ошибка синхронизации ответов для варианта $variantId: ${syncResult.message}")
                    // _syncStatusLiveData.value = syncResult // Обновить статус синхронизации
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Исключение во время синхронизации ответов для варианта $variantId: ${e.message}", e)
                // _syncStatusLiveData.value = Resource.Error("Исключение во время синхронизации: ${e.message}")
            }
        }
    }

    /**
     * Очищает все ответы пользователя для указанного варианта из локальной базы данных.
     * Вызывается после завершения варианта и закрытия соответствующего UI.
     */
    fun clearAnswersForCompletedVariant(variantId: Int) {
        viewModelScope.launch {
            try {
                Log.i("ViewModel", "clearAnswersForCompletedVariant CALLED for variantId: $variantId")
                variantRepository.clearUserAnswersForVariant(variantId)
                Log.i("ViewModel", "Successfully CALLED variantRepository.clearUserAnswersForVariant for variantId: $variantId")
                // Опционально: можно также сбросить _userAnswersForCurrentVariantLiveData, если это необходимо
                // _userAnswersForCurrentVariantLiveData.value = Resource.Success(emptyMap())
                // _variantCheckedState.value = false // Также сбросить состояние проверки, если это применимо
            } catch (e: Exception) {
                Log.e("ViewModel", "ERROR in clearAnswersForCompletedVariant for variantId $variantId: ${e.message}", e)
            }
        }
    }
} 