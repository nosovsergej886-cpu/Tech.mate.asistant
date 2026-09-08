package com.example.services

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.CancellationSignal
import java.security.MessageDigest

enum class AuthOption {
    PIN_4,
    BIOMETRIC,
    WEEKLY_EMAIL
}

class DeviceSecurityService private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("techmate_device_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_REGISTERED = "is_device_registered"
        private const val KEY_REGISTERED_EMAIL = "registered_email"
        private const val KEY_REGISTERED_USER_ID = "registered_user_id"
        private const val KEY_AUTH_OPTION = "auth_option"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_LAST_WEEKLY_VERIFIED = "last_weekly_verified"

        private const val WEEK_IN_MILLIS = 7L * 24 * 60 * 60 * 1000L

        @Volatile
        private var INSTANCE: DeviceSecurityService? = null

        fun getInstance(context: Context): DeviceSecurityService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceSecurityService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun isDeviceRegistered(): Boolean {
        return prefs.getBoolean(KEY_IS_REGISTERED, false)
    }

    fun getRegisteredEmail(): String? {
        return prefs.getString(KEY_REGISTERED_EMAIL, null)
    }

    fun getRegisteredUserId(): String? {
        return prefs.getString(KEY_REGISTERED_USER_ID, null)
    }

    fun getAuthOption(): AuthOption {
        val name = prefs.getString(KEY_AUTH_OPTION, AuthOption.PIN_4.name)
        return try {
            AuthOption.valueOf(name ?: AuthOption.PIN_4.name)
        } catch (e: Exception) {
            AuthOption.PIN_4
        }
    }

    fun getDaysLeftUntilWeeklyRefresh(): Int {
        val lastVerified = prefs.getLong(KEY_LAST_WEEKLY_VERIFIED, 0L)
        val elapsed = System.currentTimeMillis() - lastVerified
        val daysElapsed = (elapsed / (24L * 60 * 60 * 1000L)).toInt()
        val daysLeft = 7 - daysElapsed
        return daysLeft.coerceAtLeast(0)
    }

    fun isWeeklyVerificationExpired(): Boolean {
        val lastVerified = prefs.getLong(KEY_LAST_WEEKLY_VERIFIED, 0L)
        return (System.currentTimeMillis() - lastVerified) >= WEEK_IN_MILLIS
    }

    fun recordEmailVerification() {
        prefs.edit()
            .putLong(KEY_LAST_WEEKLY_VERIFIED, System.currentTimeMillis())
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val incomingHash = hashPin(pin)
        return storedHash == incomingHash
    }

    fun completeDeviceRegistration(
        email: String,
        userId: String,
        option: AuthOption,
        pin: String
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_REGISTERED, true)
            .putString(KEY_REGISTERED_EMAIL, email.trim().lowercase())
            .putString(KEY_REGISTERED_USER_ID, userId)
            .putString(KEY_AUTH_OPTION, option.name)
            .putString(KEY_PIN_HASH, hashPin(pin))
            .putLong(KEY_LAST_WEEKLY_VERIFIED, System.currentTimeMillis())
            .apply()
    }

    fun resetDeviceRegistration() {
        prefs.edit().clear().apply()
    }

    fun authenticateWithBiometrics(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val biometricManager = activity.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                    val canAuth = biometricManager?.canAuthenticate(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
                    )
                    if (canAuth == android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        onError("Отпечаток пальца или Face ID не настроены в телефоне. Используйте PIN-код.")
                        return
                    } else if (canAuth == android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                        onError("На устройстве нет сканера биометрии. Используйте PIN-код.")
                        return
                    }
                }

                val cancellationSignal = CancellationSignal()
                val executor = activity.mainExecutor

                val biometricPrompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle("Tech.Mate Безопасность")
                    .setSubtitle("Подтвердите личность мастера")
                    .setDescription("Используйте сканер отпечатка пальца или распознавание лица")
                    .setNegativeButton("Ввести PIN", executor) { _, _ ->
                        onError("Используйте PIN-код")
                    }
                    .build()

                biometricPrompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onError("Биометрия не распознана. Попробуйте снова.")
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            // errorCode 10 (BIOMETRIC_ERROR_USER_CANCELED), 13 (BIOMETRIC_ERROR_NEGATIVE_BUTTON), 5 (BIOMETRIC_ERROR_CANCELED)
                            if (errorCode == 10 || errorCode == 13 || errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                                onError("Используйте PIN-код")
                            } else {
                                onError(errString?.toString() ?: "Ошибка биометрии")
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                onError("Биометрия недоступна: используйте PIN-код")
            }
        } else {
            onError("Биометрия не поддерживается на этой версии Android. Используйте PIN-код")
        }
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
