package com.ruege.mobile.auth

sealed class AuthEvent {
    object SessionExpired : AuthEvent()
} 