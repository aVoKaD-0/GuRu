package com.ruege.mobile.data.repository

import timber.log.Timber
import androidx.room.withTransaction
import com.ruege.mobile.data.local.AppDatabase
import com.ruege.mobile.data.local.dao.VariantDao
import com.ruege.mobile.data.local.dao.VariantSharedTextDao
import com.ruege.mobile.data.local.dao.VariantTaskOptionDao
import com.ruege.mobile.data.local.dao.VariantTaskDao
import com.ruege.mobile.data.local.dao.UserVariantTaskAnswerDao
import com.ruege.mobile.data.local.entity.VariantEntity
import com.ruege.mobile.data.local.entity.VariantTaskOptionEntity
import com.ruege.mobile.data.local.entity.VariantSharedTextEntity
import com.ruege.mobile.data.local.entity.VariantTaskEntity
import com.ruege.mobile.data.local.entity.UserVariantTaskAnswerEntity
import com.ruege.mobile.data.network.api.VariantApiService
import com.ruege.mobile.data.network.dto.VariantListItemDto
import com.ruege.mobile.data.network.dto.VariantTaskOptionDto
import com.ruege.mobile.data.network.dto.VariantSharedTextDto
import com.ruege.mobile.data.network.dto.VariantTaskDto
import com.ruege.mobile.data.network.dto.VariantDetailDto
import com.ruege.mobile.data.network.dto.UserAnswerPayloadDto
import com.ruege.mobile.data.network.dto.UserAnswerResponseItemDto
import com.ruege.mobile.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private const val TAG = "VariantRepository"

