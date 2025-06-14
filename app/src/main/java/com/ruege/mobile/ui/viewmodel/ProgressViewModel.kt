package com.ruege.mobile.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.local.entity.ProgressEntity
import com.ruege.mobile.data.local.entity.ProgressSyncQueueEntity
import com.ruege.mobile.data.local.entity.SyncStatus
import com.ruege.mobile.data.repository.ProgressRepository
import com.ruege.mobile.data.repository.ProgressSyncRepository
import com.ruege.mobile.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для работы с прогрессом пользователя
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val progressSyncRepository: ProgressSyncRepository
) : ViewModel() {

    private val TAG = "ProgressViewModel"
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    
    private val _progressLiveData = MutableLiveData<List<ProgressEntity>>()
    val progressLiveData: LiveData<List<ProgressEntity>> = _progressLiveData
    
    val syncQueueLiveData: LiveData<List<ProgressSyncQueueEntity>> = progressSyncRepository.getAllSyncItems()
    
    val pendingItemsCount: LiveData<Int> = progressSyncRepository.getCountByStatus(SyncStatus.PENDING)
    
    val failedItemsCount: LiveData<Int> = progressSyncRepository.getCountByStatus(SyncStatus.FAILED)
    
    /**
     * Запускает синхронизацию прогресса с сервером
     */
    fun syncNow() {
        viewModelScope.launch {
            progressSyncRepository.syncNow(true)
        }
    }
    
    /**
     * Очищает элементы очереди синхронизации с указанным статусом
     */
    fun clearSyncQueueByStatus(status: SyncStatus) {
        viewModelScope.launch {
            try {
                progressSyncRepository.clearByStatus(status)
                Log.d(TAG, "Очищены элементы очереди со статусом: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при очистке элементов очереди со статусом: $status", e)
                _error.value = "Не удалось очистить элементы синхронизации: ${e.message}"
            }
        }
    }

    /**
     * Проверяет и устанавливает статус первоначальной настройки пользователя (например, флаг isFirstTimeUser).
     * Этот метод вызывается, чтобы определить, нужны ли какие-либо действия при первом запуске для пользователя.
     * Непосредственно НЕ загружает прогресс с сервера, для этого есть refreshProgress() или checkAndInitializeProgressAndLoad().
     */
    fun initializeProgress() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                progressRepository.checkAndSetUserSetupStatus()
                Log.d(TAG, "Проверка и установка статуса настройки пользователя выполнена")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при проверке и установке статуса настройки пользователя", e)
                _error.value = "Не удалось проверить статус настройки пользователя: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновляет данные о прогрессе из сети
     */
    fun refreshProgress() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                progressRepository.refreshProgress()
                Log.d(TAG, "Обновление прогресса выполнено")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении прогресса", e)
                _error.value = "Не удалось обновить прогресс: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Получает поток прогресса пользователя непосредственно из репозитория
     */
    fun getUserProgress(): LiveData<List<ProgressEntity>> {
        return progressRepository.getUserProgressLiveData()
    }
    
    /**
     * Обновляет прогресс для указанного контента
     */
    fun updateProgress(contentId: String, percentage: Int, syncImmediately: Boolean = false) {
        viewModelScope.launch {
            try {
                progressRepository.updateProgress(contentId, percentage, syncImmediately)
                Log.d(TAG, "Progress for $contentId updated to $percentage%")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating progress for $contentId", e)
                _error.value = "Не удалось обновить прогресс: ${e.message}"
            }
        }
    }

    /**
     * Отмечает задание как завершенное
     */
    fun markTaskAsCompleted(contentId: String, syncImmediately: Boolean = true) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "ViewModel: отмечаем задание $contentId как выполненное")
                progressRepository.markAsCompleted(contentId, syncImmediately)

                delay(300)

                try {
                    _progressLiveData.postValue(progressRepository.getUserProgressLiveData().value)
                    Log.d(TAG, "Задание $contentId отмечено как выполненное, UI обновлен из локальной БД")

                    if (syncImmediately) {
                        Log.d(TAG, "Запускаем принудительную синхронизацию после отметки задания $contentId как выполненного")
                        syncNow()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обновлении UI для задания $contentId", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отметке задания $contentId как выполненного", e)
                _error.value = "Не удалось отметить задание как выполненное: ${e.message}"
            }
        }
    }

    /**
     * Очищает все данные о прогрессе пользователя
     */
    fun clearAllProgress() {
        viewModelScope.launch {
            try {
                progressRepository.clearAllProgress()
                Log.d(TAG, "All progress cleared for user")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing progress for user", e)
                _error.value = "Не удалось очистить прогресс: ${e.message}"
            }
        }
    }
    
    /**
     * Очищает все данные о прогрессе пользователя при запуске
     */
    fun clearAllUserProgressData() {
        viewModelScope.launch {
            try {
                progressRepository.clearAllProgress()
                Log.d(TAG, "Весь прогресс пользователя очищен при запуске")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при очистке всего прогресса пользователя при запуске", e)
                _error.value = "Не удалось очистить прогресс: ${e.message}"
            }
        }
    }
    
    /**
     * Проверяет, инициализирует (если нужно на стороне сервера) и загружает прогресс.
     * Сначала всегда пытается обновить данные с сервера.
     * Затем проверяет локальное состояние для установки флага первой настройки.
     */
    fun checkAndInitializeProgressAndLoad() {
        viewModelScope.launch {
            _error.value = null
            Log.d(TAG, "checkAndInitializeProgressAndLoad: Начало операции.")

            launch {
                try {
                    Log.d(TAG, "checkAndInitializeProgressAndLoad: Запускаем PULL с сервера (refreshProgress).")
                    progressRepository.refreshProgress()
                    Log.d(TAG, "checkAndInitializeProgressAndLoad: refreshProgress завершен.")
                } catch (e: Exception) {
                    Log.e(TAG, "checkAndInitializeProgressAndLoad: Ошибка при обновлении прогресса с сервера", e)
                    _error.postValue("Не удалось обновить прогресс: ${e.message}")
                }
            }

            try {
                val isConsideredNewSetupLocally = progressRepository.checkAndSetUserSetupStatus()
                Log.d(TAG, "checkAndInitializeProgressAndLoad: checkAndSetUserSetupStatus результат: $isConsideredNewSetupLocally")

                if (isConsideredNewSetupLocally) {
                    Log.d(TAG, "checkAndInitializeProgressAndLoad: Локально определено состояние 'первой настройки'. " +
                               "Предполагается, что сервер инициализировал данные, и они были загружены.")
                } else {
                    Log.d(TAG, "checkAndInitializeProgressAndLoad: Существующий пользователь или данные уже настроены локально.")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "checkAndInitializeProgressAndLoad: Ошибка при начальной загрузке/инициализации прогресса", e)
                _error.postValue("Не удалось загрузить/инициализировать прогресс: ${e.message}")
            } finally {
                Log.d(TAG, "checkAndInitializeProgressAndLoad: Операция завершена.")
            }
        }
    }
    
    /**
     * Получает прогресс для указанного контента
     */
    fun getProgressByContentId(contentId: String): LiveData<ProgressEntity> {
        return progressRepository.getProgressByContentId(contentId)
    }

    /**
     * Сбрасывает состояние ошибки
     */
    fun clearError() {
        _error.value = null
    }
}