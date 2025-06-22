package com.ruege.mobile.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.ProgressDao
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.ContentEntity
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
    private val progressApiService: ProgressApiService,
    private val progressSyncRepository: ProgressSyncRepository,
    private val contentDao: ContentDao,
    private val userDao: UserDao
) {

    private val TAG = "ProgressRepository"
    
    private val _isFirstTimeUser = MutableLiveData<Boolean>()
    
    /**
     * Получает LiveData для отслеживания статуса первого входа пользователя
     */
    fun getIsFirstTimeUser(): LiveData<Boolean> {
        return _isFirstTimeUser
    }
    
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
                Log.d(TAG, "Проверка статуса настройки пользователя: найдено ${existingProgress.size} записей прогресса.")
                
                val taskGroupProgress = existingProgress.filter { it.contentId.startsWith("task_group_") }
                Log.d(TAG, "Найдено ${taskGroupProgress.size} записей task_group_ для проверки статуса настройки.")
                
                if (taskGroupProgress.isEmpty()) {
                    Log.d(TAG, "Локальный прогресс по заданиям (task_group_) отсутствует. Устанавливаем isFirstTimeUser = true.")
                    _isFirstTimeUser.postValue(true)
                    return@withContext true
                } else {
                    Log.d(TAG, "Локальный прогресс по заданиям (task_group_) уже существует (${taskGroupProgress.size} записей). Устанавливаем isFirstTimeUser = false.")
                    _isFirstTimeUser.postValue(false)
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при проверке статуса настройки пользователя", e)
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
                Log.d(TAG, "Начинаем обновление прогресса с сервера...")
                
                Log.d(TAG, "Вызываем forceSyncWithServer()")
                val syncResult = progressSyncRepository.forceSyncWithServer()
                
                if (!syncResult) {
                    Log.w(TAG, "Не удалось синхронизировать прогресс с сервером. Локальные данные (если есть) будут использованы.")
                    val localProgress = progressDao.getProgressByUserId(userId).first()
                    if (localProgress.isEmpty()) {
                        Log.d(TAG, "Локальные данные отсутствуют после неудачной синхронизации.")
                    } else {
                        Log.d(TAG, "Найдены локальные данные: ${localProgress.size} записей после неудачной синхронизации.")
                    }
                } else {
                    Log.d(TAG, "Прогресс успешно синхронизирован с сервером")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении прогресса из сети", e)
                 try {
                    val localProgress = progressDao.getProgressByUserId(userId).first()
                    if (localProgress.isEmpty()) {
                        Log.d(TAG, "Локальные данные отсутствуют после ошибки синхронизации.")
                    } else {
                        Log.d(TAG, "Используем существующие локальные данные: ${localProgress.size} записей после ошибки синхронизации.")
                    }
                } catch (innerException: Exception) {
                    Log.e(TAG, "Ошибка при проверке локального прогресса после основной ошибки", innerException)
                }
            }
        }
    }

    /**
     * Загружает тестовые данные о прогрессе.
     */
    private suspend fun loadMockProgress() {
        val userId = withContext(Dispatchers.IO) { userDao.getFirstUser()?.getUserId() } ?: return
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading mock progress data...")
                
                val mockProgress = listOf(
                    ProgressEntity().apply {
                        setContentId("1")
                        setPercentage(30)
                        setLastAccessed(System.currentTimeMillis())
                        setCompleted(false)
                        setTitle("Прогресс - Морфемика")
                        setDescription("")
                        setUserId(userId)
                        setSolvedTaskIds("[]")
                    },
                    ProgressEntity().apply {
                        setContentId("2")
                        setPercentage(42)
                        setLastAccessed(System.currentTimeMillis())
                        setCompleted(false)
                        setTitle("Прогресс - Синтаксис")
                        setDescription("")
                        setUserId(userId)
                        setSolvedTaskIds("[]")
                    },
                    ProgressEntity().apply {
                        setContentId("3")
                        setPercentage(0)
                        setLastAccessed(System.currentTimeMillis())
                        setCompleted(false)
                        setTitle("Прогресс - Пунктуация")
                        setDescription("")
                        setUserId(userId)
                        setSolvedTaskIds("[]")
                    }
                )
                
                progressDao.insertAll(mockProgress)
                Log.d(TAG, "Mock progress inserted successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading mock progress", e)
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
                Log.d(TAG, "Clearing progress for user from database...")
                progressDao.deleteByUserId(userId)
                
                progressSyncRepository.clearByUserId(userId)
                
                Log.d(TAG, "Progress table cleared for user.")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing progress table", e)
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
                        Log.d(TAG, "Задание $contentId достигло 100%, отмечено как выполненное")
                    } else {
                        currentProgress.setCompleted(false)
                    }
                    
                    progressDao.update(currentProgress)
                    
                    progressSyncRepository.queueProgressUpdate(currentProgress, syncImmediately)
                    
                    Log.d(TAG, "Updated progress for content $contentId to $validPercentage%")
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
                    
                    Log.d(TAG, "Created new progress for content $contentId with $validPercentage%")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating progress for content $contentId", e)
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
                Log.d(TAG, "Начинаем отмечать контент $contentId как выполненный")

                if (!contentId.startsWith("task_group_")) {
                    Log.w(TAG, "Предупреждение: contentId $contentId не соответствует формату task_group_X")
                }
                
                val timestamp = System.currentTimeMillis()
                
                val allProgress = progressDao.getProgressByUserId(userId).first()
                Log.d(TAG, "Найдено ${allProgress.size} записей прогресса")
                
                val exactMatch = allProgress.find { it.contentId == contentId }
                
                val contentEntity = contentDao.getContentByIdSync(contentId)
                
                val description = contentEntity?.description
                
                val tasksPerGroup = if (description != null) {
                    extractTaskCount(description)
                } else {
                    100
                }
                
                Log.d(TAG, "Количество заданий для $contentId: $tasksPerGroup (из description: ${description ?: "null"})")
                
                val singleTaskPercentage = if (tasksPerGroup > 0) 100 / tasksPerGroup else 1
                
                if (exactMatch != null) {
                    Log.d(TAG, "Найдено точное соответствие для $contentId, обновляем процент выполнения")
                    
                    val currentPercentage = exactMatch.percentage
                    val newPercentage = minOf(100, currentPercentage + singleTaskPercentage)
                    
                    val markCompleted = newPercentage >= 100
                    
                    if (markCompleted) {
                        progressDao.markAsCompleted(contentId, timestamp)
                        Log.d(TAG, "Задание $contentId достигло 100%, отмечено как выполненное")
                    } else {
                        progressDao.updateProgress(contentId, newPercentage, timestamp)
                        Log.d(TAG, "Обновлен процент выполнения для $contentId: $currentPercentage -> $newPercentage")
                    }
                
                val updatedProgress = progressDao.getProgressByContentId(contentId).value
                
                if (updatedProgress != null) {
                    progressSyncRepository.queueProgressUpdate(updatedProgress, syncImmediately)
                        Log.d(TAG, "Прогресс для $contentId обновлен до $newPercentage% и добавлен в очередь синхронизации")
                    } else {
                        Log.w(TAG, "Content $contentId was not found after updating progress")
                    }
                } else {
                    Log.d(TAG, "Запись для $contentId не найдена, создаем новую с начальным процентом")
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
                    
                    Log.d(TAG, "Создана новая запись прогресса для $contentId с начальным процентом $singleTaskPercentage%")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking content $contentId as completed", e)
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
            Log.e(TAG, "Ошибка при извлечении количества заданий из description: $description", e)
        }
        
        return 100
    }
} 