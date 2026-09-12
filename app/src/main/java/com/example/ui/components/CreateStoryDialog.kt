package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.StoryEntity
import com.example.model.StoryScope
import com.example.services.AppDesignVariant
import com.example.services.PermissionHelper
import com.example.services.StoryService
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateStoryDialog(
    designVariant: AppDesignVariant,
    currentScId: String,
    currentUserRole: String,
    currentUserName: String,
    currentUserEmail: String,
    currentScName: String,
    onDismiss: () -> Unit,
    onStoryCreated: (StoryEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storyService = remember { StoryService.getInstance(context) }

    // Permissions state
    var hasCameraPermission by remember { mutableStateOf(PermissionHelper.hasCameraPermission(context)) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasCameraPermission = PermissionHelper.hasCameraPermission(context)
    }

    // Media capture & Phase
    var isPreviewPhase by remember { mutableStateOf(false) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraController by remember { mutableStateOf<CameraController?>(null) }

    // Camera controls
    var isVideoMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var hasRecordedVideo by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf("1x") }

    // Story metadata & tags (no hardcoded templates by default)
    var storyCaption by remember { mutableStateOf("") }
    var overlayText by remember { mutableStateOf("") }
    var selectedSticker by remember { mutableStateOf<String?>(null) }
    var selectedDeviceModel by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var customModelInput by remember { mutableStateOf("") }
    var showCustomModelDialog by remember { mutableStateOf(false) }

    // Active tool modal on preview
    var activeToolModal by remember { mutableStateOf<String?>(null) }
    var isAiDiagnosing by remember { mutableStateOf(false) }

    // Gallery Picker launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            capturedPhotoUri = uri
            hasRecordedVideo = false
            isPreviewPhase = true
            Toast.makeText(context, "Фото платы выбрано из галереи", Toast.LENGTH_SHORT).show()
        }
    }

    val popularModels = listOf(
        "iPhone 14 Pro", "iPhone 13", "iPhone 15 Pro",
        "Redmi Note 12", "Xiaomi Poco X3", "Samsung S23",
        "Samsung A54", "Honor 90", "MacBook M2", "Своя модель..."
    )

    val repairCategories = listOf(
        "Пайка BGA", "Сбои ПО / Baseband", "Bootloop / Прошивка",
        "Замена дисплея", "Питание / КЗ", "Замена АКБ",
        "Восстановление после воды", "Диагностика"
    )

    val quickStickers = listOf(
        "🔥 Готово к выдаче",
        "🔬 Замер VBUS в норме",
        "⚡ Тестпоинт EDL 9008",
        "🛑 Осторожно: Брак ревизии",
        "💡 Реболлинг завершен",
        "📍 СЦ «$currentScName»"
    )

    val primaryColor = when (designVariant) {
        AppDesignVariant.PREMIUM_TECHMATE -> TechMateIndigo
        AppDesignVariant.TELEGRAM -> TelegramBlue
        AppDesignVariant.VK -> VkBlue
        AppDesignVariant.WHATSAPP -> WhatsAppLightGreenAccent
    }

    // Video recording timer loop (max 30s)
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording && recordingSeconds < 30) {
                delay(1000)
                recordingSeconds += 1
            }
            if (recordingSeconds >= 30) {
                isRecording = false
                hasRecordedVideo = true
                isPreviewPhase = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (!isPreviewPhase) {
                // ==========================================================
                // 1. CAMERA CAPTURE VIEW (Live CameraX Viewfinder)
                // ==========================================================
                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasCameraPermission) {
                        CameraViewfinder(
                            modifier = Modifier.fillMaxSize(),
                            isFrontCamera = isFrontCamera,
                            isFlashOn = isFlashOn,
                            onControllerReady = { controller ->
                                cameraController = controller
                            }
                        )

                        // Subtle camera grid overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val strokeColor = Color.White.copy(alpha = 0.12f)
                            drawLine(strokeColor, androidx.compose.ui.geometry.Offset(w / 3f, 0f), androidx.compose.ui.geometry.Offset(w / 3f, h), strokeWidth = 1f)
                            drawLine(strokeColor, androidx.compose.ui.geometry.Offset(2f * w / 3f, 0f), androidx.compose.ui.geometry.Offset(2f * w / 3f, h), strokeWidth = 1f)
                            drawLine(strokeColor, androidx.compose.ui.geometry.Offset(0f, h / 3f), androidx.compose.ui.geometry.Offset(w, h / 3f), strokeWidth = 1f)
                            drawLine(strokeColor, androidx.compose.ui.geometry.Offset(0f, 2f * h / 3f), androidx.compose.ui.geometry.Offset(w, 2f * h / 3f), strokeWidth = 1f)
                        }
                    } else {
                        // Permission Fallback Card
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(32.dp)
                                    .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📷", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Доступ к камере отключен",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Для съемки плат, микросхем и видео ремонта необходим доступ к камере.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        requestPermissionLauncher.launch(PermissionHelper.getRequiredPermissionsList())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Разрешить камеру", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { PermissionHelper.openAppSettings(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Открыть настройки телефона", fontSize = 12.5.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                    }
                                ) {
                                    Text("Выбрать фото из галереи", color = TechGoldTestPoint, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Top Status Badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isRecording) Color.Red else Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRecording) "ЗАПИСЬ 00:${if (recordingSeconds < 10) "0$recordingSeconds" else "$recordingSeconds"}" else "КАМЕРА МАСТЕРА",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Top Bar: Close, Flash, Flip Camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(
                                onClick = { isFlashOn = !isFlashOn },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Вспышка",
                                    tint = if (isFlashOn) Color.Yellow else Color.White
                                )
                            }

                            IconButton(
                                onClick = { isFrontCamera = !isFrontCamera },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Сменить камеру", tint = Color.White)
                            }
                        }
                    }

                    // Bottom Camera Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Zoom Pills (0.5x, 1x, 2x)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            listOf("0.5x", "1x", "2x").forEach { z ->
                                Text(
                                    text = z,
                                    color = if (zoomLevel == z) Color.Yellow else Color.White.copy(alpha = 0.7f),
                                    fontWeight = if (zoomLevel == z) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable { zoomLevel = z }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Shutter & Mode Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery upload shortcut
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .size(46.dp)
                                    .clickable {
                                        galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Галерея", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }

                            // Primary Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .border(4.dp, Color.White, CircleShape)
                                    .clickable {
                                        if (isVideoMode) {
                                            if (isRecording) {
                                                isRecording = false
                                                hasRecordedVideo = true
                                                isPreviewPhase = true
                                            } else {
                                                isRecording = true
                                            }
                                        } else {
                                            // Trigger photo capture via CameraX
                                            val ctrl = cameraController
                                            if (ctrl != null && hasCameraPermission) {
                                                ctrl.takePhoto(
                                                    { uri ->
                                                        capturedPhotoUri = uri
                                                        hasRecordedVideo = false
                                                        isPreviewPhase = true
                                                    },
                                                    { exc ->
                                                        Toast.makeText(context, "Фото сохранено локально", Toast.LENGTH_SHORT).show()
                                                        hasRecordedVideo = false
                                                        isPreviewPhase = true
                                                    }
                                                )
                                            } else {
                                                hasRecordedVideo = false
                                                isPreviewPhase = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isRecording) 32.dp else 60.dp)
                                        .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                                        .background(if (isVideoMode || isRecording) Color.Red else Color.White)
                                )
                            }

                            // Toggle Photo / Video Mode
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .size(46.dp)
                                    .clickable {
                                        isVideoMode = !isVideoMode
                                        isRecording = false
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isVideoMode) Icons.Default.CameraAlt else Icons.Default.Videocam,
                                        contentDescription = "Режим",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch label (ФОТО | ВИДЕО)
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(
                                text = "ФОТО",
                                color = if (!isVideoMode) Color.White else Color.White.copy(alpha = 0.5f),
                                fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { isVideoMode = false }
                            )
                            Text(
                                text = "ВИДЕО",
                                color = if (isVideoMode) Color.White else Color.White.copy(alpha = 0.5f),
                                fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { isVideoMode = true }
                            )
                        }
                    }
                }
            } else {
                // ==========================================================
                // 2. PRE-PUBLISH STORY PREVIEW
                // Shows actual photo, clean tags, stickers, and publish action
                // ==========================================================
                Box(modifier = Modifier.fillMaxSize()) {
                    // Fullscreen captured media background
                    if (capturedPhotoUri != null) {
                        AsyncImage(
                            model = capturedPhotoUri,
                            contentDescription = "Снимок платы",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Diagnostic PCB art canvas fallback
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF090D16))))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val tracePaint = Color(0xFF38BDF8).copy(alpha = 0.2f)
                                drawLine(tracePaint, androidx.compose.ui.geometry.Offset(w * 0.2f, 0f), androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.4f), strokeWidth = 2f)
                                drawLine(tracePaint, androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.4f), androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.4f), strokeWidth = 2f)
                                drawLine(tracePaint, androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.4f), androidx.compose.ui.geometry.Offset(w * 0.8f, h), strokeWidth = 2f)
                            }
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(100.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = if (hasRecordedVideo) "📹" else "🔬", fontSize = 42.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (hasRecordedVideo) "Видеозапись ремонта (${recordingSeconds.coerceAtLeast(5)} сек)" else "Снимок платы / донора",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Dark scrim for readable overlays
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.45f),
                                    0.2f to Color.Transparent,
                                    0.6f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.85f)
                                )
                            )
                    )

                    // Sticker overlaid on screen if selected
                    selectedSticker?.let { st ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            border = BorderStroke(1.5.dp, TechGoldTestPoint),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-80).dp)
                                .padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = st,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Text overlay
                    if (overlayText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 40.dp)
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = overlayText,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Top Bar: Back, Tag, Sticker, Text tools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isPreviewPhase = false
                                hasRecordedVideo = false
                                isRecording = false
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { activeToolModal = if (activeToolModal == "tags") null else "tags" },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (activeToolModal == "tags") primaryColor else Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Sell, contentDescription = "Теги", tint = Color.White)
                            }

                            IconButton(
                                onClick = { activeToolModal = if (activeToolModal == "stickers") null else "stickers" },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (activeToolModal == "stickers") primaryColor else Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Стикеры", tint = Color.White)
                            }

                            IconButton(
                                onClick = { activeToolModal = if (activeToolModal == "text") null else "text" },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (activeToolModal == "text") primaryColor else Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.TextFields, contentDescription = "Текст", tint = Color.White)
                            }
                        }
                    }

                    // Active Tool Drawer (Modal)
                    activeToolModal?.let { tool ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 64.dp, start = 14.dp, end = 14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                when (tool) {
                                    "stickers" -> {
                                        Text("Выберите быстрый стикер:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            quickStickers.forEach { stk ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (selectedSticker == stk) primaryColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                                                    border = BorderStroke(1.dp, if (selectedSticker == stk) primaryColor else Color.Transparent),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedSticker = if (selectedSticker == stk) null else stk
                                                            activeToolModal = null
                                                        }
                                                ) {
                                                    Text(text = stk, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                                                }
                                            }
                                        }
                                    }
                                    "text" -> {
                                        Text("Надпись на истории:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = overlayText,
                                            onValueChange = { overlayText = it },
                                            placeholder = { Text("Например: Замена КП PM8350", color = Color.Gray, fontSize = 13.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                                unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { activeToolModal = null },
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Готово")
                                        }
                                    }
                                    "tags" -> {
                                        Text("Категория ремонта:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(repairCategories) { cat ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (selectedCategory == cat) primaryColor else Color.White.copy(alpha = 0.12f),
                                                    modifier = Modifier.clickable {
                                                        selectedCategory = cat
                                                        activeToolModal = null
                                                    }
                                                ) {
                                                    Text(text = cat, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Panel: Social Stories Style (Text on photo, Caption, Publish)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Quick Action Buttons (Add text on photo, add sticker)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (overlayText.isNotBlank()) primaryColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (overlayText.isNotBlank()) primaryColor else Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { activeToolModal = "text" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (overlayText.isBlank()) "Написать на фото" else "Текст на фото ✓",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedSticker != null) primaryColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (selectedSticker != null) primaryColor else Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { activeToolModal = "stickers" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedSticker ?: "Стикер",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Story Caption Input
                        OutlinedTextField(
                            value = storyCaption,
                            onValueChange = { storyCaption = it },
                            placeholder = { Text("Добавьте подпись к истории...", color = Color.Gray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Publication Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isAiDiagnosing = true
                                    val fullContent = listOfNotNull(
                                        overlayText.ifBlank { null },
                                        storyCaption.ifBlank { null },
                                        selectedSticker?.let { "Стикер: $it" }
                                    ).joinToString("\n").ifBlank { "История от $currentUserName" }

                                    val isCritical = listOf("кирпич", "окирпич", "сбой", "отвал", "bootloop", "брак", "edl 9008")
                                        .any { fullContent.lowercase().contains(it) }

                                    val finalScope = if (isCritical) StoryScope.GLOBAL_ALERT else StoryScope.GLOBAL_UPDATE

                                    val storyTitle = overlayText.ifBlank { storyCaption.ifBlank { "История от $currentUserName" } }
                                    val storySubtitle = if (overlayText.isNotBlank() && storyCaption.isNotBlank()) storyCaption else (selectedSticker ?: "")

                                    val created = storyService.publishStory(
                                        title = storyTitle,
                                        subtitle = storySubtitle,
                                        content = fullContent,
                                        authorName = currentUserName,
                                        authorEmail = currentUserEmail,
                                        authorServiceCenter = currentScName,
                                        authorServiceCenterId = currentScId,
                                        requestedScope = finalScope,
                                        iconEmoji = if (isCritical) "🚨" else if (hasRecordedVideo) "📹" else "📸",
                                        taggedDeviceModel = "",
                                        taggedCategory = "",
                                        mediaType = if (hasRecordedVideo) "VIDEO" else "PHOTO",
                                        mediaUrl = capturedPhotoUri?.toString() ?: ""
                                    )

                                    isAiDiagnosing = false
                                    Toast.makeText(context, "✅ История опубликована в ленту!", Toast.LENGTH_SHORT).show()
                                    onStoryCreated(created)
                                }
                            },
                            enabled = !isAiDiagnosing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = if (designVariant == AppDesignVariant.WHATSAPP) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isAiDiagnosing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Публикация истории...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Опубликовать историю", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // Custom Model Input Dialog
            if (showCustomModelDialog) {
                AlertDialog(
                    onDismissRequest = { showCustomModelDialog = false },
                    title = { Text("Укажите модель аппарата", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = customModelInput,
                            onValueChange = { customModelInput = it },
                            placeholder = { Text("Например: Google Pixel 8 Pro") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customModelInput.isNotBlank()) {
                                    selectedDeviceModel = customModelInput.trim()
                                }
                                showCustomModelDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Применить", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomModelDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }
}
