package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.network.dto.response.TheoryContentDto
import com.ruege.mobile.data.repository.ContentRepository
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.utilss.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.ruege.mobile.data.repository.Result
import com.ruege.mobile.data.local.entity.DownloadedTheoryEntity
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class TheoryViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _theoryItemsState = MutableLiveData<Resource<List<ContentItem>>>()
    val theoryItemsState: LiveData<Resource<List<ContentItem>>> = _theoryItemsState

    private val _theoryContent = MutableLiveData<TheoryContentDto?>()
    val theoryContent: LiveData<TheoryContentDto?> = _theoryContent

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _downloadStatus = MutableLiveData<Resource<Unit>>()
    val downloadStatus: LiveData<Resource<Unit>> = _downloadStatus

    private val _deleteStatus = MutableLiveData<Resource<Unit>>()
    val deleteStatus: LiveData<Resource<Unit>> = _deleteStatus

    private val _isAnyTheorySelected = MutableLiveData<Boolean>()
    val isAnyTheorySelected: LiveData<Boolean> = _isAnyTheorySelected

    private val _batchDownloadResult = MutableLiveData<Resource<String>>()
    val batchDownloadResult: LiveData<Resource<String>> = _batchDownloadResult

    init {
        loadTheoryTopics()
    }

    private fun loadTheoryTopics() {
        viewModelScope.launch {
            _theoryItemsState.value = Resource.Loading()
            contentRepository.getTheoryTopicsStream().collect { entities ->
                val items = entities.map { ContentItem(it.contentId, it.title, it.description, it.type, it.parentId, it.isDownloaded, false) }
                _theoryItemsState.value = Resource.Success(items)
                _isAnyTheorySelected.value = false
            }
        }
    }

    fun loadTheoryContent(contentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _theoryContent.value = null
            _errorMessage.value = null
            Timber.d("Загрузка контента для теории: $contentId")

            try {
                val downloadedTheory = contentRepository.getDownloadedTheoryContent(contentId)
                if (downloadedTheory != null) {
                    Timber.d("Найдена скачанная теория для $contentId. Отображение из БД.")
                    val theoryDto = TheoryContentDto(
                        id = contentId.hashCode(),
                        egeNumber = 0, 
                        title = downloadedTheory.title,
                        content = downloadedTheory.htmlContent,
                        createdAt = "",
                        updatedAt = "" 
                    )
                    _theoryContent.value = theoryDto
                    _isLoading.value = false
                    return@launch
                }

                Timber.d("Скачанная теория для $contentId не найдена, загрузка из сети.")
                val theoryDto = contentRepository.getTheoryContentById(contentId)
                _isLoading.value = false

                if (theoryDto != null) {
                    if (theoryDto.content.isNotEmpty()) {
                        _theoryContent.value = theoryDto
                        Timber.d("HTML контент для $contentId (теория) успешно загружен из сети.")
                    } else {
                        _theoryContent.value = null
                        _errorMessage.value = "Содержимое теории отсутствует."
                        Timber.w("HTML контент для $contentId (теория) пуст.")
                    }
                } else {
                    _errorMessage.value = "Не удалось загрузить теорию. Проверьте подключение к интернету."
                    Timber.w("Не удалось загрузить теорию для $contentId, DTO is null.")
                    _theoryContent.value = null
                }
            } catch (e: Exception) {
                _isLoading.value = false
                Timber.e(e, "Ошибка загрузки HTML контента для $contentId (теория)")
                _errorMessage.value = "Ошибка загрузки теории: ${e.message ?: "Проверьте интернет-соединение"}"
                _theoryContent.value = null
            }
        }
    }

    fun downloadTheory(contentId: String) {
        viewModelScope.launch {
            contentRepository.downloadTheory(contentId).collect { result ->
                when (result) {
                    is Result.Loading -> _downloadStatus.value = Resource.Loading()
                    is Result.Success -> _downloadStatus.value = Resource.Success(Unit)
                    is Result.Error -> _downloadStatus.value = Resource.Error(result.message ?: "Ошибка скачивания")
                }
            }
        }
    }

    fun deleteDownloadedTheory(contentId: String) {
        viewModelScope.launch {
            contentRepository.deleteDownloadedTheory(contentId).collect { result ->
                when (result) {
                    is Result.Loading -> _deleteStatus.value = Resource.Loading()
                    is Result.Success -> _deleteStatus.value = Resource.Success(Unit)
                    is Result.Error -> _deleteStatus.value = Resource.Error(result.message ?: "Ошибка удаления")
                }
            }
        }
    }

    fun isTheoryDownloaded(contentId: String): Boolean {
        return contentRepository.getDownloadedTheory(contentId).value != null
    }

    fun getDownloadedTheory(contentId: String): LiveData<DownloadedTheoryEntity> {
        return contentRepository.getDownloadedTheory(contentId)
    }

    fun refreshTheoryTopics() {
        viewModelScope.launch {
            try {
                contentRepository.refreshTheoryTopics()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка обновления: ${e.message}"
            }
        }
    }

    fun selectAllTheories(isSelected: Boolean) {
        val currentItems = _theoryItemsState.value?.data ?: return
        val updatedItems = currentItems.map {
            it.copy(isSelected = isSelected)
        }
        _theoryItemsState.value = Resource.Success(updatedItems)
        _isAnyTheorySelected.value = updatedItems.any { it.isSelected }
    }

    fun selectTheory(item: ContentItem, isSelected: Boolean) {
        val currentItems = _theoryItemsState.value?.data ?: return
        val updatedItems = currentItems.map {
            if (it.contentId == item.contentId) {
                it.copy(isSelected = isSelected)
            } else {
                it
            }
        }
        _theoryItemsState.value = Resource.Success(updatedItems)
        _isAnyTheorySelected.value = updatedItems.any { it.isSelected }
    }

    fun downloadSelectedTheories() {
        viewModelScope.launch {
            val selectedItems = _theoryItemsState.value?.data?.filter { it.isSelected && !it.isDownloaded }

            if (selectedItems.isNullOrEmpty()) {
                _batchDownloadResult.value = Resource.Success("Нет новых теорий для скачивания.")
                return@launch
            }

            _batchDownloadResult.value = Resource.Loading()
            var successCount = 0
            var errorCount = 0

            for (item in selectedItems) {
                contentRepository.downloadTheory(item.contentId)
                    .firstOrNull { it !is Result.Loading }
                    .let { result ->
                        if (result is Result.Success) {
                            successCount++
                        } else {
                            errorCount++
                        }
                    }
            }

            val message = "Успешно скачано: $successCount. Ошибок: $errorCount."
            if (errorCount > 0) {
                _batchDownloadResult.value = Resource.Error(message)
            } else {
                _batchDownloadResult.value = Resource.Success(message)
            }
            selectAllTheories(false)
        }
    }
} 