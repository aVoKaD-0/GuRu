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
    private val practiceStatisticsDao: PracticeStatisticsDao
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
            Log.d("PracticeStatsRepo", "Checking attempts for taskId: $taskLocalDbId BEFORE insert.")
            val wasAttemptedBefore = practiceAttemptDao.wasAttemptsForTask(taskLocalDbId)
            Log.d("PracticeStatsRepo", "taskId: $taskLocalDbId, wasAttemptedBefore: $wasAttemptedBefore")

            // Всегда записываем текущую попытку
            val attempt = PracticeAttemptEntity(
                taskLocalDbId,
                isCorrect,
                timestamp
            )
            practiceAttemptDao.insert(attempt)
            Log.d("PracticeStatsRepo", "Attempt recorded for taskId: $taskLocalDbId, isCorrect: $isCorrect.")

            // Проверим еще раз состояние ПОСЛЕ insert, просто для информации
            val isNowAttempted = practiceAttemptDao.wasAttemptsForTask(taskLocalDbId)
            Log.d("PracticeStatsRepo", "taskId: $taskLocalDbId, isNowAttempted (after insert): $isNowAttempted")

            // Обновляем PracticeStatisticsEntity только если это была первая попытка для данного task_id
            if (!wasAttemptedBefore) {
                Log.d("PracticeStatsRepo", "Condition !wasAttemptedBefore is TRUE. Updating PracticeStatisticsEntity.")
                if (egeNumberForStats.isNotEmpty()) {
                    practiceStatisticsDao.createStatisticsIfNotExists(egeNumberForStats)
                    val statsEntity = practiceStatisticsDao.getStatisticsByEgeNumber(egeNumberForStats).firstOrNull()

                    if (statsEntity != null) {
                        statsEntity.totalAttempts += 1
                        if (isCorrect) {
                            statsEntity.correctAttempts += 1
                        }
                        statsEntity.lastAttemptDate = timestamp
                        
                        practiceStatisticsDao.update(statsEntity)
                        Log.d("PracticeStatsRepo", "PracticeStatisticsEntity for egeNumber: $egeNumberForStats updated. Total: ${statsEntity.totalAttempts}, Correct: ${statsEntity.correctAttempts}")
                    } else {
                        Log.e("PracticeStatsRepo", "Could not find PracticeStatisticsEntity for egeNumber: $egeNumberForStats after createIfNotExists.")
                    }
                } else {
                    Log.w("PracticeStatsRepo", "egeNumberForStats is empty for taskId: $taskLocalDbId. Cannot update PracticeStatisticsEntity even if it's the first attempt for task_id.")
                }
            } else {
                Log.d("PracticeStatsRepo", "Condition !wasAttemptedBefore is FALSE. PracticeStatisticsEntity NOT updated for egeNumber: $egeNumberForStats.")
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
