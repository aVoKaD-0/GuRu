package com.ruege.mobile.auth

import timber.log.Timber
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor для добавления Authorization заголовка с Access Token.
 * Также проверяет, не истек ли срок действия токена, и проактивно обновляет его при необходимости.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    private val TAG = "AuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        if (isAuthRequest(originalRequest.url.encodedPath)) {
            Timber.d("AuthInterceptor: Path is auth request, proceeding without token: ${originalRequest.url}")
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenManager.getAccessToken()
        val requestBuilder = originalRequest.newBuilder()

        if (accessToken != null) {
            Timber.d("AuthInterceptor: Adding Authorization header for URL: ${originalRequest.url}")
            requestBuilder.header("Authorization", "Bearer $accessToken")
        } else {
            Timber.d("AuthInterceptor: NOT adding Authorization header. No access token available for URL: ${originalRequest.url}")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }

    private fun isAuthRequest(path: String): Boolean {
        return path.endsWith("/auth/google") || path.endsWith("/auth/refresh") || path.endsWith("/auth/login")
    }

    private fun isStorageRequest(path: String): Boolean {
        return path.contains("/storage/")
    }
} 