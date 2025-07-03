package com.ruege.mobile.data.repository

import com.ruege.mobile.data.local.dao.NewsDao
import com.ruege.mobile.data.local.entity.NewsEntity
import com.ruege.mobile.data.network.api.NewsApiService
import com.ruege.mobile.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val newsDao: NewsDao,
    private val newsApiService: NewsApiService
) {

    fun getAllNewsStream(): Flow<List<NewsEntity>> {
        Timber.d("Getting all news stream from DAO")
        return newsDao.getAllNews()
    }
    
    suspend fun refreshLatestNews(limit: Int = 7): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Refreshing latest news data from network...")
                val response = newsApiService.getLatestNews(limit)
                
                if (response.isSuccessful && response.body() != null) {
                    val newsDtos = response.body()!!
                    val newsEntities = newsDtos.map { it.toEntity() }
                    
                    newsDao.insertAll(newsEntities)
                    
                    Timber.d("Successfully refreshed ${newsEntities.size} latest news items from network")
                    Resource.Success(Unit)
                } else {
                    Timber.w("Network request for latest news failed with code: ${response.code()}")
                    Resource.Error("Не удалось загрузить последние новости (код: ${response.code()})", null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing latest news from network")
                Resource.Error("Ошибка обновления последних новостей: ${e.message}", null)
            }
        }
    }

    suspend fun clearAllNews() {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Clearing all news from database...")
                newsDao.deleteAll()
                Timber.d("News table cleared.")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing news table")
            }
        }
    }
} 