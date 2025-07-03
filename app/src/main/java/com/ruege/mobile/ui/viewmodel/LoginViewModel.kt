package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.network.dto.response.AuthResponseDto
import com.ruege.mobile.data.network.dto.response.RegisterStartResponseDto
import com.ruege.mobile.data.repository.AuthRepository
import com.ruege.mobile.data.repository.LoginResult
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<AuthResponseDto>>()
    val loginState: LiveData<Resource<AuthResponseDto>> = _loginState

    private val _emailLoginState = MutableLiveData<Resource<LoginResult>>()
    val emailLoginState: LiveData<Resource<LoginResult>> = _emailLoginState

    private val _registrationStartState = MutableLiveData<Resource<RegisterStartResponseDto>>()
    val registrationStartState: LiveData<Resource<RegisterStartResponseDto>> = _registrationStartState

    private val _setUsernameState = MutableLiveData<Resource<Map<String, String>>>()
    val setUsernameState: LiveData<Resource<Map<String, String>>> = _setUsernameState

    private val _verifyCodeState = MutableLiveData<Resource<AuthResponseDto>>()
    val verifyCodeState: LiveData<Resource<AuthResponseDto>> = _verifyCodeState
    
    private val _passwordRecoveryState = MutableLiveData<Resource<Map<String, String>>>()
    val passwordRecoveryState: LiveData<Resource<Map<String, String>>> = _passwordRecoveryState

    private val _enable2faState = MutableLiveData<Resource<Map<String, String>>>()
    val enable2faState: LiveData<Resource<Map<String, String>>> = _enable2faState

    private val _resendCodeState = MutableLiveData<Resource<Unit>>()
    val resendCodeState: LiveData<Resource<Unit>> = _resendCodeState

    private val _tfaVerifyState = MutableLiveData<Resource<AuthResponseDto>>()
    val tfaVerifyState: LiveData<Resource<AuthResponseDto>> = _tfaVerifyState

    var sessionToken: String? = null

    /**
     * Запускает процесс входа через Google, отправляя токен на бэкенд.
     */
    fun performGoogleLogin(googleIdToken: String) {
        _loginState.value = Resource.Loading()
        viewModelScope.launch {
            _loginState.postValue(authRepository.loginWithGoogle(googleIdToken))
        }
    }

    /**
     * Запускает процесс входа по email и паролю.
     */
    fun performEmailLogin(email: String, password: String) {
        _emailLoginState.value = Resource.Loading()
        viewModelScope.launch {
            _emailLoginState.postValue(authRepository.loginWithEmail(email, password))
        }
    }

    /**
     * Запускает процесс регистрации через email и пароль.
     */
    fun startRegistration(email: String, password: String, recaptchaToken: String) {
        _registrationStartState.value = Resource.Loading()
        viewModelScope.launch {
            val result = authRepository.registerStart(email, password, recaptchaToken)
            if (result is Resource.Success) {
                sessionToken = result.data?.session_token
            }
            _registrationStartState.postValue(result)
        }
    }

    fun setUsername(username: String) {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            _setUsernameState.value = Resource.Error("Session token is missing")
            return
        }
        _setUsernameState.value = Resource.Loading()
        viewModelScope.launch {
            _setUsernameState.postValue(authRepository.setUsername(currentSessionToken, username))
        }
    }

    fun verifyCode(code: String) {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            _verifyCodeState.value = Resource.Error("Session token is missing")
            return
        }
        _verifyCodeState.value = Resource.Loading()
        viewModelScope.launch {
            _verifyCodeState.postValue(authRepository.verifyCode(currentSessionToken, code))
        }
    }

    fun requestPasswordRecovery(email: String, recaptchaToken: String) {
        _passwordRecoveryState.value = Resource.Loading()
        viewModelScope.launch {
            _passwordRecoveryState.postValue(authRepository.requestPasswordRecovery(email, recaptchaToken))
        }
    }

    fun enable2faOnRegistration(userId: Int) {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            _enable2faState.value = Resource.Error("Session token is missing")
            return
        }
        _enable2faState.value = Resource.Loading()
        viewModelScope.launch {
            _enable2faState.postValue(authRepository.enable2faOnRegistration(currentSessionToken, userId))
        }
    }

    fun verifyTfaCode(loginSessionToken: String, tfaCode: String) {
        _tfaVerifyState.value = Resource.Loading()
        viewModelScope.launch {
            _tfaVerifyState.postValue(authRepository.verifyTfaLogin(loginSessionToken, tfaCode))
        }
    }

    fun resendConfirmationCode() {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            _resendCodeState.value = Resource.Error("Session token is missing")
            return
        }
        _resendCodeState.value = Resource.Loading()
        viewModelScope.launch {
            _resendCodeState.postValue(authRepository.resendConfirmationCode(currentSessionToken))
        }
    }

    /**
     * Запускает процесс входа через Google, отправляя фиксированный google_id на бэкенд.
     * Используется для отладки или специальных сценариев.
     */
    fun performGoogleLoginWithId(googleId: String) {
        _loginState.value = Resource.Loading()
        viewModelScope.launch {
            _loginState.postValue(authRepository.loginWithGoogle(googleId))
        }
    }
    
    fun clearRegistrationStates() {
        _loginState.value = null
        _emailLoginState.value = null
        _registrationStartState.value = null
        _setUsernameState.value = null
        _verifyCodeState.value = null
        _passwordRecoveryState.value = null
        _enable2faState.value = null
        _resendCodeState.value = null
        _tfaVerifyState.value = null
        sessionToken = null
    }
} 