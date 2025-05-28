package com.ruege.mobile.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Для управления видимостью ProgressBar
import android.widget.Toast
import androidx.activity.viewModels // Для получения ViewModel
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.ruege.mobile.MainActivity
// import com.ruege.mobile.auth.GoogleAuthManager
import com.ruege.mobile.R
import com.ruege.mobile.data.local.TokenManager
import com.ruege.mobile.databinding.ActivityLoginBinding
import com.ruege.mobile.utils.Resource
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
// import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.AndroidEntryPoint // Аннотация для Hilt
import kotlinx.coroutines.launch
import javax.inject.Inject // Для @Inject

/**
 * Экран авторизации пользователя (с использованием Credential Manager)
 */
@AndroidEntryPoint // Требуется для внедрения зависимостей Hilt
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    // Удаляем googleAuthManager и googleSignInClient
    // private lateinit var googleAuthManager: GoogleAuthManager
    // private lateinit var googleSignInClient: GoogleSignInClient
    // Удаляем signInLauncher для старого API
    // private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    // Добавляем CredentialManager
    private lateinit var credentialManager: CredentialManager

    // Внедряем ViewModel с помощью Hilt
    private val viewModel: LoginViewModel by viewModels()

    @Inject // Внедряем TokenManager
    lateinit var tokenManager: TokenManager

    // Удаляем хардкодный ID
    // private val googleMobileClientId = "..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем наличие токенов сразу при запуске активности
        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        Log.d(TAG, "Токены при запуске: Access: ${accessToken != null}, Refresh: ${refreshToken != null}")
        
        // Если есть оба токена, сразу переходим на MainActivity
        if (accessToken != null && refreshToken != null) {
            Log.d(TAG, "Токены найдены. Переход на MainActivity")
            navigateToMain()
            return // Важно прервать выполнение onCreate после redirect
        }

        // Если токенов нет, продолжаем инициализацию экрана входа
        // Инициализируем CredentialManager
        credentialManager = CredentialManager.create(this)

        // Удаляем старую настройку Google Sign-In
        // googleAuthManager = GoogleAuthManager(this)
        // val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        //     .requestIdToken(...) // Старый ID токен использовал Mobile Client ID
        //     .requestEmail()
        //     .build()
        // googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Удаляем регистрацию старого обработчика результата
        // signInLauncher = registerForActivityResult(...) { ... }

        // Обработка нажатия на кнопку входа Google
        binding.btnGoogleSignIn.setOnClickListener {
            // Запускаем вход через Credential Manager в корутине
            lifecycleScope.launch {
                signInWithGoogleCredentialManager()
            }
        }

        // Обработчик для новой кнопки btn_google_sign_in2
        binding.btnGoogleSignIn2.setOnClickListener {
            Log.d(TAG, "Нажата кнопка btn_google_sign_in2")
            // Фиксированный google_id для отправки
            val googleId = "testnoneakeytotpopkok"
            // Показываем загрузку
            showLoading(true)
            // Вызываем метод в ViewModel для отправки google_id
            viewModel.performGoogleLoginWithId(googleId)
        }

        // Обработка нажатия на кнопку Политики конфиденциальности
        binding.btnPrivacyPolicy.setOnClickListener {
            Log.d(TAG, "Нажата кнопка Политики конфиденциальности")
            val url = "https://gu-ru-ashen.vercel.app/privacy_policy"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
        }

        // Наблюдаем за состоянием входа из ViewModel
        observeLoginState()
    }

    // Новый метод для входа через Credential Manager
    private suspend fun signInWithGoogleCredentialManager() {
        showLoading(true) // Показываем прогресс перед началом запроса

        // Получаем Web Client ID из ресурсов, сгенерированных google-services плагином
        val serverClientId = getString(R.string.default_web_client_id)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Разрешаем выбор любого аккаунта
            .setServerClientId(serverClientId) // Используем Web Client ID из ресурсов
            //.setNonce(nonce) // Можно добавить nonce для защиты от повторного воспроизведения, если бэкенд его поддерживает
            .setAutoSelectEnabled(false) // Не выбирать аккаунт автоматически, дать пользователю выбрать
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
            // Обработка успешного получения учетных данных
            handleSignInSuccess(result)

        } catch (e: GetCredentialException) {
            // Обработка различных ошибок Credential Manager
            Log.e(TAG, "Ошибка Credential Manager: ${e.javaClass.simpleName} - ${e.message}", e)
            showLoading(false)
            // TODO: Добавить более детальную обработку ошибок (UserCancelledException, NoCredentialException и т.д.)
            showError("Ошибка входа через Google: ${e.message}")
        } catch (e: Exception) {
            // Обработка других неожиданных ошибок
            Log.e(TAG, "Неожиданная ошибка при входе: ${e.message}", e)
            showLoading(false)
            showError("Произошла неожиданная ошибка: ${e.message}")
        }
    }

    // Новый метод для обработки успешного результата от Credential Manager
    private fun handleSignInSuccess(result: GetCredentialResponse) {
        val credential = result.credential
        Log.d(TAG, "Получен credential типа: ${credential::class.java.simpleName}, тип внутри: ${credential.type}")

        when (credential.type) {
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    // Пытаемся извлечь GoogleIdTokenCredential из данных
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken
                    
                    // Дополнительная проверка и логирование токена
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
                    // Отправляем токен в ViewModel для проверки на бэкенде
                    viewModel.performGoogleLogin(googleIdToken)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при извлечении GoogleIdTokenCredential из data: ${e.message}", e)
                    showLoading(false)
                    showError("Ошибка обработки учетных данных Google")
                }
            }
            // Можно добавить обработку других типов credential (PasswordCredential, CustomCredential и т.д.), если нужно
            else -> {
                Log.e(TAG, "Получен неподдерживаемый тип учетных данных: ${credential.type}")
                showLoading(false)
                showError("Неподдерживаемый тип учетных данных: ${credential.type}")
            }
        }
    }

    // Удаляем старый метод signIn()
    // private fun signIn() { ... }

    // Удаляем старый метод handleSignInResult()
    // private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) { ... }

    // Новый метод для наблюдения за LiveData (остается почти без изменений, только логика сохранения токенов)
    private fun observeLoginState() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Не вызываем showLoading(true) здесь, так как он вызывается в signInWithGoogleCredentialManager
                    // showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    Log.d(TAG, "Успешный вход через бэкенд. Токены получены.")

                    resource.data?.let { tokenDto ->
                        Log.d(TAG, "Токены получены. Access: ${tokenDto.accessToken.take(10)}..., Refresh: ${tokenDto.refreshToken.take(10)}...")
                        tokenManager.saveAccessToken(tokenDto.accessToken)
                        tokenManager.saveRefreshToken(tokenDto.refreshToken)
                        // Проверка сохранения
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

    // Вспомогательные функции для UI (остаются без изменений)
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnGoogleSignIn.isEnabled = !isLoading // Блокируем кнопку во время загрузки
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        showLoading(false) // Убедимся, что прогресс-бар скрыт при ошибке
    }

    private fun navigateToMain() {
         Log.d(TAG, "Переход на MainActivity")
         val intent = Intent(this@LoginActivity, MainActivity::class.java)
         // Очищаем стек активностей, чтобы пользователь не вернулся на экран входа кнопкой \"Назад\"
         intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
         startActivity(intent)
         // finish() вызывается автоматически из-за флагов
    }

    companion object {
         // Используем TAG из androidx.credentials для единообразия или оставляем свой
         // private const val TAG = "LoginActivity"
         private const val TAG = "LoginActivity" // Возвращаем наш TAG
    }
} 