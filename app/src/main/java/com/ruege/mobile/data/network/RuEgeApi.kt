package com.ruege.mobile.data.network

import com.ruege.mobile.data.model.AuthToken
import com.ruege.mobile.data.model.GoogleAuthRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RuEgeApi {
    @POST("api/v1/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): Response<AuthToken>
} 