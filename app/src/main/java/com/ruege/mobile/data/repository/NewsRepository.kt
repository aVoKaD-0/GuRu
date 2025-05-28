package com.ruege.mobile.data.repository

import android.util.Log
import com.ruege.mobile.data.local.dao.NewsDao
import com.ruege.mobile.data.local.entity.NewsEntity
import com.ruege.mobile.data.network.api.NewsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val newsDao: NewsDao,
    private val newsApiService: NewsApiService
) {

    private val TAG = "NewsRepository"
    private val DEFAULT_USER_ID = 1L // Заглушка

    fun getAllNewsStream(): Flow<List<NewsEntity>> {
        Log.d(TAG, "Getting all news stream from DAO")
        return newsDao.getAllNews() // Убедимся, что NewsDao.getAllNews() возвращает Flow
    }

    suspend fun refreshNews() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing news data from network...")
                
                // Получаем данные из сети
                val response = newsApiService.getNews() // Теперь вызываем напрямую
                
                if (response.isSuccessful && response.body() != null) {
                    val newsDtos = response.body()!!
                    val newsEntities = newsDtos.map { it.toEntity() } // Используем Kotlin map
                    
                    // Сначала очищаем старые данные
                    newsDao.deleteAll()
                    Log.d(TAG, "News table cleared before inserting new data from network")
                    
                    // Сохраняем в БД
                    newsDao.insertAll(newsEntities)
                    
                    Log.d(TAG, "Successfully refreshed ${newsEntities.size} news items from network")
                } else {
                    // Если не удалось получить данные из сети, просто логируем ошибку
                    Log.w(TAG, "Network request failed with code: ${response.code()}. NO mock data will be loaded.")
                    
                    // Очищаем таблицу, чтобы убрать старые данные
                    newsDao.deleteAll()
                    Log.d(TAG, "News table cleared after network error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing news from network", e)
                // Очищаем таблицу, чтобы убрать старые данные
                try {
                    newsDao.deleteAll()
                    Log.d(TAG, "News table cleared after exception")
                } catch (e2: Exception) {
                    Log.e(TAG, "Error clearing news table after exception", e2)
                }
            }
        }
    }
    
    suspend fun refreshLatestNews(limit: Int = 7) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing latest news data from network...")
                
                // Получаем последние новости из сети
                val response = newsApiService.getLatestNews(limit)
                
                if (response.isSuccessful && response.body() != null) {
                    val newsDtos = response.body()!!
                    val newsEntities = newsDtos.map { it.toEntity() }
                    
                    // Сначала очищаем старые данные
                    newsDao.deleteAll()
                    Log.d(TAG, "News table cleared before inserting latest news from network")
                    
                    // Сохраняем в БД
                    newsDao.insertAll(newsEntities)
                    
                    Log.d(TAG, "Successfully refreshed ${newsEntities.size} latest news items from network")
                } else {
                    // Если не удалось получить данные из сети, просто логируем ошибку
                    Log.w(TAG, "Network request for latest news failed with code: ${response.code()}. NO mock data will be loaded.")
                    
                    // Очищаем таблицу, чтобы убрать старые данные
                    newsDao.deleteAll()
                    Log.d(TAG, "News table cleared after network error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing latest news from network", e)
                // Очищаем таблицу, чтобы убрать старые данные
                try {
                    newsDao.deleteAll()
                    Log.d(TAG, "News table cleared after exception")
                } catch (e2: Exception) {
                    Log.e(TAG, "Error clearing news table after exception", e2)
                }
            }
        }
    }

    suspend fun clearAllNews() {
        withContext(Dispatchers.IO) { // Используем withContext
            try {
                Log.d(TAG, "Clearing all news from database...")
                newsDao.deleteAll()
                Log.d(TAG, "News table cleared.")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing news table", e)
            }
        }
    }
} 