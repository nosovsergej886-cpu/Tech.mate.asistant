package com.example.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object EmailOtpService {
    private const val RESEND_API_URL = "https://api.resend.com/emails"
    private const val RESEND_API_KEY = "re_WfKNxeLQ_FhHxxiWLqTyaqCWs3D6KAmEL"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun sendOtpCode(recipientEmail: String, code: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cleanEmail = recipientEmail.trim()
                val htmlContent = """
                    <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 540px; margin: 0 auto; background: #ffffff; border: 1px solid #e0e6ed; border-radius: 12px; overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #0288D1 0%, #01579B 100%); padding: 24px 20px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 0.5px;">Tech.Mate</h1>
                            <p style="color: #B3E5FC; margin: 4px 0 0 0; font-size: 13px;">Инженерный ассистент по ремонту электроники</p>
                        </div>
                        <div style="padding: 28px 24px; text-align: center; color: #333333;">
                            <h2 style="font-size: 18px; margin-top: 0; color: #1a202c;">Ваш код подтверждения для входа</h2>
                            <p style="font-size: 14px; color: #555555; line-height: 1.5; margin: 12px 0 20px 0;">
                                Введите этот 6-значный проверочный код в приложении для завершения авторизации:
                            </p>
                            <div style="background: #F0F7FF; border: 2px dashed #0288D1; border-radius: 10px; padding: 16px 20px; display: inline-block; margin-bottom: 20px;">
                                <span style="font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #0288D1; font-family: monospace;">$code</span>
                            </div>
                            <p style="font-size: 12px; color: #888888; margin: 0;">Код действителен в течение 10 минут. Если вы не запрашивали вход, просто проигнорируйте это письмо.</p>
                        </div>
                        <div style="background: #f8fafc; padding: 14px 20px; text-align: center; border-top: 1px solid #e2e8f0;">
                            <p style="margin: 0; font-size: 11px; color: #94a3b8;">&copy; Tech.Mate • Система помощи мастерам и инженерам</p>
                        </div>
                    </div>
                """.trimIndent()

                val jsonPayload = JSONObject().apply {
                    put("from", "Tech.Mate <onboarding@resend.dev>")
                    put("to", JSONArray().put(cleanEmail))
                    put("subject", "Код подтверждения Tech.Mate: $code")
                    put("html", htmlContent)
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(RESEND_API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $RESEND_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.i("EmailOtpService", "Resend OTP email sent successfully to $cleanEmail: $bodyStr")
                    true
                } else {
                    Log.w("EmailOtpService", "Resend OTP email failed (${response.code}): $bodyStr")
                    false
                }
            } catch (e: Exception) {
                Log.e("EmailOtpService", "Error dispatching Resend OTP email: ${e.message}", e)
                false
            }
        }
    }
}
