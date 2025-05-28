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
import com.ruege.mobile.data.local.entity.ProgressSyncQueueEntity
import com.ruege.mobile.data.local.entity.SyncStatus
import com.ruege.mobile.data.network.api.ProgressApiService
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao
import com.ruege.mobile.data.network.dto.ProgressUpdateRequest
import com.ruege.mobile.util.NetworkUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.ruege.mobile.data.mapper.toProgressUpdateDto as mapperToProgressUpdateDto
import com.ruege.mobile.data.repository.PracticeSyncRepository

/**
 * Фоновый работник для синхронизации прогресса пользователя с сервером
 */
class ProgressSyncWorker : CoroutineWorker {
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProgressSyncWorkerEntryPoint {
        fun progressSyncQueueDao(): ProgressSyncQueueDao
        fun progressApiService(): ProgressApiService
        fun practiceSyncRepository(): PracticeSyncRepository
    }
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProgressDaoEntryPoint {
        fun progressDao(): com.ruege.mobile.data.local.dao.ProgressDao
    }
    
    private val progressSyncQueueDao: ProgressSyncQueueDao
    private val progressApiService: ProgressApiService
    private val practiceSyncRepository: PracticeSyncRepository
    
    // Основной конструктор для прямого создания с зависимостями из нашей фабрики
    constructor(
        appContext: Context,
        workerParams: WorkerParameters,
        progressSyncQueueDao: ProgressSyncQueueDao,
        progressApiService: ProgressApiService,
        practiceSyncRepository: PracticeSyncRepository
    ) : super(appContext, workerParams) {
        this.progressSyncQueueDao = progressSyncQueueDao
        this.progressApiService = progressApiService
        this.practiceSyncRepository = practiceSyncRepository
    }
    
    // Вторичный конструктор для создания через WorkManager без фабрики
    constructor(appContext: Context, workerParams: WorkerParameters) : super(appContext, workerParams) {
        // Получаем зависимости через EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            ProgressSyncWorkerEntryPoint::class.java
        )
        
        this.progressSyncQueueDao = entryPoint.progressSyncQueueDao()
        this.progressApiService = entryPoint.progressApiService()
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
            
            // Если это синхронизация при выходе из приложения, добавляем тег EXIT_SYNC
            val workTags = mutableSetOf(TAG)
            if (isExitSync) {
                workTags.add(TAG_EXIT_SYNC)
            }
            
            // Настраиваем ограничения для работы
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Требуется подключение к сети
                .build()
            
