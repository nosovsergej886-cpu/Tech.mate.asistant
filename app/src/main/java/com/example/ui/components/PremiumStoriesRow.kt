package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Модель данных истории (Stories) в стиле VK.
 */
data class Story(
    val id: String,
    val title: String,
    val isViewed: Boolean = false,
    val avatarUrl: String = "",
    val subtitle: String = "Инженер СЦ",
    val description: String = "",
    val icon: ImageVector = Icons.Default.Hardware,
    val gradientColors: List<Color> = listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)),
    val taggedDevice: String = "Диагностика"
)

/**
 * Дефолтный список историй инженеров и мастеров.
 */
val defaultMockStories = listOf(
    Story(
        id = "story_1",
        title = "Алексей СЦ",
        isViewed = false,
        subtitle = "СЦ #1 «Мастер»",
        description = "При токе потребления 0.04А на iPhone 11 сразу проверяйте LDO линию питания NAND! Заменили U3100 — аппарат запустился в штатном режиме.",
        icon = Icons.Default.PhoneIphone,
        gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
        taggedDevice = "iPhone 11 (U3100)"
    ),
    Story(
        id = "story_2",
        title = "Дмитрий BGA",
        isViewed = false,
        subtitle = "Лаборатория BGA",
        description = "Реболл процессора Snapdragon 732G. Температурный профиль 285°C с нижним подогревом 160°C. Пайка без деформации текстолита платы.",
        icon = Icons.Default.Memory,
        gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF4C1D95)),
        taggedDevice = "Snapdragon 732G"
    ),
    Story(
        id = "story_3",
        title = "Сергей Админ",
        isViewed = false,
        subtitle = "Главный СЦ",
        description = "Внимание всем мастерам: HyperOS 2.0 на некоторых партиях блокирует отладку EDL 9008 без авторизованного сервисного аккаунта.",
        icon = Icons.Default.ElectricBolt,
        gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
        taggedDevice = "Xiaomi EDL 9008"
    ),
    Story(
        id = "story_4",
        title = "Михаил Lab",
        isViewed = true,
        subtitle = "Тепловизор и КЗ",
        description = "Нашли паразитное КЗ в керамическом конденсаторе C2401 под защитным экраном радиомодуля. Нагрев до 78°C при подаче 1.2В.",
        icon = Icons.Default.Speed,
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF065F46)),
        taggedDevice = "Тепловизор КЗ"
    ),
    Story(
        id = "story_5",
        title = "Иван Mac",
        isViewed = true,
        subtitle = "Ремонт Mac",
        description = "При циклической перезагрузке DFU на MacBook проверяйте шину PP1V8_SLPS2R и обвязку контроллера T2. Замена CD3217 решила проблему.",
        icon = Icons.Default.Hardware,
        gradientColors = listOf(Color(0xFF6366F1), Color(0xFF312E81)),
        taggedDevice = "MacBook T2 DFU"
    )
)

/**
 * Истории в стиле VK (ВКонтакте):
 * - Вертикальные карточки 96x138dp со скруглением углов 12dp.
 * - Первая карточка: "Ваша история" / "Опубликовать" с аватаркой и синим плюсом.
 * - Карточки мастеров: градиент/фон, аватарка в синей ВК-обводке вверху слева, имя и статус внизу.
 * - Полноэкранный просмотрщик VK Story Viewer с таймлайном, паузой при зажатии, реакциями и ответом.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumStoriesRow(
    modifier: Modifier = Modifier,
    stories: List<Story> = defaultMockStories,
    onCreateStory: (() -> Unit)? = null,
    onStoryClick: ((Story) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
    val isDark = isSystemInDarkTheme()

    var activeStoryIndex by remember { mutableStateOf<Int?>(null) }
    var storiesState by remember { mutableStateOf(stories) }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = lazyListState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Первая карточка: «Ваша история» как в VK
            item {
                VkAddStoryCard(
                    isDark = isDark,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCreateStory?.invoke()
                    }
                )
            }

            // Карточки историй мастеров в стиле VK
            itemsIndexed(storiesState, key = { _, story -> story.id }) { index, story ->
                VkStoryCard(
                    story = story,
                    isDark = isDark,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Помечаем как просмотренную
                        storiesState = storiesState.mapIndexed { idx, s ->
                            if (idx == index) s.copy(isViewed = true) else s
                        }
                        activeStoryIndex = index
                        onStoryClick?.invoke(story)
                    }
                )
            }
        }
    }

    // Полноэкранный VK Story Viewer
    activeStoryIndex?.let { startIndex ->
        VkFullScreenStoryViewer(
            stories = storiesState,
            initialIndex = startIndex,
            onDismiss = { activeStoryIndex = null }
        )
    }
}

/**
 * Карточка добавления истории ("Ваша история") в стиле VK
 */
