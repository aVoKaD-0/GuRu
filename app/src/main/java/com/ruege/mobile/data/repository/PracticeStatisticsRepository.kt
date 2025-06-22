package com.ruege.mobile.data.repository

import android.util.Log
import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.model.VariantResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeStatisticsRepository @Inject constructor(
    private val practiceAttemptDao: PracticeAttemptDao,
    private val practiceStatisticsDao: PracticeStatisticsDao,
    private val progressSyncRepository: ProgressSyncRepository
) {

    /**
     * Записывает новую попытку решения задачи и обновляет статистику.
     */
    suspend fun recordAttempt(taskItem: TaskItem, isCorrect: Boolean, timestamp: Long) {
        val taskLocalDbId = taskItem.taskId.toIntOrNull()
        val egeNumberForStats = taskItem.egeTaskNumber

        if (taskLocalDbId == null) {
            Log.e("PracticeStatsRepo", "taskLocalDbId is null for taskItem.taskId: ${taskItem.taskId}. Cannot record attempt or update associated stats.")
            return
        }

        withContext(Dispatchers.IO) {
            val attempt = PracticeAttemptEntity(
                taskLocalDbId,
                isCorrect,
                timestamp
            )
            practiceAttemptDao.insert(attempt)
            Log.d("PracticeStatsRepo", "Attempt recorded for taskId: $taskLocalDbId, isCorrect: $isCorrect.")

            if (egeNumberForStats.isNotEmpty()) {
                practiceStatisticsDao.createStatisticsIfNotExists(egeNumberForStats)
                
                practiceStatisticsDao.updateStatisticsAfterAttempt(
                    egeNumberForStats,
                    isCorrect,
                    timestamp
                )
                Log.d("PracticeStatsRepo", "PracticeStatisticsEntity for egeNumber: $egeNumberForStats updated after attempt.")

                val updatedStats = practiceStatisticsDao.getStatisticsByEgeNumberSync(egeNumberForStats)
                updatedStats?.let {
                    progressSyncRepository.queueStatisticsUpdate(it, false)
                }

            } else {
                Log.w("PracticeStatsRepo", "egeNumberForStats is empty for taskId: $taskLocalDbId. Cannot update PracticeStatisticsEntity.")
            }
        }
    }

    /**
     * Сохраняет статистику по варианту.
     */
    suspend fun saveVariantStatistics(statistics: PracticeStatisticsEntity) {
        withContext(Dispatchers.IO) {
            practiceStatisticsDao.insert(statistics)
            progressSyncRepository.queueStatisticsUpdate(statistics, false)
            Log.d("PracticeStatsRepo", "Saved variant statistics for ${statistics.egeNumber} and queued for sync")
        }
    }

    /**
     * Обновляет данные варианта в статистике для указанного номера ЕГЭ
     */
    suspend fun updateVariantData(egeNumber: String, variantResult: VariantResult) {
        withContext(Dispatchers.IO) {
            try {
                practiceStatisticsDao.createStatisticsIfNotExists(egeNumber)
                
                val currentStats = practiceStatisticsDao.getStatisticsByEgeNumberSync(egeNumber)
                
                if (currentStats != null) {
                    val variantDataJson = variantResult.toJsonString()
                    
                    currentStats.variantData = variantDataJson
                    
                    practiceStatisticsDao.update(currentStats)
                    
                    Log.d("PracticeStatsRepo", "Variant data updated for egeNumber: $egeNumber")
                    
                    progressSyncRepository.queueStatisticsUpdate(currentStats, false)
                } else {
                    Log.w("PracticeStatsRepo", "Failed to update variant data - statistics not found for egeNumber: $egeNumber")
                }
            } catch (e: Exception) {
                Log.e("PracticeStatsRepo", "Error updating variant data for egeNumber: $egeNumber", e)
                throw e
            }
        }
    }
    
    /**
     * Получает результаты варианта для указанного номера ЕГЭ
     */
    suspend fun getVariantResult(egeNumber: String): VariantResult? {
        return withContext(Dispatchers.IO) {
            try {
                val stats = practiceStatisticsDao.getStatisticsByEgeNumberSync(egeNumber)
                if (stats != null && !stats.variantData.isNullOrEmpty()) {
                    VariantResult.fromJsonString(
                        stats.variantData!!,
                        "", 
                        stats.lastAttemptDate
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("PracticeStatsRepo", "Error getting variant result for egeNumber: $egeNumber", e)
                null
            }
        }
    }

    /**
     * Возвращает Flow со списком всей статистики по заданиям, отсортированной по номеру ЕГЭ.
     */
    fun getAllStatisticsSorted(): Flow<List<PracticeStatisticsEntity>> {
        return practiceStatisticsDao.getAllStatisticsSortedByEgeNumber()
    }

    /**
     * Возвращает Flow с общей агрегированной статистикой.
     */
    fun getAggregatedStatistics(): Flow<PracticeStatisticsDao.AggregatedPracticeStatistics?> {
        return practiceStatisticsDao.getOverallAggregatedStatistics()
    }

    fun getRecentAttempts(limit: Int): Flow<List<PracticeAttemptEntity>> {
        return practiceAttemptDao.getRecentAttempts(limit)
    }

    /**
     * Получает статистику для указанного номера ЕГЭ
     */
    suspend fun getStatisticsByEgeNumber(egeNumber: String): PracticeStatisticsEntity? {
        return withContext(Dispatchers.IO) {
            practiceStatisticsDao.getStatisticsByEgeNumberSync(egeNumber)
        }
    }
}
