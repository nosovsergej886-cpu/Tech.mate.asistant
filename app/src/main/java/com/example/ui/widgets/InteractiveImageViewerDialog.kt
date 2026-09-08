package com.example.ui.widgets

import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.services.DatabaseService
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechPrimaryBlue
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Full-screen interactive viewer for Test Point diagrams, schematics, and microscope board photos.
 * Features:
 * - Smooth Pinch-to-zoom (up to 8x magnification)
 * - Double-tap quick zoom to tap position
 * - Multi-touch pan & translation with boundary clamping
 * - Boardview Inspection filters (High-Contrast, Invert Negative, Normal)
 * - 90-degree Step Rotation
 * - Electronic Millimeter / Silk Screen Grid Overlay
 * - Interactive Test Point Probe Pinpoint Placement
 * - One-tap Save to Knowledge Base & Open original link
 */
@Composable
fun InteractiveImageViewerDialog(
    imageUrl: String,
    title: String = "Тестпоинт / Схема платы",
    subtitle: String? = null,
    sourceUrl: String? = null,
    onDismiss: () -> Unit,
    onSaveToKb: ((title: String, imageUrl: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Transform State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // UI overlays & Filter Modes
    var showControls by remember { mutableStateOf(true) }
    var filterMode by remember { mutableIntStateOf(0) } // 0: Normal, 1: High Contrast, 2: Invert Boardview
    var showGridOverlay by remember { mutableStateOf(false) }
    var pinMarkerPos by remember { mutableStateOf<Offset?>(null) }
    var showMarkerHelp by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "scaleAnim"
    )

    fun resetTransform() {
        scale = 1f
        offset = Offset.Zero
        rotationAngle = 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B11))
                .onSizeChanged { containerSize = it }
        ) {
            // Main Zoomable Canvas & Image Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = { tapOffset ->
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 3.0f
                                    // Center zoom towards the double tapped point
                                    val centerX = containerSize.width / 2f
                                    val centerY = containerSize.height / 2f
                                    offset = Offset(
                                        (centerX - tapOffset.x) * 2f,
                                        (centerY - tapOffset.y) * 2f
                                    )
                                }
                            },
                            onLongPress = { longPressOffset ->
                                // Place or toggle probe testpoint marker
                                pinMarkerPos = if (pinMarkerPos != null) null else longPressOffset
                                showMarkerHelp = pinMarkerPos != null
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(0.8f, 8f)
                            scale = newScale

                            if (newScale > 1.0f) {
                                val maxOffsetX = (containerSize.width * (newScale - 1f)) / 1.5f
                                val maxOffsetY = (containerSize.height * (newScale - 1f)) / 1.5f

                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Image with transform matrix & visual filter applied
                val colorFilter = when (filterMode) {
                    1 -> {
                        // High Contrast for Boardview Traces
                        val matrix = ColorMatrix().apply {
                            val contrast = 1.6f
                            val translate = (-0.5f * contrast + 0.5f) * 255f
                            set(
                                floatArrayOf(
                                    contrast, 0f, 0f, 0f, translate,
                                    0f, contrast, 0f, 0f, translate,
                                    0f, 0f, contrast, 0f, translate,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        }
                        ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(matrix.array))
                    }
                    2 -> {
                        // Invert / Silk Screen Negate Mode
                        val matrix = ColorMatrix().apply {
                            set(
                                floatArrayOf(
                                    -1f, 0f, 0f, 0f, 255f,
                                    0f, -1f, 0f, 0f, 255f,
                                    0f, 0f, -1f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        }
                        ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(matrix.array))
                    }
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationX = offset.x
                            translationY = offset.y
                            rotationZ = rotationAngle
                        },
                    contentAlignment = Alignment.Center
                ) {
                    TechAsyncImage(
                        imageUrl = imageUrl,
                        contentDescription = title,
                        title = title,
                        sourceUrl = sourceUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter
                    )

                    // Optional Millimeter Electronics Grid Overlay
                    if (showGridOverlay) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val step = 40f
                            val gridColor = Color(0x3300E5FF)
                            for (x in 0..size.width.toInt() step step.toInt()) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(x.toFloat(), 0f),
                                    end = Offset(x.toFloat(), size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (y in 0..size.height.toInt() step step.toInt()) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y.toFloat()),
                                    end = Offset(size.width, y.toFloat()),
                                    strokeWidth = 1f
                                )
                            }
                        }
                    }
                }

                // Probe / Testpoint Pinpoint Marker
                if (pinMarkerPos != null) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = pinMarkerPos!!.x.toInt() - 16.dp.roundToPx(),
                                    y = pinMarkerPos!!.y.toInt() - 32.dp.roundToPx()
                                )
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TechGoldTestPoint,
                                tonalElevation = 4.dp
                            ) {
                                Text(
                                    text = "📍 TEST POINT (TP)",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TechGoldTestPoint,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Top Floating Toolbar (Title, Zoom %, Filters, Close)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xE6111827),
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(TechPrimaryBlue.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = TechPrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = subtitle ?: "Масштаб: ${(scale * 100).toInt()}% • Двойной тап для зума",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Reset Zoom Button
                                if (scale != 1f || offset != Offset.Zero || rotationAngle != 0f) {
                                    IconButton(
                                        onClick = { resetTransform() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Reset",
                                            tint = TechGoldTestPoint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Rotate 90 deg
                                IconButton(
                                    onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.RotateRight,
                                        contentDescription = "Rotate",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Close
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF374151), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Floating Controls (Zoom Presets, Contrast Filter, Grid, Save to KB, Share)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    color = Color(0xE6111827),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Zoom Preset Buttons (1x, 2x, 4x, Max)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Масштаб: ${(scale * 100).toInt()}%",
                                color = TechPrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1f to "1x", 2f to "2x", 4f to "4x", 6f to "6x").forEach { (targetScale, label) ->
                                    val isSelected = (scale - targetScale).let { it in -0.2f..0.2f }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) TechPrimaryBlue else Color(0xFF1F2937),
                                        modifier = Modifier.clickable {
                                            scale = targetScale
                                            if (targetScale == 1f) offset = Offset.Zero
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF374151).copy(alpha = 0.5f))

                        // Tool Action Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Filter mode toggle (Normal -> Contrast -> Invert)
                            OutlinedButton(
                                onClick = { filterMode = (filterMode + 1) % 3 },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (filterMode != 0) TechGoldTestPoint.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Icon(
                                    Icons.Default.FilterBAndW,
                                    contentDescription = null,
                                    tint = if (filterMode != 0) TechGoldTestPoint else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (filterMode) {
                                        1 -> "Контраст"
                                        2 -> "Инверсия"
                                        else -> "Фильтр"
                                    },
                                    fontSize = 11.sp,
                                    color = if (filterMode != 0) TechGoldTestPoint else Color.White
                                )
                            }

                            // Grid overlay toggle
                            OutlinedButton(
                                onClick = { showGridOverlay = !showGridOverlay },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (showGridOverlay) Color(0x3300E5FF) else Color.Transparent
                                )
                            ) {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = null,
                                    tint = if (showGridOverlay) Color(0xFF00E5FF) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Сетка",
                                    fontSize = 11.sp,
                                    color = if (showGridOverlay) Color(0xFF00E5FF) else Color.White
                                )
                            }

                            // Save to Knowledge Base
                            Button(
                                onClick = {
                                    if (onSaveToKb != null) {
                                        onSaveToKb(title, imageUrl)
                                    } else {
                                        scope.launch {
                                            try {
                                                val db = DatabaseService.getInstance(context)
                                                val guide = com.example.model.GuideData(
                                                    device = title,
                                                    problem = "Тестпоинт / Распиновка платы",
                                                    difficulty = "Легко",
                                                    timeEstimate = "5 минут",
                                                    tools = listOf("Пинцет", "Мультиметр", "Кабель"),
                                                    causes = listOf(
                                                        com.example.model.Cause(
                                                            description = "Контрольная точка Test Point / EDL / BROM",
                                                            probability = 100,
                                                            checkMethod = "Замыкание точки на GND",
                                                            normalValue = "Определение режима прошивки",
                                                            fixMethod = "Кратковременное замыкание"
                                                        )
                                                    ),
                                                    steps = listOf(
                                                        com.example.model.Step(
                                                            stepNumber = 1,
                                                            title = title,
                                                            description = "Расположение контрольных точек и компонентов",
                                                            imageUrl = imageUrl
                                                        )
                                                    ),
                                                    proTip = "Сохранено из интерактивного просмотрщика",
                                                    risks = "Соблюдайте аккуратность при замыкании пинцетом"
                                                )

                                                db.knowledgeDao.insertEntry(
                                                    com.example.model.KnowledgeBaseEntryEntity(
                                                        brand = "TestPoint",
                                                        model = title,
                                                        problem = "Схема / Тестпоинт: $title",
                                                        guideDataJson = db.toJson(guide),
                                                        addedBy = "Интерактивный зум",
                                                        isSchematic = true
                                                    )
                                                )
                                                Toast.makeText(context, "Сохранено в Базу Знаний!", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("В базу", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Open in browser / Share
                            if (!sourceUrl.isNullOrBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF1F2937), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.OpenInNew,
                                        contentDescription = "Open Source",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pin marker hint toast overlay
            if (showMarkerHelp) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "📍 Метка Test Point установлена!\nЗажмите экран в любом месте для перемещения или снятия.",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2500)
                    showMarkerHelp = false
                }
            }
        }
    }
}
