package com.ruege.mobile.data.repository

import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.network.api.PracticeApiService
import com.ruege.mobile.data.network.dto.request.PracticeAttemptSyncDto
import com.ruege.mobile.data.network.dto.request.PracticeStatisticSyncDto
import com.ruege.mobile.data.network.dto.request.PracticeSyncRequest
import com.ruege.mobile.data.network.dto.response.PracticeSyncResponse
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
    private val userRepository: UserRepository // Для получения userId
) {

    suspend fun performFullSync() = withContext(Dispatchers.IO) {
        Timber.d("Начало полной синхронизации статистики практики...")
        val userId = userRepository.getFirstUser()?.userId?.toString() // Изменено
        if (userId == null) {
            Timber.w("Пользователь не авторизован. Полная синхронизация статистики отменена.")
            return@withContext Result.Failure(Exception("Пользователь не авторизован"))
        }

        try {
            // 1. Собираем все локальные данные
            val localStats = practiceStatisticsDao.getAllStatisticsSortedByEgeNumber().first()
            val localAttempts = practiceAttemptDao.getRecentAttempts(Int.MAX_VALUE).first() // Все попытки

            val statsDto = localStats.map { it.toSyncDto() }
            val attemptsDto = localAttempts.map { it.toSyncDto() }

            val request = PracticeSyncRequest(userId, statsDto, attemptsDto)

            // 2. Отправляем на сервер
            val response = practiceApiService.syncPracticeStatistics(request)

            if (response.isSuccessful && response.body() != null) {
                val syncResponse = response.body()!!
                Timber.d("Сервер ответил, начинаем обновление локальной БД...")
                // 3. Очищаем локальные таблицы (или делаем более умный merge)
                // Для простоты пока полная очистка и вставка
                // practiceStatisticsDao.clearAllStatistics() // НЕ ОЧИЩАЕМ
                // practiceAttemptDao.clearAllAttempts()     // НЕ ОЧИЩАЕМ

                // 4. Сохраняем данные от сервера
                val serverStatsEntities = syncResponse.statistics.map { it.toEntity() }
                val serverAttemptsEntities = syncResponse.attempts.map { it.toEntity() }

                practiceStatisticsDao.insertAll(serverStatsEntities)
                practiceAttemptDao.insertAll(serverAttemptsEntities)
                Timber.d("Локальная БД обновлена данными с сервера. Статистики: ${serverStatsEntities.size}, Попытки: ${serverAttemptsEntities.size}")
                Result.Success(syncResponse) // Возвращаем ответ сервера
            } else {
                Timber.e("Ошибка полной синхронизации: ${response.code()} - ${response.message()}")
                Result.Failure(Exception("Ошибка сервера: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Исключение во время полной синхронизации")
            Result.Failure(e)
        }
    }

    // TODO: Реализовать sendUpdates (для эндпоинта /branch)
    // Этот метод будет собирать только новые/измененные данные (например, за последнюю сессию или по флагу isSynced)
    // и отправлять их на сервер.

    // Методы-мапперы для DTO <-> Entity
    private fun PracticeStatisticsEntity.toSyncDto() = PracticeStatisticSyncDto(
        egeNumber = this.egeNumber,
        totalAttempts = this.totalAttempts,
        correctAttempts = this.correctAttempts,
        lastAttemptDate = this.lastAttemptDate
    )

    private fun PracticeAttemptEntity.toSyncDto() = PracticeAttemptSyncDto(
        attemptIdLocal = this.attemptId,
        taskId = this.taskId ?: 0, // Если taskId nullable, нужно значение по умолчанию
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
        return PracticeAttemptEntity(
            this.taskId,
            this.isCorrect,
            this.attemptDate
        )
    }
} 