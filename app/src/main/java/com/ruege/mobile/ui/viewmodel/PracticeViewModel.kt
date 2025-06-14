package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.local.entity.TaskEntity
import com.ruege.mobile.data.repository.PracticeRepository
import com.ruege.mobile.data.repository.PracticeSyncRepository
import com.ruege.mobile.data.repository.PracticeStatisticsRepository
import com.ruege.mobile.data.repository.Result
import com.ruege.mobile.model.VariantResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Вспомогательный класс для отображения попытки вместе с информацией о задании
 */
data class PracticeAttemptWithTask(
    val attempt: PracticeAttemptEntity,
    val taskEntity: TaskEntity,
    val egeNumber: String,
    val isCorrect: Boolean,
    val attemptDate: Long
)

/**
 * ViewModel для работы с практикой и статистикой заданий
 */
@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val practiceSyncRepository: PracticeSyncRepository,
    private val practiceStatisticsRepository: PracticeStatisticsRepository
) : ViewModel() {

    val totalAttempts: LiveData<Int> = practiceRepository.getTotalAttempts()
    val totalCorrectAttempts: LiveData<Int> = practiceRepository.getTotalCorrectAttempts()
    
    val statisticsByType: LiveData<List<PracticeStatisticsEntity>> = 
        practiceRepository.getStatisticsWithAttempts()
  
    private val _recentAttemptsWithTask = MutableLiveData<List<PracticeAttemptWithTask>>()
    val recentAttemptsWithTask: LiveData<List<PracticeAttemptWithTask>> 
        get() = _recentAttemptsWithTask
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> 
        get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> 
        get() = _error

    /**
     * Загружает последние попытки решения заданий
     * @param limit количество попыток для загрузки
     * @param manageLoadingState управляет ли этот метод состоянием isLoading
     * @return Job запущенной корутины
     */
    fun loadRecentAttempts(limit: Int = 20, manageLoadingState: Boolean = true): kotlinx.coroutines.Job {
        if (manageLoadingState) {
            _isLoading.value = true
            _error.value = null
        }
        
        return viewModelScope.launch {
            try {
                val attempts = practiceRepository.getRecentAttempts(limit).first()
                val attemptsWithTask = mutableListOf<PracticeAttemptWithTask>()
                
                for (attempt in attempts) {
                    try {
                        val task = practiceRepository.getTaskById(attempt.taskId)
                        if (task != null) {
                            attemptsWithTask.add(
                                PracticeAttemptWithTask(
                                    attempt = attempt,
                                    taskEntity = task,
                                    egeNumber = task.egeNumber,
                                    isCorrect = attempt.isCorrect,
                                    attemptDate = attempt.attemptDate
                                )
                            )
                        }
                    } catch (e: Exception) {

                    }
                }
                
                _recentAttemptsWithTask.postValue(attemptsWithTask)
            } catch (e: Exception) {
                _error.postValue("Ошибка при загрузке попыток: ${e.message}")
            } finally {
                if (manageLoadingState) {
                    _isLoading.postValue(false)
                }
            }
        }
    }

    /**
     * Загружает статистику для конкретного номера задания ЕГЭ
     * @param egeNumber номер задания ЕГЭ
     * @return статистика по заданию
     */
    suspend fun getStatisticsByEgeNumber(egeNumber: String): PracticeStatisticsEntity? {
        return try {
            practiceStatisticsRepository.getStatisticsByEgeNumber(egeNumber)
        } catch (e: Exception) {
            _error.postValue("Ошибка при получении статистики: ${e.message}")
            null
        }
    }

    /**
     * Сохраняет результат попытки решения задания
     * @param task задание
     * @param isCorrect результат попытки (правильно/неправильно)
     * @param source источник попытки (практика, тест и т.д.)
     * @param taskType тип задания
     * @param textId идентификатор текста (если применимо)
     */
    fun saveAttempt(task: TaskEntity, isCorrect: Boolean, source: String = "practice", 
                  taskType: String = "", textId: String = "") {
        viewModelScope.launch {
            practiceRepository.saveAttempt(task, isCorrect, source, taskType, textId)
            loadRecentAttempts().join()
        }
    }

    /**
     * Вычисляет общий процент правильных ответов
     * @return процент правильных ответов или null, если нет попыток
     */
    fun calculateSuccessRate(): Float? {
        val total = totalAttempts.value ?: 0
        val correct = totalCorrectAttempts.value ?: 0
        
        return if (total > 0) {
            (correct.toFloat() / total) * 100
        } else {
            null
        }
    }

    /**
     * Запускает полную синхронизацию данных с сервером
     */
    fun syncPracticeData() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)
            
            when (val result = practiceSyncRepository.performFullSync(null)) {
                is Result.Success -> {

                }
                is Result.Failure -> {
                    _error.postValue("Ошибка синхронизации: ${result.exception.message}")
                }
                is Result.Loading -> {
                    
                }
            }
            
            loadRecentAttempts(limit = 20, manageLoadingState = false).join()

            _isLoading.postValue(false)
        }
    }

    /**
     * Сохраняет результаты варианта для указанного номера ЕГЭ
     * @param egeNumber номер задания ЕГЭ
     * @param variantResult результаты выполнения варианта
     */
    fun saveVariantResults(egeNumber: String, variantResult: VariantResult) {
        viewModelScope.launch {
            try {
                practiceStatisticsRepository.updateVariantData(egeNumber, variantResult)
            } catch (e: Exception) {
                _error.postValue("Ошибка при сохранении результатов варианта: ${e.message}")
            }
        }
    }

    /**
     * Получает результаты варианта для указанного номера ЕГЭ
     * @param egeNumber номер задания ЕГЭ
     * @return результаты варианта или null, если их нет
     */
    suspend fun getVariantResultForEgeNumber(egeNumber: String): VariantResult? {
        return try {
            practiceStatisticsRepository.getVariantResult(egeNumber)
        } catch (e: Exception) {
            _error.postValue("Ошибка при получении результатов варианта: ${e.message}")
            null
        }
    }

    /**
     * Инициализирует данные при создании ViewModel
     */
    init {
        loadRecentAttempts()
        viewModelScope.launch {
            syncPracticeData()
        }
    }
} 