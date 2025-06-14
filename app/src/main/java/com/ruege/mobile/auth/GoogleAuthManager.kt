package com.ruege.mobile.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class GoogleAuthManager(private val context: Context) {
    private val TAG = "GoogleAuthManager"
    private lateinit var googleSignInClient: GoogleSignInClient
    
    init {
        setupGoogleSignIn()
    }
    
    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("...")
                .requestEmail()
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(context, gso)
            Log.d(TAG, "Google Sign-In успешно настроен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при настройке Google Sign-In: ${e.message}", e)
        }
    }
    
    fun getGoogleSignInClient(): GoogleSignInClient {
        return googleSignInClient
    }
    
    fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "Выход выполнен")
        }
    }
} 