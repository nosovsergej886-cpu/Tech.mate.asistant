package com.example.ui.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.services.AppDesignVariant
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import com.example.data.TestPointItem
import com.example.data.TestPointRepository
import com.example.model.GuideData
import com.example.model.MessageEntity
import com.example.model.TestPointCandidate
import com.example.services.LanguageService
import com.example.ui.dialogs.PostRepairQcCard
import com.example.ui.dialogs.PostRepairQcDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: MessageEntity,
    parsedGuide: GuideData? = null,
    onSaveGuideToKb: ((GuideData) -> Unit)? = null,
    onSaveTestPointToKb: ((TestPointItem) -> Unit)? = null,
    onApproveCandidate: ((TestPointCandidate) -> Unit)? = null,
    onRejectCandidate: ((TestPointCandidate) -> Unit)? = null,
    onRatingChange: ((Int) -> Unit)? = null,
    onSpeakMessage: ((String) -> Unit)? = null,
    isSpeaking: Boolean = false,
    onStopSpeech: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val isDark = LocalIsDarkTheme.current
    val tokens = LocalDesignTokens.current
    val context = LocalContext.current
    var showFullImage by remember { mutableStateOf(false) }

    // Check if this is a Telegram Voice Note message
    val isVoiceNote = message.text.contains("[VOICE]") || message.text.contains("🎙️ [Голосовое сообщение]") || message.text.startsWith("🎙️")

    val bubbleColor = if (isUser) tokens.userBubbleColor else tokens.aiBubbleColor
    val textColor = if (isUser) tokens.userTextColor else tokens.aiTextColor
    val shape = if (isUser) tokens.userBubbleShape else tokens.aiBubbleShape
    val bubbleBorder = if (isUser) tokens.userBubbleBorder else tokens.aiBubbleBorder

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormatter.format(Date(message.timestamp))

    var isVoicePlaying by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 8.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Surface(
                shape = shape,
                color = bubbleColor,
                border = if (bubbleBorder != null) androidx.compose.foundation.BorderStroke(1.dp, bubbleBorder) else null,
                shadowElevation = if (tokens.isOled) 0.dp else 1.dp,
                modifier = Modifier.combinedClickable(
                    onClick = { },
                    onLongClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Telegram AI Message", message.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            LanguageService.getString("copied_to_clipboard"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            ) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp, top = 2.dp)) {
                    // AI Voice Header if AI message
                    if (!isUser) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                when (tokens.variant) {
                                    AppDesignVariant.TELEGRAM -> {
                                        Text(
                                            text = "🤖 Мастер ИИ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = TechPrimaryBlue
                                        )
                                    }
                                    AppDesignVariant.VK -> {
                                        Text(
                                            text = "Мастер ИИ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = VkBlue
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Verified",
                                            tint = VkBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    AppDesignVariant.WHATSAPP -> {
                                        Text(
                                            text = "~ Мастер ИИ",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp,
                                            color = if (isDark) WhatsAppLightGreenAccent else WhatsAppTeal
                                        )
                                    }
                                }
                            }

                            // Voice Speak Out Loud Button
                            if (onSpeakMessage != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSpeaking) Color.Red.copy(alpha = 0.2f) else TechPrimaryBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable {
                                        if (isSpeaking) {
                                            onStopSpeech?.invoke()
                                        } else {
                                            onSpeakMessage(message.text)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                            contentDescription = "Speak",
                                            tint = if (isSpeaking) Color.Red else TechPrimaryBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isSpeaking) "Стоп" else "Озвучить",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSpeaking) Color.Red else TechPrimaryBlue
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TELEGRAM VOICE MESSAGE BUBBLE PLAYER
                    if (isVoiceNote) {
                        var showTranscript by remember { mutableStateOf(false) }

                        // Extract duration tag if present like [VOICE:12s]
                        val durationRegex = Regex("\\[VOICE:?(\\d+)s?\\]")
                        val durationMatch = durationRegex.find(message.text)

                        val cleanVoiceText = message.text
                            .replace(durationRegex, "")
                            .replace("[VOICE]", "")
                            .replace("🎙️ [Голосовое сообщение]", "")
                            .replace("🎙️", "")
                            .replace(Regex("^\\[Голосовой запрос мастера.*?\\]:?"), "")
                            .trim()

                        val durationSeconds = durationMatch?.groupValues?.get(1)?.toIntOrNull() ?: run {
                            (cleanVoiceText.length / 10).coerceIn(3, 59)
                        }

                        val mins = durationSeconds / 60
                        val secs = durationSeconds % 60
                        val totalDurationStr = "%d:%02d".format(mins, secs)

                        var playbackSeconds by remember { mutableIntStateOf(0) }

                        LaunchedEffect(isVoicePlaying) {
                            if (isVoicePlaying) {
                                playbackSeconds = 0
                                while (playbackSeconds < durationSeconds && isVoicePlaying) {
                                    kotlinx.coroutines.delay(1000)
                                    playbackSeconds++
                                }
                                if (playbackSeconds >= durationSeconds) {
                                    isVoicePlaying = false
                                    playbackSeconds = 0
                                }
                            } else {
                                playbackSeconds = 0
                            }
                        }

                        val speechPlaybackText = if (cleanVoiceText.isNotBlank()) cleanVoiceText else "Запись голосового сообщения мастера"

                        val waveBars = remember(message.id) {
                            val seed = message.id.hashCode()
                            val random = java.util.Random(seed.toLong())
                            List(22) { random.nextInt(20) + 6 }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                // Telegram Circular Voice Play Button
                                Surface(
                                    shape = CircleShape,
                                    color = if (isUser) Color.White else TechPrimaryBlue,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable {
                                            isVoicePlaying = !isVoicePlaying
                                            if (isVoicePlaying) {
                                                onSpeakMessage?.invoke(speechPlaybackText)
                                            } else {
                                                onStopSpeech?.invoke()
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isVoicePlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Play Voice Note",
                                            tint = if (isUser) TelegramUserBubble else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Waveform Visualizer Canvas
                                Column(modifier = Modifier.weight(1f)) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                    ) {
                                        val barWidth = 3.5.dp.toPx()
                                        val space = 2.5.dp.toPx()
                                        val playedBarIndex = if (isVoicePlaying && durationSeconds > 0) {
                                            ((playbackSeconds.toFloat() / durationSeconds) * waveBars.size).toInt()
                                        } else -1

                                        waveBars.forEachIndexed { i, barHeightDp ->
                                            val x = i * (barWidth + space)
                                            val heightPx = barHeightDp.dp.toPx()
                                            val y = (size.height - heightPx) / 2f
                                            val isBarPlayed = i <= playedBarIndex && isVoicePlaying

                                            val barColor = when {
                                                isUser -> if (isBarPlayed) Color.White else Color.White.copy(alpha = 0.45f)
                                                else -> if (isBarPlayed) TechPrimaryBlue else TechPrimaryBlue.copy(alpha = 0.35f)
                                            }

                                            drawRoundRect(
                                                color = barColor,
                                                topLeft = Offset(x, y),
                                                size = Size(barWidth, heightPx),
                                                cornerRadius = CornerRadius(2.dp.toPx())
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val timeDisplay = if (isVoicePlaying) {
                                            "▶ %d:%02d / %s".format(playbackSeconds / 60, playbackSeconds % 60, totalDurationStr)
                                        } else {
                                            "🎙 Голосовое $totalDurationStr"
                                        }

                                        Text(
                                            text = timeDisplay,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (showTranscript) "Скрыть" else "Текст",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) Color.White else TechPrimaryBlue,
                                            modifier = Modifier.clickable { showTranscript = !showTranscript }
                                        )
                                    }
                                }
                            }

                            // Collapsible Transcription Text
                            AnimatedVisibility(visible = showTranscript || !isUser) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = "📝 $cleanVoiceText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.9f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Extract embedded markdown image URLs, standalone image URLs, and Google Search Data
                        val imageMarkdownRegex = Regex("!\\[(.*?)\\]\\((https?://[^\\s\\)]+)\\)")
                        val standaloneImageUrlRegex = Regex("(https?://[^\\s]+\\.(?:jpg|jpeg|png|webp|gif)(?:\\?[^\\s]*)?|https?://images\\.unsplash\\.com/[^\\s]+)")
                        val testpointTagRegex = Regex("\\[TESTPOINT_DATA:([^\\]]+)\\]")
                        val googleSearchTagRegex = Regex("\\[GOOGLE_SEARCH_DATA:([\\s\\S]*?)\\]")
                        val candidateTagRegex = Regex("\\[TESTPOINT_CANDIDATE:([\\s\\S]*?)\\]")

                        val candidateMatch = candidateTagRegex.find(message.text)
                        val candidateData = remember(candidateMatch?.value) {
                            candidateMatch?.groupValues?.getOrNull(1)?.let { jsonStr ->
                                try {
                                    val obj = org.json.JSONObject(jsonStr)
                                    TestPointCandidate(
                                        brand = obj.optString("brand", ""),
                                        model = obj.optString("model", ""),
                                        cpu = obj.optString("cpu", ""),
                                        imageUrl = obj.optString("imageUrl", ""),
                                        source = obj.optString("source", ""),
                                        title = obj.optString("title", ""),
                                        attempt = obj.optInt("attempt", 0),
                                        status = obj.optString("status", "PENDING")
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }

                        val tpMatch = testpointTagRegex.find(message.text)
                        val tpId = tpMatch?.groupValues?.getOrNull(1)
                        val testPointItem = remember(message.text, tpId) {
                            if (!tpId.isNullOrBlank()) {
                                TestPointRepository.items.firstOrNull { it.id == tpId }
                            } else {
                                val found = TestPointRepository.findByModel(message.text)
                                if (found != null) {
                                    found
                                } else {
                                    val lower = message.text.lowercase()
                                    val hasTp = lower.contains("testpoint") || lower.contains("тестпоинт") || 
                                                lower.contains("тест поинт") || lower.contains("edl 9008") || 
                                                lower.contains("brom") || lower.contains("eub")
                                    if (hasTp && message.role != "user") {
                                        val firstLine = message.text.lines().firstOrNull { it.isNotBlank() } ?: ""
                                        val (b, m) = TestPointRepository.normalizeBrand("", firstLine)
                                        val cpu = when {
                                            lower.contains("edl") || lower.contains("9008") || lower.contains("qualcomm") -> "Qualcomm EDL 9008"
                                            lower.contains("brom") || lower.contains("mediatek") || lower.contains("mtk") -> "MediaTek BROM"
                                            lower.contains("eub") || lower.contains("exynos") -> "Exynos EUB"
                                            lower.contains("kirin") || lower.contains("com 1.0") -> "Kirin COM 1.0"
                                            else -> "TestPoint / EDL"
                                        }
                                        TestPointItem(
                                            id = "tp_gen_" + System.currentTimeMillis(),
                                            brand = b,
                                            model = m.ifBlank { "Сервисный режим" },
                                            cpuType = cpu,
                                            description = message.text.lines().find { it.contains("точк") || it.contains("замык") || it.contains("плат") } ?: "Контрольные точки TestPoint для перевода в сервисный режим",
                                            imageUrl = "",
                                            frpGuide = message.text,
                                            toolsNeeded = listOf("Пинцет", "USB Кабель", "UnlockTool / Chimera")
                                        )
                                    } else null
                                }
                            }
                        }

                        val googleSearchMatch = googleSearchTagRegex.find(message.text)
                        val googleSearchJson = googleSearchMatch?.groupValues?.getOrNull(1)
                        val googleSearchImages = remember(googleSearchJson) {
                            if (!googleSearchJson.isNullOrBlank()) {
                                try {
                                    val arr = org.json.JSONArray(googleSearchJson)
                                    val list = mutableListOf<com.example.services.CustomSearchImageItem>()
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(
                                            com.example.services.CustomSearchImageItem(
                                                title = obj.optString("title", "TestPoint Photo"),
                                                link = obj.optString("link", ""),
                                                displayLink = obj.optString("displayLink", "google.com"),
                                                thumbnailLink = obj.optString("thumbnailLink", ""),
                                                contextLink = obj.optString("contextLink", ""),
                                                snippet = obj.optString("snippet", "")
                                            )
                                        )
                                    }
                                    list
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }
                        }

                        // Collect all unique images with their titles
                        val foundImages = remember(message.text, testPointItem, googleSearchImages) {
                            val list = mutableListOf<Pair<String, String>>()
                            
                            // 1. Check Google Custom Search images
                            googleSearchImages.forEach { item ->
                                if (list.none { it.first == item.link }) {
                                    list.add(Pair(item.link, item.title))
                                }
                            }

                            // 2. Check markdown images
                            imageMarkdownRegex.findAll(message.text).forEach { match ->
                                val alt = match.groupValues[1].ifBlank { "Схема / Фото платы" }
                                val url = match.groupValues[2]
                                if (list.none { it.first == url }) {
                                    list.add(Pair(url, alt))
                                }
                            }

                            // 3. Check standalone URLs
                            standaloneImageUrlRegex.findAll(message.text).forEach { match ->
                                val url = match.value
                                if (list.none { it.first == url }) {
                                    list.add(Pair(url, "Фото из сети"))
                                }
                            }

                            // 4. Check TestPoint item images
                            if (testPointItem != null) {
                                if (list.none { it.first == testPointItem.imageUrl }) {
                                    list.add(Pair(testPointItem.imageUrl, "Расположение TestPoint"))
                                }
                                testPointItem.additionalImages.forEachIndexed { idx, addUrl ->
                                    if (list.none { it.first == addUrl }) {
                                        list.add(Pair(addUrl, "Схема / Точки (вид ${idx + 2})"))
                                    }
                                }
                            }

                            list.toList()
                        }

                        // Clean display text by stripping image markdown and tags
                        var displayText = message.text
                        imageMarkdownRegex.findAll(message.text).forEach { match ->
                            displayText = displayText.replace(match.value, "")
                        }
                        if (tpMatch != null) {
                            displayText = displayText.replace(tpMatch.value, "")
                        }
                        if (googleSearchMatch != null) {
                            displayText = displayText.replace(googleSearchMatch.value, "")
                        }
                        if (candidateMatch != null) {
                            displayText = displayText.replace(candidateMatch.value, "")
                        }
                        displayText = displayText.trim()

                        var activeFullImageUrl by remember { mutableStateOf<String?>(null) }
                        var activeFullImageTitle by remember { mutableStateOf("Схема / Фото платы") }
                        var activeFullImageSourceUrl by remember { mutableStateOf<String?>(null) }

                        // Image preview if attached directly to user/AI message entity
                        if (!message.imageUrl.isNullOrEmpty()) {
                            TechAsyncImage(
                                imageUrl = message.imageUrl,
                                contentDescription = "Attachment",
                                title = if (message.text.contains("микроскоп", ignoreCase = true)) "🔬 Снимок микроскопа" else "Фото детали / платы",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        activeFullImageUrl = message.imageUrl
                                        activeFullImageTitle = if (message.text.contains("микроскоп", ignoreCase = true)) "🔬 Снимок микроскопа" else "Фото детали / платы"
                                        activeFullImageSourceUrl = null
                                    },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Render multi-image gallery if images found
                        if (foundImages.isNotEmpty()) {
                            if (foundImages.size == 1) {
                                val (singleUrl, singleAlt) = foundImages.first()
                                val singleSource = googleSearchImages.firstOrNull { it.link == singleUrl }?.contextLink
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            activeFullImageUrl = singleUrl
                                            activeFullImageTitle = singleAlt
                                            activeFullImageSourceUrl = singleSource
                                        },
                                    colors = CardDefaults.cardColors(containerColor = if (isDark) TelegramDarkSurfaceVariant else TelegramInputLight)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "🖼 $singleAlt",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = TechPrimaryBlue
                                            )
                                        }
                                        TechAsyncImage(
                                            imageUrl = singleUrl,
                                            contentDescription = singleAlt,
                                            title = singleAlt,
                                            sourceUrl = singleSource,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(190.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            } else {
                                // Horizontal Multi-Photo Carousel
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "📸 Найдено фото и схем: ${foundImages.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TechPrimaryBlue,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        foundImages.forEachIndexed { index, (url, caption) ->
                                            val photoSource = googleSearchImages.firstOrNull { it.link == url }?.contextLink
                                            Card(
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = if (isDark) TelegramDarkSurfaceVariant else TelegramInputLight),
                                                modifier = Modifier
                                                    .width(180.dp)
                                                    .clickable {
                                                        activeFullImageUrl = url
                                                        activeFullImageTitle = caption
                                                        activeFullImageSourceUrl = photoSource
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(4.dp)) {
                                                    TechAsyncImage(
                                                        imageUrl = url,
                                                        contentDescription = caption,
                                                        title = caption,
                                                        sourceUrl = photoSource,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(120.dp)
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Text(
                                                        text = "№${index + 1}: $caption",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = textColor.copy(alpha = 0.8f),
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Main text
                        if (displayText.isNotBlank()) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                ),
                                color = textColor
                            )
                        }

                        // Interactive TestPoint Candidate Verification Card
                        if (candidateData != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                ),
                                border = BorderStroke(
                                    1.5.dp,
                                    when (candidateData.status) {
                                        "APPROVED" -> Color(0xFF10B981)
                                        "REJECTED" -> Color(0xFFEF4444).copy(alpha = 0.5f)
                                        else -> TechGoldTestPoint
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = "🔍", fontSize = 14.sp)
                                            Text(
                                                text = LanguageService.getString("tp_verify_title"),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = TechGoldTestPoint
                                            )
                                        }
                                        if (candidateData.source.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = TechPrimaryBlue.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = candidateData.source,
                                                    fontSize = 10.sp,
                                                    color = TechPrimaryBlue,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${candidateData.brand} ${candidateData.model}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    if (candidateData.cpu.isNotBlank()) {
                                        Text(
                                            text = "⚙️ ${candidateData.cpu}",
                                            fontSize = 11.sp,
                                            color = textColor.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }

                                    // High-resolution motherboard photo
                                    if (candidateData.imageUrl.isNotBlank()) {
                                        TechAsyncImage(
                                            imageUrl = candidateData.imageUrl,
                                            contentDescription = candidateData.title,
                                            title = "Плата ${candidateData.model} (${candidateData.cpu})",
                                            sourceUrl = candidateData.source,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    activeFullImageUrl = candidateData.imageUrl
                                                    activeFullImageTitle = "${candidateData.model} - ${candidateData.title}"
                                                    activeFullImageSourceUrl = candidateData.source
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    when (candidateData.status) {
                                        "APPROVED" -> {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(8.dp)
                                                ) {
                                                    Text(text = "✅", fontSize = 14.sp)
                                                    Text(
                                                        text = LanguageService.getString("tp_approved_saved"),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }
                                            }
                                        }
                                        "REJECTED" -> {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(8.dp)
                                                ) {
                                                    Text(text = "❌", fontSize = 14.sp)
                                                    Text(
                                                        text = LanguageService.getString("tp_rejected_searching"),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = Color(0xFFEF4444)
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            // PENDING state - Interactive approval buttons
                                            Column {
                                                Text(
                                                    text = LanguageService.getString("tp_is_this_board_desc"),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = textColor.copy(alpha = 0.9f),
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { onApproveCandidate?.invoke(candidateData) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = LanguageService.getString("tp_yes_approve"),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                    OutlinedButton(
                                                        onClick = { onRejectCandidate?.invoke(candidateData) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = LanguageService.getString("tp_no_reject"),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFEF4444)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TestPoint Action Card (Save to KB, Search Google, Search YouTube)
                        if (testPointItem != null && candidateData == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = "📍", fontSize = 14.sp)
                                        Text(
                                            text = "Тестпоинт: ${testPointItem.brand} ${testPointItem.model}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TechGoldTestPoint
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Save to Knowledge Base button
                                        Button(
                                            onClick = {
                                                if (onSaveTestPointToKb != null) {
                                                    onSaveTestPointToKb(testPointItem)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (message.isSaved) Color(0xFF16A34A) else TechPrimaryBlue
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (message.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (message.isSaved) "В базе" else "Сохранить в базу",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Search Google Images button
                                        OutlinedButton(
                                            onClick = {
                                                val searchTarget = testPointItem.searchQuery.ifBlank {
                                                    "${testPointItem.brand} ${testPointItem.model} test point"
                                                }
                                                val uri = Uri.parse("https://www.google.com/search?tbm=isch&q=${Uri.encode(searchTarget)}")
                                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Google фото", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Google Custom Search Sources & Live Results Card
                        if (googleSearchImages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF0FDF4)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Google Custom Search: ${googleSearchImages.size} фото из профильных форумов",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Source badges
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        googleSearchImages.forEach { item ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFDCFCE7),
                                                modifier = Modifier.clickable {
                                                    if (!item.contextLink.isNullOrBlank()) {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.contextLink)).apply {
                                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                        }
                                                        try {
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            // Fallback
                                                        }
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFF16A34A))
                                                    Text(
                                                        text = item.displayLink,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF166534)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Interactive Full-screen Pinch-To-Zoom Image Viewer Overlay
                        if (activeFullImageUrl != null) {
                            InteractiveImageViewerDialog(
                                imageUrl = activeFullImageUrl!!,
                                title = activeFullImageTitle,
                                subtitle = if (testPointItem != null) "${testPointItem.brand} ${testPointItem.model}" else null,
                                sourceUrl = activeFullImageSourceUrl,
                                onDismiss = {
                                    activeFullImageUrl = null
                                    activeFullImageSourceUrl = null
                                }
                            )
                        }
                    }

                    // Timestamp & Rating Row inside bubble
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isUser && onRatingChange != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { onRatingChange(if (message.rating == 1) 0 else 1) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text(
                                        text = "👍",
                                        fontSize = 10.sp,
                                        color = if (message.rating == 1) Color(0xFF4CAF50) else textColor.copy(alpha = 0.4f)
                                    )
                                }
                                IconButton(
                                    onClick = { onRatingChange(if (message.rating == -1) 0 else -1) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text(
                                        text = "👎",
                                        fontSize = 10.sp,
                                        color = if (message.rating == -1) Color(0xFFE53935) else textColor.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }

                        Text(
                            text = timeString,
                            fontSize = 11.sp,
                            color = when {
                                isUser && isDark -> Color.White.copy(alpha = 0.75f)
                                isUser && !isDark -> Color(0xFF437340) // Subtle green-grey in Telegram light user bubbles
                                isDark -> TelegramTimestampDark
                                else -> TelegramTimestampLight
                            }
                        )

                        // Checkmarks for User Messages per variant
                        if (isUser) {
                            val checkText = "✓"
val checkColor = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight
                            Text(
                                text = checkText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = checkColor
                            )
                        }
                    }
                }
            }

            // Embedded Guide Card if guide is present
            if (parsedGuide != null) {
                Spacer(modifier = Modifier.height(4.dp))
                GuideCard(
                    guide = parsedGuide,
                    isAlreadySaved = message.isSaved,
                    onSaveToKb = if (onSaveGuideToKb != null) {
                        { onSaveGuideToKb(parsedGuide) }
                    } else null
                )
            }

            // Automatic Post-Repair QC Verification Card for AI responses containing repair steps/guide
            var showQcChecklistDialog by remember { mutableStateOf(false) }
            val hasRepairSteps = remember(message.text, parsedGuide) {
                !isUser && (
                    parsedGuide != null ||
                    message.text.contains("Шаг 1", ignoreCase = true) ||
                    message.text.contains("Алгоритм", ignoreCase = true) ||
                    message.text.contains("Диагностика", ignoreCase = true) ||
                    message.text.contains("Устранение", ignoreCase = true) ||
                    message.text.contains("QC", ignoreCase = true) ||
                    message.text.contains("Проверка работоспособности", ignoreCase = true)
                )
            }

            if (hasRepairSteps) {
                Spacer(modifier = Modifier.height(6.dp))
                val deviceName = parsedGuide?.device ?: run {
                    val firstLine = message.text.lines().firstOrNull { it.isNotBlank() } ?: "Устройство"
                    firstLine.take(30)
                }
                PostRepairQcCard(
                    deviceName = deviceName,
                    onOpenFullChecklist = { showQcChecklistDialog = true }
                )
            }

            if (showQcChecklistDialog) {
                val deviceName = parsedGuide?.device ?: "Устройство"
                PostRepairQcDialog(
                    deviceName = deviceName,
                    onDismiss = { showQcChecklistDialog = false }
                )
            }
        }
    }

    // Fullscreen Image Dialog
    if (showFullImage && !message.imageUrl.isNullOrEmpty()) {
        Dialog(
            onDismissRequest = { showFullImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showFullImage = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = message.imageUrl,
                    contentDescription = "Full Image",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

