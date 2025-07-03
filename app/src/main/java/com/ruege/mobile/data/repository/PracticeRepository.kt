package com.ruege.mobile.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.asLiveData
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

    fun getRecentAttempts(limit: Int): Flow<List<PracticeAttemptEntity>> {
        return practiceAttemptDao.getRecentAttempts(limit)
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

    fun getStatisticsWithAttempts(): LiveData<List<PracticeStatisticsEntity>> {
        return practiceStatisticsDao.getStatisticsWithAttempts().asLiveData()
    }

    fun getEssayStatistics(): LiveData<List<PracticeStatisticsEntity>> {
        return practiceStatisticsDao.getEssayStatistics().asLiveData()
    }

    fun getVariantStatistics(): LiveData<List<PracticeStatisticsEntity>> {
        return practiceStatisticsDao.getVariantStatistics().asLiveData()
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