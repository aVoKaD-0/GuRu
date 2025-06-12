package com.ruege.mobile.data.repository

import android.util.Log
import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.model.TaskItem
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
}
