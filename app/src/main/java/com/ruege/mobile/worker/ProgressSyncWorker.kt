package com.ruege.mobile.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.dao.ProgressDao
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.ProgressSyncQueueEntity
import com.ruege.mobile.data.local.entity.SyncStatus
import com.ruege.mobile.data.mapper.toProgressUpdateDto
import com.ruege.mobile.data.network.api.PracticeApiService
import com.ruege.mobile.data.network.api.ProgressApiService
import com.ruege.mobile.data.network.dto.ProgressUpdateRequest
import com.ruege.mobile.data.network.dto.request.PracticeStatisticSyncDto
import com.ruege.mobile.data.network.dto.request.PracticeStatisticsBranchRequest
import com.ruege.mobile.data.repository.PracticeSyncRepository
import com.ruege.mobile.utils.NetworkUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Фоновый работник для синхронизации прогресса пользователя с сервером
 */
class ProgressSyncWorker : CoroutineWorker {
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProgressSyncWorkerEntryPoint {
        fun progressSyncQueueDao(): ProgressSyncQueueDao
        fun progressApiService(): ProgressApiService
        fun practiceApiService(): PracticeApiService
        fun practiceSyncRepository(): PracticeSyncRepository
    }
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DaoEntryPoint {
        fun progressDao(): ProgressDao
        fun practiceStatisticsDao(): PracticeStatisticsDao
        fun userDao(): UserDao
    }
    
    private val progressSyncQueueDao: ProgressSyncQueueDao
    private val progressApiService: ProgressApiService
    private val practiceApiService: PracticeApiService
    private val practiceSyncRepository: PracticeSyncRepository
    
    constructor(
        appContext: Context,
        workerParams: WorkerParameters,
        progressSyncQueueDao: ProgressSyncQueueDao,
        progressApiService: ProgressApiService,
        practiceApiService: PracticeApiService,
        practiceSyncRepository: PracticeSyncRepository
    ) : super(appContext, workerParams) {
        this.progressSyncQueueDao = progressSyncQueueDao
        this.progressApiService = progressApiService
        this.practiceApiService = practiceApiService
        this.practiceSyncRepository = practiceSyncRepository
    }
    
    constructor(appContext: Context, workerParams: WorkerParameters) : super(appContext, workerParams) {
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            ProgressSyncWorkerEntryPoint::class.java
        )
        
