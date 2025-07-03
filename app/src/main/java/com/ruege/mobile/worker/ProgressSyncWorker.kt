package com.ruege.mobile.worker

import android.content.Context
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
import timber.log.Timber

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
            
            Timber.d("Scheduled periodic sync every $intervalMinutes minutes")
        }
        
        /**
         * Запускает одноразовую синхронизацию прогресса.
         * @param context контекст приложения
         * @param expedited флаг для приоритетного выполнения работы
         * @param isExitSync флаг, указывающий, что это синхронизация при закрытии приложения
         */
        @JvmStatic
        fun startOneTimeSync(context: Context, expedited: Boolean = true, isExitSync: Boolean = false) {
            Timber.d("Запуск одноразовой синхронизации, expedited=$expedited, isExitSync=$isExitSync")

            val workName = if (isExitSync) WORK_NAME_EXIT else WORK_NAME_ONE_TIME

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequestBuilder = OneTimeWorkRequestBuilder<ProgressSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    15,
                    TimeUnit.SECONDS
                )
                .addTag(TAG_NORMAL_SYNC)

            if (isExitSync) {
                workRequestBuilder.addTag(TAG_EXIT_SYNC)
            }

            if (expedited) {
                workRequestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }

            val workRequest = workRequestBuilder.build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    workRequest
                )
        }
    }
    
    /**
     * Выполняет фоновую работу по синхронизации прогресса
     */
    override suspend fun doWork(): Result {
        val isExitSync = tags.contains(TAG_EXIT_SYNC)
        
        Timber.d("Starting sync work in ${applicationContext.packageName}, exit mode: $isExitSync")
        
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            Timber.w("No network connection. Aborting sync.")
            return if(isExitSync) Result.success() else Result.retry()
        }

        return try {
            withContext(Dispatchers.IO) {
                val statusesToSync = listOf(SyncStatus.PENDING.getValue(), SyncStatus.FAILED.getValue())
                val pendingItems = progressSyncQueueDao.getItemsByStatusesSync(statusesToSync, 200)

                if (pendingItems.isEmpty()) {
                    Timber.d("No pending or failed items to sync. Work completed.")
                    return@withContext Result.success()
                }
                
                Timber.d("Found ${pendingItems.size} pending or failed items to sync")
                
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
                    Timber.d("Sync work completed successfully (exit mode: $isExitSync)")
                    Result.success()
                } else {
                    if (isExitSync) Result.success() else Result.retry()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during sync work")
            if (isExitSync) Result.success() else Result.failure()
        }
    }

    private suspend fun syncProgressItems(items: List<ProgressSyncQueueEntity>, isExitSync: Boolean): Boolean {
        Timber.d("Syncing ${items.size} progress items.")
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, DaoEntryPoint::class.java)
        val progressDao = entryPoint.progressDao()

        val updateRequests = items.map { item ->
            val progressEntity = try {
                progressDao.getProgressByContentIdSync(item.itemId)
            } catch (e: Exception) {
                Timber.e(e, "Error getting full progress entity for ${item.itemId}")
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
                        Timber.d("Successfully synced progress for ${item.itemId}")
                    } else {
                        item.syncStatus = SyncStatus.FAILED
                        Timber.w("Failed to sync progress for ${item.itemId}: ${itemResponse?.message}")
                    }
                    progressSyncQueueDao.update(item)
                }
                return true
            } else {
                Timber.e("Progress batch sync failed: ${response.code()} ${response.message()}")
                if (response.code() in 400..499) { 
                    items.forEach {
                        it.syncStatus = SyncStatus.FAILED
                        progressSyncQueueDao.update(it)
                    }
                }
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during progress batch sync")
            return false
        }
    }

    private suspend fun syncStatisticsItems(items: List<ProgressSyncQueueEntity>, isExitSync: Boolean): Boolean {
        Timber.d("Syncing ${items.size} statistics items.")
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, DaoEntryPoint::class.java)
        val statisticsDao = entryPoint.practiceStatisticsDao()
        val userDao = entryPoint.userDao()

        val updateRequests = items.mapNotNull { item ->
            val statsEntity = try {
                statisticsDao.getStatisticsByEgeNumberSync(item.itemId)
            } catch (e: Exception) {
                Timber.e(e, "Error getting full statistics entity for ${item.itemId}")
                null
            }

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
            Timber.w("No valid statistics items to sync after fetching from DB.")
            return true 
        }

        try {
            val userId = userDao.getFirstUser()?.getUserId()?.toString()
            if (userId == null) {
                Timber.e("Worker: could not get user ID for statistics sync")
                return false
            }

            val lastSyncTimestamp = 0L

            val request = PracticeStatisticsBranchRequest(
                userId = userId.toString(),
                lastKnownServerSyncTimestamp = lastSyncTimestamp,
                newOrUpdatedAggregatedStatistics = updateRequests,
                newAttempts = emptyList()
            )
            val response = practiceApiService.updatePracticeStatistics(request)

            if (response.isSuccessful) {
                val syncResponse = response.body()
                items.forEach { item ->
                    item.syncStatus = SyncStatus.SYNCED
                    Timber.d("Successfully synced statistics for ${item.itemId}")
                    progressSyncQueueDao.update(item)
                }
                return true
            } else {
                Timber.e("Statistics batch sync failed: ${response.code()} ${response.message()}")
                if (response.code() in 400..499) {
                    items.forEach {
                        it.syncStatus = SyncStatus.FAILED
                        progressSyncQueueDao.update(it)
                    }
                }
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during statistics batch sync")
            return false
        }
    }
} 