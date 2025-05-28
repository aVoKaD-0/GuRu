package com.ruege.mobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.local.entity.TaskEntity
import com.ruege.mobile.data.repository.PracticeRepository
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
    private val practiceRepository: PracticeRepository
) : ViewModel() {

    // Общая статистика
    val totalAttempts: LiveData<Int> = practiceRepository.getTotalAttempts()
    val totalCorrectAttempts: LiveData<Int> = practiceRepository.getTotalCorrectAttempts()
    
    // Статистика по типам заданий (номерам ЕГЭ)
    val statisticsByType: LiveData<List<PracticeStatisticsEntity>> = 
        practiceRepository.getStatisticsWithAttempts()
    
    // Последние попытки с информацией о заданиях
    private val _recentAttemptsWithTask = MutableLiveData<List<PracticeAttemptWithTask>>()
    val recentAttemptsWithTask: LiveData<List<PracticeAttemptWithTask>> 
        get() = _recentAttemptsWithTask
    
    // Состояние загрузки данных
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> 
        get() = _isLoading

    // Состояние ошибки
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> 
        get() = _error

    /**
     * Загружает последние попытки решения заданий
     * @param limit количество попыток для загрузки
     */
    fun loadRecentAttempts(limit: Int = 20) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
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
                        // Пропускаем попытки, для которых не найдены задания
                    }
                }
                
                _recentAttemptsWithTask.value = attemptsWithTask
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Ошибка при загрузке попыток: ${e.message}"
                _isLoading.value = false
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
            practiceRepository.getStatisticsByEgeNumber(egeNumber).first()
        } catch (e: Exception) {
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
        practiceRepository.saveAttempt(task, isCorrect, source, taskType, textId)
        // Обновляем список последних попыток
        loadRecentAttempts()
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
     * Инициализирует данные при создании ViewModel
     */
    init {
        loadRecentAttempts()
    }
} 