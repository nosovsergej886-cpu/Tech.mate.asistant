package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.services.AppDesignVariant

val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalDesignVariant = staticCompositionLocalOf { AppDesignVariant.PREMIUM_TECHMATE }
val LocalDesignTokens = staticCompositionLocalOf { getDesignTokens(false, AppDesignVariant.PREMIUM_TECHMATE) }

data class DesignTokens(
    val chatBackground: Color,
    val topBarBackground: Color,
    val topBarContentColor: Color,
    val userBubbleColor: Color,
    val aiBubbleColor: Color,
    val userTextColor: Color,
    val aiTextColor: Color,
    val userBubbleBorder: Color? = null,
    val aiBubbleBorder: Color? = null,
    val userBubbleShape: Shape,
    val aiBubbleShape: Shape,
    val cardShape: Shape,
    val cardBorder: Color? = null,
    val chipShape: Shape,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val glowingAccent: Color,
    val actionButtonShape: Shape,
    val fabColor: Color,
    val isOled: Boolean = false,
    val variant: AppDesignVariant = AppDesignVariant.PREMIUM_TECHMATE
)

// 0. Tech.Mate Premium (Deep Black, Deep Gray, Deep Blue & Glassmorphism)
val DeepPitchBlack = Color(0xFF020408)
val DeepObsidianBlack = Color(0xFF000000)
val DeepCharcoalGray = Color(0xFF0A0F1D)
val DeepSlateGray = Color(0xFF101726)
val DeepSteelGray = Color(0xFF172236)
val DeepGraphiteBorder = Color(0xFF1E2D47)
val DeepRoyalBlue = Color(0xFF1D4ED8)
val DeepMidnightBlue = Color(0xFF0C1D42)
val DeepCobaltBlue = Color(0xFF2563EB)
val DeepCyanGlow = Color(0xFF38BDF8)
val DeepIceBlue = Color(0xFF60A5FA)

// Glassmorphic translucent layers
val GlassSurfaceDark = Color(0xBF0A1020)
val GlassCardDark = Color(0xD90D1526)
val GlassTopBarDark = Color(0xF2050913)
val GlassBorderColor = Color(0x3838BDF8)

// Legacy aliases
val TechMateIndigo = DeepCobaltBlue
val TechMatePurple = DeepRoyalBlue
val TechMateSky = DeepCyanGlow
val TechMateDarkBg = DeepPitchBlack
val TechMateDarkSurface = DeepCharcoalGray
val TechMateDarkSurfaceVariant = DeepSlateGray
val TechMateCardDark = DeepSteelGray

private val TechMatePremiumDarkColorScheme = darkColorScheme(
    primary = DeepCobaltBlue,
    onPrimary = Color.White,
    primaryContainer = DeepSlateGray,
    secondary = DeepRoyalBlue,
    onSecondary = Color.White,
    tertiary = DeepCyanGlow,
    background = DeepPitchBlack,
    surface = DeepCharcoalGray,
    surfaceVariant = DeepSlateGray,
    onBackground = Color.White,
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val TechMatePremiumLightColorScheme = lightColorScheme(
    primary = TechMateIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    secondary = TechMatePurple,
    onSecondary = Color.White,
    tertiary = Color(0xFF0284C7),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

// 1. Telegram Color Scheme
private val TelegramDarkColorScheme = darkColorScheme(
    primary = TelegramBlue,
    onPrimary = Color.White,
    primaryContainer = TelegramDarkSurfaceVariant,
    secondary = TelegramGreenOnline,
    onSecondary = Color.Black,
    tertiary = TelegramLightBlue,
    background = TelegramDarkBg,
    surface = TelegramDarkSurface,
    surfaceVariant = TelegramDarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = TelegramTextSecondary
)

private val TelegramLightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EEF9),
    secondary = TelegramGreenOnline,
    onSecondary = Color.White,
    tertiary = TelegramLightBlue,
    background = TelegramChatBgLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EEF3),
    onBackground = Color(0xFF17212B),
    onSurface = Color(0xFF17212B),
    onSurfaceVariant = Color(0xFF707579)
)

// 2. VK Color Scheme
private val VkDarkColorScheme = darkColorScheme(
    primary = VkBlue,
    onPrimary = Color.White,
    primaryContainer = VkDarkSurfaceVariant,
    secondary = VkLightBlue,
    onSecondary = Color.White,
    tertiary = Color(0xFF71AAEB),
    background = VkDarkBg,
    surface = VkDarkSurface,
    surfaceVariant = VkDarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = VkTextSecondaryDark
)

