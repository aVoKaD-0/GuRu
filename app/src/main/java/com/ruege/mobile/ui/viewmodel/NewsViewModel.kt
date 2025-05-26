package com.ruege.mobile.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.local.entity.NewsEntity
import com.ruege.mobile.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для работы с новостями.
 */
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val TAG = "NewsViewModel"

    /**
     * LiveData с данными новостей.
     */
    val newsLiveData: LiveData<List<NewsEntity>> = newsRepository
        .getAllNewsStream()
        .asLiveData(viewModelScope.coroutineContext) // Указываем context
    
    /**
     * Загружает последние новости через специальный API эндпоинт.
     */
    fun loadLatestNews(limit: Int = 7) {
        viewModelScope.launch {
            try {
                newsRepository.refreshLatestNews(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing latest news", e)
            }
        }
    }

    /**
     * Очищает все новости.
     * (Метод остается, но может быть неиспользуемым)
     */
    fun clearAllNews() {
        viewModelScope.launch { // Запускаем корутину в viewModelScope
            try {
                newsRepository.clearAllNews() // Вызываем suspend функцию напрямую
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing news", e)
            }
        }
    }
} 