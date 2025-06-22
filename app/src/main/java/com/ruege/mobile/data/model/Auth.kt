package com.ruege.mobile.data.model

data class GoogleAuthRequest(
    val id_token: String
)

data class AuthToken(
    val access_token: String,
    val token_type: String,
    val user_id: Int
)