            // Создаем объект работы
            val workRequest = OneTimeWorkRequestBuilder<ProgressSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR, // Линейная задержка для повторных попыток
                    15, // Минимальное время задержки
                    TimeUnit.SECONDS // Единица измерения времени
                )
                .addTag(TAG)
                .apply {
                    // Добавляем теги
                    workTags.forEach { tag ->
                        addTag(tag)
                    }
                    
                    // Если нужно экспедированное выполнение
                    if (expedited) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .build()
            
            // Отменяем все предыдущие работы с тем же тегом
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(TAG)
            
            // Запускаем работу
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "${TAG}_${System.currentTimeMillis()}", // Уникальное имя работы
                    ExistingWorkPolicy.REPLACE, // Заменяем существующую работу
                    workRequest
                )
        }
        
        /**
         * Запускает синхронизацию при выходе из приложения с повышенным приоритетом
         * @param context контекст приложения
         */
        @JvmStatic
        fun startExitSync(context: Context) {
            // Используем новый метод с флагом isExitSync
            startOneTimeSync(context, true, true)
            
            // Также выполняем прямую синхронизацию через репозиторий если возможно
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
            // Не отменяем задачи EXIT-синхронизации, т.к. они критически важны
            Log.d(TAG, "Cancelled regular sync work (exit sync preserved)")
        }
    }
    
    /**
     * Выполняет фоновую работу по синхронизации прогресса
     */
    override suspend fun doWork(): Result {
        // Определяем, является ли это синхронизацией при выходе из приложения
        val isExitSync = tags.contains(TAG_EXIT_SYNC)
        
        Log.d(TAG, "Starting sync work in ${applicationContext.packageName}, exit mode: $isExitSync")
        
        return try {
            withContext(Dispatchers.IO) {
                // Проверяем, есть ли сеть (для не-exit-синхронизации это критично)
                if (!NetworkUtils.isNetworkAvailable(applicationContext) && !isExitSync) {
                    Log.w(TAG, "No network connection. Rescheduling sync.")
                    return@withContext Result.retry()
                }
                
                // Получаем все записи со статусом PENDING (не более 100 за раз)
                val pendingItems = progressSyncQueueDao.getItemsByStatusSync(SyncStatus.PENDING.getValue(), 100)
                
                if (pendingItems.isEmpty()) {
                    Log.d(TAG, "No pending items to sync. Work completed.")
                    return@withContext Result.success()
                }
                
                Log.d(TAG, "Found ${pendingItems.size} pending items to sync")
                
                // Создаем список запросов на обновление
                val updateRequests = pendingItems.map { item ->
                    // Получаем полную информацию о прогрессе из базы данных
                    val progressEntity = try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            applicationContext,
                            ProgressDaoEntryPoint::class.java
                        )
                        entryPoint.progressDao().getProgressByContentIdSync(item.contentId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting full progress entity", e)
                        null
                    }
                    
                    // Если удалось получить полную информацию о прогрессе, используем ее
                    if (progressEntity != null) {
                        try {
                            Log.d(TAG, "📱 Создаем запрос из progressEntity для ${item.contentId}, процент: ${progressEntity.getPercentage()}, выполнено: ${progressEntity.isCompleted()}")
                            // Используем расширение для преобразования из Entity в DTO
                            mapperToProgressUpdateDto(progressEntity)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting entity to DTO", e)
                            // Фолбэк на простое преобразование
                            Log.d(TAG, "📱 Фолбэк: создаем запрос из элемента очереди для ${item.contentId}, процент: ${item.percentage}, выполнено: ${item.isCompleted()}")
                            ProgressUpdateRequest(
                                contentId = item.contentId,
                                percentage = item.percentage,
                                completed = item.isCompleted(),
                                timestamp = item.timestamp
                            )
                        }
                    } else {
                        // Создаем запрос только с основной информацией из очереди
                        Log.d(TAG, "📱 Создаем запрос из элемента очереди для ${item.contentId}, процент: ${item.percentage}, выполнено: ${item.isCompleted()}")
                        ProgressUpdateRequest(
                            contentId = item.contentId,
                            percentage = item.percentage,
                            completed = item.isCompleted(),
                            timestamp = item.timestamp
                        )
                    }
                }
                
                try {
                    Log.d(TAG, "📱 Подготовлено ${updateRequests.size} запросов для отправки на сервер")
                    updateRequests.forEach { request ->
                        Log.d(TAG, "📱 Запрос: contentId=${request.contentId}, percentage=${request.percentage}, completed=${request.completed}")
                    }
                    
                    // Проверяем подключение перед отправкой
                    if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                        Log.e(TAG, "📱 Нет подключения к сети. Отправка отложена.")
                        if (!isExitSync) {
                            return@withContext Result.retry()
                        }
                    }
                    
                    // Логируем информацию о API сервисе
                    Log.d(TAG, "📱 API сервис: ${progressApiService.javaClass.name}")
                    
                    // Отправляем пакетный запрос
                    Log.d(TAG, "📱 Отправляем запрос на эндпоинт /progress/batch")
                    val response = progressApiService.updateProgressBatch(updateRequests)
                    
                    Log.d(TAG, "📱 Получен ответ от сервера: isSuccessful=${response.isSuccessful}, code=${response.code()}, message=${response.message()}")
                    
                    if (response.isSuccessful) {
                        val responseList = response.body()
                        
                        if (responseList != null && responseList.isNotEmpty()) {
                            Log.d(TAG, "📱 Получен ответ от сервера с ${responseList.size} элементами")
                            // Выводим первые несколько ответов для отладки
                            responseList.take(3).forEach { resp ->
                                Log.d(TAG, "📱 Ответ: contentId=${resp.contentId}, success=${resp.success}, message=${resp.message}")
                            }
                            
                            // Проходим по всем элементам и обновляем их статус на основе ответа сервера
                            val itemsWithResponses = pendingItems.map { item ->
                                // Находим соответствующий ответ по content_id
                                val itemResponse = responseList.find { it.contentId == item.contentId }
                                item to itemResponse
                            }
                            
                            // Обновляем статус элементов
                            for ((item, itemResponse) in itemsWithResponses) {
                                if (itemResponse != null && itemResponse.success) {
                                    item.syncStatus = SyncStatus.SYNCED
                                    Log.d(TAG, "📱 Успешная синхронизация для ${item.contentId}")
                                } else {
                                    // Если не нашли ответ или он не успешен, элемент будет повторно синхронизирован
                                    // в следующий раз
                                    item.syncStatus = SyncStatus.FAILED
                                    Log.d(TAG, "📱 Ошибка синхронизации для ${item.contentId}: ${itemResponse?.message ?: "нет ответа"}")
                                }
                                progressSyncQueueDao.update(item)
                            }
                            
                            val successCount = itemsWithResponses.count { it.second?.success == true }
                            Log.d(TAG, "📱 Batch-синхронизация основного прогресса завершена: $successCount успешных обновлений из ${pendingItems.size}")

                            // ---> СИНХРОНИЗАЦИЯ СТАТИСТИКИ ПРАКТИКИ <--- (начало)
                            Log.d(TAG, "Запуск синхронизации статистики практики...")
                            try {
                                val practiceSyncResult = practiceSyncRepository.performFullSync()
                                if (practiceSyncResult is com.ruege.mobile.data.repository.Result.Success<*>) {
                                    Log.d(TAG, "Синхронизация статистики практики успешно завершена.")
                                } else if (practiceSyncResult is com.ruege.mobile.data.repository.Result.Failure) {
                                    Log.e(TAG, "Ошибка при синхронизации статистики практики: ${practiceSyncResult.exception.message}")
                                    // Не меняем общий результат worker'а, просто логируем ошибку
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Исключение во время синхронизации статистики практики", e)
                                // Не меняем общий результат worker'а, просто логируем ошибку
                            }
                            // ---> СИНХРОНИЗАЦИЯ СТАТИСТИКИ ПРАКТИКИ <--- (конец)

                        } else {
                            Log.e(TAG, "Server returned empty response list for main progress")
                            // Не обновляем статус записей, чтобы они были синхронизированы позже
                            if (!isExitSync) {
                                return@withContext Result.retry()
                            }
                        }
                    } else {
                        Log.e(TAG, "Batch sync failed: ${response.code()} ${response.message()}")
                        
                        // Помечаем записи как FAILED, если это критическая ошибка
                        if (response.code() == 401 || response.code() == 403) {
                            pendingItems.forEach { item ->
                                item.syncStatus = SyncStatus.FAILED
                                progressSyncQueueDao.update(item)
                            }
                        }
                        
                        // Для остальных ошибок просто возвращаем retry
                        if (!isExitSync) {
                            return@withContext Result.retry()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during batch sync", e)
                    if (!isExitSync) {
                        return@withContext Result.retry()
                    }
                }
                
                Log.d(TAG, "Sync work completed successfully (exit mode: $isExitSync)")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync work", e)
            // Для синхронизации при выходе всегда возвращаем успех,
            // чтобы система не пыталась перезапускать работу
            if (isExitSync) Result.success() else Result.failure()
        }
    }
} 