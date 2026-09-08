package com.example.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionItemState(
    val titleRu: String,
    val descriptionRu: String,
    val iconEmoji: String,
    val isGranted: Boolean,
    val permissions: List<String>
)

object PermissionHelper {

    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun hasMicrophonePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun hasMediaPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

    fun areAllGranted(context: Context): Boolean =
        hasCameraPermission(context) &&
        hasMicrophonePermission(context) &&
        hasNotificationPermission(context) &&
        hasMediaPermission(context)

    fun hasAllEssentialPermissions(context: Context): Boolean = areAllGranted(context)

    fun getRequiredPermissionsList(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return list.toTypedArray()
    }

    fun getPermissionsState(context: Context): List<PermissionItemState> {
        return listOf(
            PermissionItemState(
                titleRu = "Камера",
                descriptionRu = "Для съемки плат, макросъемки тестпоинтов и сторис ремонта",
                iconEmoji = "📷",
                isGranted = hasCameraPermission(context),
                permissions = listOf(Manifest.permission.CAMERA)
            ),
            PermissionItemState(
                titleRu = "Микрофон",
                descriptionRu = "Для записи звука видеоотчетов и отправки голосовых сообщений",
                iconEmoji = "🎙️",
                isGranted = hasMicrophonePermission(context),
                permissions = listOf(Manifest.permission.RECORD_AUDIO)
            ),
            PermissionItemState(
                titleRu = "Фото и Медиа",
                descriptionRu = "Для прикрепления схем, даташитов и фото дефектов из галереи",
                iconEmoji = "🖼️",
                isGranted = hasMediaPermission(context),
                permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            ),
            PermissionItemState(
                titleRu = "Уведомления",
                descriptionRu = "Для оперативных алертов о заказах, сообщениях коллег и ответах ИИ",
                iconEmoji = "🔔",
                isGranted = hasNotificationPermission(context),
                permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else emptyList()
            )
        )
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
