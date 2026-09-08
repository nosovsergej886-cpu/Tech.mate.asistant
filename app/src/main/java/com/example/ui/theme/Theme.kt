package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.services.AppDesignVariant

val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalDesignVariant = staticCompositionLocalOf { AppDesignVariant.TELEGRAM }
val LocalDesignTokens = staticCompositionLocalOf { getDesignTokens(false, AppDesignVariant.TELEGRAM) }

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
    val variant: AppDesignVariant = AppDesignVariant.TELEGRAM
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
        AppDesignVariant.TELEGRAM -> if (darkTheme) TelegramDarkColorScheme else TelegramLightColorScheme
        AppDesignVariant.VK -> if (darkTheme) VkDarkColorScheme else VkLightColorScheme
        AppDesignVariant.WHATSAPP -> if (darkTheme) WhatsAppDarkColorScheme else WhatsAppLightColorScheme
    }
}

fun getDesignTokens(darkTheme: Boolean, variant: AppDesignVariant): DesignTokens {
    return when (variant) {
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
    designVariant: AppDesignVariant = AppDesignVariant.TELEGRAM,
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
