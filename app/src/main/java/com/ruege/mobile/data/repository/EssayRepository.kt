package com.ruege.mobile.data.repository

import android.util.Log
import androidx.lifecycle.asFlow
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.data.network.api.EssayApiService
import com.ruege.mobile.data.network.dto.response.EssayContentDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.ruege.mobile.data.network.dto.response.EssaySummaryDto

@Singleton
class EssayRepository @Inject constructor(
    private val contentDao: ContentDao,
    private val essayApiService: EssayApiService,
    private val externalScope: CoroutineScope,
    private val userDao: UserDao
) {
    private val TAG = "EssayRepository"

    private val _contentLoaded = MutableStateFlow(false)
    val contentLoaded: StateFlow<Boolean> = _contentLoaded.asStateFlow()

    init {
        externalScope.launch {
            userDao.getFirstUserFlow().collect { user ->
                if (user == null) {
                    _contentLoaded.value = false
                    Timber.d("User logged out or not present, essay content loaded flag reset.")
                }
            }
        }
    }

    private val essayContentCache = mutableMapOf<String, EssayContentDto>()

    fun getEssayTopicsStream(): Flow<List<ContentEntity>> {
        Timber.d("Getting essay topics stream from DAO")
        return contentDao.getContentsByType("essay").asFlow()
    }

    suspend fun refreshEssayTopics() {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("LOG_CHAIN: EssayRepository.refreshEssayTopics - Начало.")
                val response = essayApiService.getAllEssayTopics()

                if (response.isSuccessful && response.body() != null) {
                    val essaySummaries = response.body()!!
                    
                    val allContentEntities = essaySummaries.map { dto -> 
                        val entity = dto.toContentEntity()
                        entity.setDownloaded(false)
                        entity
                    }
                    
                    contentDao.insertAll(allContentEntities)
                    Timber.d("LOG_CHAIN: EssayRepository.refreshEssayTopics - Успешно. Загружено ${allContentEntities.size} тем.")
                } else {
                    Timber.w("LOG_CHAIN: EssayRepository.refreshEssayTopics - Ошибка. Код: ${response.code()}.")
                }
            } catch (e: Exception) {
                Timber.e(e, "LOG_CHAIN: EssayRepository.refreshEssayTopics - Исключение")
            } finally {
                _contentLoaded.value = true
                Timber.d("LOG_CHAIN: EssayRepository.refreshEssayTopics - Блок finally. _contentLoaded = true")
            }
        }
    }

    suspend fun getEssayContentById(contentId: String): EssayContentDto? {
        if (essayContentCache.containsKey(contentId)) {
            Timber.d("Getting essay content from memory cache for ID: $contentId")
            return essayContentCache[contentId]
        }

        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Getting essay content from network for ID: $contentId")
                val response = essayApiService.getEssayContent(contentId)
                
                if (response.isSuccessful && response.body() != null) {
                    val essayContent = response.body()!!
                    Timber.d("Successfully loaded essay content from network, caching result.")
                    essayContentCache[contentId] = essayContent
                    return@withContext essayContent
                } else {
                    Timber.w("Failed to load essay content from network. Code: ${response.code()}")
                    return@withContext null
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading essay content", e)
                return@withContext null
            }
        }
    }
} 