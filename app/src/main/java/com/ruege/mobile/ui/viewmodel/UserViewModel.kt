package com.ruege.mobile.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.local.entity.UserEntity
import com.ruege.mobile.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadUserByGoogleId(googleId: String) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val user = userRepository.getByGoogleId(googleId)
                _currentUser.postValue(user)
                _isLoading.postValue(false)
            } catch (e: Exception) {
                _error.postValue("Ошибка загрузки данных пользователя: ${e.message}")
                _isLoading.postValue(false)
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