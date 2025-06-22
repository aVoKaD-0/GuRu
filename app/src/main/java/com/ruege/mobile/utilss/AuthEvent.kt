package com.ruege.mobile.utilss

sealed class AuthEvent {
    object SessionExpired : AuthEvent()
} 