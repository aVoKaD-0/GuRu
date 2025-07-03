package com.ruege.mobile.data.repository

import timber.log.Timber
import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.model.VariantResult
import com.ruege.mobile.model.EssayResultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
            Timber.e("PracticeStatsRepo", "taskLocalDbId is null for taskItem.taskId: ${taskItem.taskId}. Cannot record attempt or update associated stats.")
            return
        }

        withContext(Dispatchers.IO) {
            val attempt = PracticeAttemptEntity(
                taskLocalDbId,
                isCorrect,
                timestamp
            )
            practiceAttemptDao.insert(attempt)
            Timber.d("PracticeStatsRepo", "Attempt recorded for taskId: $taskLocalDbId, isCorrect: $isCorrect.")

            if (egeNumberForStats.isNotEmpty()) {
                practiceStatisticsDao.createStatisticsIfNotExists(egeNumberForStats)
                
                practiceStatisticsDao.updateStatisticsAfterAttempt(
                    egeNumberForStats,
                    isCorrect,
                    timestamp
                )
                Timber.d("PracticeStatsRepo", "PracticeStatisticsEntity for egeNumber: $egeNumberForStats updated after attempt.")

                val updatedStats = practiceStatisticsDao.getStatisticsByEgeNumberSync(egeNumberForStats)
                updatedStats?.let {
                    progressSyncRepository.queueStatisticsUpdate(it, false)
                }

            } else {
                Timber.w("PracticeStatsRepo", "egeNumberForStats is empty for taskId: $taskLocalDbId. Cannot update PracticeStatisticsEntity.")
            }
        }
    }

    /**
     * Записывает попытку решения эссе и обновляет статистику.
     */
    suspend fun recordEssayAttempt(title: String, essayContent: String, result: String, timestamp: Long) {
        val egeNumberForStats = "essay:$title"
        withContext(Dispatchers.IO) {
            practiceStatisticsDao.createStatisticsIfNotExists(egeNumberForStats)

            val existingStats = practiceStatisticsDao.getStatisticsByEgeNumberSync(egeNumberForStats)!!
            
            existingStats.totalAttempts += 1
            existingStats.correctAttempts += 1 
            existingStats.lastAttemptDate = timestamp
            existingStats.variantData = EssayResultData(essayContent, result).toJsonString()

            practiceStatisticsDao.update(existingStats)
            
            progressSyncRepository.queueStatisticsUpdate(existingStats, false)
            Timber.d("PracticeStatsRepo", "Essay stats for '$title' updated.")
        }
    }

    /**
     * Сохраняет статистику по варианту.
     */
    suspend fun saveVariantStatistics(statistics: PracticeStatisticsEntity) {
        withContext(Dispatchers.IO) {
            practiceStatisticsDao.insert(statistics)
            progressSyncRepository.queueStatisticsUpdate(statistics, false)
            Timber.d("PracticeStatsRepo", "Saved variant statistics for ${statistics.egeNumber} and queued for sync")
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
                    
                    Timber.d("PracticeStatsRepo", "Variant data updated for egeNumber: $egeNumber")
                    
                    progressSyncRepository.queueStatisticsUpdate(currentStats, false)
                } else {
                    Timber.w("PracticeStatsRepo", "Failed to update variant data - statistics not found for egeNumber: $egeNumber")
                }
            } catch (e: Exception) {
                Timber.e("PracticeStatsRepo", "Error updating variant data for egeNumber: $egeNumber", e)
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
                Timber.e("PracticeStatsRepo", "Error getting variant result for egeNumber: $egeNumber", e)
                null
            }
        }
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
