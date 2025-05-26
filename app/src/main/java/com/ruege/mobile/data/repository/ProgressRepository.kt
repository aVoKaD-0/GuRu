package com.ruege.mobile.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.ProgressDao
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.data.local.entity.ProgressEntity
import com.ruege.mobile.data.network.api.ProgressApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    private val contentDao: ContentDao
) {

    private val TAG = "ProgressRepository"
    
    // Мокка userId для тестирования
    private val DEFAULT_USER_ID = 1L // Теперь Long
    
    // Признак первого запуска/регистрации пользователя
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
        // Получаем поток от DAO
        return progressDao.getProgressByUserId(DEFAULT_USER_ID)
    }

    /**
     * Получает LiveData со списком прогресса пользователя.
     */
    fun getUserProgressLiveData(): LiveData<List<ProgressEntity>> {
        // Преобразуем Flow в LiveData
        return progressDao.getProgressByUserId(DEFAULT_USER_ID).asLiveData()
    }

    /**
     * Проверяет и инициализирует прогресс пользователя.
     * Если у пользователя нет прогресса, создает начальный прогресс с 0%.
     * @return true если прогресс был создан, false если прогресс уже существует
     */
    suspend fun initializeUserProgress(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем, есть ли у пользователя прогресс
                val existingProgress = progressDao.getProgressByUserId(DEFAULT_USER_ID).first()
                
                Log.d(TAG, "Проверка прогресса пользователя: найдено ${existingProgress.size} записей.")
                
                // Проверим наличие записей task_group_
                val taskGroupProgress = existingProgress.filter { it.contentId.startsWith("task_group_") }
                Log.d(TAG, "Найдено ${taskGroupProgress.size} записей task_group_")
                
                if (taskGroupProgress.isEmpty()) {
                    Log.d(TAG, "Пользователь не имеет записей прогресса для заданий. Инициализация с нуля.")
                    
                    // Получаем список всех доступных contentId, для которых уже существуют записи в таблице contents
                    val contentIds = progressSyncRepository.getAvailableContentIds() 
                    Log.d(TAG, "Получено ${contentIds.size} доступных contentIds из базы")
                    
                    // Создаем базовые записи для типов заданий ЕГЭ
                    try {
                        // Создаем прогресс для всех 27 типов заданий ЕГЭ
                        for (i in 1..27) {
                            try {
                                val taskGroupId = "task_group_$i"
                                val entity = ProgressEntity()
                                entity.setContentId(taskGroupId)
                                entity.setPercentage(0)
                                entity.setLastAccessed(System.currentTimeMillis())
                                entity.setCompleted(false)
                                entity.setTitle("Задание $i")
                                entity.setDescription("")
                                entity.setUserId(DEFAULT_USER_ID)
                                entity.setSolvedTaskIds("[]")

                                progressDao.insert(entity)
                                progressSyncRepository.queueProgressUpdate(entity, false)
                                Log.d(TAG, "Создан начальный прогресс для ${entity.getContentId()}")
                            } catch (e: Exception) {
                                Log.w(TAG, "Не удалось создать прогресс для task_group_$i: ${e.message}")
                            }
                        }
                        
                        // Дополнительно добавляем прогресс для существующего контента, если есть
                        if (contentIds.isNotEmpty()) {
                            for (contentId in contentIds.filter { !it.startsWith("task_group_") }.take(10)) {
                                try {
                                    val entity = ProgressEntity()
                                    entity.setContentId(contentId)
                                    entity.setPercentage(0)
                                    entity.setLastAccessed(System.currentTimeMillis())
                                    entity.setCompleted(false)
                                    entity.setTitle("Контент $contentId")
                                    entity.setDescription("")
                                    entity.setUserId(DEFAULT_USER_ID)
                                    entity.setSolvedTaskIds("[]")
                                    
                                    progressDao.insert(entity)
                                    progressSyncRepository.queueProgressUpdate(entity, false)
                                    Log.d(TAG, "Создан прогресс для контента ${entity.getContentId()}")
                                } catch (e: Exception) {
                                    Log.w(TAG, "Не удалось создать прогресс для $contentId: ${e.message}")
                                }
                            }
                        }
                        
                        // После создания всех записей, давайте проверим, что они действительно созданы
                        val createdProgress = progressDao.getProgressByUserId(DEFAULT_USER_ID).first()
                        val createdTaskGroups = createdProgress.filter { it.contentId.startsWith("task_group_") }
                        Log.d(TAG, "После инициализации: всего записей=${createdProgress.size}, task_group_=${createdTaskGroups.size}")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при создании базовых записей прогресса", e)
                    }
                    
                    // Помечаем, что это первый запуск пользователя
                    _isFirstTimeUser.postValue(true)
                    return@withContext true
                } else {
                    Log.d(TAG, "Пользователь уже имеет записи о прогрессе для заданий (${taskGroupProgress.size} записей).")
                    _isFirstTimeUser.postValue(false)
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при инициализации прогресса пользователя", e)
                _isFirstTimeUser.postValue(false)
                return@withContext false
            }
        }
    }

    /**
     * Обновляет данные о прогрессе из сети.
     */
    suspend fun refreshProgress() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начинаем обновление прогресса с сервера...")
                
                // Запускаем принудительную синхронизацию с сервером
                Log.d(TAG, "Вызываем forceSyncWithServer()")
                val syncResult = progressSyncRepository.forceSyncWithServer()
                
                if (!syncResult) {
                    Log.w(TAG, "Не удалось синхронизировать прогресс с сервером, загружаем локальные данные")
                    
                    // Проверяем есть ли локальные данные
                    val localProgress = progressDao.getProgressByUserId(DEFAULT_USER_ID).first()
                    
                    if (localProgress.isEmpty()) {
                        Log.d(TAG, "Локальные данные отсутствуют, инициализируем базовый прогресс")
                        initializeUserProgress()
                    } else {
                        Log.d(TAG, "Найдены локальные данные: ${localProgress.size} записей")
                    }
                } else {
                    Log.d(TAG, "Прогресс успешно синхронизирован с сервером")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении прогресса из сети", e)
                
                // Проверяем есть ли локальные данные
                try {
                    val localProgress = progressDao.getProgressByUserId(DEFAULT_USER_ID).first()
                    
                    if (localProgress.isEmpty()) {
                        Log.d(TAG, "Локальные данные отсутствуют, инициализируем базовый прогресс")
                        initializeUserProgress()
                    } else {
                        Log.d(TAG, "Используем существующие локальные данные: ${localProgress.size} записей")
                    }
                } catch (innerException: Exception) {
                    Log.e(TAG, "Ошибка при проверке локального прогресса", innerException)
                    loadMockProgress()
                }
            }
        }
    }

    /**
     * Загружает тестовые данные о прогрессе.
     */
    private suspend fun loadMockProgress() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading mock progress data...")
                
                val mockProgress = listOf(
                    // Создаем объекты через пустой конструктор и сеттеры
                    ProgressEntity().apply {
                        setContentId("1")
                        setPercentage(30)
                        setLastAccessed(System.currentTimeMillis())
                        setCompleted(false)
                        setTitle("Прогресс - Морфемика")
                        setDescription("")
                        setUserId(DEFAULT_USER_ID)
                        setSolvedTaskIds("[]")
                    },
                    ProgressEntity().apply {
                        setContentId("2")
                        setPercentage(42)
                        setLastAccessed(System.currentTimeMillis())
                        setCompleted(false)
                        setTitle("Прогресс - Синтаксис")
                        setDescription("")
                        setUserId(DEFAULT_USER_ID)
                        setSolvedTaskIds("[]")
                    },
                    ProgressEntity().apply {
                        setContentId("3")
                        setPercentage(0)
                        setLastAccessed(System.currentTimeMillis())
                        setCompleted(false)
                        setTitle("Прогресс - Пунктуация")
                        setDescription("")
                        setUserId(DEFAULT_USER_ID)
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
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing progress for user from database...")
                // Удаляем прогресс для конкретного пользователя
                progressDao.deleteByUserId(DEFAULT_USER_ID)
                
                // Удаляем записи из очереди синхронизации
                progressSyncRepository.clearByUserId(DEFAULT_USER_ID)
                
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
        withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                
                // Получаем текущий прогресс
                val currentProgress = progressDao.getProgressByContentId(contentId).value
                
                // Получаем данные о контенте для определения порога выполнения
                val contentEntity = contentDao.getContentByIdSync(contentId)
                
                // Копируем description в локальную переменную для безопасной работы
                val description = contentEntity?.description
                
                // Ограничиваем percentage значением от 0 до 100
                val validPercentage = percentage.coerceIn(0, 100)
                
                if (currentProgress != null) {
                    // Обновляем существующий прогресс
                    currentProgress.setPercentage(validPercentage)
                    currentProgress.setLastAccessed(timestamp)
                    
                    // Если достигли 100%, помечаем как завершенный
                    if (validPercentage >= 100) {
                        currentProgress.setCompleted(true)
                        Log.d(TAG, "Задание $contentId достигло 100%, отмечено как выполненное")
                    } else {
                        // Иначе убеждаемся, что не отмечено как выполненное
                        currentProgress.setCompleted(false)
                    }
                    
                    progressDao.update(currentProgress)
                    
                    // Добавляем запись в очередь синхронизации
                    progressSyncRepository.queueProgressUpdate(currentProgress, syncImmediately)
                    
                    Log.d(TAG, "Updated progress for content $contentId to $validPercentage%")
                } else {
                    // Создаем новую запись о прогрессе
                    val newProgress = ProgressEntity().apply {
                        setContentId(contentId)
                        setPercentage(validPercentage)
                        setLastAccessed(timestamp)
                        setCompleted(validPercentage >= 100) // Только если 100%, то completed = true
                        setTitle(if (contentId.startsWith("task_group_")) {
                            "Задание ${contentId.replace("task_group_", "")}"
                        } else {
                            "Прогресс $contentId"
                        })
                        setDescription(description ?: "") // По умолчанию пустая строка
                        setUserId(DEFAULT_USER_ID)
                        setSolvedTaskIds("[]")
                    }
                    
                    progressDao.insert(newProgress)
                    
                    // Добавляем запись в очередь синхронизации
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
        // Инициализируем репозиторий синхронизации
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
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начинаем отмечать контент $contentId как выполненный")

                // Защита от некорректных ID
                if (!contentId.startsWith("task_group_")) {
                    Log.w(TAG, "Предупреждение: contentId $contentId не соответствует формату task_group_X")
                }
                
                val timestamp = System.currentTimeMillis()
                
                // Получаем все записи прогресса
                val allProgress = progressDao.getProgressByUserId(DEFAULT_USER_ID).first()
                Log.d(TAG, "Найдено ${allProgress.size} записей прогресса")
                
                // Проверяем существует ли точная запись для этого contentId
                val exactMatch = allProgress.find { it.contentId == contentId }
                
                // Получаем данные о контенте для определения количества заданий
                val contentEntity = contentDao.getContentByIdSync(contentId)
                
                // Копируем description в локальную переменную для безопасной работы
                val description = contentEntity?.description
                
                // Определяем количество заданий на основе description
                val tasksPerGroup = if (description != null) {
                    extractTaskCount(description)
                } else {
                    // Значение по умолчанию, если не удалось получить из description
                    100
                }
                
                Log.d(TAG, "Количество заданий для $contentId: $tasksPerGroup (из description: ${description ?: "null"})")
                
                // Вычисляем процент за одно задание
                val singleTaskPercentage = if (tasksPerGroup > 0) 100 / tasksPerGroup else 1
                
                if (exactMatch != null) {
                    Log.d(TAG, "Найдено точное соответствие для $contentId, обновляем процент выполнения")
                    
                    // Рассчитываем новый процент выполнения
                    val currentPercentage = exactMatch.percentage
                    // Увеличиваем процент на величину одного задания, но не больше 100%
                    val newPercentage = minOf(100, currentPercentage + singleTaskPercentage)
                    
                    // Определяем, нужно ли отмечать задание как выполненное
                    val markCompleted = newPercentage >= 100
                    
                    if (markCompleted) {
                        // Если достигли 100%, отмечаем как выполненное
                        progressDao.markAsCompleted(contentId, timestamp)
                        Log.d(TAG, "Задание $contentId достигло 100%, отмечено как выполненное")
                    } else {
                        // Иначе просто обновляем процент
                        progressDao.updateProgress(contentId, newPercentage, timestamp)
                        Log.d(TAG, "Обновлен процент выполнения для $contentId: $currentPercentage -> $newPercentage")
                    }
                
                // Получаем обновленную запись для синхронизации
                val updatedProgress = progressDao.getProgressByContentId(contentId).value
                
                if (updatedProgress != null) {
                    // Добавляем запись в очередь синхронизации
                    progressSyncRepository.queueProgressUpdate(updatedProgress, syncImmediately)
                        Log.d(TAG, "Прогресс для $contentId обновлен до $newPercentage% и добавлен в очередь синхронизации")
                    } else {
                        Log.w(TAG, "Content $contentId was not found after updating progress")
                    }
                } else {
                    // Если записи нет, создаем новую с начальным процентом
                    Log.d(TAG, "Запись для $contentId не найдена, создаем новую с начальным процентом")
                    val newProgress = ProgressEntity().apply {
                        setContentId(contentId)
                        setPercentage(singleTaskPercentage) // Начальный процент за первое задание
                        setLastAccessed(timestamp)
                        setCompleted(false) // Не отмечаем как завершенное сразу
                        setTitle("Задание ${contentId.replace("task_group_", "")}")
                        setDescription(description ?: "") // По умолчанию пустая строка
                        setUserId(DEFAULT_USER_ID)
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
            // Паттерн для поиска числа перед словом "заданий" или "задание" или "задания"
            val pattern = Pattern.compile("(\\d+)\\s+(заданий|задание|задания)")
            val matcher = pattern.matcher(description)
            
            if (matcher.find()) {
                val countStr = matcher.group(1)
                return countStr.toInt()
            }
            
            // Альтернативный подход - искать просто числа в описании
            val numberPattern = Pattern.compile("(\\d+)")
            val numberMatcher = numberPattern.matcher(description)
            
            if (numberMatcher.find()) {
                val countStr = numberMatcher.group(1)
                return countStr.toInt()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при извлечении количества заданий из description: $description", e)
        }
        
        // Если не удалось распарсить, возвращаем значение по умолчанию
        return 100
    }
} 