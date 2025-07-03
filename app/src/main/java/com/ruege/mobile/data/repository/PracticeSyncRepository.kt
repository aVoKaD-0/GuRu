package com.ruege.mobile.data.repository

import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.network.api.PracticeApiService
import com.ruege.mobile.data.network.dto.response.PracticeAttemptSyncResponseDto
import com.ruege.mobile.data.network.dto.request.PracticeStatisticSyncDto
import com.ruege.mobile.data.network.dto.response.PracticeStatisticsGetResponse
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
) {

    suspend fun performFullSync(timestamp: Long?): Result<PracticeStatisticsGetResponse> = withContext(Dispatchers.IO) {
        Timber.d("Начало полной PULL-синхронизации статистики практики...")

        try {
            val response = practiceApiService.syncPracticeStatistics(timestamp)

            if (response.isSuccessful && response.body() != null) {
                val syncResponse = response.body()!!
                Timber.d("Сервер ответил, начинаем обновление локальной БД...")
                Timber.d("Raw DTOs from Moshi: ${syncResponse.statistics}")

                val serverStats = syncResponse.statistics.map { it.toEntity() }
                Timber.d("Entities after mapping: ${serverStats.joinToString { stat -> "[egeNumber=${stat.egeNumber}]" }}")
                
                try {
                    val localStatsMap = practiceStatisticsDao.getAllStatisticsSortedByEgeNumber().first().associateBy { it.egeNumber }
                    val statsToUpdate = mutableListOf<PracticeStatisticsEntity>()

                    for (serverStat in serverStats) {
                        val localStat = localStatsMap[serverStat.egeNumber]
                        if (localStat == null) {
                            statsToUpdate.add(serverStat)
                        } else {
                            if (serverStat.lastAttemptDate > localStat.lastAttemptDate) {
                                statsToUpdate.add(serverStat)
                            }
                        }
                    }

                    Timber.d("Содержимое списка statsToUpdate перед вставкой в БД (${statsToUpdate.size} элементов):")
                    statsToUpdate.forEachIndexed { index, stat ->
                        Timber.d("  [$index]: egeNumber='${stat.egeNumber}', totalAttempts=${stat.totalAttempts}, correctAttempts=${stat.correctAttempts}, lastAttemptDate=${stat.lastAttemptDate}, variantData=${stat.variantData?.length ?: "null"} chars")
                    }

                    if (statsToUpdate.isNotEmpty()) {
                        practiceStatisticsDao.insertAll(statsToUpdate)
                        Timber.d("Вставлено/обновлено ${statsToUpdate.size} записей статистики.")
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

    private fun PracticeStatisticSyncDto.toEntity(): PracticeStatisticsEntity {
        return PracticeStatisticsEntity().apply {
            egeNumber = this@toEntity.egeNumber
            totalAttempts = this@toEntity.totalAttempts
            correctAttempts = this@toEntity.correctAttempts
            lastAttemptDate = this@toEntity.lastAttemptDate
            variantData = this@toEntity.variantData
        }
    }

    private fun PracticeAttemptSyncResponseDto.toEntity(): PracticeAttemptEntity {
        val entity = PracticeAttemptEntity()
        entity.taskId = this.taskId
        entity.isCorrect = this.isCorrect
        entity.attemptDate = this.attemptDate
        return entity
    }
} 