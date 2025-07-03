package com.ruege.mobile.data.repository

import android.content.Context
import timber.log.Timber
import androidx.lifecycle.LiveData
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.ProgressDao
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao
import com.ruege.mobile.data.local.entity.ProgressEntity
import com.ruege.mobile.data.local.entity.ProgressSyncQueueEntity
import com.ruege.mobile.data.local.entity.SyncStatus
import com.ruege.mobile.data.network.api.ProgressApiService
import com.ruege.mobile.data.network.dto.ProgressUpdateRequest
import com.ruege.mobile.utils.NetworkUtils
import com.ruege.mobile.worker.ProgressSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.ruege.mobile.data.mapper.toProgressEntity
import android.content.SharedPreferences
import com.ruege.mobile.data.mapper.parseJsonSolvedTaskIds
import kotlinx.coroutines.flow.map
import androidx.lifecycle.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.network.api.PracticeApiService
import com.ruege.mobile.data.network.dto.request.PracticeStatisticSyncDto
import com.ruege.mobile.data.network.dto.request.PracticeStatisticsBranchRequest
import com.ruege.mobile.data.local.dao.TaskDao

/**
 * Репозиторий для управления синхронизацией прогресса между локальной базой данных и сервером
 */
@Singleton
class ProgressSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progressDao: ProgressDao,
    private val progressSyncQueueDao: ProgressSyncQueueDao,
    private val progressApiService: ProgressApiService,
    private val practiceApiService: PracticeApiService,
    private val contentDao: ContentDao,
    private val userDao: UserDao,
    private val practiceStatisticsDao: PracticeStatisticsDao,
    private val practiceSyncRepository: PracticeSyncRepository,
    private val taskDao: TaskDao
) {
    private val TAG = "ProgressSyncRepository"
    private val PREFS_NAME = "ProgressSyncPrefs"
    private val KEY_LAST_SYNC_TIMESTAMP = "lastSyncTimestamp"
    private val KEY_LAST_STATS_SYNC_TIMESTAMP = "lastStatsSyncTimestamp"
    private val KEY_COMPLETED_TASKS_COUNTER = "completedTasksCounter"
    private val TASKS_THRESHOLD_FOR_SYNC = 20

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    private fun getLastStatsSyncTimestamp(): Long? {
        val timestamp = sharedPreferences.getLong(KEY_LAST_STATS_SYNC_TIMESTAMP, -1L)
        return if (timestamp == -1L) null else timestamp
    }

    private fun saveLastStatsSyncTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_STATS_SYNC_TIMESTAMP, timestamp).apply()
    }
    
    private fun getCompletedTasksCounter(): Int {
        return sharedPreferences.getInt(KEY_COMPLETED_TASKS_COUNTER, 0)
    }
    
    private fun incrementCompletedTasksCounter() {
        val currentCount = getCompletedTasksCounter()
        sharedPreferences.edit().putInt(KEY_COMPLETED_TASKS_COUNTER, currentCount + 1).apply()
        
        if ((currentCount + 1) >= TASKS_THRESHOLD_FOR_SYNC) {
            Timber.d("Достигнут порог в $TASKS_THRESHOLD_FOR_SYNC выполненных заданий, запускаем синхронизацию")
            
            syncNow(true)
            
            repositoryScope.launch {
                try {
                    Timber.d("Пороговая синхронизация: отправка прямого запроса на синхронизацию с сервером после $TASKS_THRESHOLD_FOR_SYNC заданий")
                    
                    val pendingItems = progressSyncQueueDao.getItemsByStatusSync(SyncStatus.PENDING.getValue(), 100)
                    
                    if (pendingItems.isNotEmpty()) {
                        Timber.d("Пороговая синхронизация: найдено ${pendingItems.size} записей для синхронизации")
                        
                        pendingItems.forEach { item ->
                            try {
                                val updateRequest = ProgressUpdateRequest(
                                    contentId = item.itemId,
                                    percentage = item.percentage,
                                    completed = item.isCompleted(),
                                    timestamp = item.timestamp,
                                    solvedTaskIds = parseJsonSolvedTaskIds(item.getSolvedTaskIds())
                                )
                                
                                val response = progressApiService.updateProgress(updateRequest)
                                
                                if (response.isSuccessful) {
                                    item.syncStatus = SyncStatus.SYNCED
                                    progressSyncQueueDao.update(item)
                                    Timber.d("Пороговая синхронизация: успешно синхронизирован ${item.itemId}")
                                } else {
                                    item.syncStatus = SyncStatus.FAILED
                                    progressSyncQueueDao.update(item)
                                    Timber.d("Пороговая синхронизация: ошибка синхронизации ${item.itemId}, код ${response.code()}")
                                }
                            } catch (e: Exception) {
                                Timber.d("Пороговая синхронизация: исключение при синхронизации ${item.itemId}", e)
                                item.syncStatus = SyncStatus.FAILED
                                progressSyncQueueDao.update(item)
                            }
                        }
                        
                        forceSyncWithServer()
                    } else {
                        Timber.d("Пороговая синхронизация: нет записей для синхронизации, запрашиваем актуальный прогресс с сервера")
                        
                        forceSyncWithServer()
                    }
                } catch (e: Exception) {
                    Timber.d("Пороговая синхронизация: ошибка при выполнении прямой синхронизации", e)
                }
            }
            
            resetCompletedTasksCounter()
        } else {
            Timber.d("Счетчик заданий увеличен до ${currentCount + 1}, порог для синхронизации: $TASKS_THRESHOLD_FOR_SYNC")
        }
    }
    
    private fun resetCompletedTasksCounter() {
        sharedPreferences.edit().putInt(KEY_COMPLETED_TASKS_COUNTER, 0).apply()
        Timber.d("Счетчик выполненных заданий сброшен в 0")
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
        
        val solvedTaskIds = progress.getSolvedTaskIds()
        
        Timber.d("🔄 Добавление в очередь синхронизации: itemId=${progress.getContentId()}, percentage=${progress.getPercentage()}, completed=${progress.isCompleted()}, syncImmediately=$syncImmediately")
        
        val syncQueueEntity = ProgressSyncQueueEntity(
            progress.getContentId(),
            ProgressSyncQueueEntity.ITEM_TYPE_PROGRESS,
            progress.getPercentage(),
            progress.isCompleted(),
            timestamp,
            userId,
            SyncStatus.PENDING,
            solvedTaskIds
        )
        
        val existingItem = progressSyncQueueDao.getItemByItemId(progress.getContentId())
        
        if (existingItem != null) {
            syncQueueEntity.setId(existingItem.id)
            Timber.d("🔄 Обновляем существующую запись в очереди: itemId=${progress.getContentId()}, id=${existingItem.id}")
        }
        
        val id = progressSyncQueueDao.insert(syncQueueEntity)
        Timber.d("🔄 Добавлен прогресс в очередь синхронизации: itemId=${progress.getContentId()}, id=$id")
        
        if (progress.isCompleted() && progress.contentId.startsWith("task_group_")) {
            incrementCompletedTasksCounter()
            Timber.d("🔄 Увеличен счетчик выполненных заданий для: ${progress.getContentId()}")
        }
        
        if (syncImmediately && NetworkUtils.isNetworkAvailable(context)) {
            Timber.d("🔄 Запрошена принудительная синхронизация для ${progress.getContentId()}, запускаем немедленно")
            ProgressSyncWorker.startOneTimeSync(context, true)
            syncNow(true, false)
            Timber.d("🔄 Прямой вызов syncNow для немедленной синхронизации contentId=${progress.getContentId()}")
        } else {
            Timber.d("🔄 Синхронизация для ${progress.getContentId()} добавлена в очередь (syncImmediately=$syncImmediately, сеть: ${NetworkUtils.isNetworkAvailable(context)})")
            
            val completedCount = getCompletedTasksCounter()
            Timber.d("🔄 Текущий счетчик выполненных заданий: $completedCount")
            
            if (completedCount >= TASKS_THRESHOLD_FOR_SYNC && NetworkUtils.isNetworkAvailable(context)) {
                Timber.d("🔄 Достигнут порог заданий ($completedCount >= $TASKS_THRESHOLD_FOR_SYNC), запускаем синхронизацию")
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
     * Удаляет записи с указанным статусом из очереди
     * @param status статус синхронизации
     */
    suspend fun clearByStatus(status: SyncStatus) = withContext(Dispatchers.IO) {
        progressSyncQueueDao.deleteByStatus(status.getValue())
        Timber.d("Cleared sync queue items with status: ${status.getValue()}")
    }
    
    /**
     * Удаляет все записи пользователя из очереди
     * @param userId ID пользователя
     */
    suspend fun clearByUserId(userId: Long) = withContext(Dispatchers.IO) {
        progressSyncQueueDao.deleteByUserId(userId)
        Timber.d("Cleared sync queue items for user: $userId")
    }
    
    /**
     * Запускает однократную синхронизацию прогресса
     * @param expedited нужно ли выполнить синхронизацию немедленно
     * @param isAppClosing выполняется ли синхронизация при закрытии приложения
     */
    fun syncNow(expedited: Boolean = false, isAppClosing: Boolean = false) {
        Timber.d("syncNow called with expedited: $expedited, isAppClosing: $isAppClosing")
        if (isAppClosing) {
            ProgressSyncWorker.startOneTimeSync(context, expedited = false, isExitSync = true)
            Timber.d("Exit sync has been scheduled using WorkManager.")
        } else {
            ProgressSyncWorker.startOneTimeSync(context, expedited = expedited, isExitSync = false)
            Timber.d("One-time sync has been scheduled using WorkManager.")
        }
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
        Timber.d("🚀 Инициализация ProgressSyncRepository начата")
        
        try {
            repositoryScope.launch {
                val pendingCount = progressSyncQueueDao.getCountByStatusSync(SyncStatus.PENDING.getValue())
                val failedCount = progressSyncQueueDao.getCountByStatusSync(SyncStatus.FAILED.getValue())
                val totalCount = progressSyncQueueDao.getAllItemsSync().size
                
                Timber.d("🚀 Статистика очереди синхронизации: pending=$pendingCount, failed=$failedCount, всего=$totalCount")
                
                ProgressSyncWorker.schedulePeriodicSync(context)
                Timber.d("🚀 Периодическая синхронизация запланирована")
                
                if (pendingCount > 0 && NetworkUtils.isNetworkAvailable(context)) {
                    Timber.d("🚀 Обнаружены ожидающие записи ($pendingCount), запускаем немедленную синхронизацию")
                    syncNow(true)
                } else {
                    Timber.d("🚀 Нет записей для немедленной синхронизации или отсутствует подключение к сети")
                }
            }
        } catch (e: Exception) {
            Timber.d("🚀 Ошибка при инициализации ProgressSyncRepository", e)
        }
        
        Timber.d("🚀 Инициализация ProgressSyncRepository завершена")
    }
    
    /**
     * Принудительно синхронизирует прогресс пользователя с сервером
     * @return true, если синхронизация прошла успешно
     */
    suspend fun forceSyncWithServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("⚡ Начинаем принудительную синхронизацию с сервером")
            
            if (!NetworkUtils.isNetworkAvailable(context)) {
                Timber.d("⚠️ Сеть недоступна, невозможно синхронизироваться с сервером")
                return@withContext false
            }
            
            val statusesToSync = listOf(SyncStatus.PENDING.getValue(), SyncStatus.FAILED.getValue())
            val pendingItems = progressSyncQueueDao.getItemsByStatusesSync(statusesToSync, 100)
            if (pendingItems.isNotEmpty()) {
                Timber.d("🔄 Найдено ${pendingItems.size} элементов в очереди (PENDING или FAILED), отправляем перед запросом синхронизации")
                val syncSuccess = processSyncItems(pendingItems)
                if (!syncSuccess) {
                    Timber.d("⚠️ Не удалось отправить элементы из очереди")
                }
            } else {
                Timber.d("ℹ️ Очередь синхронизации пуста, нечего отправлять")
            }
            
            val lastTimestamp = getLastSyncTimestamp()
            Timber.d("🕒 Запрашиваем синхронизацию прогресса с timestamp: $lastTimestamp")
            
            try {
                val response = progressApiService.syncProgress(lastTimestamp)
                
                if (response.isSuccessful) {
                    val serverProgressDtoList = response.body()
                    
                    if (serverProgressDtoList != null) {
                        if (serverProgressDtoList.isNotEmpty()) {
                            Timber.d("✅ Получено ${serverProgressDtoList.size} записей прогресса с сервера")
                            
                            val entitiesToInsert = serverProgressDtoList.mapNotNull { dto ->
                                dto.toProgressEntity()
                            }
                            
                            if (entitiesToInsert.isNotEmpty()) {
                                val existingProgressMap = progressDao.getAllProgressListSync().associateBy { it.getContentId() }

                                val toUpdate = mutableListOf<ProgressEntity>()
                                val toInsert = mutableListOf<ProgressEntity>()
                                val toQueueForSync = mutableListOf<ProgressEntity>()

                                for (serverEntity in entitiesToInsert) {
                                    val localEntity = existingProgressMap[serverEntity.getContentId()]

                                    if (localEntity != null) {
                                        if (serverEntity.getLastAccessed() > localEntity.getLastAccessed()) {
                                            val localSolvedIds = localEntity.getSolvedTaskIdsList().toMutableSet()
                                            val serverSolvedIds = serverEntity.getSolvedTaskIdsList()
                                            localSolvedIds.addAll(serverSolvedIds)
                                            
                                            val mergedEntity = serverEntity
                                            mergedEntity.setSolvedTaskIds(ProgressEntity.listToJsonString(localSolvedIds.toList()))
                                            val totalTasks = getTotalTasksCount(mergedEntity.getContentId())
                                            val newPercentage = calculatePercentage(localSolvedIds.size, totalTasks)
                                            mergedEntity.setPercentage(newPercentage)
                                            mergedEntity.setCompleted(newPercentage >= 100)                                           
                                            mergedEntity.setLastAccessed(maxOf(serverEntity.getLastAccessed(), localEntity.getLastAccessed()))
                                            toUpdate.add(mergedEntity)
                                            Timber.d("✅ Обновление с объединением для ${serverEntity.getContentId()}, сервер новее. Сервер: ${serverEntity.getLastAccessed()}, Локально: ${localEntity.getLastAccessed()}. Объединенные ID: ${localSolvedIds.size}")
                                        } else if (localEntity.getLastAccessed() > serverEntity.getLastAccessed()) {
                                            toQueueForSync.add(localEntity)
                                            Timber.d("✅ Локальные данные для ${localEntity.getContentId()} новее серверных. Сервер: ${serverEntity.getLastAccessed()}, Локально: ${localEntity.getLastAccessed()}. Будет добавлено в очередь.")
                                        }
                                    } else {
                                        toInsert.add(serverEntity)
                                        Timber.d("✅ Новая запись с сервера для ${serverEntity.getContentId()}")
                                    }
                                }
                                
                                if (toUpdate.isNotEmpty()) {
                                    progressDao.updateAll(toUpdate)
                                    Timber.d("✅ Успешно обновлено ${toUpdate.size} существующих записей прогресса после объединения")
                                }
                                
                                if (toInsert.isNotEmpty()) {
                                    progressDao.insertAll(toInsert)
                                    Timber.d("✅ Успешно добавлено ${toInsert.size} новых записей прогресса")
                                }

                                if (toQueueForSync.isNotEmpty()) {
                                    for (entityToSync in toQueueForSync) {
                                        queueProgressUpdate(entityToSync, true)
                                    }
                                    Timber.d("✅ Добавлено в очередь на синхронизацию ${toQueueForSync.size} локально обновленных записей")
                                }
                                
                                Timber.d("✅ Всего обработано ${entitiesToInsert.size} записей прогресса (обновлено с объединением: ${toUpdate.size}, новых: ${toInsert.size}, поставлено в очередь: ${toQueueForSync.size})")
                            }
                            
                            val maxTimestamp = serverProgressDtoList.mapNotNull { it.timestamp }.maxOrNull() ?: System.currentTimeMillis()
                            saveLastSyncTimestamp(maxTimestamp)
                            Timber.d("🕒 Сохранена новая метка времени синхронизации: $maxTimestamp")
                        } else {
                            Timber.d("ℹ️ Сервер вернул пустой список прогресса")
                            
                            if (lastTimestamp == null) {
                                val currentTime = System.currentTimeMillis()
                                saveLastSyncTimestamp(currentTime)
                                Timber.d("🕒 Сохранена текущая метка времени: $currentTime")
                            }
                        }
                        return@withContext true
                    } else {
                        Timber.d("⚠️ Сервер вернул null вместо списка прогресса")
                    }
                } else {
                    Timber.d("🚫 Ошибка при запросе прогресса: ${response.code()} ${response.message()}")
                    if (response.errorBody() != null) {
                        try {
                            Timber.d("🚫 Тело ошибки: ${response.errorBody()?.string()}")
                        } catch (e: Exception) {
                            Timber.d("🚫 Не удалось прочитать тело ошибки", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d("🚫 Ошибка сети при синхронизации прогресса", e)
                e.printStackTrace()
            }
            
            return@withContext false
        } catch (e: Exception) {
            Timber.d("🚫 Общая ошибка при forceSyncWithServer", e)
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Обрабатывает элементы очереди синхронизации пакетом
     * @param items список элементов для синхронизации
     * @return true, если синхронизация прошла успешно
     */
    private suspend fun processSyncItems(items: List<ProgressSyncQueueEntity>): Boolean {
        if (items.isEmpty()) {
            Timber.d("Нет элементов для синхронизации")
            return true
        }

        Timber.d("📊 Начинаем обработку пакетной синхронизации для ${items.size} элементов")

        val progressItems = items.filter { it.itemType == ProgressSyncQueueEntity.ITEM_TYPE_PROGRESS }
        val statisticsItems = items.filter { it.itemType == ProgressSyncQueueEntity.ITEM_TYPE_STATISTICS }

        var progressSyncSuccess = true
        if (progressItems.isNotEmpty()) {
            progressSyncSuccess = processProgressItems(progressItems)
        }

        var statisticsSyncSuccess = true
        if (statisticsItems.isNotEmpty()) {
            statisticsSyncSuccess = processStatisticsItems(statisticsItems)
        }

        return progressSyncSuccess && statisticsSyncSuccess
    }

    private suspend fun processProgressItems(items: List<ProgressSyncQueueEntity>): Boolean {
        Timber.d("📊 Обработка ${items.size} элементов прогресса для batch-синхронизации")
        try {
            val groupedItems = items.groupBy { it.itemId }
            val latestItems = groupedItems.mapValues { (_, items) -> items.maxByOrNull { it.timestamp } }.values.filterNotNull()
            
            Timber.d("📊 Обработка ${latestItems.size} уникальных элементов прогресса (было ${items.size})")
            
            val contentIds = latestItems.map { it.itemId }
            val progressEntities = progressDao.getProgressByContentIdsSync(contentIds)
            val progressEntityMap = progressEntities.associateBy { it.getContentId() }
            
            val updateRequests = latestItems.map { item ->
                val progressEntity = progressEntityMap[item.itemId]
                if (progressEntity != null) {
                    toProgressUpdateDto(progressEntity)
                } else {
                    ProgressUpdateRequest(
                        contentId = item.itemId,
                        percentage = item.percentage,
                        completed = item.isCompleted(),
                        timestamp = item.timestamp,
                        solvedTaskIds = parseJsonSolvedTaskIds(item.getSolvedTaskIds())
                    )
                }
            }
            
            if (updateRequests.isEmpty()) {
                Timber.d("Нет запросов для обновления прогресса после фильтрации")
                return true
            }
            
            Timber.d("📊 Отправляем batch-запрос прогресса на сервер для ${updateRequests.size} элементов")
            
            val response = progressApiService.updateProgressBatch(updateRequests)
            
            Timber.d("📊 Получен ответ от сервера для прогресса: isSuccessful=${response.isSuccessful}, code=${response.code()}")
            
            if (response.isSuccessful) {
                val responseList = response.body() ?: emptyList()
                val responseMap = responseList.associateBy { it.contentId }
                
                for (item in items) {
                    val itemResponse = responseMap[item.itemId]
                    if (itemResponse?.success == true) {
                        item.syncStatus = SyncStatus.SYNCED
                    } else {
                        item.syncStatus = SyncStatus.FAILED
                    }
                    progressSyncQueueDao.update(item)
                }
                
                val allSuccess = responseList.all { it.success }
                Timber.d("Batch-синхронизация прогресса завершена, все успешно: $allSuccess")
                return allSuccess
            } else {
                Timber.d("Ошибка при batch-синхронизации прогресса: ${response.code()} ${response.message()}")
                items.forEach { item ->
                    item.syncStatus = SyncStatus.FAILED
                    progressSyncQueueDao.update(item)
                }
                return false
            }
        } catch (e: Exception) {
            Timber.d("Исключение при batch-синхронизации прогресса", e)
            items.forEach { item ->
                item.syncStatus = SyncStatus.FAILED
                progressSyncQueueDao.update(item)
            }
            return false
        }
    }

    private suspend fun processStatisticsItems(items: List<ProgressSyncQueueEntity>): Boolean {
        Timber.d("📊 Обработка ${items.size} элементов статистики для batch-синхронизации.")
        try {
            val updateRequests = items.mapNotNull { item ->
                val statsEntity = practiceStatisticsDao.getStatisticsByEgeNumberSync(item.itemId)
                statsEntity?.let {
                    PracticeStatisticSyncDto(
                        egeNumber = it.egeNumber,
                        totalAttempts = it.totalAttempts,
                        correctAttempts = it.correctAttempts,
                        lastAttemptDate = it.lastAttemptDate,
                        variantData = it.variantData
                    )
                }
            }

            if (updateRequests.isEmpty()) {
                Timber.d("Нет валидных элементов статистики для синхронизации после поиска в БД.")
                items.forEach { item ->
                    item.syncStatus = SyncStatus.FAILED
                    progressSyncQueueDao.update(item)
                }
                return true
            }

            val userId = userDao.getFirstUser()?.getUserId()
            if (userId == null) {
                Timber.d("Не удалось получить ID пользователя для синхронизации статистики.")
                return false
            }
            val lastSyncTimestamp = getLastStatsSyncTimestamp() ?: 0L

            val request = PracticeStatisticsBranchRequest(
                userId = userId.toString(),
                lastKnownServerSyncTimestamp = lastSyncTimestamp,
                newOrUpdatedAggregatedStatistics = updateRequests,
                newAttempts = emptyList()
            )

            val response = practiceApiService.updatePracticeStatistics(request)
            
            Timber.d("📊 Получен ответ от сервера для статистики: isSuccessful=${response.isSuccessful}, code=${response.code()}")

            if (response.isSuccessful) {
                val branchResponse = response.body()
                if (branchResponse != null) {
                    saveLastStatsSyncTimestamp(branchResponse.newServerSyncTimestamp)
                    Timber.d("🕒 Сохранена новая метка времени синхронизации статистики из branch-ответа: ${branchResponse.newServerSyncTimestamp}")
                }
                items.forEach { item ->
                    item.syncStatus = SyncStatus.SYNCED
                    progressSyncQueueDao.update(item)
                }
                Timber.d("Batch-синхронизация статистики успешно завершена.")
                return true
            } else {
                Timber.d("Batch-синхронизация статистики не удалась: ${response.code()} ${response.message()}")
                items.forEach { item ->
                    item.syncStatus = SyncStatus.FAILED
                    progressSyncQueueDao.update(item)
                }
                return false
            }
        } catch (e: Exception) {
            Timber.d("Ошибка во время batch-синхронизации статистики", e)
             items.forEach { item ->
                item.syncStatus = SyncStatus.FAILED
                progressSyncQueueDao.update(item)
            }
            return false
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
            var progressEntity = progressDao.getProgressByContentIdSync(taskGroupId)

            if (progressEntity == null) {
                Timber.d("Прогресс для группы $taskGroupId не найден, создаем новый.")
                progressEntity = ProgressEntity()
                progressEntity.setContentId(taskGroupId)
                progressEntity.setPercentage(0)
                progressEntity.setCompleted(false)
                progressEntity.setLastAccessed(System.currentTimeMillis())
                progressEntity.setSolvedTaskIds("[]")

                progressDao.insert(progressEntity)
                Timber.d("Новая сущность ProgressEntity для $taskGroupId создана и сохранена.")
                progressEntity = progressDao.getProgressByContentIdSync(taskGroupId)
                if (progressEntity == null) {
                    Timber.d("Не удалось создать или получить ProgressEntity для $taskGroupId после insert.")
                    return@withContext false
                }
            }
            
            val currentSolved = progressEntity.getSolvedTaskIdsList().toMutableList()
            
            if (!currentSolved.contains(solvedTaskId)) {
                currentSolved.add(solvedTaskId)
                
                val totalTasksCount = getTotalTasksCount(taskGroupId)
                Timber.d("Общее количество заданий для группы $taskGroupId: $totalTasksCount")
                
                val newPercentage = calculatePercentage(currentSolved.size, totalTasksCount)
                val newLastAccessed = System.currentTimeMillis()
                val newSolvedTaskIds = ProgressEntity.listToJsonString(currentSolved)
                
                progressEntity.setPercentage(newPercentage)
                progressEntity.setLastAccessed(newLastAccessed)
                progressEntity.setCompleted(newPercentage >= 100)
                progressEntity.setSolvedTaskIds(newSolvedTaskIds)
                
                progressDao.update(progressEntity)
                
                queueProgressUpdate(progressEntity, syncImmediately)
                
                Timber.d("Добавлено решенное задание $solvedTaskId в группу $taskGroupId. Текущий прогресс: $newPercentage%")
                return@withContext true
            } else {
                Timber.d("Задание $solvedTaskId уже отмечено как решенное в группе $taskGroupId")
                return@withContext false
            }
        } catch (e: Exception) {
            Timber.d("Ошибка при добавлении решенного задания", e)
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
            val contentEntity = contentDao.getContentByIdSync(taskGroupId)

            if (contentEntity?.isDownloaded == true) {
                val egeNumber = taskGroupId.removePrefix("task_group_")
                val count = taskDao.getTaskCountByEgeNumberSync(egeNumber)
                if (count > 0) {
                    Timber.d("Подсчитано $count загруженных заданий для $taskGroupId")
                    return count
                }
            }
            
            if (contentEntity != null && contentEntity.description != null) {
                val description = contentEntity.description
                Timber.d("Описание для $taskGroupId: $description")
                
                val pattern = java.util.regex.Pattern.compile("(\\d+)\\s+(заданий|задание|задания)")
                val matcher = pattern.matcher(description)
                
                if (matcher.find()) {
                    val countStr = matcher.group(1)
                    val count = countStr.toInt()
                    Timber.d("Извлечено количество заданий для $taskGroupId: $count")
                    return count
                }
                
                val numberPattern = java.util.regex.Pattern.compile("(\\d+)")
                val numberMatcher = numberPattern.matcher(description)
                
                if (numberMatcher.find()) {
                    val countStr = numberMatcher.group(1)
                    val count = countStr.toInt()
                    Timber.d("Извлечено число из описания для $taskGroupId: $count")
                    return count
                }
            }
            
            Timber.d("Используем предопределенные значения для $taskGroupId")
            val groupNumber = taskGroupId.replace("task_group_", "").toIntOrNull() ?: 0
            
            return when (groupNumber) {
                1 -> 10
                2 -> 8
                3 -> 15
                4 -> 12
                5 -> 99
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
                27 -> 1
                else -> 10
            }
        } catch (e: Exception) {
            Timber.d("Ошибка при получении количества заданий для $taskGroupId", e)
            return 10
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

        return progressDao.getProgressByContentId(contentId).asFlow()
            .map { entity: ProgressEntity? ->
                if (entity != null && !entity.getSolvedTaskIds().isNullOrEmpty()) {
                    try {
                        parseJsonSolvedTaskIds(entity.getSolvedTaskIds()) ?: emptyList<String>()
                    } catch (e: org.json.JSONException) {
                        Timber.d("Ошибка JSON парсинга solvedTaskIds для contentId $contentId: ${entity.getSolvedTaskIds()}", e)
                        emptyList<String>()
                    } catch (e: Exception) {
                        Timber.d("Общая ошибка парсинга solvedTaskIds для contentId $contentId: ${entity.getSolvedTaskIds()}", e)
                        emptyList<String>()
                    }
                } else {
                    emptyList<String>()
                }
            }
    }

    suspend fun queueStatisticsUpdate(statistics: PracticeStatisticsEntity, syncImmediately: Boolean = false): Long = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val userId = userDao.getFirstUser()?.getUserId() ?: -1L

        if (userId == -1L) {
            Timber.d("Не удалось получить пользователя для добавления статистики в очередь")
            return@withContext -1L
        }

        Timber.d("🔄 Добавление статистики в очередь синхронизации: itemId=${statistics.egeNumber}")

        val syncQueueEntity = ProgressSyncQueueEntity(
            statistics.egeNumber,
            ProgressSyncQueueEntity.ITEM_TYPE_STATISTICS,
            0,
            false,
            timestamp,
            userId,
            SyncStatus.PENDING,
            "[]"
        )

        val existingItem = progressSyncQueueDao.getItemByItemId(statistics.egeNumber)
        if (existingItem != null) {
            syncQueueEntity.setId(existingItem.id)
            Timber.d("🔄 Обновляем существующую запись статистики в очереди: itemId=${statistics.egeNumber}, id=${existingItem.id}")
        }

        val id = progressSyncQueueDao.insert(syncQueueEntity)
        Timber.d("🔄 Добавлена статистика в очередь: itemId=${statistics.egeNumber}, id=$id")

        if (syncImmediately) {
            syncNow(true)
        }

        return@withContext id
    }
} 