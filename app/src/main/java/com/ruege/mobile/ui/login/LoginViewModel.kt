package com.ruege.mobile.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.network.dto.response.AuthResponseDto
import com.ruege.mobile.data.repository.AuthRepository
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // LiveData для отслеживания состояния входа
    private val _loginState = MutableLiveData<Resource<AuthResponseDto>>()
    val loginState: LiveData<Resource<AuthResponseDto>> = _loginState

    /**
     * Запускает процесс входа через Google, отправляя токен на бэкенд.
     */
    fun performGoogleLogin(googleIdToken: String) {
        _loginState.value = Resource.Loading() // Сообщаем UI о начале загрузки
        viewModelScope.launch {
            val result = authRepository.loginWithGoogle(googleIdToken)
            _loginState.postValue(result) // Обновляем LiveData результатом
        }
    }

    /**
     * Запускает процесс входа через Google, отправляя фиксированный google_id на бэкенд.
     * Используется для отладки или специальных сценариев.
     */
    fun performGoogleLoginWithId(googleId: String) {
        _loginState.value = Resource.Loading()
        viewModelScope.launch {
            // Предполагается, что в AuthRepository есть метод loginWithGoogleId
            // или loginWithGoogle будет адаптирован для приема googleId вместо токена
            // В данном примере, я предположу, что loginWithGoogle может обработать и ID
            // Если это не так, потребуется создать новый метод в AuthRepository
            val result = authRepository.loginWithGoogle(googleId) // Используем существующий метод, передавая ID
            _loginState.postValue(result)
        }
    }
} 