package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.local.entity.UserEntity
import com.ruege.mobile.data.repository.UserRepository
import com.ruege.mobile.data.repository.AuthRepository
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _twoFactorAuthResult = MutableLiveData<Resource<Unit>>()
    val twoFactorAuthResult: LiveData<Resource<Unit>> = _twoFactorAuthResult
    
    private val _passwordChangeResult = MutableLiveData<Resource<Unit>>()
    val passwordChangeResult: LiveData<Resource<Unit>> = _passwordChangeResult

    fun changePassword() {
        viewModelScope.launch {
            _passwordChangeResult.postValue(Resource.Loading())
            val result = authRepository.changePassword()
            _passwordChangeResult.postValue(result)
        }
    }

    fun toggleTwoFactorAuth(enable: Boolean) {
        viewModelScope.launch {
            _twoFactorAuthResult.postValue(Resource.Loading())
            val result = if (enable) {
                authRepository.enable2fa()
            } else {
                authRepository.disable2fa()
            }

            when (result) {
                is Resource.Success -> {
                    val updatedUser = _currentUser.value?.apply {
                        this.setIs2faEnabled(enable)
                    }
                    if (updatedUser != null) {
                        userRepository.insert(
                            username = updatedUser.username,
                            email = updatedUser.email,
                            is2faEnabled = updatedUser.isIs2faEnabled(),
                            avatarUrl = updatedUser.avatarUrl
                        )
                    }
                    _currentUser.postValue(updatedUser)
                    _twoFactorAuthResult.postValue(Resource.Success(Unit))
                }
                is Resource.Error -> {
                    _twoFactorAuthResult.postValue(Resource.Error(result.message ?: "Unknown error"))
                }
                is Resource.Loading -> {
                    
                }
            }
        }
    }

    fun getFirstUser() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val hasUsers = userRepository.hasUsers()
                if (hasUsers) {
                    val user = userRepository.getFirstUser()
                    _currentUser.postValue(user)
                } else {
                    _currentUser.postValue(null)
                }
                _isLoading.postValue(false)
            } catch (e: Exception) {
                _error.postValue("Ошибка загрузки данных пользователя: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }
} 