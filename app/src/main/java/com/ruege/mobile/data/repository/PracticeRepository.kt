package com.ruege.mobile.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.dao.TaskDao
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.local.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PracticeRepository(
    private val practiceAttemptDao: PracticeAttemptDao,
    private val practiceStatisticsDao: PracticeStatisticsDao,
    private val taskDao: TaskDao
) {
    fun getAttemptsByTaskId(taskId: Int): Flow<List<PracticeAttemptEntity>> {
        return practiceAttemptDao.getAttemptsByTaskId(taskId)
    }

    fun getAttemptsByEgeNumber(egeNumber: String): Flow<List<PracticeAttemptEntity>> {
        return practiceAttemptDao.getAttemptsByEgeNumber(egeNumber)
    }

    fun getRecentAttempts(limit: Int): Flow<List<PracticeAttemptEntity>> {
        return practiceAttemptDao.getRecentAttempts(limit)
    }

    fun getAttemptCountForTask(taskId: Int): Flow<Int> {
        return practiceAttemptDao.getAttemptCountForTask(taskId)
    }

    fun getTotalAttempts(): LiveData<Int> = liveData(Dispatchers.IO) {
        practiceStatisticsDao.getOverallAggregatedStatistics().map { aggStats ->
            aggStats?.totalAttempts ?: 0
        }.collect { emit(it) }
    }

    fun getTotalCorrectAttempts(): LiveData<Int> = liveData(Dispatchers.IO) {
        practiceStatisticsDao.getOverallAggregatedStatistics().map { aggStats ->
            aggStats?.correctAttempts ?: 0
        }.collect { emit(it) }
    }

    fun getStatisticsByEgeNumber(egeNumber: String): Flow<PracticeStatisticsEntity?> {
        return practiceStatisticsDao.getStatisticsByEgeNumber(egeNumber)
    }

    fun getAllStatisticsByAttemptsDesc(): Flow<List<PracticeStatisticsEntity>> {
        return practiceStatisticsDao.getAllStatisticsByAttemptsDesc()
    }

    fun getAllStatisticsByRecentActivity(): Flow<List<PracticeStatisticsEntity>> {
        return practiceStatisticsDao.getAllStatisticsByRecentActivity()
    }

    suspend fun saveAttempt(task: TaskEntity, isCorrect: Boolean) {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()

            val attempt = PracticeAttemptEntity(
                task.id,
                isCorrect,
                currentTime
            )
            practiceAttemptDao.insert(attempt)

            task.egeNumber?.let { egeNum -> 
                 practiceStatisticsDao.createStatisticsIfNotExists(egeNum)
                practiceStatisticsDao.updateStatisticsAfterAttempt(
                    egeNum,
                    isCorrect,
                    currentTime
                )
            }
        }
    }

    suspend fun saveAttempt(task: TaskEntity, isCorrect: Boolean, source: String, taskType: String, textId: String) {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()

            val attempt = PracticeAttemptEntity(
                task.id,
                isCorrect,
                currentTime
            )
            practiceAttemptDao.insert(attempt)
            
            task.egeNumber?.let { egeNum ->
                practiceStatisticsDao.createStatisticsIfNotExists(egeNum)
                practiceStatisticsDao.updateStatisticsAfterAttempt(
                    egeNum,
                    isCorrect,
                    currentTime
                )
            }
        }
    }

    fun getStatisticsWithAttempts(): LiveData<List<PracticeStatisticsEntity>> = liveData(Dispatchers.IO) {
        practiceStatisticsDao.getStatisticsWithAttempts().collect { emit(it ?: emptyList()) }
    }

    fun getVariantStatistics(): LiveData<List<PracticeStatisticsEntity>> = liveData(Dispatchers.IO) {
        practiceStatisticsDao.getVariantStatistics().collect { emit(it ?: emptyList()) }
    }

    /**
     * Получает задание по его идентификатору
     * @param taskId идентификатор задания
     * @return задание или null, если не найдено
     */
    suspend fun getTaskById(taskId: Int): TaskEntity? {
        return withContext(Dispatchers.IO) {
            taskDao.getTaskByIdSync(taskId)
        }
    }
} 