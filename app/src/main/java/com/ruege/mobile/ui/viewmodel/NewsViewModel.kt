package com.ruege.mobile.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading


    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /**
     * LiveData с данными новостей.
     */
    val newsLiveData: LiveData<List<NewsEntity>> = newsRepository
        .getAllNewsStream()
        .asLiveData(viewModelScope.coroutineContext)
    
    /**
     * Загружает последние новости через специальный API эндпоинт.
     */
    fun loadLatestNews(limit: Int = 7) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null 
            try {
                newsRepository.refreshLatestNews(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing latest news", e)
                _error.value = "Не удалось загрузить новости: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Очищает все новости.
     * (Метод остается, но может быть неиспользуемым)
     */
    fun clearAllNews() {
        viewModelScope.launch { 
            
            
            try {
                newsRepository.clearAllNews() 
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing news", e)
            }
        }
    }
    
    /**
     * Сбрасывает состояние ошибки
     */
    fun clearError() {
        _error.value = null
    }
} 