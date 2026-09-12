package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun StoriesBar(
    onOpenAlgorithm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storyService = remember { StoryService.getInstance(context) }
    val authService = remember { AuthService.getInstance(context) }
    val currentUser by authService.currentUser.collectAsState()

    val currentScId = currentUser?.serviceCenterId ?: "default_sc"
    val allStories by storyService.getStories(currentScId).collectAsState(initial = emptyList())

    val designVariant = LocalDesignVariant.current
    val isDark = LocalIsDarkTheme.current

    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterOptions = listOf("Все", "🚨 Сбои ПО", "🚀 Обновления", "🏢 Мой СЦ")

    var viewingStoryIndex by remember { mutableStateOf<Int?>(null) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }

    val filteredStories = remember(allStories, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> allStories.filter { it.isPermanent || it.scope == StoryScope.GLOBAL_ALERT.name }
            2 -> allStories.filter { it.isPermanent || it.scope == StoryScope.GLOBAL_UPDATE.name }
            3 -> allStories.filter { it.isPermanent || it.scope == StoryScope.LOCAL_SC.name }
            else -> allStories
        }
    }

    val barBackground = when (designVariant) {
        AppDesignVariant.PREMIUM_TECHMATE -> if (isDark) TechMateDarkSurface else Color.White
        AppDesignVariant.TELEGRAM -> if (isDark) TelegramDarkSurface else Color.White
        AppDesignVariant.VK -> if (isDark) VkDarkSurface else Color.White
        AppDesignVariant.WHATSAPP -> if (isDark) WhatsAppDarkSurface else Color.White
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBackground)
            .padding(vertical = 8.dp)
    ) {
        // Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(filterOptions) { index, filterName ->
                val isSelected = selectedFilterIndex == index
                val chipColor = when {
                    isSelected -> when (designVariant) {
                        AppDesignVariant.PREMIUM_TECHMATE -> TechMateIndigo
                        AppDesignVariant.TELEGRAM -> TelegramBlue
                        AppDesignVariant.VK -> VkBlue
                        AppDesignVariant.WHATSAPP -> if (isDark) WhatsAppLightGreenAccent else WhatsAppTeal
                    }
                    isDark -> Color(0xFF1E293B)
                    else -> Color(0xFFF1F5F9)
                }
                val chipTextColor = when {
                    isSelected -> if (designVariant == AppDesignVariant.WHATSAPP && isDark) Color.Black else Color.White
                    isDark -> Color(0xFFE2E8F0)
                    else -> Color(0xFF475569)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = chipColor,
                    modifier = Modifier.clickable { selectedFilterIndex = index }
                ) {
                    Text(
                        text = filterName,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = chipTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stories Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. ADD STORY BUTTON
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showCreateStoryDialog = true }
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить историю",
                                tint = when (designVariant) {
                                    AppDesignVariant.PREMIUM_TECHMATE -> TechMateIndigo
                                    AppDesignVariant.TELEGRAM -> TelegramBlue
                                    AppDesignVariant.VK -> VkBlue
                                    AppDesignVariant.WHATSAPP -> if (isDark) WhatsAppLightGreenAccent else WhatsAppTeal
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Создать",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            // 2. STORIES ITEMS (Filtered)
            itemsIndexed(filteredStories) { index, story ->
                val ringBrush = when {
                    story.isPermanent -> Brush.sweepGradient(
                        listOf(TechGoldTestPoint, Color(0xFFFFB800), TechGoldTestPoint)
                    )
                    story.scope == StoryScope.GLOBAL_ALERT.name -> Brush.sweepGradient(
                        listOf(Color(0xFFFF334B), Color(0xFFFF6B00), Color(0xFFFF334B))
                    )
                    story.scope == StoryScope.GLOBAL_UPDATE.name -> Brush.sweepGradient(
                        listOf(TelegramBlue, Color(0xFF38BDF8), TelegramBlue)
                    )
                    designVariant == AppDesignVariant.VK -> Brush.sweepGradient(
                        listOf(Color(0xFFFF334B), Color(0xFFE02476), Color(0xFF7044FF), Color(0xFF0077FF), Color(0xFFFF334B))
                    )
                    designVariant == AppDesignVariant.WHATSAPP -> Brush.sweepGradient(
                        listOf(WhatsAppLightGreenAccent, Color(0xFF25D366), WhatsAppLightGreenAccent)
                    )
                    else -> Brush.sweepGradient(
                        listOf(TelegramBlue, Color(0xFF22D3EE), TelegramBlue)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (story.isPermanent) {
                                onOpenAlgorithm()
                            } else {
                                viewingStoryIndex = index
                            }
                        }
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Ring
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(ringBrush)
                                .padding(2.5.dp)
                        ) {
                            // Inner Avatar
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = story.iconEmoji,
                                    fontSize = 22.sp
                                )
                            }
                        }

                        // Status Badge
                        if (story.isPermanent) {
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(TechGoldTestPoint),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("★", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (story.scope == StoryScope.GLOBAL_ALERT.name) {
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF334B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (story.isPermanent) "Этапы ремонта" else story.title.take(10),
                        fontSize = 11.sp,
                        fontWeight = if (story.isPermanent) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            story.isPermanent -> TechGoldTestPoint
                            isDark -> Color(0xFFE2E8F0)
                            else -> Color(0xFF1E293B)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    // STORY VIEWER DIALOG
    viewingStoryIndex?.let { index ->
        val currentStory = filteredStories.getOrNull(index)
        if (currentStory != null) {
            StoryViewerDialog(
                story = currentStory,
                allStories = filteredStories,
                currentIndex = index,
                onDismiss = { viewingStoryIndex = null },
                onSelectIndex = { viewingStoryIndex = it },
                onOpenFullAlgorithm = onOpenAlgorithm
            )
        } else {
            viewingStoryIndex = null
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
                Toast.makeText(context, "История опубликована!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun StoryViewerDialog(
    story: StoryEntity,
    allStories: List<StoryEntity>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelectIndex: (Int) -> Unit,
    onOpenFullAlgorithm: () -> Unit
) {
    val context = LocalContext.current
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Auto-advance timer (6 seconds per story)
    LaunchedEffect(currentIndex, isPaused) {
        progress = 0f
        val stepMs = 50L
        val totalMs = 6000L
        val increment = stepMs.toFloat() / totalMs.toFloat()

        while (progress < 1f && !isPaused) {
            delay(stepMs)
            progress += increment
        }
        if (progress >= 1f && !isPaused) {
            if (currentIndex < allStories.size - 1) {
                onSelectIndex(currentIndex + 1)
            } else {
                onDismiss()
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
                .background(Color(0xFF0F172A))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                if (currentIndex > 0) onSelectIndex(currentIndex - 1)
                            } else {
                                if (currentIndex < allStories.size - 1) onSelectIndex(currentIndex + 1)
                                else onDismiss()
                            }
                        }
                    )
                }
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Progress Indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allStories.forEachIndexed { idx, _ ->
                        val barProgress = when {
                            idx < currentIndex -> 1f
                            idx == currentIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
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

                Spacer(modifier = Modifier.height(12.dp))

                // Header Row (Author, Tag, Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji / Avatar
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(story.iconEmoji, fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = story.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Scope Badge
                            val (badgeText, badgeBg, badgeTextColor) = when (story.scope) {
                                StoryScope.ALGORITHM.name -> Triple("РЕГЛАМЕНТ", TechGoldTestPoint, Color.Black)
                                StoryScope.GLOBAL_ALERT.name -> Triple("ДЛЯ ВСЕХ • СБОЙ ПО", Color(0xFFFF334B), Color.White)
                                StoryScope.GLOBAL_UPDATE.name -> Triple("ДЛЯ ВСЕХ • ОБНОВЛЕНИЕ", TelegramBlue, Color.White)
                                else -> Triple("МОЙ СЦ", Color(0xFF64748B), Color.White)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = story.authorServiceCenter,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Story Content Container (Scrollable)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    border = BorderStroke(
                        1.dp,
                        when (story.scope) {
                            StoryScope.GLOBAL_ALERT.name -> Color(0xFFFF334B).copy(alpha = 0.6f)
                            StoryScope.ALGORITHM.name -> TechGoldTestPoint.copy(alpha = 0.6f)
                            else -> Color(0xFF334155)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp)
                    ) {
                        // Warning banner if global alert
                        if (story.scope == StoryScope.GLOBAL_ALERT.name) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFF334B).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFFF334B).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF334B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Критическая информация! Публично для всех мастеров и СЦ",
                                        color = Color(0xFFFF334B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Text(
                            text = story.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (story.subtitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = story.subtitle,
                                color = TechGoldTestPoint,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // VIDEO PLAYER PREVIEW IF VIDEO STORY
                        if (story.iconEmoji == "📹" || story.content.contains("[ВИДЕО")) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0B1320),
                                border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = TelegramBlue.copy(alpha = 0.25f),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = TelegramBlue,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "📹 Видеозапись мастера",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Записано прямо в приложении • Качество HD",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = story.content,
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (story.isPermanent) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpenFullAlgorithm()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TechGoldTestPoint),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Открыть интерактивный чеклист 12 шагов", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("TechMate Story", "${story.title}\n${story.content}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Текст предупреждения скопирован в буфер", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Скопировать")
                    }

                    Button(
                        onClick = {
                            if (currentIndex < allStories.size - 1) onSelectIndex(currentIndex + 1)
                            else onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (story.scope) {
                                StoryScope.GLOBAL_ALERT.name -> Color(0xFFFF334B)
                                StoryScope.ALGORITHM.name -> TechGoldTestPoint
                                else -> TelegramBlue
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (currentIndex < allStories.size - 1) "Дальше →" else "Закрыть",
                            color = if (story.scope == StoryScope.ALGORITHM.name) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

