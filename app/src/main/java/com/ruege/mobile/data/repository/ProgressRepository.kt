package com.ruege.mobile.data.repository

import timber.log.Timber
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.ProgressDao
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.ProgressEntity
import com.ruege.mobile.data.network.api.ProgressApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.regex.Pattern

/**
 * Репозиторий для работы с данными о прогрессе пользователя.
 */
@Singleton
class ProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val progressSyncRepository: ProgressSyncRepository,
    private val contentDao: ContentDao,
    private val userDao: UserDao
) {

    private val TAG = "ProgressRepository"
    
    private val _isFirstTimeUser = MutableLiveData<Boolean>()
    
    /**
     * Получает поток прогресса пользователя.
     */
    fun getUserProgressStream(): Flow<List<ProgressEntity>> {
        return userDao.getFirstUserFlow().flatMapLatest { user ->
            user?.getUserId()?.let { userId ->
                progressDao.getProgressByUserId(userId)
            } ?: flowOf(emptyList())
        }
    }

    /**
     * Получает LiveData со списком прогресса пользователя.
     */
    fun getUserProgressLiveData(): LiveData<List<ProgressEntity>> {
        return getUserProgressStream().asLiveData()
    }

    /**
     * Проверяет наличие локального прогресса для заданий и устанавливает флаг _isFirstTimeUser.
     * НЕ СОЗДАЕТ начальные записи прогресса, предполагается, что они приходят с сервера.
     * @return true если локальный прогресс по заданиям отсутствует (потенциально первая настройка), false иначе.
     */
    suspend fun checkAndSetUserSetupStatus(): Boolean {
        val userId = withContext(Dispatchers.IO) { userDao.getFirstUser()?.getUserId() } ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val existingProgress = progressDao.getProgressByUserId(userId).first()
                Timber.d("Проверка статуса настройки пользователя: найдено ${existingProgress.size} записей прогресса.")
                
                val taskGroupProgress = existingProgress.filter { it.contentId.startsWith("task_group_") }
                Timber.d("Найдено ${taskGroupProgress.size} записей task_group_ для проверки статуса настройки.")
                
                if (taskGroupProgress.isEmpty()) {
                    Timber.d("Локальный прогресс по заданиям (task_group_) отсутствует. Устанавливаем isFirstTimeUser = true.")
                    _isFirstTimeUser.postValue(true)
                    return@withContext true
                } else {
                    Timber.d("Локальный прогресс по заданиям (task_group_) уже существует (${taskGroupProgress.size} записей). Устанавливаем isFirstTimeUser = false.")
                    _isFirstTimeUser.postValue(false)
                    return@withContext false
                }
            } catch (e: Exception) {
                Timber.d("Ошибка при проверке статуса настройки пользователя", e)
                _isFirstTimeUser.postValue(false)
                return@withContext false
            }
        }
    }

    /**
     * Обновляет данные о прогрессе из сети.
     */
    suspend fun refreshProgress() {
        val userId = withContext(Dispatchers.IO) { userDao.getFirstUser()?.getUserId() } ?: return
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Начинаем обновление прогресса с сервера...")
                
                Timber.d("Вызываем forceSyncWithServer()")
                val syncResult = progressSyncRepository.forceSyncWithServer()
                
                if (!syncResult) {
                    Timber.d("Не удалось синхронизировать прогресс с сервером. Локальные данные (если есть) будут использованы.")
                    val localProgress = progressDao.getProgressByUserId(userId).first()
                    if (localProgress.isEmpty()) {
                        Timber.d("Локальные данные отсутствуют после неудачной синхронизации.")
                    } else {
                        Timber.d("Найдены локальные данные: ${localProgress.size} записей после неудачной синхронизации.")
                    }
                } else {
                    Timber.d("Прогресс успешно синхронизирован с сервером")
                }
            } catch (e: Exception) {
                Timber.d("Ошибка при обновлении прогресса из сети", e)
                 try {
                    val localProgress = progressDao.getProgressByUserId(userId).first()
                    if (localProgress.isEmpty()) {
                        Timber.d("Локальные данные отсутствуют после ошибки синхронизации.")
                    } else {
                        Timber.d("Используем существующие локальные данные: ${localProgress.size} записей после ошибки синхронизации.")
                    }
                } catch (innerException: Exception) {
                    Timber.d("Ошибка при проверке локального прогресса после основной ошибки", innerException)
                }
            }
        }
    }

    /**
     * Очищает все данные о прогрессе.
     */
    suspend fun clearAllProgress() {
        val userId = withContext(Dispatchers.IO) { userDao.getFirstUser()?.getUserId() } ?: return
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Clearing progress for user from database...")
                progressDao.deleteByUserId(userId)
                
                progressSyncRepository.clearByUserId(userId)
                
                Timber.d("Progress table cleared for user.")
            } catch (e: Exception) {
                Timber.d("Error clearing progress table", e)
            }
        }
    }
    
    /**
     * Обновляет прогресс для указанного контента и добавляет запись в очередь синхронизации.
     * @param contentId ID контента
     * @param percentage процент выполнения
     * @param syncImmediately нужно ли синхронизировать немедленно
     */
    suspend fun updateProgress(contentId: String, percentage: Int, syncImmediately: Boolean = false) {
        val userId = withContext(Dispatchers.IO) { userDao.getFirstUser()?.getUserId() } ?: return
        withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                
                val currentProgress = progressDao.getProgressByContentId(contentId).value
                
                val contentEntity = contentDao.getContentByIdSync(contentId)
                
                val description = contentEntity?.description
                
                val validPercentage = percentage.coerceIn(0, 100)
                
                if (currentProgress != null) {
                    currentProgress.setPercentage(validPercentage)
                    currentProgress.setLastAccessed(timestamp)
                    
                    if (validPercentage >= 100) {
                        currentProgress.setCompleted(true)
                        Timber.d("Задание $contentId достигло 100%, отмечено как выполненное")
                    } else {
                        currentProgress.setCompleted(false)
                    }
                    
                    progressDao.update(currentProgress)
                    
                    progressSyncRepository.queueProgressUpdate(currentProgress, syncImmediately)
                    
                    Timber.d("Updated progress for content $contentId to $validPercentage%")
                } else {
                    val newProgress = ProgressEntity().apply {
                        setContentId(contentId)
                        setPercentage(validPercentage)
                        setLastAccessed(timestamp)
                        setCompleted(validPercentage >= 100)
                        setTitle(if (contentId.startsWith("task_group_")) {
                            "Задание ${contentId.replace("task_group_", "")}"
                        } else {
                            "Прогресс $contentId"
                        })
                        setDescription(description ?: "")
                        setUserId(userId)
                        setSolvedTaskIds("[]")
                    }
                    
                    progressDao.insert(newProgress)
                    
                    progressSyncRepository.queueProgressUpdate(newProgress, syncImmediately)
                    
                    Timber.d("Created new progress for content $contentId with $validPercentage%")
                }
            } catch (e: Exception) {
                Timber.d("Error updating progress for content $contentId", e)
            }
        }
    }
    
    /**
     * Инициализирует репозиторий.
     */
    fun initialize() {
        progressSyncRepository.initialize()
    }
    
    /**
     * Получает прогресс для указанного контента.
     * @param contentId ID контента
     * @return LiveData с прогрессом
     */
    fun getProgressByContentId(contentId: String): LiveData<ProgressEntity> {
        return progressDao.getProgressByContentId(contentId)
    }
    
    /**
     * Устанавливает для контента статус "завершен".
     * @param contentId ID контента
     * @param syncImmediately нужно ли синхронизировать немедленно
     */
    suspend fun markAsCompleted(contentId: String, syncImmediately: Boolean = true) {
        val userId = withContext(Dispatchers.IO) { userDao.getFirstUser()?.getUserId() } ?: return
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Начинаем отмечать контент $contentId как выполненный")

                if (!contentId.startsWith("task_group_")) {
                    Timber.d("Предупреждение: contentId $contentId не соответствует формату task_group_X")
                }
                
                val timestamp = System.currentTimeMillis()
                
                val allProgress = progressDao.getProgressByUserId(userId).first()
                Timber.d("Найдено ${allProgress.size} записей прогресса")
                
                val exactMatch = allProgress.find { it.contentId == contentId }
                
                val contentEntity = contentDao.getContentByIdSync(contentId)
                
                val description = contentEntity?.description
                
                val tasksPerGroup = if (description != null) {
                    extractTaskCount(description)
                } else {
                    100
                }
                
                Timber.d("Количество заданий для $contentId: $tasksPerGroup (из description: ${description ?: "null"})")
                
                val singleTaskPercentage = if (tasksPerGroup > 0) 100 / tasksPerGroup else 1
                
                if (exactMatch != null) {
                    Timber.d("Найдено точное соответствие для $contentId, обновляем процент выполнения")
                    
                    val currentPercentage = exactMatch.percentage
                    val newPercentage = minOf(100, currentPercentage + singleTaskPercentage)
                    
                    val markCompleted = newPercentage >= 100
                    
                    if (markCompleted) {
                        progressDao.markAsCompleted(contentId, timestamp)
                        Timber.d("Задание $contentId достигло 100%, отмечено как выполненное")
                    } else {
                        progressDao.updateProgress(contentId, newPercentage, timestamp)
                        Timber.d("Обновлен процент выполнения для $contentId: $currentPercentage -> $newPercentage")
                    }
                
                val updatedProgress = progressDao.getProgressByContentId(contentId).value
                
                if (updatedProgress != null) {
                    progressSyncRepository.queueProgressUpdate(updatedProgress, syncImmediately)
                        Timber.d("Прогресс для $contentId обновлен до $newPercentage% и добавлен в очередь синхронизации")
                    } else {
                        Timber.d("Content $contentId was not found after updating progress")
                    }
                } else {
                    Timber.d("Запись для $contentId не найдена, создаем новую с начальным процентом")
                    val newProgress = ProgressEntity().apply {
                        setContentId(contentId)
                        setPercentage(singleTaskPercentage)
                        setLastAccessed(timestamp)
                        setCompleted(false)
                        setTitle("Задание ${contentId.replace("task_group_", "")}")
                        setDescription(description ?: "")
                        setUserId(userId)
                        setSolvedTaskIds("[]")
                    }
                    
                    progressDao.insert(newProgress)
                    progressSyncRepository.queueProgressUpdate(newProgress, syncImmediately)
                    
                    Timber.d("Создана новая запись прогресса для $contentId с начальным процентом $singleTaskPercentage%")
                }
            } catch (e: Exception) {
                Timber.d("Error marking content $contentId as completed", e)
            }
        }
    }
    
    /**
     * Извлекает количество заданий из строки description
     * @param description строка с описанием, например "100 заданий на тему X"
     * @return количество заданий или 100 по умолчанию
     */
    private fun extractTaskCount(description: String): Int {
        try {
            val pattern = Pattern.compile("(\\d+)\\s+(заданий|задание|задания)")
            val matcher = pattern.matcher(description)
            
            if (matcher.find()) {
                val countStr = matcher.group(1)
                return countStr.toInt()
            }
            
            val numberPattern = Pattern.compile("(\\d+)")
            val numberMatcher = numberPattern.matcher(description)
            
            if (numberMatcher.find()) {
                val countStr = numberMatcher.group(1)
                return countStr.toInt()
            }
        } catch (e: Exception) {
            Timber.d("Ошибка при извлечении количества заданий из description: $description", e)
        }
        
        return 100
    }
} 