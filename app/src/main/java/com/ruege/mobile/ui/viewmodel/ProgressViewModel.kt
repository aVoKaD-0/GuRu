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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
        progressSyncRepository.syncNow(true)
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
     * Инициализирует прогресс пользователя при первом запуске
     */
    fun initializeProgress() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                progressRepository.initializeUserProgress()
                Log.d(TAG, "Инициализация прогресса пользователя выполнена")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при инициализации прогресса пользователя", e)
                _error.value = "Не удалось инициализировать прогресс: ${e.message}"
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
                Log.d(TAG, "Прогресс для $contentId обновлен на $percentage%")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении прогресса для $contentId", e)
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
                Log.d(TAG, "Весь прогресс пользователя очищен")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при очистке всего прогресса пользователя", e)
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
     * Проверяет наличие прогресса, инициализирует его при необходимости (для новых пользователей)
     * и загружает актуальные данные с сервера (для существующих пользователей).
     * Управляет состоянием isLoading и error для всего процесса.
     */
    fun checkAndInitializeProgressAndLoad() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null 
            Log.d(TAG, "checkAndInitializeProgressAndLoad: Начало операции, isLoading=true.")
            try {
                val isNewlyInitialized = progressRepository.initializeUserProgress()

                if (isNewlyInitialized) {
                    Log.d(TAG, "checkAndInitializeProgressAndLoad: Новый пользователь. Прогресс инициализирован локально.")
                    Log.d(TAG, "checkAndInitializeProgressAndLoad: Новый пользователь - запускаем немедленную синхронизацию (PUSH).")
                    progressSyncRepository.syncNow(expedited = true)
                } else {
                    Log.d(TAG, "checkAndInitializeProgressAndLoad: Существующий пользователь. Запускаем PULL с сервера.")
                    
                    progressRepository.refreshProgress() 
                }
                Log.d(TAG, "checkAndInitializeProgressAndLoad: Локальная инициализация/обновление завершено.")
            } catch (e: Exception) {
                Log.e(TAG, "checkAndInitializeProgressAndLoad: Ошибка при начальной загрузке/инициализации прогресса", e)
                _error.postValue("Не удалось загрузить/инициализировать прогресс: ${e.message}")
            } finally {
                _isLoading.value = false
                Log.d(TAG, "checkAndInitializeProgressAndLoad: Операция завершена, isLoading установлен в false.")
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