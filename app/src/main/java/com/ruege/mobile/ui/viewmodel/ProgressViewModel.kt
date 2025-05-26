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
    
    // LiveData для хранения списка прогресса пользователя
    private val _progressLiveData = MutableLiveData<List<ProgressEntity>>()
    val progressLiveData: LiveData<List<ProgressEntity>> = _progressLiveData
    
    // Для отслеживания состояния первого входа пользователя
    private val _isFirstTimeUser = MutableLiveData<Boolean>(false)
    val isFirstTimeUser: LiveData<Boolean> = _isFirstTimeUser
    
    // LiveData для отображения элементов в очереди синхронизации
    val syncQueueLiveData: LiveData<List<ProgressSyncQueueEntity>> = progressSyncRepository.getAllSyncItems()
    
    // LiveData для отслеживания количества ожидающих элементов
    val pendingItemsCount: LiveData<Int> = progressSyncRepository.getCountByStatus(SyncStatus.PENDING)
    
    // LiveData для отслеживания количества элементов с ошибкой
    val failedItemsCount: LiveData<Int> = progressSyncRepository.getCountByStatus(SyncStatus.FAILED)
    
    /**
     * Геттер для свойства isFirstTimeUser, необходимый для Java-кода
     */
    fun getIsFirstTimeUser(): LiveData<Boolean> {
        return isFirstTimeUser
    }
    
    /**
     * Запускает синхронизацию прогресса с сервером
     */
    fun syncNow() {
        progressSyncRepository.syncNow(true)
    }
    
    /**
     * Очищает элементы очереди синхронизации с указанным статусом
     * @param status статус элементов для очистки
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
     * @param contentId ID контента
     * @param percentage процент выполнения
     * @param syncImmediately нужно ли синхронизировать немедленно
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
     * @param contentId ID контента
     * @param syncImmediately нужно ли синхронизировать немедленно
     */
    fun markTaskAsCompleted(contentId: String, syncImmediately: Boolean = true) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "ViewModel: отмечаем задание $contentId как выполненное")
                
                // Отмечаем задание как выполненное (добавится в очередь синхронизации)
                // Если syncImmediately=true, запускаем синхронизацию с сервером после задания
                progressRepository.markAsCompleted(contentId, syncImmediately)
                
                // Добавляем задержку, чтобы данные успели обновиться в базе
                delay(300)
                
                // Загружаем обновленные данные ТОЛЬКО из локальной базы, без синхронизации с сервером
                try {
                    // Локальное обновление UI без обращения к серверу
                    _progressLiveData.postValue(progressRepository.getUserProgressLiveData().value)
                    Log.d(TAG, "Задание $contentId отмечено как выполненное, UI обновлен из локальной БД")
                    
                    // Дополнительно запускаем синхронизацию, если syncImmediately=true
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
     * Проверяет наличие прогресса и инициализирует его, если нужно
     */
    fun checkAndInitializeProgress() {
        viewModelScope.launch {
            try {
                val result = progressRepository.initializeUserProgress()
                _isFirstTimeUser.postValue(result)
                if (result) {
                    Log.d(TAG, "Прогресс пользователя успешно инициализирован")
                } else {
                    Log.d(TAG, "Инициализация не требуется, прогресс уже существует")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при проверке и инициализации прогресса", e)
                _error.value = "Ошибка инициализации прогресса: ${e.message}"
            }
        }
    }
    
    /**
     * Загружает начальный прогресс
     */
    fun loadInitialProgress() {
        viewModelScope.launch {
            try {
                // Обновляем данные прогресса с сервера
                progressRepository.refreshProgress()
                
                // Получаем данные прогресса
                val progressData = progressRepository.getUserProgressLiveData()
                // Подписываемся на обновления через обсервер
                progressData.observeForever { progressList ->
                    if (progressList != null) {
                        _progressLiveData.postValue(progressList)
                    }
                }
                Log.d(TAG, "Начальный прогресс загружен")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке начального прогресса", e)
                _error.value = "Не удалось загрузить прогресс: ${e.message}"
            }
        }
    }

    /**
     * Получает прогресс для указанного контента
     * @param contentId ID контента
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