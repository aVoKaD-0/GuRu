package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.network.dto.response.AuthResponseDto
import com.ruege.mobile.data.repository.AuthRepository
import com.ruege.mobile.utilss.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _loginState = MutableLiveData<Resource<AuthResponseDto>>()
    val loginState: LiveData<Resource<AuthResponseDto>> = _loginState

    /**
     * Запускает процесс входа через Google, отправляя токен на бэкенд.
     */
    fun performGoogleLogin(googleIdToken: String) {
        _loginState.value = Resource.Loading() 
        viewModelScope.launch {
            val result = authRepository.loginWithGoogle(googleIdToken)
            _loginState.postValue(result) 
        }
    }

    /**
     * Запускает процесс входа через Google, отправляя фиксированный google_id на бэкенд.
     * Используется для отладки или специальных сценариев.
     */
    fun performGoogleLoginWithId(googleId: String) {
        _loginState.value = Resource.Loading()
        viewModelScope.launch {
            val result = authRepository.loginWithGoogle(googleId) 
            _loginState.postValue(result)
        }
    }
} 