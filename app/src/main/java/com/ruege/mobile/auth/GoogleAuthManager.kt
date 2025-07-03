package com.ruege.mobile.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import timber.log.Timber
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ruege.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleSignInClient: GoogleSignInClient
) {
    private val TAG = "GoogleAuthManager"
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    suspend fun signIn(activity: Activity, classicSignInLauncher: ActivityResultLauncher<Intent>): SignInResult {
        val serverClientId = context.getString(R.string.default_web_client_id)
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            Timber.d("Requesting credentials via Credential Manager...")
            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activity,
            )
            handleSignInSuccess(result)
        } catch (e: GetCredentialException) {
            Timber.d("Credential Manager error: ${e.javaClass.simpleName} - ${e.message}", e)
            if (e is NoCredentialException) {
                Timber.d("No credentials found, falling back to classic Google Sign-In.")
                classicSignInLauncher.launch(googleSignInClient.signInIntent)
                return SignInResult.Error("Fallback to classic sign-in")
            }
            SignInResult.Error("Google sign-in error: ${e.message}")
        } catch (e: Exception) {
            Timber.d("Unexpected sign-in error: ${e.message}", e)
            SignInResult.Error("An unexpected error occurred: ${e.message}")
        }
    }

    private fun handleSignInSuccess(result: GetCredentialResponse): SignInResult {
        val credential = result.credential
        Timber.d("Credential received: ${credential::class.java.simpleName}, type: ${credential.type}")

        return when (credential.type) {
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken

                    val segments = googleIdToken.split(".")
                    if (segments.size != 3) {
                        val errorMessage = "Invalid Google ID token format. Must be a JWT with 3 segments."
                        Timber.d(errorMessage)
                        return SignInResult.Error(errorMessage)
                    }

                    Timber.d("Successfully extracted Google ID Token, length: ${googleIdToken.length}")
                    SignInResult.Success(googleIdToken)
                } catch (e: Exception) {
                    Timber.d("Error extracting GoogleIdTokenCredential from data: ${e.message}", e)
                    SignInResult.Error("Error processing Google credentials")
                }
            }
            else -> {
                val errorMessage = "Unsupported credential type: ${credential.type}"
                Timber.d(errorMessage)
                SignInResult.Error(errorMessage)
            }
        }
    }

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().addOnCompleteListener {
                Timber.d("GoogleSignInClient sign-out complete.")
            }
        } catch (e: Exception) {
            Timber.d("Error during GoogleSignInClient sign-out", e)
        }
        Timber.d("App-level sign-out should clear tokens.")
    }
} 