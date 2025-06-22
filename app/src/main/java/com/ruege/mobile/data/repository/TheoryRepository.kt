package com.ruege.mobile.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.DownloadedTheoryDao
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.data.local.entity.DownloadedTheoryEntity
import com.ruege.mobile.data.network.api.TheoryApiService
import com.ruege.mobile.data.network.dto.response.TheoryContentDto
import com.ruege.mobile.data.network.dto.response.TheorySummaryDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TheoryRepository @Inject constructor(
    private val contentDao: ContentDao,
    private val theoryApiService: TheoryApiService,
    private val downloadedTheoryDao: DownloadedTheoryDao,
    private val externalScope: CoroutineScope,
    private val userDao: UserDao
) {

    private val TAG = "TheoryRepository"

    private val _contentLoaded = MutableStateFlow(false)
    val contentLoaded: StateFlow<Boolean> = _contentLoaded.asStateFlow()

    private val theoryContentCache = mutableMapOf<String, TheoryContentDto>()

    init {
        externalScope.launch {
            userDao.getFirstUserFlow().collect { user ->
                if (user == null) {
                    _contentLoaded.value = false
                    Timber.d("User logged out or not present, theory content loaded flag reset.")
                }
            }
        }
    }

    fun getTheoryTopicsStream(): Flow<List<ContentEntity>> {
        Timber.d("Getting theory topics stream from DB")
        return contentDao.getContentsByType("theory").asFlow()
            .combine(downloadedTheoryDao.getAllIdsAsFlow()) { entities, downloadedIds ->
                val downloadedIdsSet = downloadedIds.toSet()
                entities.map { entity ->
                    entity.apply {
                        this.isDownloaded = downloadedIdsSet.contains(this.contentId)
                    }
                }
            }
    }

    suspend fun refreshTheoryTopics() {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("LOG_CHAIN: TheoryRepository.refreshTheoryTopics - Начало.")
                val response = theoryApiService.getAllTheory()

                if (response.isSuccessful && response.body() != null) {
                    val theorySummaries = response.body()!!
                    val theoryEntities = theorySummaries.map { it.toContentEntity() }
                    contentDao.insertAll(theoryEntities)
                    Timber.d("LOG_CHAIN: TheoryRepository.refreshTheoryTopics - Успешно. Загружено и сохранено ${theoryEntities.size} тем.")
                } else {
                    Timber.w("LOG_CHAIN: TheoryRepository.refreshTheoryTopics - Ошибка. Код: ${response.code()}.")
                }
            } catch (e: Exception) {
                Timber.e(e, "LOG_CHAIN: TheoryRepository.refreshTheoryTopics - Исключение.")
            } finally {
                _contentLoaded.value = true
                Timber.d("LOG_CHAIN: TheoryRepository.refreshTheoryTopics - Блок finally. _contentLoaded = true")
            }
        }
    }

    suspend fun getTheoryContentById(contentId: String): TheoryContentDto? {
        if (theoryContentCache.containsKey(contentId)) {
            Timber.d("Getting theory content from memory cache for ID: $contentId")
            return theoryContentCache[contentId]
        }

        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Getting theory content from network for ID: $contentId")
                val response = theoryApiService.getTheoryContent(contentId)

                if (response.isSuccessful && response.body() != null) {
                    val theoryContent = response.body()!!
                    Timber.d("Successfully loaded theory content from network, caching result.")
                    theoryContentCache[contentId] = theoryContent
                    return@withContext theoryContent
                } else {
                    Timber.w("Failed to load theory content from network. Code: ${response.code()}")
                    return@withContext null
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading theory content", e)
                return@withContext null
            }
        }
    }

    suspend fun downloadTheory(contentId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            val theoryDto = getTheoryContentById(contentId)
            if (theoryDto != null) {
                val entity = DownloadedTheoryEntity(
                    contentId,
                    theoryDto.title,
                    theoryDto.content,
                    System.currentTimeMillis()
                )
                downloadedTheoryDao.insert(entity)
                contentDao.updateDownloadStatus(contentId, true)
                emit(Result.Success(Unit))
            } else {
                emit(Result.Error("Не удалось получить данные для скачивания"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Неизвестная ошибка"))
        }
    }.flowOn(Dispatchers.IO)

    fun getDownloadedTheory(contentId: String): LiveData<DownloadedTheoryEntity> {
        return downloadedTheoryDao.getDownloadedTheoryById(contentId)
    }

    fun deleteDownloadedTheory(contentId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            downloadedTheoryDao.deleteById(contentId)
            contentDao.updateDownloadStatus(contentId, false)
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Неизвестная ошибка"))
        }
    }.flowOn(Dispatchers.IO)

    fun getTheoryContent(contentId: String): Flow<Result<TheoryContentDto>> = flow {
        emit(Result.Loading)
        val cachedContent = theoryContentCache[contentId]
        if (cachedContent != null) {
            emit(Result.Success(cachedContent))
            return@flow
        }

        try {
            val response = theoryApiService.getTheoryContent(contentId)
            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!
                theoryContentCache[contentId] = content
                emit(Result.Success(content))
            } else {
                emit(Result.Error("Ошибка загрузки: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Result.Error("Исключение: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getDownloadedTheoryContent(contentId: String): DownloadedTheoryEntity? {
        return withContext(Dispatchers.IO) {
            downloadedTheoryDao.getById(contentId)
        }
    }
} 