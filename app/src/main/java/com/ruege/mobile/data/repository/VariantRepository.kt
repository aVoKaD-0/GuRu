package com.ruege.mobile.data.repository

import android.util.Log
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
import com.ruege.mobile.utils.networkBoundResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

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
        Log.d(TAG, "VariantRepository initialized. variantApiService is null: ${variantApiService == null}")
    }

    // Получаем список вариантов. Используем networkBoundResource для кэширования и обновления.
    fun getVariants(): Flow<Resource<List<VariantEntity>>> = networkBoundResource(
        query = {
            variantDao.getAllVariants()
        },
        fetch = {
            Log.d(TAG, "networkBoundResource/fetch: Attempting to fetch variants from network...")
            val response = variantApiService.getVariants()
            Log.d(TAG, "networkBoundResource/fetch: Fetched variants. Success: ${response.isSuccessful}, Code: ${response.code()}")
            response
        },
        saveFetchResult = { responseBody: List<VariantListItemDto>? ->
            Log.d(TAG, "networkBoundResource/saveFetchResult: Saving variants. Count: ${responseBody?.size ?: 0}")
            responseBody?.let { dtoList ->
                val variantEntities = dtoList.map { it.toEntity() }
                withContext(Dispatchers.IO) {
                    variantDao.insertOrUpdateVariants(variantEntities)
                    Log.d(TAG, "networkBoundResource/saveFetchResult: Variants saved to DB.")
                }
            }
        },
        shouldFetch = { cachedData ->
            val fetch = true // Пока всегда true для отладки
            Log.d(TAG, "networkBoundResource/shouldFetch: Result = $fetch. Cached data is null: ${cachedData == null}")
            fetch
        },
        onFetchFailed = { throwable ->
            Log.e(TAG, "networkBoundResource/onFetchFailed: Error fetching variants", throwable)
        },
        resourceName = "VariantsList"
    )

    // Маппер из DTO в Entity
    private fun VariantListItemDto.toEntity(): VariantEntity {
        // Сервер возвращает created_at в формате ISO_OFFSET_DATE_TIME (с 'Z' или смещением)
        // А VariantEntity.updatedAt - это String? и может отсутствовать в списке
        // В VariantEntity нет поля task_count из DTO списка, но есть в AppDatabase Entity (oops, исправить)
        // В DTO списка нет updated_at, а в Entity есть. Для списка будем ставить updated_at = createdAt.
        return VariantEntity(
            variantId = this.variantId,
            name = this.name,
            description = this.description ?: "", // если null, ставим пустую строку
            isOfficial = this.isOfficial,
            createdAt = this.createdAt,
            updatedAt = this.createdAt, // Для списка вариантов ставим updatedAt = createdAt
            taskCount = this.taskCount, 
            isDownloaded = false, // По умолчанию не загружен
            lastAccessedAt = null // По умолчанию не было доступа
        )
    }
    
    // Метод для обновления времени последнего доступа к варианту
    suspend fun updateVariantLastAccessedTime(variantId: Int) {
        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        variantDao.updateLastAccessedTimestamp(variantId, timestamp)
    }

    // Метод для обновления статуса загрузки варианта
    suspend fun updateVariantDownloadedStatus(variantId: Int, isDownloaded: Boolean) {
        variantDao.updateDownloadStatus(variantId, isDownloaded)
    }
    
    // Получение списка загруженных вариантов
    fun getDownloadedVariants(): Flow<List<VariantEntity>> {
        return variantDao.getDownloadedVariants()
    }

    // --- Методы для получения деталей варианта из БД --- 

    fun getVariantDetails(variantId: Int): Flow<VariantEntity?> {
        return variantDao.getVariantByIdFlow(variantId) // Предполагаем, что есть такой метод
    }

    fun getSharedTextsForVariant(variantId: Int): Flow<List<VariantSharedTextEntity>> {
        return variantSharedTextDao.getSharedTextsByVariantIdFlow(variantId) // Предполагаем, что есть такой метод
    }

    fun getTasksForVariant(variantId: Int): Flow<List<VariantTaskEntity>> {
        return variantTaskDao.getTasksByVariantIdFlow(variantId) // Предполагаем, что есть такой метод
    }

    fun getOptionsForTask(variantTaskId: Int): Flow<List<VariantTaskOptionEntity>> {
        return variantTaskOptionDao.getOptionsByTaskIdFlow(variantTaskId) // Предполагаем, что есть такой метод
    }

    // TODO: Добавить методы для загрузки деталей варианта, когда это потребуется.
    // Это будет включать запрос к variantApiService.getVariantDetails(),
    // маппинг VariantDetailDto в VariantEntity, List<VariantSharedTextEntity>, List<VariantTaskEntity>,
    // и сохранение их с помощью соответствующих DAO.

    // --- Mappers DTO -> Entity for Variant Details ---

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
        // val currentTime = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // Больше не нужно генерировать время здесь
        return VariantSharedTextEntity(
            variantSharedTextId = this.id, // DTO теперь содержит правильный id (variant_shared_text_id)
            variantId = this.variantId,    // DTO теперь содержит variant_id, используем его вместо variantIdParam, если они должны совпадать.
                                          // Или, если variantIdParam - это ID основного варианта, а this.variantId - ID из самого shared_text, то нужно выбрать правильный.
                                          // Судя по JSON, this.variantId из DTO и variantIdParam должны быть одним и тем же ID варианта, к которому текст относится.
                                          // Оставляю this.variantId, так как он приходит с сервера для этого текста.
            textContent = this.textContent,
            sourceDescription = this.sourceDescription, // Теперь это поле есть в DTO
            createdAt = this.createdAt,             // Теперь это поле есть в DTO
            updatedAt = this.updatedAt              // Теперь это поле есть в DTO
        )
    }

    fun VariantTaskDto.toEntity(variantIdParam: Int): VariantTaskEntity {
        // val currentTime = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // Используем время из DTO, если есть
        return VariantTaskEntity(
            variantTaskId = this.id, // Это variant_task_id из DTO
            variantId = this.variantId,    // Используем variantId из DTO, он должен совпадать с variantIdParam
            orderInVariant = this.orderInVariant,
            taskStatement = this.taskStatement ?: "",
            maxPoints = this.maxPoints,
            egeNumber = this.egeNumber ?: "",
            title = this.title ?: "Задание ${this.orderInVariant}", // Используем title из DTO или генерируем
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
            description = this.description ?: "", // Добавил ?: "" для безопасности
            isOfficial = this.isOfficial,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt ?: this.createdAt, // Если updatedAt null, используем createdAt
            taskCount = this.tasks.size // Вычисляем taskCount из размера списка tasks
            // isDownloaded and lastAccessedAt are managed locally and should be preserved or set initially
        )
    }

