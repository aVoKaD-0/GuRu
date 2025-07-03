package com.ruege.mobile.auth

sealed class SignInResult {
    data class Success(val idToken: String) : SignInResult()
    data class Error(val message: String) : SignInResult()
} 