@Singleton
class VariantRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val variantApiService: VariantApiService,
    private val variantDao: VariantDao,
    private val variantSharedTextDao: VariantSharedTextDao,
    private val variantTaskDao: VariantTaskDao,
    private val variantTaskOptionDao: VariantTaskOptionDao,
    private val userVariantTaskAnswerDao: UserVariantTaskAnswerDao
) {

    init { 
        Timber.d("VariantRepository initialized. variantApiService is null: ${variantApiService == null}")
    }

    fun getVariants(): Flow<Resource<List<VariantEntity>>> {
        return variantDao.getAllVariants().map { variants ->
            if (variants.isEmpty()) {
                Resource.Loading() 
            } else {
                Resource.Success(variants)
            }
        }
    }

    suspend fun fetchVariantsFromServer() {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Attempting to fetch variants from network...")
                val response = variantApiService.getVariants()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
                    if (dtoList.isNotEmpty()) {
                        
                        val existingVariants = variantDao.getAllVariants().first()
                        val existingVariantsMap = existingVariants.associateBy { it.variantId }

                        val variantsToUpdate = mutableListOf<VariantEntity>()
                        val variantsToInsert = mutableListOf<VariantEntity>()

                        dtoList.forEach { dto ->
                            val existingVariant = existingVariantsMap[dto.variantId]
                            if (existingVariant != null) {
                                val updatedVariant = existingVariant.copy(
                                    name = dto.name,
                                    description = dto.description ?: "",
                                    isOfficial = dto.isOfficial,
                                    createdAt = dto.createdAt,
                                    updatedAt = dto.createdAt,
                                    taskCount = dto.taskCount
                                )
                                variantsToUpdate.add(updatedVariant)
                            } else {
                                variantsToInsert.add(dto.toEntity())
                            }
                        }

                        if (variantsToUpdate.isNotEmpty()) {
                            variantDao.updateVariants(variantsToUpdate)
                            Timber.d("Updated ${variantsToUpdate.size} variants.")
                        }
                        
                        if (variantsToInsert.isNotEmpty()) {
                            variantDao.insertOrUpdateVariants(variantsToInsert)
                            Timber.d("Inserted ${variantsToInsert.size} new variants.")
                        } else {
                            Timber.d("не знаю что вывести")
                        }

                    } else {
                        Timber.d("Fetched and saved/updated 0 variants from DB. Error/")
                    }
                } else {
                    Timber.d("Failed to fetch variants from network. Code: ${response.code()}")
                }
            } catch (e: Exception) {
                Timber.d("Error fetching variants and saving to DB", e)
            }
        }
    }

    private fun VariantListItemDto.toEntity(): VariantEntity {
        return VariantEntity(
            variantId = this.variantId,
            name = this.name,
            description = this.description ?: "",
            isOfficial = this.isOfficial,
            createdAt = this.createdAt,
            updatedAt = this.createdAt,
            taskCount = this.taskCount, 
            isDownloaded = false,
            lastAccessedAt = null
        )
    }
    
    suspend fun updateVariantLastAccessedTime(variantId: Int) {
        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        variantDao.updateLastAccessedTimestamp(variantId, timestamp)
    }

    suspend fun updateVariantTimer(variantId: Int, timeInMillis: Long) {
        withContext(Dispatchers.IO) {
            variantDao.updateRemainingTime(variantId, timeInMillis)
        }
    }

    suspend fun updateVariantDownloadedStatus(variantId: Int, isDownloaded: Boolean) {
        variantDao.updateDownloadStatus(variantId, isDownloaded)
    }

    fun getVariantDetails(variantId: Int): Flow<VariantEntity?> {
        return variantDao.getVariantByIdFlow(variantId)
    }

    fun getSharedTextsForVariant(variantId: Int): Flow<List<VariantSharedTextEntity>> {
        return variantSharedTextDao.getSharedTextsByVariantIdFlow(variantId)
    }

    fun getTasksForVariant(variantId: Int): Flow<List<VariantTaskEntity>> {
        return variantTaskDao.getTasksByVariantIdFlow(variantId)
    }

    fun VariantTaskOptionDto.toEntity(variantTaskId: Int): VariantTaskOptionEntity {
        return VariantTaskOptionEntity(
            id = this.variantId,
            variantTaskId = variantTaskId,
            text = this.userSubmittedAnswer,
            isCorrect = this.isCorrect,
            feedback = this.explanation,
            imageUrl = this.imageUrl
        )
    }

    fun VariantSharedTextDto.toEntity(variantIdParam: Int): VariantSharedTextEntity {
        return VariantSharedTextEntity(
            variantSharedTextId = this.id,
            variantId = this.variantId,
            textContent = this.textContent,
            sourceDescription = this.sourceDescription,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    fun VariantTaskDto.toEntity(variantIdParam: Int): VariantTaskEntity {
        return VariantTaskEntity(
            variantTaskId = this.id,
            variantId = this.variantId,
            orderInVariant = this.orderInVariant,
            taskStatement = this.taskStatement ?: "",
            maxPoints = this.maxPoints,
            egeNumber = this.egeNumber ?: "",
            title = this.title ?: "Задание ${this.orderInVariant}",
            originalTaskId = this.originalTaskId,
            variantSharedTextId = this.variantSharedTextId,
            difficulty = this.difficulty ?: 0, 
            taskType = this.taskType ?: "unknown",
            solutionText = this.solutionText,
            explanationText = this.explanationText,
            timeLimit = this.timeLimit ?: 0,
            createdAt = this.createdAt ?: Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            updatedAt = this.updatedAt ?: Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )
    }

    fun VariantDetailDto.toEntity(): VariantEntity {
        return VariantEntity(
            variantId = this.variantId,
            name = this.name,
            description = this.description ?: "",
            isOfficial = this.isOfficial,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt ?: this.createdAt,
            taskCount = this.tasks.size
        )
    }

    suspend fun fetchAndSaveVariantDetails(variantId: Int): Resource<VariantEntity> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Запрос деталей для варианта ID: $variantId")
                val response = variantApiService.getVariantById(variantId)
                if (response.isSuccessful) {
                    val variantDetailDto = response.body()
                    if (variantDetailDto != null) {
                        Timber.d("Детали варианта ID: $variantId успешно получены.")
                        appDatabase.withTransaction {
                            val existingEntity = variantDao.getVariantById(variantId)
                            val variantEntity = variantDetailDto.toEntity().copy(
                                isDownloaded = existingEntity?.isDownloaded ?: false,
                                lastAccessedAt = existingEntity?.lastAccessedAt
                            )
                            variantDao.insertOrUpdateVariant(variantEntity)
                            Timber.d("Основная информация для варианта ID: $variantId сохранена/обновлена.")

                            val sharedTextEntities = variantDetailDto.sharedTexts?.map { it.toEntity(variantId) }
                            if (!sharedTextEntities.isNullOrEmpty()) {
                                variantSharedTextDao.insertOrUpdateSharedTexts(sharedTextEntities)
                                Timber.d("Общие тексты для варианта ID: $variantId сохранены: ${sharedTextEntities.size} шт.")
                            }

                            val taskEntities = mutableListOf<VariantTaskEntity>()
                            val allOptionEntities = mutableListOf<VariantTaskOptionEntity>()

                            variantDetailDto.tasks.forEach { taskDto ->
                                val taskEntity = taskDto.toEntity(variantId)
                                taskEntities.add(taskEntity)

                                taskDto.options?.let { optionDtos ->
                                    val optionEntities = optionDtos.map { it.toEntity(taskEntity.variantTaskId) }
                                    allOptionEntities.addAll(optionEntities)
                                }
                            }

                            if (taskEntities.isNotEmpty()) {
                                variantTaskDao.insertOrUpdateTasks(taskEntities)
                                Timber.d("Задания для варианта ID: $variantId сохранены: ${taskEntities.size} шт.")
                            }

                            if (allOptionEntities.isNotEmpty()) {
                                variantTaskOptionDao.insertOrUpdateOptions(allOptionEntities)
                                Timber.d("Опции для заданий варианта ID: $variantId сохранены: ${allOptionEntities.size} шт.")
                            }
                        }
                        val updatedVariant = variantDao.getVariantById(variantId)
                        if (updatedVariant != null) {
                            Timber.d("Возвращаем успешно сохраненный вариант ID: $variantId из БД")
                            Resource.Success(updatedVariant)
                        } else {
                            Timber.e(
                                TAG,
                                "Ошибка: вариант ID: $variantId не найден в БД после сохранения."
                            )
                            Resource.Error("Ошибка: вариант не найден в БД после сохранения.", null)
                        }
                    } else {
                        Timber.d("Тело ответа для деталей варианта ID: $variantId пустое.")
                        Resource.Error("Тело ответа для деталей варианта пустое.", null)
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Timber.e(
                        TAG,
                        "Ошибка при получении деталей варианта ID: $variantId. Код: ${response.code()}, Сообщение: $errorMsg"
                    )
                    Resource.Error("Ошибка сети: $errorMsg", null)
                }
            } catch (e: Exception) {
                Timber.d("Исключение при получении или сохранении деталей варианта ID: $variantId", e)
                Resource.Error("Исключение: ${e.message}", null)
            }
        }
    }

    suspend fun deleteDownloadedVariant(variantId: Int): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                appDatabase.withTransaction {
                    variantTaskDao.deleteTasksByVariantId(variantId)
                    variantSharedTextDao.deleteSharedTextsByVariantId(variantId)
                    userVariantTaskAnswerDao.deleteAnswersForVariant(variantId)
                    variantDao.updateDownloadStatus(variantId, false)
                }
                Timber.d("Successfully deleted downloaded data for variant $variantId")
                Resource.Success(Unit)
            } catch (e: Exception) {
                Timber.d("Error deleting downloaded variant data for variant $variantId", e)
                Resource.Error("Ошибка удаления варианта: ${e.message}")
            }
        }
    }

    suspend fun getUserAnswersForVariantList(variantId: Int): List<UserVariantTaskAnswerEntity> {
        return withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.getAnswersForVariantList(variantId)
        }
    }
    
    suspend fun getUserAnswersForVariant(variantId: Int): Map<Int, UserVariantTaskAnswerEntity> {
        return withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.getAnswersForVariantList(variantId)
                .associateBy { it.variantTaskId }
        }
    }

    suspend fun getTask(variantTaskId: Int): VariantTaskEntity? {
        return withContext(Dispatchers.IO) {
            variantTaskDao.getTaskById(variantTaskId)
        }
    }

    /**
     * Сохраняет или обновляет ответ пользователя для конкретного задания.
     * variantTaskId используется как PrimaryKey в UserVariantTaskAnswerEntity.
     */
    suspend fun saveUserAnswer(variantId: Int, variantTaskId: Int, answerText: String): UserVariantTaskAnswerEntity? {
        return withContext(Dispatchers.IO) {
            val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val answerEntity = UserVariantTaskAnswerEntity(
                variantTaskId = variantTaskId,
                variantId = variantId,
                userSubmittedAnswer = answerText,
                isSubmissionCorrect = null, 
                pointsAwarded = null,
                checkResult = null,
                answeredTimestamp = timestamp
            )
            userVariantTaskAnswerDao.upsertAnswer(answerEntity)
            Timber.d("Ответ для задания $variantTaskId сохранен/обновлен.")
            userVariantTaskAnswerDao.getAnswerByTaskId(variantTaskId) 
        }
    }

    /**
     * Обновляет результат проверки ответа пользователя (правильность и баллы).
     */
    suspend fun updateUserAnswerResult(variantTaskId: Int, isCorrect: Boolean, pointsAwarded: Int) {
        withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.updateSubmissionResult(variantTaskId, isCorrect, pointsAwarded)
            Timber.d("Результат для ответа на задание $variantTaskId обновлен: correct=$isCorrect, points=$pointsAwarded")
        }
    }

    /**
     * Удаляет все ответы пользователя для указанного варианта.
     * Может быть полезно при сбросе прогресса по варианту.
     */
    suspend fun clearUserAnswersForVariant(variantId: Int) {
        withContext(Dispatchers.IO) {
            Timber.i("clearUserAnswersForVariant CALLED for variantId: $variantId")
            try {
                userVariantTaskAnswerDao.deleteAnswersForVariant(variantId)
                Timber.i("Successfully CALLED userVariantTaskAnswerDao.deleteAnswersForVariant for variantId: $variantId")
            } catch (e: Exception) {
                Timber.d("ERROR in userVariantTaskAnswerDao.deleteAnswersForVariant for variantId $variantId: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun submitUserAnswers(variantId: Int, answers: List<UserAnswerPayloadDto>): Resource<List<UserAnswerResponseItemDto>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Отправка ${answers.size} ответов для варианта $variantId на сервер.")
                val response = variantApiService.submitAnswers(variantId, answers)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        Timber.i("Ответы для варианта $variantId успешно отправлены. Получено ${responseBody.size} элементов в ответе.")
                        Resource.Success(responseBody)
                    } else {
                        Timber.d("Отправка ответов для варианта $variantId прошла успешно (код ${response.code()}), но тело ответа пустое.")
                        Resource.Error("Тело ответа от сервера пустое.", null)
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Timber.d("Ошибка отправки ответов для варианта $variantId. Код: ${response.code()}. Сообщение: $errorMsg")
                    Resource.Error("Ошибка сети при отправке ответов: $errorMsg (код ${response.code()})", null)
                }
            } catch (e: Exception) {
                Timber.d("Исключение при отправке ответов для варианта $variantId: ${e.message}", e)
                Resource.Error("Исключение при отправке ответов: ${e.message}", null)
            }
        }

    }

    suspend fun clearAllDownloadedVariants() {
        appDatabase.withTransaction {
            variantDao.resetAllDownloadStatuses()
            variantSharedTextDao.deleteAll()
            variantTaskDao.deleteAll()
            variantTaskOptionDao.deleteAll()
            userVariantTaskAnswerDao.deleteAll()
        }
    }

    suspend fun saveCheckIdForVariantTask(variantTaskId: Int, checkId: String) {
        withContext(Dispatchers.IO) {
            variantTaskDao.updateCheckId(variantTaskId, checkId)
        }
    }

    suspend fun saveEssayCheckResult(variantTaskId: Int, result: String) {
        withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.updateCheckResult(variantTaskId, result)
            variantTaskDao.updateCheckId(variantTaskId, null)
        }
    }
} 