suspend fun fetchAndSaveVariantDetails(variantId: Int): Resource<VariantEntity> {
    return withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Запрос деталей для варианта ID: $variantId")
            val response = variantApiService.getVariantById(variantId)
            if (response.isSuccessful) {
                val variantDetailDto = response.body()
                if (variantDetailDto != null) {
                    Log.d(TAG, "Детали варианта ID: $variantId успешно получены.")
                    appDatabase.withTransaction {
                            // 1. Удаляем старые зависимые данные для этого варианта
                            // Порядок удаления: от самых зависимых к менее зависимым, или тех, на которые есть FK с CASCADE
                            // UserVariantTaskAnswerEntity (зависит от VariantTaskEntity и VariantEntity)
                            userVariantTaskAnswerDao.deleteAnswersForVariant(variantId)
                            Log.d(TAG, "Старые UserVariantTaskAnswerEntity для варианта ID: $variantId удалены.")

                            // VariantTaskOptionEntity (зависит от VariantTaskEntity) - уже было
                            variantTaskOptionDao.deleteOptionsByVariantId(variantId) // Это удаляет опции, связанные через variant_id напрямую или каскадно от task_id
                            Log.d(TAG, "Старые VariantTaskOptionEntity для варианта ID: $variantId удалены.")
                            
                            // VariantTaskEntity (зависит от VariantEntity и VariantSharedTextEntity)
                            variantTaskDao.deleteTasksByVariantId(variantId) // НУЖНО ДОБАВИТЬ МЕТОД в VariantTaskDao
                            Log.d(TAG, "Старые VariantTaskEntity для варианта ID: $variantId удалены.")

                            // VariantSharedTextEntity (зависит от VariantEntity) - уже было
                        variantSharedTextDao.deleteSharedTextsForVariant(variantId)
                            Log.d(TAG, "Старые VariantSharedTextEntity для варианта ID: $variantId удалены.")

                            // 2. Сохраняем основную информацию о варианте (родительская сущность)
                        val existingEntity = variantDao.getVariantById(variantId)
                        val variantEntity = variantDetailDto.toEntity().copy(
                            isDownloaded = existingEntity?.isDownloaded ?: false,
                            lastAccessedAt = existingEntity?.lastAccessedAt
                        )
                            variantDao.insertOrUpdateVariant(variantEntity) // Использует OnConflictStrategy.REPLACE
                            Log.d(TAG, "Основная информация для варианта ID: $variantId сохранена/обновлена.")

                            // 3. Сохраняем общие тексты (зависят от VariantEntity)
                            val sharedTextEntities = variantDetailDto.sharedTexts?.map { it.toEntity(variantId) }
                        if (!sharedTextEntities.isNullOrEmpty()) {
                            variantSharedTextDao.insertOrUpdateSharedTexts(sharedTextEntities)
                                Log.d(TAG, "Общие тексты для варианта ID: $variantId сохранены: ${sharedTextEntities.size} шт.")
                        }

                            // 4. Сохраняем задания (зависят от VariantEntity и VariantSharedTextEntity)
                        val taskEntities = mutableListOf<VariantTaskEntity>()
                        val allOptionEntities = mutableListOf<VariantTaskOptionEntity>()
                            val userAnswerEntities = mutableListOf<UserVariantTaskAnswerEntity>()

                        variantDetailDto.tasks.forEach { taskDto ->
                            val taskEntity = taskDto.toEntity(variantId)
                            taskEntities.add(taskEntity)

                            taskDto.options?.let { optionDtos ->
                                val optionEntities = optionDtos.map { it.toEntity(taskEntity.variantTaskId) }
                                allOptionEntities.addAll(optionEntities)
                            }

                                taskDto.userVariantTaskAnswers?.let { userAnswerDto ->
                                    val userAnswerEntity = UserVariantTaskAnswerEntity(
                                        variantTaskId = userAnswerDto.variantTaskId,
                                        variantId = userAnswerDto.variantId,
                                        userSubmittedAnswer = userAnswerDto.userSubmittedAnswer,
                                        isSubmissionCorrect = userAnswerDto.isCorrect,
                                        pointsAwarded = userAnswerDto.orderPosition,
                                        answeredTimestamp = userAnswerDto.updatedAt
                                    )
                                    userAnswerEntities.add(userAnswerEntity)
                                    Log.d(TAG, "Подготовлен ответ пользователя для задачи ${taskDto.id}: ${userAnswerDto.userSubmittedAnswer}")
                                }
                        }

                        if (taskEntities.isNotEmpty()) {
                                variantTaskDao.insertOrUpdateTasks(taskEntities) // Должен быть OnConflictStrategy.REPLACE или IGNORE если task_id уникален глобально, или удалять надо было раньше
                                Log.d(TAG, "Задания для варианта ID: $variantId сохранены: ${taskEntities.size} шт.")
                        }

                            // 5. Сохраняем опции заданий (зависят от VariantTaskEntity)
                        if (allOptionEntities.isNotEmpty()) {
                            variantTaskOptionDao.insertOrUpdateOptions(allOptionEntities)
                                Log.d(TAG, "Опции для заданий варианта ID: $variantId сохранены: ${allOptionEntities.size} шт.")
                        }

                            // 6. Сохраняем ответы пользователя (зависят от VariantTaskEntity и VariantEntity)
                            if (userAnswerEntities.isNotEmpty()) {
                                userVariantTaskAnswerDao.upsertAnswers(userAnswerEntities)
                                Log.d(TAG, "Ответы пользователя для варианта ID: $variantId сохранены: ${userAnswerEntities.size} шт.")
                            }
                        }
                    val updatedVariant = variantDao.getVariantById(variantId)
                    if (updatedVariant != null) {
                        Log.d(TAG, "Возвращаем успешно сохраненный вариант ID: $variantId из БД")
                        Resource.Success(updatedVariant)
                    } else {
                        Log.e(
                            TAG,
                            "Ошибка: вариант ID: $variantId не найден в БД после сохранения."
                        )
                        Resource.Error("Ошибка: вариант не найден в БД после сохранения.", null)
                    }
                } else {
                    Log.e(TAG, "Тело ответа для деталей варианта ID: $variantId пустое.")
                    Resource.Error("Тело ответа для деталей варианта пустое.", null)
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Log.e(
                    TAG,
                    "Ошибка при получении деталей варианта ID: $variantId. Код: ${response.code()}, Сообщение: $errorMsg"
                )
                Resource.Error("Ошибка сети: $errorMsg", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при получении или сохранении деталей варианта ID: $variantId", e)
            Resource.Error("Исключение: ${e.message}", null)
        }
    }
    }

    // --- Методы для работы с ответами пользователя ---

    suspend fun saveUserAnswer(answer: UserVariantTaskAnswerEntity) {
        withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.upsertAnswer(answer)
        }
    }

    suspend fun saveUserAnswers(answers: List<UserVariantTaskAnswerEntity>) {
        withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.upsertAnswers(answers)
        }
    }

    suspend fun getUserAnswerForTask(variantTaskId: Int): UserVariantTaskAnswerEntity? {
        return withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.getAnswerByTaskId(variantTaskId)
        }
    }

    fun getUserAnswerForTaskFlow(variantTaskId: Int): Flow<UserVariantTaskAnswerEntity?> {
        return userVariantTaskAnswerDao.getAnswerByTaskIdFlow(variantTaskId)
    }

    fun getUserAnswersForVariantFlow(variantId: Int): Flow<List<UserVariantTaskAnswerEntity>> {
        return userVariantTaskAnswerDao.getAnswersForVariant(variantId)
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

    suspend fun updateUserTaskAnswerResult(variantTaskId: Int, isCorrect: Boolean, points: Int) {
        withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.updateSubmissionResult(variantTaskId, isCorrect, points)
            Log.d(TAG, "Результат ответа для variantTaskId: $variantTaskId обновлен: isCorrect=$isCorrect, points=$points")
        }
    }

    suspend fun deleteUserAnswersForVariant(variantId: Int) {
        withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.deleteAnswersForVariant(variantId)
        }
    }

    // --- Методы для управления статусом варианта ---

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
                answeredTimestamp = timestamp
            )
            userVariantTaskAnswerDao.upsertAnswer(answerEntity)
            Log.d(TAG, "Ответ для задания $variantTaskId сохранен/обновлен.")
            // Возвращаем сохраненную/обновленную сущность
            userVariantTaskAnswerDao.getAnswerByTaskId(variantTaskId) 
        }
    }

    /**
     * Обновляет результат проверки ответа пользователя (правильность и баллы).
     */
    suspend fun updateUserAnswerResult(variantTaskId: Int, isCorrect: Boolean, pointsAwarded: Int) {
        withContext(Dispatchers.IO) {
            userVariantTaskAnswerDao.updateSubmissionResult(variantTaskId, isCorrect, pointsAwarded)
            Log.d(TAG, "Результат для ответа на задание $variantTaskId обновлен: correct=$isCorrect, points=$pointsAwarded")
        }
    }

    /**
     * Удаляет все ответы пользователя для указанного варианта.
     * Может быть полезно при сбросе прогресса по варианту.
     */
    suspend fun clearUserAnswersForVariant(variantId: Int) {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "clearUserAnswersForVariant CALLED for variantId: $variantId")
            try {
                userVariantTaskAnswerDao.deleteAnswersForVariant(variantId)
                Log.i(TAG, "Successfully CALLED userVariantTaskAnswerDao.deleteAnswersForVariant for variantId: $variantId")
            } catch (e: Exception) {
                Log.e(TAG, "ERROR in userVariantTaskAnswerDao.deleteAnswersForVariant for variantId $variantId: ${e.message}", e)
                throw e // Пробрасываем исключение дальше, если это необходимо для вызывающего кода
            }
        }
    }

    // --- Методы для синхронизации с сервером ---
    suspend fun submitUserAnswers(variantId: Int, answers: List<UserAnswerPayloadDto>): Resource<List<UserAnswerResponseItemDto>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Отправка ${answers.size} ответов для варианта $variantId на сервер.")
                val response = variantApiService.submitAnswers(variantId, answers)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        Log.i(TAG, "Ответы для варианта $variantId успешно отправлены. Получено ${responseBody.size} элементов в ответе.")
                        // Тут можно дополнительно обработать ответ сервера, если это необходимо.
                        // Например, обновить какие-то локальные статусы на основе ответа.
                        // Пока просто возвращаем успех с данными.
                        Resource.Success(responseBody)
                    } else {
                        Log.w(TAG, "Отправка ответов для варианта $variantId прошла успешно (код ${response.code()}), но тело ответа пустое.")
                        Resource.Error("Тело ответа от сервера пустое.", null)
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Log.e(TAG, "Ошибка отправки ответов для варианта $variantId. Код: ${response.code()}. Сообщение: $errorMsg")
                    Resource.Error("Ошибка сети при отправке ответов: $errorMsg (код ${response.code()})", null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при отправке ответов для варианта $variantId: ${e.message}", e)
                Resource.Error("Исключение при отправке ответов: ${e.message}", null)
            }
        }

    }
}

// Generic Resource class (если у вас его еще нет, создайте в utils)
// sealed class Resource<T>(val data: T? = null, val error: Throwable? = null) {
//     class Success<T>(data: T) : Resource<T>(data)
//     class Loading<T>(data: T? = null) : Resource<T>(data)
//     class Error<T>(throwable: Throwable, data: T? = null) : Resource<T>(data, throwable)
// } 