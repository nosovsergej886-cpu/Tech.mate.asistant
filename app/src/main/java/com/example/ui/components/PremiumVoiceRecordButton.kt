package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Состояния голосовой записи согласно ТЗ
 */
enum class VoiceRecordState {
    Idle,
    Recording,
    Processing,
    Done
}

/**
 * МОДУЛЬ 3: PremiumVoiceRecordButton (Голосовая запись с AI-саммари)
 *
 * Особенности реализации:
 * - Реагирует на долгое нажатие (Press down -> начало записи, release -> завершение).
 * - Во время записи кнопка плавно трансформируется в анимированную звуковую волну (Canvas с динамическими амплитудами).
 * - Экран плавно затемняется с полупрозрачной дымкой и индикатором времени.
 * - Состояния: Idle -> Recording -> Processing (элегантный лоадер "ИИ анализирует...") -> Done.
 * - Результат передаётся через колбэк [onSummaryReady] в формате:
 *   "🎙️ Голосовая заметка. 📌 Суть: [краткое саммари]. ✅ Задачи: [список]"
 */
@Composable
fun PremiumVoiceRecordButton(
    modifier: Modifier = Modifier,
    onSummaryReady: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var state by remember { mutableStateOf(VoiceRecordState.Idle) }
    var recordingDurationSec by remember { mutableStateOf(0) }

    // Анимация таймера записи
    LaunchedEffect(state) {
        if (state == VoiceRecordState.Recording) {
            recordingDurationSec = 0
            while (state == VoiceRecordState.Recording) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }

    // Симуляция AI-обработки при переходе в Processing
    LaunchedEffect(state) {
        if (state == VoiceRecordState.Processing) {
            delay(2200) // Имитация обращения к нейросети
            state = VoiceRecordState.Done
            delay(500)

            // Формируем результат согласно требованиям ТЗ
            val voiceSummaryMessage = """
                🎙️ Голосовая заметка (длительность: ${recordingDurationSec.coerceAtLeast(1)}с)
                📌 Суть: Диагностика цепи питания процессора и дежурных напряжений. Выявлена просадка линии PP_VDD_MAIN до 1.2В.
                ✅ Задачи:
                • Выполнить замер падения напряжения на конденсаторах обвязки PMIC.
                • Проверить термокамерой локальный нагрев ключей верхнего плеча.
                • Провести согласование замены ШИМ-контроллера с клиентом.
            """.trimIndent()

            onSummaryReady(voiceSummaryMessage)
            state = VoiceRecordState.Idle
        }
    }

    // Затемнение фона экрана при записи и обработке
    val screenDimAlpha by animateFloatAsState(
        targetValue = when (state) {
            VoiceRecordState.Recording -> 0.72f
            VoiceRecordState.Processing -> 0.60f
            else -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "screenDimAlpha"
    )

    // Масштабирование кнопки при записи
    val buttonScale by animateFloatAsState(
        targetValue = when (state) {
            VoiceRecordState.Recording -> 1.15f
            VoiceRecordState.Processing -> 1.05f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Оверлей затемнения экрана
        if (screenDimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = screenDimAlpha))
            )
        }

        // Информационный баннер над кнопкой при записи и обработке
        AnimatedVisibility(
            visible = state == VoiceRecordState.Recording || state == VoiceRecordState.Processing,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0x666366F1))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state == VoiceRecordState.Recording) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Запись аудио... 0:${recordingDurationSec.toString().padStart(2, '0')}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (isDark) Color.White else Color.Black
                        )
                    } else if (state == VoiceRecordState.Processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF6366F1),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "✨ ИИ анализирует аудио...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF6366F1)
                        )
                    }
                }
            }
        }

        // Сама кнопка записи
        val buttonColor by animateColorAsState(
            targetValue = when (state) {
                VoiceRecordState.Recording -> Color(0xFFDC2626)
                VoiceRecordState.Processing -> Color(0xFF6366F1)
                VoiceRecordState.Done -> Color(0xFF10B981)
                VoiceRecordState.Idle -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            },
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "voiceButtonColor"
        )

        Surface(
            modifier = Modifier
                .scale(buttonScale)
                .size(if (state == VoiceRecordState.Recording) 72.dp else 48.dp)
                .shadow(
                    elevation = if (state == VoiceRecordState.Recording) 14.dp else 3.dp,
                    shape = CircleShape,
                    spotColor = if (state == VoiceRecordState.Recording) Color(0x88DC2626) else Color(0x446366F1)
                )
                .clip(CircleShape)
                .pointerInput(state) {
                    detectTapGestures(
                        onPress = {
                            if (state == VoiceRecordState.Idle) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                state = VoiceRecordState.Recording
                                tryAwaitRelease()
                                if (state == VoiceRecordState.Recording) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    state = VoiceRecordState.Processing
                                }
                            }
                        }
                    )
                },
            shape = CircleShape,
            color = buttonColor,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    VoiceRecordState.Idle -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Удерживайте для голосовой записи",
                            tint = if (isDark) Color.White else Color(0xFF334155),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    VoiceRecordState.Recording -> {
                        // Анимированная звуковая волна на Canvas
                        SoundWaveVisualizer(modifier = Modifier.size(44.dp))
                    }
                    VoiceRecordState.Processing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    }
                    VoiceRecordState.Done -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Готово",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Анимированная звуковая волна с динамической амплитудой на базе Canvas
 */
@Composable
private fun SoundWaveVisualizer(
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    barColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundWaveTransition")

    // Создаем фазовые сдвиги для каждой линии звуковой волны
    val animPhase1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )

    val animPhase2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 440, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )

    val animPhase3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / (barCount * 2f)
        val gap = barWidth

        val phases = listOf(animPhase1, animPhase2, animPhase1 * 0.9f, animPhase3, animPhase2 * 0.85f)

        for (i in 0 until barCount) {
            val phase = phases[i % phases.size]
            val barHeight = (height * 0.25f) + (height * 0.70f * phase)
            val startX = (i * (barWidth + gap)) + (gap / 2f)
            val startY = (height - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(startX, startY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
