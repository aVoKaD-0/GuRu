package com.ruege.mobile.data.repository

import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.network.api.PracticeApiService
import com.ruege.mobile.data.network.dto.request.PracticeAttemptSyncDto
import com.ruege.mobile.data.network.dto.request.PracticeStatisticSyncDto
import com.ruege.mobile.data.repository.Result
import com.ruege.mobile.data.network.dto.response.PracticeStatisticsGetResponse
import com.ruege.mobile.data.mapper.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeSyncRepository @Inject constructor(
    private val practiceApiService: PracticeApiService,
    private val practiceStatisticsDao: PracticeStatisticsDao,
    private val practiceAttemptDao: PracticeAttemptDao,
    private val userRepository: UserRepository 
) {

    suspend fun performFullSync(timestamp: Long?): Result<PracticeStatisticsGetResponse> = withContext(Dispatchers.IO) {
        Timber.d("Начало полной PULL-синхронизации статистики практики...")

        try {
            val response = practiceApiService.syncPracticeStatistics(timestamp)

            if (response.isSuccessful && response.body() != null) {
                val syncResponse = response.body()!!
                Timber.d("Сервер ответил, начинаем обновление локальной БД...")

                val serverStats = syncResponse.statistics.map { it.toEntity() }
                
                try {
                    val localStatsMap = practiceStatisticsDao.getAllStatisticsSortedByEgeNumber().first().associateBy { it.egeNumber }
                    val statsToUpdate = mutableListOf<PracticeStatisticsEntity>()
                    val statsToInsert = mutableListOf<PracticeStatisticsEntity>()

                    for (serverStat in serverStats) {
                        val localStat = localStatsMap[serverStat.egeNumber]
                        if (localStat != null) {
                            if (serverStat.lastAttemptDate > localStat.lastAttemptDate) {
                                statsToUpdate.add(serverStat)
                            }
                        } else {
                            statsToInsert.add(serverStat)
                        }
                    }

                    if (statsToInsert.isNotEmpty()) {
                        practiceStatisticsDao.insertAll(statsToInsert)
                        Timber.d("Вставлено ${statsToInsert.size} новых записей статистики.")
                    }
                    if (statsToUpdate.isNotEmpty()) {
                        practiceStatisticsDao.updateAll(statsToUpdate)
                        Timber.d("Обновлено ${statsToUpdate.size} записей статистики.")
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при слиянии данных статистики")
                }
                
                Timber.d("Локальная БД обновлена данными с сервера.")
                
                Result.Success(syncResponse)
            } else {
                Timber.e("Ошибка полной синхронизации: ${response.code()} - ${response.message()}")
                Result.Error("Ошибка сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Исключение во время полной синхронизации")
            Result.Error(e.message.toString())
        }
    }

    private fun PracticeStatisticsEntity.toSyncDto() = PracticeStatisticSyncDto(
        egeNumber = this.egeNumber,
        totalAttempts = this.totalAttempts,
        correctAttempts = this.correctAttempts,
        lastAttemptDate = this.lastAttemptDate
    )

    private fun PracticeAttemptEntity.toSyncDto() = PracticeAttemptSyncDto(
        attemptIdLocal = this.attemptId,
        taskId = this.taskId ?: 0,
        isCorrect = this.isCorrect,
        attemptDate = this.attemptDate
    )

    private fun com.ruege.mobile.data.network.dto.response.PracticeStatisticSyncResponseDto.toEntity(): PracticeStatisticsEntity {
        return PracticeStatisticsEntity(
            this.egeNumber,
            this.totalAttempts,
            this.correctAttempts,
            this.lastAttemptDate
        )
    }

    private fun com.ruege.mobile.data.network.dto.response.PracticeAttemptSyncResponseDto.toEntity(): PracticeAttemptEntity {
        val entity = PracticeAttemptEntity()
        entity.taskId = this.taskId
        entity.isCorrect = this.isCorrect
        entity.attemptDate = this.attemptDate
        return entity
    }
} 