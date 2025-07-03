package com.ruege.mobile.ui.viewmodel

import timber.log.Timber
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
    
    init {
        viewModelScope.launch {
            checkAndInitializeProgressAndLoad()
        }
    }

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
                Timber.d("Очищены элементы очереди со статусом: $status")
            } catch (e: Exception) {
                Timber.d("Ошибка при очистке элементов очереди со статусом: $status", e)
                _error.value = "Не удалось очистить элементы синхронизации: ${e.message}"
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
     * Проверяет, инициализирует (если нужно на стороне сервера) и загружает прогресс.
     * Сначала всегда пытается обновить данные с сервера.
     * Затем проверяет локальное состояние для установки флага первой настройки.
     */
    fun checkAndInitializeProgressAndLoad() {
        viewModelScope.launch {
            _error.value = null
            Timber.d("checkAndInitializeProgressAndLoad: Начало операции.")

            launch {
                try {
                    Timber.d("checkAndInitializeProgressAndLoad: Запускаем PULL с сервера (refreshProgress).")
                    progressRepository.refreshProgress()
                    Timber.d("checkAndInitializeProgressAndLoad: refreshProgress завершен.")
                } catch (e: Exception) {
                    Timber.d("checkAndInitializeProgressAndLoad: Ошибка при обновлении прогресса с сервера", e)
                    _error.postValue("Не удалось обновить прогресс: ${e.message}")
                }
            }

            try {
                val isConsideredNewSetupLocally = progressRepository.checkAndSetUserSetupStatus()
                Timber.d("checkAndInitializeProgressAndLoad: checkAndSetUserSetupStatus результат: $isConsideredNewSetupLocally")

                if (isConsideredNewSetupLocally) {
                    Timber.d("checkAndInitializeProgressAndLoad: Локально определено состояние 'первой настройки'. " +
                               "Предполагается, что сервер инициализировал данные, и они были загружены.")
                } else {
                    Timber.d("checkAndInitializeProgressAndLoad: Существующий пользователь или данные уже настроены локально.")
                }
                
            } catch (e: Exception) {
                Timber.d("checkAndInitializeProgressAndLoad: Ошибка при начальной загрузке/инициализации прогресса", e)
                _error.postValue("Не удалось загрузить/инициализировать прогресс: ${e.message}")
            } finally {
                Timber.d("checkAndInitializeProgressAndLoad: Операция завершена.")
            }
        }
    }
}