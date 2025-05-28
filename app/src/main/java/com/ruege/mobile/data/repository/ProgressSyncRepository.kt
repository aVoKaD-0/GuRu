package com.ruege.mobile.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.ruege.mobile.data.local.AppDatabase
import com.ruege.mobile.data.local.dao.ProgressDao
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao
import com.ruege.mobile.data.local.entity.ProgressEntity
import com.ruege.mobile.data.local.entity.ProgressSyncQueueEntity
import com.ruege.mobile.data.local.entity.SyncStatus
import com.ruege.mobile.data.network.api.ProgressApiService
import com.ruege.mobile.data.network.dto.ProgressUpdateRequest
import com.ruege.mobile.data.network.dto.response.ProgressSyncItemDto
import com.ruege.mobile.util.NetworkUtils
import com.ruege.mobile.worker.ProgressSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.ruege.mobile.data.mapper.toProgressEntity
import android.content.SharedPreferences
import com.ruege.mobile.data.mapper.toProgressUpdateDto
import org.json.JSONArray
import com.ruege.mobile.data.mapper.parseJsonSolvedTaskIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.lifecycle.asFlow

/**
 * Репозиторий для управления синхронизацией прогресса между локальной базой данных и сервером
 */
@Singleton
class ProgressSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progressDao: ProgressDao,
    private val progressSyncQueueDao: ProgressSyncQueueDao,
    private val progressApiService: ProgressApiService
) {
    private val TAG = "ProgressSyncRepository"
    private val PREFS_NAME = "ProgressSyncPrefs"
    private val KEY_LAST_SYNC_TIMESTAMP = "lastSyncTimestamp"
    private val KEY_COMPLETED_TASKS_COUNTER = "completedTasksCounter"
    private val TASKS_THRESHOLD_FOR_SYNC = 20 // Порог количества заданий для синхронизации

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getLastSyncTimestamp(): Long? {
        val timestamp = sharedPreferences.getLong(KEY_LAST_SYNC_TIMESTAMP, -1L)
        return if (timestamp == -1L) null else timestamp
    }

    private fun saveLastSyncTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }
    
    private fun getCompletedTasksCounter(): Int {
        return sharedPreferences.getInt(KEY_COMPLETED_TASKS_COUNTER, 0)
    }
    
    private fun incrementCompletedTasksCounter() {
        val currentCount = getCompletedTasksCounter()
        sharedPreferences.edit().putInt(KEY_COMPLETED_TASKS_COUNTER, currentCount + 1).apply()
        
        // Проверяем, достигнут ли порог для синхронизации
        if ((currentCount + 1) >= TASKS_THRESHOLD_FOR_SYNC) {
            Log.d(TAG, "Достигнут порог в $TASKS_THRESHOLD_FOR_SYNC выполненных заданий, запускаем синхронизацию")
            
            // Запускаем синхронизацию через Worker
            syncNow(true)
            
            // Также запускаем прямую синхронизацию с сервером через API
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Пороговая синхронизация: отправка прямого запроса на синхронизацию с сервером после $TASKS_THRESHOLD_FOR_SYNC заданий")
                    
                    // Получаем все записи PENDING из очереди синхронизации (не более 100 за раз)
                    val pendingItems = progressSyncQueueDao.getItemsByStatusSync(SyncStatus.PENDING.getValue(), 100)
                    
                    if (pendingItems.isNotEmpty()) {
                        Log.d(TAG, "Пороговая синхронизация: найдено ${pendingItems.size} записей для синхронизации")
                        
                        // Превращаем каждую запись в запрос и отправляем на сервер
                        pendingItems.forEach { item ->
                            try {
                                val updateRequest = ProgressUpdateRequest(
                                    contentId = item.contentId,
                                    percentage = item.percentage,
                                    completed = item.isCompleted(),
                                    timestamp = item.timestamp,
                                    solvedTaskIds = parseJsonSolvedTaskIds(item.getSolvedTaskIds())
                                )
                                
                                val response = progressApiService.updateProgress(updateRequest)
                                
                                if (response.isSuccessful) {
                                    // Обновляем статус записи на SYNCED
                                    item.syncStatus = SyncStatus.SYNCED
                                    progressSyncQueueDao.update(item)
                                    Log.d(TAG, "Пороговая синхронизация: успешно синхронизирован ${item.contentId}")
                                } else {
                                    // В случае ошибки помечаем как FAILED
                                    item.syncStatus = SyncStatus.FAILED
                                    progressSyncQueueDao.update(item)
                                    Log.e(TAG, "Пороговая синхронизация: ошибка синхронизации ${item.contentId}, код ${response.code()}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Пороговая синхронизация: исключение при синхронизации ${item.contentId}", e)
                                // В случае исключения помечаем как FAILED
                                item.syncStatus = SyncStatus.FAILED
                                progressSyncQueueDao.update(item)
                            }
                        }
                        
                        // После обработки всех записей запрашиваем новый прогресс с сервера
                        forceSyncWithServer()
                    } else {
                        Log.d(TAG, "Пороговая синхронизация: нет записей для синхронизации, запрашиваем актуальный прогресс с сервера")
                        
                        // Даже если нет записей для синхронизации, запрашиваем новый прогресс
                        forceSyncWithServer()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Пороговая синхронизация: ошибка при выполнении прямой синхронизации", e)
                }
            }
            
            // Сбрасываем счетчик после синхронизации
            resetCompletedTasksCounter()
        } else {
            Log.d(TAG, "Счетчик заданий увеличен до ${currentCount + 1}, порог для синхронизации: $TASKS_THRESHOLD_FOR_SYNC")
        }
    }
    
    private fun resetCompletedTasksCounter() {
        sharedPreferences.edit().putInt(KEY_COMPLETED_TASKS_COUNTER, 0).apply()
        Log.d(TAG, "Счетчик выполненных заданий сброшен в 0")
    }

    /**
     * Добавляет запись об обновлении прогресса в очередь синхронизации
     * @param progress объект с данными о прогрессе
     * @param syncImmediately нужно ли запустить синхронизацию немедленно
     * @return id добавленной записи
     */
    suspend fun queueProgressUpdate(progress: ProgressEntity, syncImmediately: Boolean = false): Long = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val userId = progress.getUserId()
        
        // Получаем список решенных задач из ProgressEntity
        val solvedTaskIds = progress.getSolvedTaskIds()
        
        Log.d(TAG, "🔄 Добавление в очередь синхронизации: contentId=${progress.getContentId()}, percentage=${progress.getPercentage()}, completed=${progress.isCompleted()}, syncImmediately=$syncImmediately")
        
        // Создаем новую запись для очереди синхронизации
        val syncQueueEntity = ProgressSyncQueueEntity(
            progress.getContentId(),
            progress.getPercentage(),
            progress.isCompleted(),
            timestamp,
            userId,
            SyncStatus.PENDING,
            solvedTaskIds
        )
        
        // Проверяем, есть ли уже запись с таким же contentId
        val existingItem = progressSyncQueueDao.getItemByContentId(progress.getContentId())
        
        if (existingItem != null) {
            // Если запись уже есть, обновляем её id
            syncQueueEntity.setId(existingItem.id)
            Log.d(TAG, "🔄 Обновляем существующую запись в очереди: contentId=${progress.getContentId()}, id=${existingItem.id}")
        }
        
        // Добавляем запись в очередь
        val id = progressSyncQueueDao.insert(syncQueueEntity)
        Log.d(TAG, "🔄 Добавлен прогресс в очередь синхронизации: contentId=${progress.getContentId()}, id=$id")
        
        // Если задание выполнено (100%), увеличиваем счетчик выполненных заданий
        if (progress.isCompleted() && progress.contentId.startsWith("task_group_")) {
            incrementCompletedTasksCounter()
            Log.d(TAG, "🔄 Увеличен счетчик выполненных заданий для: ${progress.getContentId()}")
        }
        
        // Если запрошена немедленная синхронизация (при открытии или закрытии приложения), 
        // то выполняем её независимо от счетчика заданий
        if (syncImmediately && NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "🔄 Запрошена принудительная синхронизация для ${progress.getContentId()}, запускаем немедленно")
            ProgressSyncWorker.startOneTimeSync(context, true)
            // Добавляем прямой вызов syncNow для надежности
            syncNow(true, false)
            Log.d(TAG, "🔄 Прямой вызов syncNow для немедленной синхронизации contentId=${progress.getContentId()}")
        } else {
            Log.d(TAG, "🔄 Синхронизация для ${progress.getContentId()} добавлена в очередь (syncImmediately=$syncImmediately, сеть: ${NetworkUtils.isNetworkAvailable(context)})")
            
            // Проверяем счетчик заданий и запускаем синхронизацию, если нужно
            val completedCount = getCompletedTasksCounter()
            Log.d(TAG, "🔄 Текущий счетчик выполненных заданий: $completedCount")
            
            if (completedCount >= TASKS_THRESHOLD_FOR_SYNC && NetworkUtils.isNetworkAvailable(context)) {
                Log.d(TAG, "🔄 Достигнут порог заданий ($completedCount >= $TASKS_THRESHOLD_FOR_SYNC), запускаем синхронизацию")
                resetCompletedTasksCounter()
                syncNow(true, false)
            }
        }
        
        return@withContext id
    }
    
    /**
     * Получает количество записей с определенным статусом
     * @param status статус синхронизации
     * @return LiveData с количеством записей
     */
    fun getCountByStatus(status: SyncStatus): LiveData<Int> {
        return progressSyncQueueDao.getCountByStatus(status.getValue())
    }
    
    /**
     * Получает все записи с указанным статусом
     * @param status статус синхронизации
     * @return LiveData со списком записей
     */
    fun getItemsByStatus(status: SyncStatus): LiveData<List<ProgressSyncQueueEntity>> {
        return progressSyncQueueDao.getItemsByStatus(status.getValue())
    }
    
    /**
     * Получает все записи, принадлежащие пользователю
     * @param userId ID пользователя
     * @return LiveData со списком записей
     */
    fun getItemsByUserId(userId: Long): LiveData<List<ProgressSyncQueueEntity>> {
        return progressSyncQueueDao.getItemsByUserId(userId)
    }
    
    /**
     * Удаляет записи с указанным статусом из очереди
     * @param status статус синхронизации
     */
    suspend fun clearByStatus(status: SyncStatus) = withContext(Dispatchers.IO) {
        progressSyncQueueDao.deleteByStatus(status.getValue())
        Log.d(TAG, "Cleared sync queue items with status: ${status.getValue()}")
    }
    
    /**
     * Удаляет все записи пользователя из очереди
     * @param userId ID пользователя
     */
    suspend fun clearByUserId(userId: Long) = withContext(Dispatchers.IO) {
        progressSyncQueueDao.deleteByUserId(userId)
        Log.d(TAG, "Cleared sync queue items for user: $userId")
    }
    
    /**
     * Запускает фоновый процесс периодической синхронизации прогресса
     * @param intervalMinutes интервал синхронизации в минутах
     */
    fun startPeriodicSync(intervalMinutes: Long = 60) {
        ProgressSyncWorker.schedulePeriodicSync(context, intervalMinutes)
        Log.d(TAG, "Started periodic sync with interval $intervalMinutes minutes")
    }
    
    /**
     * Запускает однократную синхронизацию прогресса
     * @param expedited нужно ли выполнить синхронизацию немедленно
     * @param isAppClosing выполняется ли синхронизация при закрытии приложения
     */
    fun syncNow(expedited: Boolean = true, isAppClosing: Boolean = false) {
        // Проверяем доступность сети перед запуском синхронизации
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "Синхронизация отложена: сеть недоступна")
            return
        }
        
        // Если это закрытие приложения или требуется немедленная синхронизация,
        // выполняем пакетную синхронизацию напрямую без Worker
        if (isAppClosing || expedited) {
            Log.d(TAG, "Запуск batch-синхронизации ${if (isAppClosing) "при закрытии приложения" else "с высоким приоритетом"}")
            
            // Запускаем в отдельном потоке
            val job = GlobalScope.launch(Dispatchers.IO) {
                try {
                    // Получаем все записи PENDING из очереди синхронизации
                    val pendingItems = progressSyncQueueDao.getItemsByStatusSync(SyncStatus.PENDING.getValue(), 100)
                    
                    if (pendingItems.isNotEmpty()) {
                        Log.d(TAG, "Batch-синхронизация: найдено ${pendingItems.size} записей")
                        
                        // Пакетная обработка
                        processSyncItems(pendingItems)
                        
                        // Обновляем timestamp последней синхронизации
                        saveLastSyncTimestamp(System.currentTimeMillis())
                    } else {
                        Log.d(TAG, "Batch-синхронизация: нет записей для синхронизации")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка batch-синхронизации", e)
                }
            }
            
            // Для закрытия приложения ждем завершения с таймаутом
            if (isAppClosing) {
                try {
                    kotlinx.coroutines.runBlocking { 
                        kotlinx.coroutines.withTimeout(5000) {
                            job.join()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Batch-синхронизация: превышено время ожидания", e)
                }
            }
        } else {
            // Для неприоритетных запросов используем Worker
            ProgressSyncWorker.startOneTimeSync(context, false)
            Log.d(TAG, "Запущена фоновая синхронизация через Worker")
        }
    }
    
    /**
     * Отменяет периодическую синхронизацию прогресса
     */
    fun stopPeriodicSync() {
        ProgressSyncWorker.cancelSync(context)
        Log.d(TAG, "Stopped periodic sync")
    }
    
    /**
     * Возвращает LiveData для наблюдения за всеми элементами очереди синхронизации
     * @return LiveData со списком всех элементов очереди
     */
    fun getAllSyncItems(): LiveData<List<ProgressSyncQueueEntity>> {
        return progressSyncQueueDao.getAllItems()
    }
    
    /**
     * Инициализирует репозиторий и планирует периодическую синхронизацию.
     * Этот метод необходимо вызвать при запуске приложения.
     */
    fun initialize() {
        Log.d(TAG, "🚀 Инициализация ProgressSyncRepository начата")
        
        // Обновляем кэш для работы с репозиторием
        try {
            GlobalScope.launch(Dispatchers.IO) {
                // Подсчитываем количество записей в очереди
                val pendingCount = progressSyncQueueDao.getCountByStatusSync(SyncStatus.PENDING.getValue())
                val failedCount = progressSyncQueueDao.getCountByStatusSync(SyncStatus.FAILED.getValue())
                val totalCount = progressSyncQueueDao.getAllItemsSync().size
                
                Log.d(TAG, "🚀 Статистика очереди синхронизации: pending=$pendingCount, failed=$failedCount, всего=$totalCount")
                
                // Планируем периодическую синхронизацию
                ProgressSyncWorker.schedulePeriodicSync(context)
                Log.d(TAG, "🚀 Периодическая синхронизация запланирована")
                
                // Если есть ожидающие записи, запускаем синхронизацию сразу
                if (pendingCount > 0 && NetworkUtils.isNetworkAvailable(context)) {
                    Log.d(TAG, "🚀 Обнаружены ожидающие записи ($pendingCount), запускаем немедленную синхронизацию")
                    syncNow(true)
                } else {
                    Log.d(TAG, "🚀 Нет записей для немедленной синхронизации или отсутствует подключение к сети")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🚀 Ошибка при инициализации ProgressSyncRepository", e)
        }
        
        Log.d(TAG, "🚀 Инициализация ProgressSyncRepository завершена")
    }
    
    /**
     * Принудительно синхронизирует прогресс пользователя с сервером
     * @return true, если синхронизация прошла успешно
     */
    suspend fun forceSyncWithServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "⚡ Начинаем принудительную синхронизацию с сервером")
            
            if (!NetworkUtils.isNetworkAvailable(context)) {
                Log.w(TAG, "⚠️ Сеть недоступна, невозможно синхронизироваться с сервером")
                return@withContext false
            }
            
            // Сначала проверяем наличие элементов в очереди и отправляем их
            val pendingItems = progressSyncQueueDao.getItemsByStatusSync(SyncStatus.PENDING.getValue(), 100)
            if (pendingItems.isNotEmpty()) {
                Log.d(TAG, "🔄 Найдено ${pendingItems.size} элементов в очереди, отправляем перед запросом синхронизации")
                val syncSuccess = processSyncItems(pendingItems)
                if (!syncSuccess) {
                    Log.w(TAG, "⚠️ Не удалось отправить элементы из очереди")
                }
            } else {
                Log.d(TAG, "ℹ️ Очередь синхронизации пуста, нечего отправлять")
            }
            
            val lastTimestamp = getLastSyncTimestamp()
            Log.d(TAG, "🕒 Запрашиваем синхронизацию прогресса с timestamp: $lastTimestamp")
            
            try {
                // Получаем прогресс с сервера, передавая lastTimestamp
                val response = progressApiService.syncProgress(lastTimestamp)
                
                if (response.isSuccessful) {
                    val serverProgressDtoList = response.body()
                    
                    if (serverProgressDtoList != null) {
                        if (serverProgressDtoList.isNotEmpty()) {
                            Log.d(TAG, "✅ Получено ${serverProgressDtoList.size} записей прогресса с сервера")
                            
                            val entitiesToInsert = serverProgressDtoList.mapNotNull { dto ->
                                dto.toProgressEntity()
                            }
                            
                            if (entitiesToInsert.isNotEmpty()) {
                                // Получаем список существующих ID контента и их временные метки
                                val existingProgressMap = progressDao.getAllProgressListSync().associateBy { it.getContentId() }

                                val toUpdate = mutableListOf<ProgressEntity>()
                                val toInsert = mutableListOf<ProgressEntity>()
                                val toQueueForSync = mutableListOf<ProgressEntity>()

                                for (serverEntity in entitiesToInsert) {
                                    val localEntity = existingProgressMap[serverEntity.getContentId()]

                                    if (localEntity != null) {
                                        // Запись существует локально
                                        if (serverEntity.getLastAccessed() > localEntity.getLastAccessed()) {
                                            // Данные на сервере новее или такие же
                                            // Объединяем solvedTaskIds
                                            val localSolvedIds = localEntity.getSolvedTaskIdsList().toMutableSet()
                                            val serverSolvedIds = serverEntity.getSolvedTaskIdsList()
                                            localSolvedIds.addAll(serverSolvedIds)
                                            
                                            val mergedEntity = serverEntity // Берем за основу серверную, т.к. она новее в целом
                                            mergedEntity.setSolvedTaskIds(ProgressEntity.listToJsonString(localSolvedIds.toList()))
                                            // Пересчитываем процент и completed на основе объединенных solvedTaskIds
                                            val totalTasks = getTotalTasksCount(mergedEntity.getContentId()) // Нужна эта функция
                                            val newPercentage = calculatePercentage(localSolvedIds.size, totalTasks) // Нужна эта функция
                                            mergedEntity.setPercentage(newPercentage)
                                            mergedEntity.setCompleted(newPercentage >= 100)                                           
                                            // Убедимся, что lastAccessed берется максимальный из двух
                                            mergedEntity.setLastAccessed(maxOf(serverEntity.getLastAccessed(), localEntity.getLastAccessed()))
                                            toUpdate.add(mergedEntity)
                                            Log.d(TAG, "✅ Обновление с объединением для ${serverEntity.getContentId()}, сервер новее или равен. Сервер: ${serverEntity.getLastAccessed()}, Локально: ${localEntity.getLastAccessed()}. Объединенные ID: ${localSolvedIds.size}")
                                        } else {
                                            // Локальные данные новее, их нужно отправить на сервер
                                            // Не обновляем локальные данные серверными, а ставим локальные в очередь на синхронизацию
                                            toQueueForSync.add(localEntity)
                                            Log.d(TAG, "✅ Локальные данные для ${localEntity.getContentId()} новее серверных. Сервер: ${serverEntity.getLastAccessed()}, Локально: ${localEntity.getLastAccessed()}. Будет добавлено в очередь.")
                                        }
                                    } else {
                                        // Новая запись с сервера
                                        toInsert.add(serverEntity)
                                        Log.d(TAG, "✅ Новая запись с сервера для ${serverEntity.getContentId()}")
                                    }
                                }
                                
                                // Обновляем существующие записи
                                if (toUpdate.isNotEmpty()) {
                                    progressDao.updateAll(toUpdate)
                                    Log.d(TAG, "✅ Успешно обновлено ${toUpdate.size} существующих записей прогресса после объединения")
                                }
                                
                                // Вставляем новые записи
                                if (toInsert.isNotEmpty()) {
                                    progressDao.insertAll(toInsert)
                                    Log.d(TAG, "✅ Успешно добавлено ${toInsert.size} новых записей прогресса")
                                }

                                // Добавляем в очередь на синхронизацию те локальные записи, что оказались новее серверных
                                if (toQueueForSync.isNotEmpty()) {
                                    for (entityToSync in toQueueForSync) {
                                        queueProgressUpdate(entityToSync, true) // syncImmediately = true
                                    }
                                    Log.d(TAG, "✅ Добавлено в очередь на синхронизацию ${toQueueForSync.size} локально обновленных записей")
                                }
                                
                                Log.d(TAG, "✅ Всего обработано ${entitiesToInsert.size} записей прогресса (обновлено с объединением: ${toUpdate.size}, новых: ${toInsert.size}, поставлено в очередь: ${toQueueForSync.size})")
                            }
                            
                            // Обновляем метку времени последней синхронизации
                            val maxTimestamp = serverProgressDtoList.mapNotNull { it.timestamp }.maxOrNull() ?: System.currentTimeMillis()
                            saveLastSyncTimestamp(maxTimestamp)
                            Log.d(TAG, "🕒 Сохранена новая метка времени синхронизации: $maxTimestamp")
                        } else {
                            Log.d(TAG, "ℹ️ Сервер вернул пустой список прогресса")
                            
                            // Если это был полный запрос, сохраняем текущее время
                            if (lastTimestamp == null) {
                                val currentTime = System.currentTimeMillis()
                                saveLastSyncTimestamp(currentTime)
                                Log.d(TAG, "🕒 Сохранена текущая метка времени: $currentTime")
                            }
                        }
                        return@withContext true
                    } else {
                        Log.w(TAG, "⚠️ Сервер вернул null вместо списка прогресса")
                    }
                } else {
                    Log.e(TAG, "🚫 Ошибка при запросе прогресса: ${response.code()} ${response.message()}")
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "🚫 Тело ошибки: ${response.errorBody()?.string()}")
                        } catch (e: Exception) {
                            Log.e(TAG, "🚫 Не удалось прочитать тело ошибки", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "🚫 Ошибка сети при синхронизации прогресса", e)
                e.printStackTrace()
            }
            
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "🚫 Общая ошибка при forceSyncWithServer", e)
            e.printStackTrace()
            return@withContext false
        }
    }
    
    /**
     * Синхронизирует указанный элемент прогресса немедленно (синхронно)
     * @param contentId ID контента для синхронизации
     * @return true, если синхронизация прошла успешно
     */
    suspend fun syncContentImmediately(contentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Проверяем, есть ли сеть
            if (!NetworkUtils.isNetworkAvailable(context)) {
                Log.w(TAG, "Network not available, can't sync content $contentId")
                return@withContext false
            }
            
            // Загружаем прогресс для этого контента
            val progressEntity = progressDao.getProgressByContentId(contentId).value
            
            if (progressEntity != null) {
                // Создаем DTO для запроса
                val updateRequest = ProgressUpdateRequest(
                    contentId = progressEntity.getContentId(),
                    percentage = progressEntity.getPercentage(),
                    completed = progressEntity.isCompleted(),
                    timestamp = progressEntity.getLastAccessed(),
                    solvedTaskIds = parseJsonSolvedTaskIds(progressEntity.getSolvedTaskIds())
                )
                
                // Отправляем запрос на сервер
                val response = progressApiService.updateProgress(updateRequest)
                
                if (response.isSuccessful) {
                    val syncResponse = response.body()
                    
                    if (syncResponse != null && syncResponse.success) {
                        Log.d(TAG, "Successfully synced content $contentId with server")
                        return@withContext true
                    } else {
                        Log.w(TAG, "Server rejected sync for content $contentId: ${syncResponse?.message}")
                    }
                } else {
                    Log.e(TAG, "Failed to sync content $contentId: ${response.code()} ${response.message()}")
                }
            } else {
                Log.w(TAG, "No local progress found for content $contentId")
            }
            
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error during immediate sync for content $contentId", e)
            return@withContext false
        }
    }

    /**
     * Получает список доступных ID контента из базы данных.
     * Используется для инициализации прогресса только для существующих элементов контента.
     * @return Список ID контента
     */
    suspend fun getAvailableContentIds(): List<String> = withContext(Dispatchers.IO) {
        try {
            // Получаем список ID контента из ContentDao
            val db = AppDatabase.getInstance(context)
            val contentIds = db.contentDao().getAllContentIds()
            
            // Если нет записей, возвращаем базовые ID для типов заданий
            if (contentIds.isEmpty()) {
                Log.w(TAG, "В таблице contents нет записей, используем фиксированный список ID для типов заданий")
                return@withContext getLocalTaskTypeContentIds()
            }
            
            Log.d(TAG, "Получены доступные ID контента: ${contentIds.size}")
            return@withContext contentIds
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении доступных ID контента", e)
            // В случае ошибки возвращаем пустой список
            return@withContext emptyList()
        }
    }

    private suspend fun getLocalTaskTypeContentIds(): List<String> {
        return withContext(Dispatchers.IO) {
            // Эта логика может быть заменена на запрос к ContentDao, если типы заданий хранятся там
            (1..27).map { "task_group_$it" } // Изменено task_type_ на task_group_
        }
    }

    /**
     * Обрабатывает элементы очереди синхронизации пакетом
     * @param items список элементов для синхронизации
     * @return true, если синхронизация прошла успешно
     */
    private suspend fun processSyncItems(items: List<ProgressSyncQueueEntity>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) {
            Log.d(TAG, "Нет элементов для синхронизации")
            return@withContext true
        }
        
        Log.d(TAG, "📊 Начинаем обработку пакетной синхронизации для ${items.size} элементов")
        
        try {
            // Группируем элементы по contentId для избежания дублирования
            val groupedItems = items.groupBy { it.contentId }
            
            // Для каждого contentId берем самый свежий элемент
            val latestItems = groupedItems.mapValues { (_, items) -> 
                items.maxByOrNull { it.timestamp } 
            }.values.filterNotNull()
            
            Log.d(TAG, "📊 Обработка ${latestItems.size} элементов прогресса для batch-синхронизации (было ${items.size})")
            
            // Получаем все нужные progressEntity из базы за один запрос
            val contentIds = latestItems.map { it.contentId }
            Log.d(TAG, "📊 ContentIds для синхронизации: $contentIds")
            val progressEntities = progressDao.getProgressByContentIdsSync(contentIds)
            
            // Создаем карту contentId -> progressEntity для быстрого поиска
            val progressEntityMap = progressEntities.associateBy { it.getContentId() }
            
            // Создаем запросы для обновления, используя кэшированные данные
            val updateRequests = latestItems.map { item ->
                // Ищем объект в карте
                val progressEntity = progressEntityMap[item.contentId]
                
                // Если прогресс найден, используем его данные, иначе создаем из элемента очереди
                if (progressEntity != null) {
                    Log.d(TAG, "📊 Используем данные из progressEntity для ${item.contentId}, percentage=${progressEntity.getPercentage()}, completed=${progressEntity.isCompleted()}")
                    toProgressUpdateDto(progressEntity)
                } else {
                    Log.d(TAG, "📊 Создаем запрос из очереди для ${item.contentId}, percentage=${item.percentage}, completed=${item.isCompleted()}")
                    ProgressUpdateRequest(
                        contentId = item.contentId,
                        percentage = item.percentage,
                        completed = item.isCompleted(),
                        timestamp = item.timestamp,
                        solvedTaskIds = parseJsonSolvedTaskIds(item.getSolvedTaskIds())
                    )
                }
            }
            
            if (updateRequests.isEmpty()) {
                Log.d(TAG, "Нет запросов для обновления после фильтрации")
                return@withContext true
            }
            
            Log.d(TAG, "📊 Отправляем batch-запрос на сервер для ${updateRequests.size} элементов")
            // Логируем запросы для диагностики
            updateRequests.forEach { request ->
                Log.d(TAG, "📊 Запрос: contentId=${request.contentId}, percentage=${request.percentage}, completed=${request.completed}")
            }
            
            // Проверяем сетевое подключение перед отправкой
            if (!NetworkUtils.isNetworkAvailable(context)) {
                Log.w(TAG, "❌ Сеть недоступна. Синхронизация отложена.")
                return@withContext false
            }
            
            Log.d(TAG, "📊 Отправка запроса на URL: ${progressApiService.javaClass.name}")
            
            // Отправляем batch-запрос на сервер
            val response = progressApiService.updateProgressBatch(updateRequests)
            
            Log.d(TAG, "📊 Получен ответ от сервера: isSuccessful=${response.isSuccessful}, code=${response.code()}")
            
            if (response.isSuccessful) {
                val responseList = response.body()
                
                if (responseList != null) {
                    Log.d(TAG, "📊 Получен список ответов от сервера: size=${responseList.size}")
                    // Создаем карту contentId -> ответ для быстрого поиска
                    val responseMap = responseList.associateBy { it.contentId }
                    
                    // Обновляем статус всех исходных элементов
                    for (item in items) {
                        // Находим соответствующий ответ по content_id
                        val itemResponse = responseMap[item.contentId]
                        
                        // Если нашли ответ для этого элемента и он успешный, помечаем как SYNCED
                        if (itemResponse != null && itemResponse.success) {
                            item.syncStatus = SyncStatus.SYNCED
                        } else {
                            // Если элемент не найден в ответах или неуспешный, помечаем как FAILED
                            item.syncStatus = SyncStatus.FAILED
                        }
                        
                        progressSyncQueueDao.update(item)
                    }
                    
                    val allSuccess = responseList.all { it.success }
                    Log.d(TAG, "Batch-синхронизация завершена, обновлено ${items.size} элементов, все успешно: $allSuccess")
                    return@withContext true
                } else {
                    Log.e(TAG, "Сервер вернул пустой список ответов при batch-синхронизации")
                }
            } else {
                Log.e(TAG, "Ошибка при batch-синхронизации: ${response.code()} ${response.message()}")
                
                // Если это проблема авторизации, пометим все как FAILED
                if (response.code() == 401 || response.code() == 403) {
                    for (item in items) {
                        item.syncStatus = SyncStatus.FAILED
                        progressSyncQueueDao.update(item)
                    }
                }
            }
            
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при batch-синхронизации", e)
            return@withContext false
        }
    }

    /**
     * Добавляет решенное задание в прогресс пользователя
     * @param taskGroupId ID группы заданий (например, task_group_1)
     * @param solvedTaskId ID конкретного решенного задания
     * @param syncImmediately нужно ли немедленно синхронизировать
     */
    suspend fun addSolvedTask(taskGroupId: String, solvedTaskId: String, syncImmediately: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            // Получаем текущий прогресс
            var progressEntity = progressDao.getProgressByContentIdSync(taskGroupId)

            // Если сущность ProgressEntity не найдена, создаем новую
            if (progressEntity == null) {
                Log.d(TAG, "Прогресс для группы $taskGroupId не найден, создаем новый.")
                // Важно: Убедитесь, что у вас есть конструктор или фабричный метод для ProgressEntity,
                // который принимает taskGroupId и устанавливает начальные значения.
                // Предполагается, что ProgressEntity имеет конструктор или метод для инициализации с contentId.
                // Если у вас есть поле userId в ProgressEntity, его также нужно установить.
                // Пример: progressEntity = ProgressEntity(contentId = taskGroupId, userId = getCurrentUserId())
                // Для простоты, предположим, что ProgressEntity может быть создан с taskGroupId
                // и остальные поля будут установлены ниже или имеют значения по умолчанию.
                // Вам нужно будет адаптировать эту часть под вашу структуру ProgressEntity.
                // ВАЖНО: Убедитесь, что вы правильно устанавливаете userId, если он используется.
                // В данном примере userId не устанавливается явно, предполагая, что он либо не нужен
                // для этой операции, либо будет установлен в другом месте, либо ProgressEntity() его обработает.
                progressEntity = ProgressEntity() // Убедитесь, что этот конструктор корректен или используйте подходящий
                progressEntity.setContentId(taskGroupId)
                // Устанавливаем начальные значения для новой сущности, если это необходимо
                progressEntity.setPercentage(0)
                progressEntity.setCompleted(false)
                progressEntity.setLastAccessed(System.currentTimeMillis())
                progressEntity.setSolvedTaskIds("[]") // Пустой JSON массив для solvedTaskIds

                // Сохраняем новую сущность в БД перед тем, как добавлять в нее решенные задания
                // Это важно, так как дальнейшая логика может полагаться на существующую запись
                progressDao.insert(progressEntity)
                Log.d(TAG, "Новая сущность ProgressEntity для $taskGroupId создана и сохранена.")
                // Перезагружаем сущность из БД, чтобы убедиться, что работаем с актуальной версией
                // (особенно если insert возвращает id или есть авто-инкрементные поля)
                progressEntity = progressDao.getProgressByContentIdSync(taskGroupId)
                if (progressEntity == null) {
                    Log.e(TAG, "Не удалось создать или получить ProgressEntity для $taskGroupId после insert.")
                    return@withContext false // Выходим, если создание не удалось
                }
            }
            
            // Получаем текущий список решенных заданий
            val currentSolved = progressEntity.getSolvedTaskIdsList().toMutableList()
            
            // Проверяем, не добавлено ли уже это задание
            if (!currentSolved.contains(solvedTaskId)) {
                // Добавляем новое решенное задание
                currentSolved.add(solvedTaskId)
                
                // Получаем общее количество заданий для данного типа
                val totalTasksCount = getTotalTasksCount(taskGroupId)
                Log.d(TAG, "Общее количество заданий для группы $taskGroupId: $totalTasksCount")
                
                // Рассчитываем процент выполнения на основе количества решенных заданий
                val newPercentage = calculatePercentage(currentSolved.size, totalTasksCount)
                val newLastAccessed = System.currentTimeMillis()
                val newSolvedTaskIds = ProgressEntity.listToJsonString(currentSolved)
                
                // Обновляем существующую сущность вместо создания новой
                progressEntity.setPercentage(newPercentage)
                progressEntity.setLastAccessed(newLastAccessed)
                progressEntity.setCompleted(newPercentage >= 100)
                progressEntity.setSolvedTaskIds(newSolvedTaskIds)
                
                // Сохраняем в локальную БД (обновляем, так как сущность уже существует или только что создана)
                progressDao.update(progressEntity)
                
                // Добавляем в очередь синхронизации
                queueProgressUpdate(progressEntity, syncImmediately)
                
                Log.d(TAG, "Добавлено решенное задание $solvedTaskId в группу $taskGroupId. Текущий прогресс: $newPercentage%")
                return@withContext true
            } else {
                Log.d(TAG, "Задание $solvedTaskId уже отмечено как решенное в группе $taskGroupId")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении решенного задания", e)
            return@withContext false
        }
    }
    
    /**
     * Возвращает общее количество заданий для конкретного типа заданий
     * @param taskGroupId ID группы заданий
     * @return общее количество заданий в группе
     */
    private fun getTotalTasksCount(taskGroupId: String): Int {
        try {
            // Получаем информацию о контенте из базы данных
            val contentEntity = AppDatabase.getInstance(context).contentDao().getContentByIdSync(taskGroupId)
            
            if (contentEntity != null && contentEntity.description != null) {
                // Извлекаем количество заданий из описания
                val description = contentEntity.description
                Log.d(TAG, "Описание для $taskGroupId: $description")
                
                // Паттерн для поиска числа перед словом "заданий" или "задание" или "задания"
                val pattern = java.util.regex.Pattern.compile("(\\d+)\\s+(заданий|задание|задания)")
                val matcher = pattern.matcher(description)
                
                if (matcher.find()) {
                    val countStr = matcher.group(1)
                    val count = countStr.toInt()
                    Log.d(TAG, "Извлечено количество заданий для $taskGroupId: $count")
                    return count
                }
                
                // Альтернативный подход - искать просто числа в описании
                val numberPattern = java.util.regex.Pattern.compile("(\\d+)")
                val numberMatcher = numberPattern.matcher(description)
                
                if (numberMatcher.find()) {
                    val countStr = numberMatcher.group(1)
                    val count = countStr.toInt()
                    Log.d(TAG, "Извлечено число из описания для $taskGroupId: $count")
                    return count
                }
            }
            
            // Если не удалось получить данные из базы или распарсить описание,
            // используем предопределенные значения как запасной вариант
            Log.d(TAG, "Используем предопределенные значения для $taskGroupId")
            val groupNumber = taskGroupId.replace("task_group_", "").toIntOrNull() ?: 0
            
            return when (groupNumber) {
                1 -> 10  // Предположим, что в задании 1 - 10 подзаданий
                2 -> 8   // В задании 2 - 8 подзаданий
                3 -> 15  // и т.д.
                4 -> 12
                5 -> 99  // В задании 5 - 99 подзаданий
                6 -> 8
                7 -> 10
                8 -> 12
                9 -> 15
                10 -> 10
                11 -> 8
                12 -> 10
                13 -> 12
                14 -> 8
                15 -> 10
                16 -> 12
                17 -> 10
                18 -> 12
                19 -> 10
                20 -> 8
                21 -> 10
                22 -> 8
                23 -> 15
                24 -> 10
                25 -> 10
                26 -> 12
                27 -> 1   // Последнее задание (сочинение) обычно одно
                else -> 10 // По умолчанию предполагаем 10 заданий
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении количества заданий для $taskGroupId", e)
            return 10 // Значение по умолчанию в случае ошибки
        }
    }
    
    /**
     * Рассчитывает процент выполнения на основе количества решенных заданий
     * @param solvedCount количество решенных заданий
     * @param totalCount общее количество заданий
     * @return процент выполнения (от 0 до 100)
     */
    private fun calculatePercentage(solvedCount: Int, totalCount: Int): Int {
        if (totalCount <= 0) return 0
        return ((solvedCount.toFloat() / totalCount) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Преобразует сущность прогресса в DTO для обновления на сервере
     */
    private fun toProgressUpdateDto(progressEntity: ProgressEntity): ProgressUpdateRequest {
        // Преобразуем JSON-строку с решенными заданиями в список
        val solvedTaskIds = progressEntity.getSolvedTaskIdsList().takeIf { it.isNotEmpty() }
        
        return ProgressUpdateRequest(
            contentId = progressEntity.getContentId(),
            percentage = progressEntity.getPercentage(),
            completed = progressEntity.isCompleted(),
            timestamp = progressEntity.getLastAccessed(),
            solvedTaskIds = solvedTaskIds
        )
    }

    /**
     * Получает поток ID решенных задач для указанной категории ЕГЭ.
     * @param categoryId ID категории (например, "1", "2", ... "27")
     * @return Flow, который эмитит список ID решенных задач (List<String>)
     */
    fun getSolvedTaskIdsForEgeCategory(categoryId: String): kotlinx.coroutines.flow.Flow<List<String>> {
        val contentId = "task_group_$categoryId"

        // Используем asFlow() для преобразования LiveData<ProgressEntity> в Flow<ProgressEntity?>
        return progressDao.getProgressByContentId(contentId).asFlow()
            .map { entity: ProgressEntity? -> // Явно указываем тип entity как ProgressEntity?
                if (entity != null && !entity.getSolvedTaskIds().isNullOrEmpty()) {
                    try {
                        // parseJsonSolvedTaskIds может вернуть null, поэтому обеспечиваем возврат emptyList() в этом случае
                        parseJsonSolvedTaskIds(entity.getSolvedTaskIds()) ?: emptyList<String>()
                    } catch (e: org.json.JSONException) {
                        Log.e(TAG, "Ошибка JSON парсинга solvedTaskIds для contentId $contentId: ${entity.getSolvedTaskIds()}", e)
                        emptyList<String>()
                    } catch (e: Exception) {
                        Log.e(TAG, "Общая ошибка парсинга solvedTaskIds для contentId $contentId: ${entity.getSolvedTaskIds()}", e)
                        emptyList<String>()
                    }
                } else {
                    emptyList<String>()
                }
            }
    }
} 