private val VkLightColorScheme = lightColorScheme(
    primary = VkBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EBF1),
    secondary = VkLightBlue,
    onSecondary = Color.White,
    tertiary = Color(0xFF3F8AE0),
    background = VkLightBg,
    surface = VkLightSurface,
    surfaceVariant = VkLightSurfaceVariant,
    onBackground = Color(0xFF222222),
    onSurface = Color(0xFF222222),
    onSurfaceVariant = VkTextSecondaryLight
)

// 3. WhatsApp Color Scheme
private val WhatsAppDarkColorScheme = darkColorScheme(
    primary = WhatsAppTeal,
    onPrimary = Color.White,
    primaryContainer = WhatsAppDarkSurfaceVariant,
    secondary = WhatsAppLightGreenAccent,
    onSecondary = Color.Black,
    tertiary = WhatsAppCheckmarkBlue,
    background = WhatsAppDarkBg,
    surface = WhatsAppDarkSurface,
    surfaceVariant = WhatsAppDarkSurfaceVariant,
    onBackground = Color(0xFFE9EDEF),
    onSurface = Color(0xFFE9EDEF),
    onSurfaceVariant = WhatsAppTextSecondaryDark
)

private val WhatsAppLightColorScheme = lightColorScheme(
    primary = WhatsAppGreenHeader,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCF8C6),
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    tertiary = WhatsAppLightGreenAccent,
    background = WhatsAppLightBg,
    surface = WhatsAppLightSurface,
    surfaceVariant = WhatsAppLightSurfaceVariant,
    onBackground = Color(0xFF111B21),
    onSurface = Color(0xFF111B21),
    onSurfaceVariant = WhatsAppTextSecondaryLight
)

fun getThemeColorScheme(darkTheme: Boolean, variant: AppDesignVariant): ColorScheme {
    return when (variant) {
        AppDesignVariant.PREMIUM_TECHMATE -> if (darkTheme) TechMatePremiumDarkColorScheme else TechMatePremiumLightColorScheme
        AppDesignVariant.TELEGRAM -> if (darkTheme) TelegramDarkColorScheme else TelegramLightColorScheme
        AppDesignVariant.VK -> if (darkTheme) VkDarkColorScheme else VkLightColorScheme
        AppDesignVariant.WHATSAPP -> if (darkTheme) WhatsAppDarkColorScheme else WhatsAppLightColorScheme
    }
}