        this.progressSyncQueueDao = entryPoint.progressSyncQueueDao()
        this.progressApiService = entryPoint.progressApiService()
        this.practiceApiService = entryPoint.practiceApiService()
        this.practiceSyncRepository = entryPoint.practiceSyncRepository()
    }

    companion object {
        private const val TAG = "ProgressSyncWorker"
        private const val WORK_NAME_PERIODIC = "progress_sync_periodic"
        private const val WORK_NAME_ONE_TIME = "progress_sync_one_time"
        private const val WORK_NAME_EXIT = "progress_sync_exit"
        
        private const val TAG_NORMAL_SYNC = "progress_sync_work"
        private const val TAG_EXIT_SYNC = "progress_sync_exit"
        
        /**
         * Планирует периодическую синхронизацию прогресса
         * @param context контекст приложения
         * @param intervalMinutes интервал синхронизации в минутах
         */
        @JvmStatic
        fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val workRequest = PeriodicWorkRequestBuilder<ProgressSyncWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    30, TimeUnit.SECONDS
                )
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            
            Log.d(TAG, "Scheduled periodic sync every $intervalMinutes minutes")
        }
        
        /**
         * Запускает одноразовую синхронизацию прогресса.
         * @param context контекст приложения
         * @param expedited флаг для приоритетного выполнения работы
         * @param isExitSync флаг, указывающий, что это синхронизация при закрытии приложения
         */
        @JvmStatic
        fun startOneTimeSync(context: Context, expedited: Boolean = true, isExitSync: Boolean = false) {
            Log.d(TAG, "Запуск одноразовой синхронизации, expedited=$expedited, isExitSync=$isExitSync")
            
            val workTags = mutableSetOf(TAG)
            if (isExitSync) {
                workTags.add(TAG_EXIT_SYNC)
            }
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) 
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<ProgressSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR, 
                    15, 
                    TimeUnit.SECONDS
                )
                .addTag(TAG)
                .apply {
                    workTags.forEach { tag ->
                        addTag(tag)
                    }
                    
                    if (expedited) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .build()
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(TAG)
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "${TAG}_${System.currentTimeMillis()}", 
                    ExistingWorkPolicy.REPLACE, 
                    workRequest
                )
        }
        
        /**
         * Запускает синхронизацию при выходе из приложения с повышенным приоритетом
         * @param context контекст приложения
         */
        @JvmStatic
        fun startExitSync(context: Context) {
            startOneTimeSync(context, true, true)
            
            try {
                Log.d(TAG, "Пытаемся получить ProgressSyncRepository для прямой синхронизации")
                val appContext = context.applicationContext
                
                if (appContext is com.ruege.mobile.MobileApplication) {
                    val progressSyncRepository = try {
                        appContext.progressSyncRepository
                    } catch (e: Exception) {
                        Log.e(TAG, "Не удалось получить progressSyncRepository", e)
                        null
                    }
                    
                    progressSyncRepository?.let {
                        Log.d(TAG, "Выполняем прямую синхронизацию через ProgressSyncRepository (exit mode)")
                        it.syncNow(true, true)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при прямой синхронизации через ProgressSyncRepository", e)
            }
            
            Log.d(TAG, "Started critical EXIT sync with highest priority")
        }
        
        /**
         * Отменяет все задачи синхронизации
         * @param context контекст приложения
         */
        @JvmStatic
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_ONE_TIME)
            Log.d(TAG, "Cancelled regular sync work (exit sync preserved)")
        }
    }
    
    /**
     * Выполняет фоновую работу по синхронизации прогресса
     */
    override suspend fun doWork(): Result {
        val isExitSync = tags.contains(TAG_EXIT_SYNC)
        
        Log.d(TAG, "Starting sync work in ${applicationContext.packageName}, exit mode: $isExitSync")
        
        return try {
            withContext(Dispatchers.IO) {
                if (!NetworkUtils.isNetworkAvailable(applicationContext) && !isExitSync) {
                    Log.w(TAG, "No network connection. Rescheduling sync.")
                    return@withContext Result.retry()
                }
                
                val statusesToSync = listOf(SyncStatus.PENDING.getValue(), SyncStatus.FAILED.getValue())
                val pendingItems = progressSyncQueueDao.getItemsByStatusesSync(statusesToSync, 200)

                if (pendingItems.isEmpty()) {
                    Log.d(TAG, "No pending or failed items to sync. Work completed.")
                    return@withContext Result.success()
                }
                
                Log.d(TAG, "Found ${pendingItems.size} pending or failed items to sync")
                
                val progressItems = pendingItems.filter { it.itemType == ProgressSyncQueueEntity.ITEM_TYPE_PROGRESS }
                val statisticsItems = pendingItems.filter { it.itemType == ProgressSyncQueueEntity.ITEM_TYPE_STATISTICS }
                
                var progressSyncSuccess = true
                if (progressItems.isNotEmpty()) {
                    progressSyncSuccess = syncProgressItems(progressItems, isExitSync)
                }
                
                var statisticsSyncSuccess = true
                if (statisticsItems.isNotEmpty()) {
                    statisticsSyncSuccess = syncStatisticsItems(statisticsItems, isExitSync)
                }

                if (progressSyncSuccess && statisticsSyncSuccess) {
                    Log.d(TAG, "Sync work completed successfully (exit mode: $isExitSync)")
                    Result.success()
                } else {
                    if (isExitSync) Result.success() else Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync work", e)
            if (isExitSync) Result.success() else Result.failure()
        }
    }

    private suspend fun syncProgressItems(items: List<ProgressSyncQueueEntity>, isExitSync: Boolean): Boolean {
        Log.d(TAG, "Syncing ${items.size} progress items.")
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, DaoEntryPoint::class.java)
        val progressDao = entryPoint.progressDao()

        val updateRequests = items.map { item ->
            val progressEntity = try {
                progressDao.getProgressByContentIdSync(item.itemId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting full progress entity for ${item.itemId}", e)
                null
            }

            if (progressEntity != null) {
                toProgressUpdateDto(progressEntity)
            } else {
                ProgressUpdateRequest(
                    contentId = item.itemId,
                    percentage = item.percentage,
                    completed = item.isCompleted(),
                    timestamp = item.timestamp
                )
            }
        }

        try {
            val response = progressApiService.updateProgressBatch(updateRequests)
            if (response.isSuccessful) {
                val responseList = response.body() ?: emptyList()
                val responseMap = responseList.associateBy { it.contentId }
                items.forEach { item ->
                    val itemResponse = responseMap[item.itemId]
                    if (itemResponse?.success == true) {
                        item.syncStatus = SyncStatus.SYNCED
                        Log.d(TAG, "Successfully synced progress for ${item.itemId}")
                    } else {
                        item.syncStatus = SyncStatus.FAILED
                        Log.w(TAG, "Failed to sync progress for ${item.itemId}: ${itemResponse?.message}")
                    }
                    progressSyncQueueDao.update(item)
                }
                return true
            } else {
                Log.e(TAG, "Progress batch sync failed: ${response.code()} ${response.message()}")
                if (response.code() in 400..499) { 
                    items.forEach {
                        it.syncStatus = SyncStatus.FAILED
                        progressSyncQueueDao.update(it)
                    }
                }
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during progress batch sync", e)
            return false
        }
    }

    private suspend fun syncStatisticsItems(items: List<ProgressSyncQueueEntity>, isExitSync: Boolean): Boolean {
        Log.d(TAG, "Syncing ${items.size} statistics items.")
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, DaoEntryPoint::class.java)
        val statisticsDao = entryPoint.practiceStatisticsDao()
        val userDao = entryPoint.userDao()

        val updateRequests = items.mapNotNull { item ->
            val statsEntity = try {
                statisticsDao.getStatisticsByEgeNumberSync(item.itemId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting full statistics entity for ${item.itemId}", e)
                null
            }

            statsEntity?.let {
                PracticeStatisticSyncDto(
                    egeNumber = it.egeNumber,
                    totalAttempts = it.totalAttempts,
                    correctAttempts = it.correctAttempts,
                    lastAttemptDate = it.lastAttemptDate
                )
            }
        }

        if (updateRequests.isEmpty()) {
            Log.w(TAG, "No valid statistics items to sync after fetching from DB.")
            return true 
        }

        try {
            val userId = userDao.getFirstUser()?.getUserId()?.toString()
            if (userId == null) {
                Log.e(TAG, "Worker: could not get user ID for statistics sync")
                return false
            }

            val lastSyncTimestamp = 0L

            val request = PracticeStatisticsBranchRequest(
                userId = userId,
                lastKnownServerSyncTimestamp = lastSyncTimestamp,
                newOrUpdatedAggregatedStatistics = updateRequests,
                newAttempts = emptyList() 
            )
            val response = practiceApiService.updatePracticeStatistics(request)

            if (response.isSuccessful) {
                val syncResponse = response.body()
                items.forEach { item ->
                    item.syncStatus = SyncStatus.SYNCED
                    Log.d(TAG, "Successfully synced statistics for ${item.itemId}")
                    progressSyncQueueDao.update(item)
                }
                return true
            } else {
                Log.e(TAG, "Statistics batch sync failed: ${response.code()} ${response.message()}")
                if (response.code() in 400..499) {
                    items.forEach {
                        it.syncStatus = SyncStatus.FAILED
                        progressSyncQueueDao.update(it)
                    }
                }
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during statistics batch sync", e)
            return false
        }
    }
} 