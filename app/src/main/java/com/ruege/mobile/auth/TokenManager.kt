package com.ruege.mobile.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Класс для безопасного хранения и получения токенов аутентификации с использованием Android Keystore.
 */
@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private fun getSecretKey(): SecretKey {
        try {
            val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            return existingKey?.secretKey ?: generateSecretKey()
        } catch (e: Exception) {
            Timber.d("Ошибка получения ключа из KeyStore: ${e.message}", e)
            try {
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                    Timber.d("Старый ключ удален из KeyStore")
                }
                return generateSecretKey()
            } catch (e2: Exception) {
                Timber.d("Не удалось пересоздать ключ: ${e2.message}", e2)
                throw e2
            }
        }
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        val key = keyGenerator.generateKey()
        Timber.d("Новый ключ шифрования создан")
        return key
    }

    private fun encrypt(data: String): Pair<String, String>? {
        return try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            Pair(Base64.encodeToString(iv, Base64.DEFAULT), Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
        } catch (e: Exception) {
            Timber.d("Ошибка шифрования: ${e.javaClass.simpleName} - ${e.message}", e)
            null
        }
    }

    private fun decrypt(encryptedData: String, ivString: String): String? {
        return try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivString, Base64.DEFAULT)
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Timber.d("Ошибка дешифрования: ${e.javaClass.simpleName} - ${e.message}", e)
            
            if (e.message?.contains("Signature/MAC verification failed") == true || 
                e.javaClass.simpleName == "AEADBadTagException") {
                Timber.d("Обнаружена ошибка верификации MAC, пробуем пересоздать ключ")
                try {
                    if (keyStore.containsAlias(KEY_ALIAS)) {
                        keyStore.deleteEntry(KEY_ALIAS)
                        Timber.d("Ключ удален из-за ошибки верификации")
                    }
                    generateSecretKey()
                } catch (e2: Exception) {
                    Timber.d("Ошибка при пересоздании ключа: ${e2.message}", e2)
                }
            }
            null
        }
    }

    /**
     * Сохраняет Access Token.
     */
    fun saveAccessToken(token: String) {
        Timber.d("Сохранение access token (длина: ${token.length})")
        encrypt(token)?.let { (iv, encryptedToken) ->
            sharedPreferences.edit {
                putString(KEY_ACCESS_TOKEN, encryptedToken)
                putString(KEY_ACCESS_TOKEN_IV, iv)
            }
            Timber.d("Access token успешно зашифрован и сохранен $encryptedToken")
        } ?: Timber.d("Не удалось зашифровать access token")
    }

    /**
     * Получает Access Token.
     * @return Access Token или null, если не сохранен или ошибка дешифрования.
     */
    fun getAccessToken(): String? {
        val encryptedToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        val iv = sharedPreferences.getString(KEY_ACCESS_TOKEN_IV, null)
        
        if (encryptedToken == null || iv == null) {
            Timber.d("Access token не найден в хранилище")
            return null
        }
        
        val token = decrypt(encryptedToken, iv)
        if (token == null) {
            Timber.d("Не удалось расшифровать access token. Удаляем старые данные токена.")
            sharedPreferences.edit {
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_ACCESS_TOKEN_IV)
                remove(KEY_ACCESS_TOKEN_EXPIRES_AT)
            }
        } else {
            Timber.d("Access token успешно получен (длина: ${token.length})")
        }
        return token
    }

    /**
     * Сохраняет Refresh Token.
     */
    fun saveRefreshToken(token: String) {
        Timber.d("Сохранение refresh token (длина: ${token.length})")
        encrypt(token)?.let { (iv, encryptedToken) ->
            sharedPreferences.edit {
                putString(KEY_REFRESH_TOKEN, encryptedToken)
                putString(KEY_REFRESH_TOKEN_IV, iv)
            }
            Timber.d("Refresh token успешно зашифрован и сохранен $encryptedToken")
        } ?: Timber.d("Не удалось зашифровать refresh token")
    }

    /**
     * Получает Refresh Token.
     * @return Refresh Token или null, если не сохранен или ошибка дешифрования.
     */
    fun getRefreshToken(): String? {
        val encryptedToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        val iv = sharedPreferences.getString(KEY_REFRESH_TOKEN_IV, null)
        
        if (encryptedToken == null || iv == null) {
            Timber.d("Refresh token не найден в хранилище")
            return null
        }
        
        val token = decrypt(encryptedToken, iv)
        if (token == null) {
            Timber.d("Не удалось расшифровать refresh token. Удаляем старый refresh token.")

            sharedPreferences.edit {
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_REFRESH_TOKEN_IV)
            }
        } else {
            Timber.d("Refresh token успешно получен (длина: ${token.length})")
        }
        return token
    }

    /**
     * Очищает сохраненные токены (например, при выходе).
     */
    fun clearTokens() {
        sharedPreferences.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_ACCESS_TOKEN_IV)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_REFRESH_TOKEN_IV)
            remove(KEY_ACCESS_TOKEN_EXPIRES_AT)
        }
        
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Timber.d("Ключ шифрования удален при очистке токенов")
            }
        } catch (e: Exception) {
            Timber.d("Ошибка при удалении ключа шифрования: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "auth_prefs_v2" 
        private const val KEY_ACCESS_TOKEN = "access_token_encrypted"
        private const val KEY_ACCESS_TOKEN_IV = "access_token_iv"
        private const val KEY_REFRESH_TOKEN = "refresh_token_encrypted"
        private const val KEY_REFRESH_TOKEN_IV = "refresh_token_iv"
        private const val KEY_ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "auth_token_key_alias"
        private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
    }

    /**
     * Сохраняет время истечения срока действия Access Token.
     * @param expiresInSeconds срок действия токена в секундах
     */
    fun saveTokenExpiresIn(expiresInSeconds: Int) {
        Timber.d("Сохранение срока действия токена: $expiresInSeconds секунд")
        val expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        sharedPreferences.edit {
            putLong(KEY_ACCESS_TOKEN_EXPIRES_AT, expiresAtMillis)
        }
    }
    
    /**
     * Проверяет, истек ли срок действия Access Token.
     * @param bufferSeconds дополнительное время в секундах до истечения срока для упреждающего обновления
     * @return true, если токен истек или истечет в ближайшее время (с учетом буфера)
     */
    fun isAccessTokenExpired(bufferSeconds: Int = 60): Boolean {
        val expiresAt = sharedPreferences.getLong(KEY_ACCESS_TOKEN_EXPIRES_AT, 0)
        if (expiresAt == 0L) return true
        
        val currentTime = System.currentTimeMillis()
        val bufferMillis = bufferSeconds * 1000L
        val isExpired = currentTime + bufferMillis >= expiresAt
        
        if (isExpired) {
            Timber.d("Токен доступа истек или скоро истечет")
        }
        
        return isExpired
    }
}