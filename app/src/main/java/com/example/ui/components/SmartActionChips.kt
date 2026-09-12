package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Модель данных для умного действия / подсказки
 */
data class SmartActionChipData(
    val id: String,
    val text: String,
    val icon: ImageVector,
    val prompt: String = text,
    val accentColor: Color = Color(0xFF6366F1)
)

/**
 * Стандартный набор умных подсказок для работы мастера в чате
 */
val defaultSmartActionChips = listOf(
    SmartActionChipData(
        id = "shorten",
        text = "📝 Сделай короче",
        icon = Icons.Default.ShortText,
        prompt = "Сделай предыдущий ответ короче, оставь только конкретные шаги и контрольные замеры.",
        accentColor = Color(0xFF3B82F6)
    ),
    SmartActionChipData(
        id = "rephrase",
        text = "🔄 Перефразируй",
        icon = Icons.Default.Replay,
        prompt = "Перефразируй понятным инженерным языком без лишней теории.",
        accentColor = Color(0xFF10B981)
    ),
    SmartActionChipData(
        id = "save_knowledge",
        text = "💾 В знания",
        icon = Icons.Default.BookmarkBorder,
        prompt = "Сохрани этот кейс в базу знаний СЦ с тегами.",
        accentColor = Color(0xFFF59E0B)
    ),
    SmartActionChipData(
        id = "datasheet",
        text = "🔍 Найти даташит",
        icon = Icons.Default.ManageSearch,
        prompt = "Найди распиновку микросхемы и контрольные напряжения.",
        accentColor = Color(0xFF8B5CF6)
    ),
    SmartActionChipData(
        id = "testpoint",
        text = "⚡ Прозвонка цепи",
        icon = Icons.Default.ElectricBolt,
        prompt = "Подскажи алгоритм прозвонки цепи на КЗ мультиметром в режиме падения напряжения.",
        accentColor = Color(0xFFEC4899)
    )
)

/**
 * МОДУЛЬ 2: SmartActionChips (Умные подсказки в чате)
 *
 * Особенности реализации:
 * - Размещается над строкой ввода в чате.
 * - Плавное появление через `AnimatedVisibility` с `expandHorizontally` + `fadeIn`
 *   при помощи пружинной физики `spring(stiffness = Spring.StiffnessLow)`.
 * - Дизайн: `RoundedCornerShape(20.dp)`, фон `MaterialTheme.colorScheme.surfaceVariant`
 *   с эффектом Glassmorphism и тонкой обводкой `BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))` для темной темы.
 * - Анимация цвета фона при нажатии с помощью `animateColorAsState`.
 * - Тактильный виброотклик `HapticFeedbackType.TextHandleMove`.
 */
@Composable
fun SmartActionChips(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    chips: List<SmartActionChipData> = defaultSmartActionChips,
    onChipClick: (SmartActionChipData) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    AnimatedVisibility(
        visible = isVisible,
        enter = expandHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        ),
        exit = shrinkHorizontally(
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        ) + fadeOut()
    ) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(chips, key = { it.id }) { chip ->
                SmartActionChipItem(
                    chip = chip,
                    isDark = isDark,
                    onClick = { onChipClick(chip) }
                )
            }
        }
    }
}

/**
 * Элемент отдельного динамического чипса подсказки
 */
@Composable
private fun SmartActionChipItem(
    chip: SmartActionChipData,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Анимация цвета фона при тапе / нажатии
    val defaultBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    }

    val pressedBg = chip.accentColor.copy(alpha = if (isDark) 0.35f else 0.20f)

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isPressed) pressedBg else defaultBg,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chipBgColor"
    )

    Surface(
        modifier = Modifier
            .shadow(
                elevation = if (isDark) 2.dp else 1.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = chip.accentColor.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(20.dp),
        color = animatedBackgroundColor,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = chip.icon,
                contentDescription = null,
                tint = chip.accentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = chip.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.85f)
            )
        }
    }
}