@Composable
fun VkAddStoryCard(
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "addStoryScale"
    )

    Surface(
        modifier = Modifier
            .width(96.dp)
            .height(138.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF262628) else Color(0xFFF0F2F5),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Аватар с синим бейджем "+" как в VK
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF3B82F6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🔧",
                        fontSize = 22.sp
                    )
                }

                // Синий плюс
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF2688EB))
                        .border(1.5.dp, if (isDark) Color(0xFF262628) else Color(0xFFF0F2F5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить историю",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = "Ваша\nистория",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                color = if (isDark) Color.White else Color(0xFF19191A)
            )

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/**
 * Карточка истории мастера в ленте (стиль VK)
 */
@Composable
fun VkStoryCard(
    story: Story,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "storyCardScale"
    )

    // Градиентная обводка аватара: ярко-синяя VK если не просмотрена, нейтральная если просмотрена
    val avatarBorderColor = if (!story.isViewed) Color(0xFF2688EB) else Color.White.copy(alpha = 0.35f)

    Surface(
        modifier = Modifier
            .width(96.dp)
            .height(138.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Градиентный технологический фон
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = story.gradientColors
                        )
                    )
            )

            // Темный градиент для читаемости текста снизу
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )

            // Аватар автора в левом верхнем углу в рамке VK
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(2.dp, avatarBorderColor, CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    story.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Имя автора и тег внизу карточки
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = story.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = story.taggedDevice,
                    fontSize = 9.5.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Полноэкранный просмотрщик историй VK (VK Fullscreen Story Viewer):
 * - Сегментированный прогресс-бар сверху (по 5 секунд на историю).
 * - Шапка: аватарка автора, имя, СЦ, время, кнопка закрытия.
 * - Центр: полноэкранный контент ремонта, диагностика, замеры.
 * - Тап слева — предыдущая сторис, тап справа — следующая сторис.
 * - Зажатие экрана (hold) — пауза показа.
 * - Нижняя панель: ответ на историю, лайк, репост.
 */
@Composable
fun VkFullScreenStoryViewer(
    stories: List<Story>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.size - 1)) }
    val currentStory = stories.getOrNull(currentIndex) ?: return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isPaused by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    var isLiked by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }

    // Анимация таймлайна для текущей истории
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            val remaining = (1f - progress.value).coerceAtLeast(0f)
            val duration = (5000 * remaining).toInt().coerceAtLeast(100)

            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration, easing = LinearEasing)
            )

            // Если анимация завершилась успешно (не прервана), переключаем на следующую
            if (progress.value >= 0.99f) {
                if (currentIndex < stories.size - 1) {
                    progress.snapTo(0f)
                    currentIndex++
                } else {
                    onDismiss()
                }
            }
        }
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
                .background(Color.Black)
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Нажатие слева -> предыдущая история
                                if (currentIndex > 0) {
                                    currentIndex--
                                    scope.launch { progress.snapTo(0f) }
                                } else {
                                    scope.launch { progress.snapTo(0f) }
                                }
                            } else {
                                // Нажатие справа -> следующая история
                                if (currentIndex < stories.size - 1) {
                                    currentIndex++
                                    scope.launch { progress.snapTo(0f) }
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Фоновый контент истории
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                currentStory.gradientColors.first(),
                                Color(0xFF0F172A),
                                Color.Black
                            )
                        )
                    )
            )

            // Центральный блок диагностической информации
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 60.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Иконка и категория ремонта
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            currentStory.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2688EB).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color(0xFF2688EB).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "📱 ${currentStory.taggedDevice}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Карточка с описанием кейса
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.90f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "⚡ Протокол диагностики",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "LIVE СЦ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentStory.description,
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Верхняя панель: прогресс-бары и шапка автора (с учётом status bar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.45f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp, bottom = 20.dp)
            ) {
                // Сегментированный таймлайн (как в VK)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val barProgress = when {
                            idx < currentIndex -> 1f
                            idx == currentIndex -> progress.value
                            else -> 0f
                        }

                        LinearProgressIndicator(
                            progress = { barProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Шапка с автором и кнопкой закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Аватарка автора
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2688EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (currentStory.title.firstOrNull() ?: 'М').uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                currentStory.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "2 ч назад",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            currentStory.subtitle,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.5.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Нижняя панель действий (как в VK): ответ, сердечко, репост
            // Оптимизировано под жестовую навигацию и полосу навигации телефона
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f), Color.Black.copy(alpha = 0.95f))
                        )
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp)
                    .padding(top = 10.dp, bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Поле ответа на историю
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, if (isInputFocused || replyText.isNotEmpty()) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = replyText,
                                onValueChange = {
                                    replyText = it
                                    isPaused = true
                                },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        isInputFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            isPaused = true
                                        }
                                    },
                                decorationBox = { innerTextField ->
                                    if (replyText.isEmpty()) {
                                        Text(
                                            text = "Ответить на историю...",
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 13.5.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    if (replyText.isNotBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val sentMsg = replyText.trim()
                                        replyText = ""
                                        isInputFocused = false
                                        isPaused = false
                                        Toast.makeText(context, "Ответ отправлен автору (${currentStory.title}): $sentMsg", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Введите сообщение для ответа", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Отправить",
                                    tint = if (replyText.isNotBlank()) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Кнопка Лайка
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLiked = !isLiked
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Лайк",
                            tint = if (isLiked) Color(0xFFFF3347) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка репоста / поделиться
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            Toast.makeText(context, "Ссылка на историю скопирована", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Поделиться",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
