package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.StoryEntity
import com.example.model.StoryScope
import com.example.services.AppDesignVariant
import com.example.services.AuthService
import com.example.services.StoryService
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * StoryOverlay component that displays active stories as circular progress-indicated
 * icons above the main messenger interface.
 * Matches VK / Telegram / Instagram aesthetic:
 * - Filter stories by device model or repair topic
 * - Circular story avatars with gradient progress rings
 * - Full-screen edge-to-edge media viewer with swipe gestures and reaction bar
 */
@Composable
fun StoryOverlay(
    modifier: Modifier = Modifier,
    onOpenAlgorithm: () -> Unit = {}
) {
    val context = LocalContext.current
    val storyService = remember { StoryService.getInstance(context) }
    val authService = remember { AuthService.getInstance(context) }
    val currentUser by authService.currentUser.collectAsState()

    val currentScId = currentUser?.serviceCenterId ?: "default_sc"
    val allStories by storyService.getStories(currentScId).collectAsState(initial = emptyList())

    val designVariant = LocalDesignVariant.current
    val isDark = LocalIsDarkTheme.current

    // Topic & Device filter
    var selectedTagFilter by remember { mutableStateOf("Все") }
    val filterOptions = listOf("Все", "📱 iPhone", "📱 Xiaomi", "📱 Samsung", "🔧 BGA", "⚡ ПО / Bootloop", "🔋 Питание")

    val filteredStories = remember(allStories, selectedTagFilter) {
        if (selectedTagFilter == "Все") {
            allStories
        } else {
            val key = when (selectedTagFilter) {
                "📱 iPhone" -> "iphone"
                "📱 Xiaomi" -> "redmi|xiaomi|poco"
                "📱 Samsung" -> "samsung"
                "🔧 BGA" -> "bga|пайка|ребол"
                "⚡ ПО / Bootloop" -> "bootloop|прошивк|ios|сбой"
                "🔋 Питание" -> "vbus|питани|кз|акб"
                else -> selectedTagFilter.lowercase()
            }
            val regex = Regex(key, RegexOption.IGNORE_CASE)
            allStories.filter {
                regex.containsMatchIn(it.title) ||
                regex.containsMatchIn(it.subtitle) ||
                regex.containsMatchIn(it.content) ||
                regex.containsMatchIn(it.taggedDeviceModel) ||
                regex.containsMatchIn(it.taggedCategory)
            }
        }
    }

    val scGroups = remember(filteredStories) {
        filteredStories.groupBy { it.authorServiceCenter.ifBlank { "СЦ «ТехноМастер»" } }.entries.toList()
    }

    var viewingGroupIndex by remember { mutableStateOf<Int?>(null) }
    var viewingStoryInGroupIndex by remember { mutableIntStateOf(0) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }

    val containerBg = when (designVariant) {
        AppDesignVariant.TELEGRAM -> if (isDark) TelegramDarkSurface else Color.White
        AppDesignVariant.VK -> if (isDark) VkDarkSurface else Color.White
        AppDesignVariant.WHATSAPP -> if (isDark) WhatsAppDarkSurface else Color.White
    }

    Surface(
        color = containerBg,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            // 1. Topic / Model Filter Pills
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedTagFilter == filter
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            when (designVariant) {
                                AppDesignVariant.TELEGRAM -> TelegramBlue
                                AppDesignVariant.VK -> VkBlue
                                AppDesignVariant.WHATSAPP -> WhatsAppLightGreenAccent
                            }
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            0.5.dp,
                            if (isSelected) Color.Transparent else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.clickable { selectedTagFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) {
                                if (designVariant == AppDesignVariant.WHATSAPP) Color.Black else Color.White
                            } else {
                                if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            },
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Stories circular row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Ваша история" / Добавить сторис (Add Story circle with "+")
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { showCreateStoryDialog = true }
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🔧", fontSize = 24.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(
                                        when (designVariant) {
                                            AppDesignVariant.TELEGRAM -> TelegramBlue
                                            AppDesignVariant.VK -> VkBlue
                                            AppDesignVariant.WHATSAPP -> WhatsAppLightGreenAccent
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Добавить",
                                    tint = if (designVariant == AppDesignVariant.WHATSAPP) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Ваша история",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active circular story icons
                itemsIndexed(filteredStories) { _, story ->
                    StoryCircleIcon(
                        story = story,
                        variant = designVariant,
                        isDark = isDark,
                        onClick = {
                            val targetGroup = scGroups.indexOfFirst { entry -> entry.value.any { it.id == story.id } }.coerceAtLeast(0)
                            val targetStory = scGroups.getOrNull(targetGroup)?.value?.indexOfFirst { it.id == story.id }?.coerceAtLeast(0) ?: 0
                            viewingGroupIndex = targetGroup
                            viewingStoryInGroupIndex = targetStory
                        }
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    // FULL-SCREEN VK/TELEGRAM STORY MEDIA VIEWER
    viewingGroupIndex?.let { gIdx ->
        if (scGroups.isNotEmpty() && gIdx in scGroups.indices) {
            FullScreenStoryMediaViewer(
                scGroups = scGroups,
                initialGroupIndex = gIdx,
                initialStoryIndex = viewingStoryInGroupIndex,
                designVariant = designVariant,
                onDismiss = { viewingGroupIndex = null },
                onOpenFullAlgorithm = onOpenAlgorithm
            )
        } else {
            viewingGroupIndex = null
        }
    }

    // CREATE STORY DIALOG
    if (showCreateStoryDialog) {
        CreateStoryDialog(
            designVariant = designVariant,
            currentScId = currentScId,
            currentUserRole = currentUser?.role ?: "master",
            currentUserName = currentUser?.name ?: "Мастер",
            currentUserEmail = currentUser?.email ?: "",
            currentScName = currentUser?.serviceCenterName ?: "СЦ «ТехноМастер»",
            onDismiss = { showCreateStoryDialog = false },
            onStoryCreated = {
                showCreateStoryDialog = false
            }
        )
    }
}

/**
 * Circular progress-indicated Story icon
 */
@Composable
fun StoryCircleIcon(
    story: StoryEntity,
    variant: AppDesignVariant,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val isAlert = story.scope == StoryScope.GLOBAL_ALERT.name
    val isPermanent = story.isPermanent
    val isVideo = story.iconEmoji == "📹" || story.mediaType == "VIDEO" || story.content.contains("[ВИДЕО")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (isAlert) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val ringGradient: List<Color> = remember(story, variant) {
        when {
            isAlert -> listOf(Color(0xFFFF334B), Color(0xFFFF7A00), Color(0xFFFF334B))
            isPermanent -> listOf(TechGoldTestPoint, Color(0xFFFFB300), TechGoldTestPoint)
            variant == AppDesignVariant.TELEGRAM -> listOf(TelegramBlue, Color(0xFF38BDF8), Color(0xFF64B5F6))
            variant == AppDesignVariant.VK -> listOf(VkBlue, Color(0xFF60A5FA), Color(0xFF2563EB))
            variant == AppDesignVariant.WHATSAPP -> listOf(WhatsAppLightGreenAccent, Color(0xFF10B981), Color(0xFF059669))
            else -> listOf(TelegramBlue, Color(0xFF38BDF8), Color(0xFF64B5F6))
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .scale(pulseScale)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.sweepGradient(colors = ringGradient),
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Surface(
                shape = CircleShape,
                color = when {
                    isAlert -> Color(0xFF3B0B14)
                    isPermanent -> Color(0xFF261E0A)
                    isDark -> Color(0xFF1E293B)
                    else -> Color(0xFFF1F5F9)
                },
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = story.iconEmoji,
                        fontSize = 24.sp
                    )
                }
            }

            when {
                isVideo -> {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                isAlert -> {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFFF334B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                isPermanent -> {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(TechGoldTestPoint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("★", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isPermanent) "12 Этапов" else story.title.take(11),
            fontSize = 11.sp,
            fontWeight = if (isPermanent || isAlert) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isAlert -> Color(0xFFFF334B)
                isPermanent -> TechGoldTestPoint
                isDark -> Color(0xFFE2E8F0)
                else -> Color(0xFF1E293B)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Full-screen story media viewer matching VK / Telegram / Instagram aesthetic:
 * - Edge-to-edge media background (100% full screen)
 * - Top segmented auto-advancing progress bars
 * - Author avatar, name, Service Center badge, timestamp, device model/category tag chips
 * - Tap Left (prev), Tap Right (next), Hold to pause
 * - Swipe Left to skip entire SC group, Swipe Right for prev SC group, Swipe Down to dismiss
 * - Bottom overlay with caption, AI diagnosis pill, reaction icons (❤️ 🔥 💡 👏), and quick reply
 */
@Composable
fun FullScreenStoryMediaViewer(
    scGroups: List<Map.Entry<String, List<StoryEntity>>>,
    initialGroupIndex: Int,
    initialStoryIndex: Int,
    designVariant: AppDesignVariant,
    onDismiss: () -> Unit,
    onOpenFullAlgorithm: () -> Unit
) {
    val context = LocalContext.current
    val storyService = remember { StoryService.getInstance(context) }
    val scope = rememberCoroutineScope()

    var currentGroupIndex by remember(initialGroupIndex) {
        mutableIntStateOf(initialGroupIndex.coerceIn(0, (scGroups.size - 1).coerceAtLeast(0)))
    }
    var currentStoryIndex by remember(initialStoryIndex, currentGroupIndex) {
        mutableIntStateOf(initialStoryIndex)
    }

    val currentGroup = scGroups.getOrNull(currentGroupIndex)
    val groupStories = currentGroup?.value ?: emptyList()
    val currentStory = groupStories.getOrNull(currentStoryIndex) ?: groupStories.firstOrNull()

    if (currentStory == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isSoundMuted by remember { mutableStateOf(false) }
    var userReaction by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }

    val isVideo = currentStory.iconEmoji == "📹" || currentStory.mediaType == "VIDEO" || currentStory.content.contains("[ВИДЕО")

    fun nextStory() {
        if (currentStoryIndex < groupStories.size - 1) {
            currentStoryIndex += 1
        } else if (currentGroupIndex < scGroups.size - 1) {
            currentGroupIndex += 1
            currentStoryIndex = 0
        } else {
            onDismiss()
        }
    }

    fun prevStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex -= 1
        } else if (currentGroupIndex > 0) {
            currentGroupIndex -= 1
            val prevSize = scGroups[currentGroupIndex].value.size
            currentStoryIndex = (prevSize - 1).coerceAtLeast(0)
        }
    }

    fun skipCurrentScGroup() {
        if (currentGroupIndex < scGroups.size - 1) {
            currentGroupIndex += 1
            currentStoryIndex = 0
            val nextScName = scGroups[currentGroupIndex].key
            Toast.makeText(context, "⏭️ Пропущен СЦ. Переход к: $nextScName", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Все истории СЦ просмотрены", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    fun prevScGroup() {
        if (currentGroupIndex > 0) {
            currentGroupIndex -= 1
            currentStoryIndex = 0
            val prevScName = scGroups[currentGroupIndex].key
            Toast.makeText(context, "⏮️ Возврат к СЦ: $prevScName", Toast.LENGTH_SHORT).show()
        }
    }

    // Story timer loop: 6s per photo, 10s per video
    val totalMs = if (isVideo) 10000L else 6000L
    LaunchedEffect(currentGroupIndex, currentStoryIndex, isPaused) {
        progress = 0f
        val stepMs = 50L
        val increment = stepMs.toFloat() / totalMs.toFloat()

        while (progress < 1f && !isPaused) {
            delay(stepMs)
            progress += increment
        }
        if (progress >= 1f && !isPaused) {
            nextStory()
        }
    }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

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
                .background(Color.Black)
                .pointerInput(currentGroupIndex, currentStoryIndex) {
                    detectDragGestures(
                        onDragStart = {
                            isPaused = true
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                        },
                        onDragEnd = {
                            isPaused = false
                            if (totalDragY > 140f) {
                                onDismiss()
                            } else if (totalDragX < -80f) {
                                skipCurrentScGroup()
                            } else if (totalDragX > 80f) {
                                prevScGroup()
                            }
                        },
                        onDragCancel = {
                            isPaused = false
                        }
                    )
                }
        ) {
            // ==========================================================
            // 1. FULLSCREEN IMMERSIVE MEDIA BACKGROUND (VK / TELEGRAM STYLE)
            // ==========================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentGroupIndex, currentStoryIndex) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                if (offset.x < size.width * 0.35f) {
                                    prevStory()
                                } else {
                                    nextStory()
                                }
                            }
                        )
                    }
                    .background(
                        when {
                            currentStory.scope == StoryScope.GLOBAL_ALERT.name -> Brush.verticalGradient(
                                listOf(Color(0xFF3B0B14), Color(0xFF1B0307), Color(0xFF0F0003))
                            )
                            currentStory.isPermanent -> Brush.verticalGradient(
                                listOf(Color(0xFF2E2208), Color(0xFF140F03), Color(0xFF0A0701))
                            )
                            else -> Brush.verticalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF090D16))
                            )
                        }
                    )
            ) {
                // PCB Traces / Media Canvas Art
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val tracePaint = if (currentStory.scope == StoryScope.GLOBAL_ALERT.name) {
                        Color(0xFFFF334B).copy(alpha = 0.2f)
                    } else {
                        Color(0xFF38BDF8).copy(alpha = 0.2f)
                    }
                    drawLine(tracePaint, androidx.compose.ui.geometry.Offset(w * 0.15f, 0f), androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.5f), strokeWidth = 2f)
                    drawLine(tracePaint, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.5f), androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.5f), strokeWidth = 2f)
                    drawLine(tracePaint, androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.5f), androidx.compose.ui.geometry.Offset(w * 0.85f, h), strokeWidth = 2f)
                }

                // Central Media Focus
                if (currentStory.mediaUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = currentStory.mediaUrl,
                        contentDescription = "Медиа сторис",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 90.dp, bottom = 220.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.size(110.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(currentStory.iconEmoji, fontSize = 48.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isVideo) "📹 Запись ремонта мастера" else "🔬 Снимок платы / донора",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ==========================================================
            // 2. TOP OVERLAY: PROGRESS BARS + AUTHOR + TAGS + CLOSE
            // ==========================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Segmented Progress Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupStories.forEachIndexed { idx, _ ->
                        val barProgress = when {
                            idx < currentStoryIndex -> 1f
                            idx == currentStoryIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(barProgress)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Author Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(currentStory.iconEmoji, fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = currentStory.authorName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Только что",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "🏢 ${currentGroup?.key ?: "СЦ мастера"} (${currentStoryIndex + 1}/${groupStories.size})",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Button to skip to next SC
                        if (scGroups.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier.clickable { skipCurrentScGroup() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("След. СЦ", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (isVideo) {
                            IconButton(onClick = { isSoundMuted = !isSoundMuted }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (isSoundMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Звук",
                                    tint = if (isSoundMuted) Color.Red else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Tags Pill Row (Device Model & Category)
                if (currentStory.taggedDeviceModel.isNotBlank() || currentStory.taggedCategory.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStory.taggedDeviceModel.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "📱 ${currentStory.taggedDeviceModel}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (currentStory.taggedCategory.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "🔧 ${currentStory.taggedCategory}",
                                    color = TechGoldTestPoint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================================
            // 3. BOTTOM OVERLAY: CAPTION + AI STATUS + REACTIONS + REPLY
            // ==========================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp)
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                // AI Diagnosis Status Pill
                val isAlert = currentStory.scope == StoryScope.GLOBAL_ALERT.name
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAlert) Color(0xFFFF334B).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isAlert) Color(0xFFFF334B) else Color(0xFF10B981).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAlert) "🚨" else "🤖", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAlert) "ИИ-АЛЕРТ: Экстренно для ВСЕХ сервисных центров" else "ИИ TechMate: Успех ремонта подтвержден",
                            color = if (isAlert) Color(0xFFFF334B) else Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title & Content Text
                Text(
                    text = currentStory.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (currentStory.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentStory.subtitle,
                        color = TechGoldTestPoint,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = currentStory.content.lines().take(2).joinToString(" "),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (currentStory.isPermanent) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenFullAlgorithm()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TechGoldTestPoint),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Text("Открыть 12 этапов регламента", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reactions + Reply Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Emoji Reactions
                    listOf("❤️", "🔥", "💡", "👏").forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = if (userReaction == emoji) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    userReaction = emoji
                                    scope.launch {
                                        storyService.incrementViews(currentStory.id)
                                        Toast.makeText(context, "Реакция $emoji отправлена мастеру!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }

                    // Reply text box
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (replyText.isEmpty()) "Ответить мастеру..." else replyText,
                                color = if (replyText.isEmpty()) Color.White.copy(alpha = 0.5f) else Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        Toast.makeText(context, "Диалог с мастером ${currentStory.authorName} (${currentGroup?.key}) открыт", Toast.LENGTH_SHORT).show()
                                    }
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Отправить",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable {
                                        Toast.makeText(context, "Сообщение отправлено мастеру!", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
