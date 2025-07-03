package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.repository.NewsRepository
import com.ruege.mobile.model.NewsItem
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
     * LiveData с данными новостей в формате для UI.
     */
    val newsItemsLiveData: LiveData<List<NewsItem>> = newsRepository
        .getAllNewsStream()
        .asLiveData(viewModelScope.coroutineContext)
        .map { newsEntityList ->
            newsEntityList.map { entity ->
                NewsItem(
                    entity.title,
                    formatDate(entity.publicationDate),
                    entity.description,
                    entity.imageUrl,
                    null
                )
            }
        }
    
    init {
        loadLatestNews()
    }

    /**
     * Загружает последние новости через специальный API эндпоинт.
     */
    fun loadLatestNews(limit: Int = 7) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null 
            val result = newsRepository.refreshLatestNews(limit)
            if (result is Resource.Error) {
                Timber.e(result.message ?: "Unknown error refreshing latest news")
                _error.value = result.message ?: "Не удалось загрузить новости"
            }
            _isLoading.value = false
        }
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
            val netDate = Date(timestamp)
            sdf.format(netDate)
        } catch (e: Exception) {
            Timber.w(e, "Error formatting date from timestamp: $timestamp")
            "Дата не указана"
        }
    }
} 