fun getDesignTokens(darkTheme: Boolean, variant: AppDesignVariant): DesignTokens {
    return when (variant) {
        AppDesignVariant.PREMIUM_TECHMATE -> DesignTokens(
            chatBackground = if (darkTheme) DeepPitchBlack else Color(0xFFF8FAFC),
            topBarBackground = if (darkTheme) GlassTopBarDark else Color.White,
            topBarContentColor = if (darkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
            userBubbleColor = if (darkTheme) Color(0xEB1E3A8A) else DeepCobaltBlue,
            aiBubbleColor = if (darkTheme) GlassCardDark else Color.White,
            userTextColor = Color.White,
            aiTextColor = if (darkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
            userBubbleBorder = if (darkTheme) Color(0x6660A5FA) else null,
            aiBubbleBorder = if (darkTheme) Color(0x3838BDF8) else Color.Black.copy(alpha = 0.06f),
            userBubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
            aiBubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            cardShape = RoundedCornerShape(18.dp),
            cardBorder = if (darkTheme) GlassBorderColor else Color.Black.copy(alpha = 0.08f),
            chipShape = RoundedCornerShape(12.dp),
            primaryAccent = DeepCobaltBlue,
            secondaryAccent = DeepRoyalBlue,
            glowingAccent = DeepCyanGlow,
            actionButtonShape = RoundedCornerShape(14.dp),
            fabColor = DeepCobaltBlue,
            isOled = darkTheme,
            variant = AppDesignVariant.PREMIUM_TECHMATE
        )

        AppDesignVariant.TELEGRAM -> DesignTokens(
            chatBackground = if (darkTheme) TelegramChatBgDark else TelegramChatBgLight,
            topBarBackground = if (darkTheme) TelegramHeaderDark else TelegramBlue,
            topBarContentColor = Color.White,
            userBubbleColor = if (darkTheme) TelegramUserBubbleDark else TelegramUserBubbleLight,
            aiBubbleColor = if (darkTheme) TelegramAiBubbleDark else TelegramAiBubbleLight,
            userTextColor = if (darkTheme) Color.White else Color(0xFF17212B),
            aiTextColor = if (darkTheme) Color.White else Color(0xFF17212B),
            userBubbleBorder = null,
            aiBubbleBorder = null,
            userBubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
            aiBubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            cardShape = RoundedCornerShape(16.dp),
            cardBorder = null,
            chipShape = RoundedCornerShape(12.dp),
            primaryAccent = TelegramBlue,
            secondaryAccent = TelegramGreenOnline,
            glowingAccent = TelegramLightBlue,
            actionButtonShape = RoundedCornerShape(12.dp),
            fabColor = TelegramBlue,
            isOled = false,
            variant = AppDesignVariant.TELEGRAM
        )

        AppDesignVariant.VK -> DesignTokens(
            chatBackground = if (darkTheme) VkDarkBg else VkLightBg,
            topBarBackground = if (darkTheme) VkDarkSurface else VkBlue,
            topBarContentColor = Color.White,
            userBubbleColor = if (darkTheme) VkUserBubbleDark else VkUserBubbleLight,
            aiBubbleColor = if (darkTheme) VkAiBubbleDark else VkAiBubbleLight,
            userTextColor = Color.White,
            aiTextColor = if (darkTheme) Color.White else Color(0xFF222222),
            userBubbleBorder = null,
            aiBubbleBorder = if (darkTheme) Color(0xFF333333) else Color(0xFFE1E3E6),
            userBubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp),
            aiBubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp),
            cardShape = RoundedCornerShape(12.dp),
            cardBorder = if (darkTheme) Color(0xFF333333) else Color(0xFFE1E3E6),
            chipShape = RoundedCornerShape(8.dp),
            primaryAccent = VkBlue,
            secondaryAccent = VkLightBlue,
            glowingAccent = Color(0xFF71AAEB),
            actionButtonShape = RoundedCornerShape(10.dp),
            fabColor = VkBlue,
            isOled = false,
            variant = AppDesignVariant.VK
        )

        AppDesignVariant.WHATSAPP -> DesignTokens(
            chatBackground = if (darkTheme) WhatsAppDarkBg else WhatsAppLightBg,
            topBarBackground = if (darkTheme) WhatsAppDarkSurface else WhatsAppGreenHeader,
            topBarContentColor = Color.White,
            userBubbleColor = if (darkTheme) WhatsAppUserBubbleDark else WhatsAppUserBubbleLight,
            aiBubbleColor = if (darkTheme) WhatsAppAiBubbleDark else WhatsAppAiBubbleLight,
            userTextColor = if (darkTheme) Color(0xFFE9EDEF) else Color(0xFF111B21),
            aiTextColor = if (darkTheme) Color(0xFFE9EDEF) else Color(0xFF111B21),
            userBubbleBorder = null,
            aiBubbleBorder = null,
            userBubbleShape = RoundedCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            aiBubbleShape = RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            cardShape = RoundedCornerShape(10.dp),
            cardBorder = null,
            chipShape = RoundedCornerShape(16.dp),
            primaryAccent = WhatsAppTeal,
            secondaryAccent = WhatsAppLightGreenAccent,
            glowingAccent = WhatsAppLightGreenAccent,
            actionButtonShape = RoundedCornerShape(24.dp),
            fabColor = WhatsAppLightGreenAccent,
            isOled = false,
            variant = AppDesignVariant.WHATSAPP
        )
    }
}

@Composable
fun TechMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    designVariant: AppDesignVariant = AppDesignVariant.PREMIUM_TECHMATE,
    content: @Composable () -> Unit
) {
    val colorScheme = getThemeColorScheme(darkTheme, designVariant)
    val tokens = getDesignTokens(darkTheme, designVariant)

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalDesignVariant provides designVariant,
        LocalDesignTokens provides tokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * Glassmorphic surface modifier for modern frosted glass UI cards, dialogs and bars.
 * Combines deep dark translucent background, subtle border highlight and smooth clipping.
 */
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color(0xD90D1526),
    borderColor: Color = Color(0x3838BDF8),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderColor, shape)

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color(0xD90D1526),
    borderColor: Color = Color(0x3838BDF8),
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.glassmorphic(
            shape = shape,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            borderWidth = borderWidth
        )
    ) {
        content()
    }
}
