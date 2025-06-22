package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.data.network.dto.response.EssayContentDto
import com.ruege.mobile.data.repository.EssayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EssayViewModel @Inject constructor(
    private val essayRepository: EssayRepository
) : ViewModel() {

    private val TAG = "EssayViewModel"

    val essayTopicsLiveData: LiveData<List<ContentEntity>> = essayRepository
        .getEssayTopicsStream()
        .asLiveData()

    private val _essayContent = MutableLiveData<EssayContentDto?>()
    val essayContent: LiveData<EssayContentDto?> = _essayContent

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        refreshEssayTopics()
    }

    private fun refreshEssayTopics() {
        viewModelScope.launch {
            Timber.d("LOG_CHAIN: EssayViewModel.refreshEssayTopics - Start.")
            try {
                essayRepository.refreshEssayTopics()
                Timber.d("LOG_CHAIN: EssayViewModel.refreshEssayTopics - Finished.")
            } catch (e: Exception) {
                Timber.e(e, "LOG_CHAIN: EssayViewModel.refreshEssayTopics - Error: ${e.message}")
            }
        }
    }
    
    fun loadEssayContent(contentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _essayContent.value = null
            _errorMessage.value = null
            Timber.d("Loading HTML content for essay: $contentId")

            try {
                val essayDto = essayRepository.getEssayContentById(contentId)
                _isLoading.value = false

                if (essayDto != null) {
                    if (essayDto.content.isNotEmpty()) {
                        _essayContent.value = essayDto
                        Timber.d("HTML content for $contentId (essay) loaded successfully.")
                    } else {
                        _essayContent.value = null
                        _errorMessage.value = "Essay content is empty."
                        Timber.w("HTML content for $contentId (essay) is empty.")
                    }
                } else {
                    _errorMessage.value = "Failed to load essay (DTO is null)."
                    Timber.w("Failed to load essay for $contentId, DTO is null.")
                    _essayContent.value = null
                }
            } catch (e: Exception) {
                _isLoading.value = false
                Timber.e(e, "Error loading HTML content for $contentId (essay)")
                _errorMessage.value = "Error loading essay: ${e.message ?: "Unknown error"}"
                _essayContent.value = null
            }
        }
    }

    fun clearContent() {
        _essayContent.value = null
        _errorMessage.value = null
        Timber.d("Active essay content cleared.")
    }
} 