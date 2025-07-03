package com.ruege.mobile.data.repository

import com.ruege.mobile.data.network.dto.response.AuthResponseDto
import com.ruege.mobile.data.network.dto.response.Login2faResponseDto

sealed class LoginResult {
    data class Success(val authResponse: AuthResponseDto) : LoginResult()
    data class TwoFactorRequired(val tfaResponse: Login2faResponseDto) : LoginResult()
} 