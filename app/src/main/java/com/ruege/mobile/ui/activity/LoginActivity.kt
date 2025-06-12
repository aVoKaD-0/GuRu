package com.ruege.mobile.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.ruege.mobile.MainActivity
import com.ruege.mobile.R
import com.ruege.mobile.data.local.TokenManager
import com.ruege.mobile.databinding.ActivityLoginBinding
import com.ruege.mobile.utilss.Resource
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ruege.mobile.ui.viewmodel.LoginViewModel

/**
 * Экран авторизации пользователя (с использованием Credential Manager)
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    
    
    

    private lateinit var credentialManager: CredentialManager

    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        Log.d(TAG, "Токены при запуске: Access: ${accessToken != null}, Refresh: ${refreshToken != null}")
        
        if (accessToken != null && refreshToken != null) {
            Log.d(TAG, "Токены найдены. Переход на MainActivity")
            navigateToMain()
            return
        }

        
        credentialManager = CredentialManager.create(this)

        
        
        
        
        
        
        

        
        

        binding.btnGoogleSignIn.setOnClickListener {
            lifecycleScope.launch {
                signInWithGoogleCredentialManager()
            }
        }

        binding.btnGoogleSignIn2.setOnClickListener {
            Log.d(TAG, "Нажата кнопка btn_google_sign_in2")
            val googleId = "testnoneakeytotpopkok"
            showLoading(true)
            viewModel.performGoogleLoginWithId(googleId)
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            Log.d(TAG, "Нажата кнопка Политики конфиденциальности")
            val url = "https://gu-ru-ashen.vercel.app/privacy_policy"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
        }

        observeLoginState()
    }

    private suspend fun signInWithGoogleCredentialManager() {
        showLoading(true)

        val serverClientId = getString(R.string.default_web_client_id)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            
            .setAutoSelectEnabled(false)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            Log.d(TAG, "Запрос учетных данных через Credential Manager...")
            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = this@LoginActivity,
            )
            handleSignInSuccess(result)

        } catch (e: GetCredentialException) {
            Log.e(TAG, "Ошибка Credential Manager: ${e.javaClass.simpleName} - ${e.message}", e)
            showLoading(false)
            showError("Ошибка входа через Google: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка при входе: ${e.message}", e)
            showLoading(false)
            showError("Произошла неожиданная ошибка: ${e.message}")
        }
    }

    private fun handleSignInSuccess(result: GetCredentialResponse) {
        val credential = result.credential
        Log.d(TAG, "Получен credential типа: ${credential::class.java.simpleName}, тип внутри: ${credential.type}")

        when (credential.type) {
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken
                    
                    val segments = googleIdToken.split(".")
                    Log.d(TAG, "Google Token: Количество сегментов JWT: ${segments.size}")
                    Log.d(TAG, "Google Token: Первые 30 символов: ${googleIdToken.take(30)}...")
                    
                    if (segments.size != 3) {
                        Log.e(TAG, "Некорректный формат Google ID токена. Должен быть JWT с 3 сегментами.")
                        showLoading(false)
                        showError("Ошибка аутентификации: некорректный формат токена")
                        return
                    }
                    
                    Log.d(TAG, "Успешно извлечен Google ID Token, длина: ${googleIdToken.length}")
                    viewModel.performGoogleLogin(googleIdToken)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при извлечении GoogleIdTokenCredential из data: ${e.message}", e)
                    showLoading(false)
                    showError("Ошибка обработки учетных данных Google")
                }
            }
            else -> {
                Log.e(TAG, "Получен неподдерживаемый тип учетных данных: ${credential.type}")
                showLoading(false)
                showError("Неподдерживаемый тип учетных данных: ${credential.type}")
            }
        }
    }

    
    

    
    

    private fun observeLoginState() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    
                }
                is Resource.Success -> {
                    showLoading(false)
                    Log.d(TAG, "Успешный вход через бэкенд. Токены получены.")

                    resource.data?.let { tokenDto ->
                        Log.d(TAG, "Токены получены. Access: ${tokenDto.accessToken.take(10)}..., Refresh: ${tokenDto.refreshToken.take(10)}...")
                        tokenManager.saveAccessToken(tokenDto.accessToken)
                        tokenManager.saveRefreshToken(tokenDto.refreshToken)
                        val savedAccess = tokenManager.getAccessToken()
                        val savedRefresh = tokenManager.getRefreshToken()
                        Log.d(TAG, "Токены сохранены? Access: ${savedAccess != null}, Refresh: ${savedRefresh != null}")
                        Toast.makeText(this, "Вход выполнен успешно!", Toast.LENGTH_LONG).show()
                        navigateToMain()
                    } ?: run {
                        Log.e(TAG, "Данные токенов пусты после успешного ответа.")
                        showError("Ошибка: получены пустые данные токенов")
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Log.e(TAG, "Ошибка входа через бэкенд: ${resource.message}")
                    showError(resource.message ?: "Неизвестная ошибка бэкенда")
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnGoogleSignIn.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        showLoading(false)
    }

    private fun navigateToMain() {
         Log.d(TAG, "Переход на MainActivity")
         val intent = Intent(this@LoginActivity, MainActivity::class.java)
         intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
         startActivity(intent)
    }

    companion object {
         
         
         private const val TAG = "LoginActivity"
    }
} 