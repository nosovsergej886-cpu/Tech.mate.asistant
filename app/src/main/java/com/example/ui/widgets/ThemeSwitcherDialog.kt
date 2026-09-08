package com.example.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.services.AppDesignVariant
import com.example.services.AppThemeMode
import com.example.services.ThemeManager
import com.example.ui.theme.*

@Composable
fun ThemeSwitcherDialog(
    themeManager: ThemeManager,
    onDismiss: () -> Unit
) {
    val currentVariant by themeManager.designVariant.collectAsState()
    val currentMode by themeManager.themeMode.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) TelegramDarkSurface else Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(18.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Оформление и Дизайн",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Трансформация стиля и структуры",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Light / Dark Mode Buttons
                Text(
                    text = "РЕЖИМ ОСВЕЩЕНИЯ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeModeButton(
                        title = "Тёмная",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentMode == AppThemeMode.DARK,
                        modifier = Modifier.weight(1f),
                        onClick = { themeManager.setThemeMode(AppThemeMode.DARK) }
                    )

                    ThemeModeButton(
                        title = "Светлая",
                        icon = Icons.Default.LightMode,
                        isSelected = currentMode == AppThemeMode.LIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = { themeManager.setThemeMode(AppThemeMode.LIGHT) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Messenger Style & Structure (2-column tiles)
                Text(
                    text = "СТИЛЬ МЕССЕНДЖЕРА И СТРУКТУРА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Полностью меняется структура чатов, шапки, баблы и кнопки ввода под выбранный стиль:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2-Column Grid (Row 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tile 1: Aero
                    MessengerTile(
                        variant = AppDesignVariant.TELEGRAM,
                        title = "Облачный",
                        iconEmoji = "☁️",
                        features = listOf("Плавающий FAB", "Облачные баблы", "Микрофон в строке"),
                        color1 = TelegramBlue,
                        color2 = Color(0xFF4CAF50),
                        color3 = if (isDark) TelegramChatBgDark else TelegramChatBgLight,
                        isSelected = currentVariant == AppDesignVariant.TELEGRAM,
                        modifier = Modifier.weight(1f),
                        onClick = { themeManager.setDesignVariant(AppDesignVariant.TELEGRAM) }
                    )

                    // Tile 2: Connect
                    MessengerTile(
                        variant = AppDesignVariant.VK,
                        title = "Портальный",
                        iconEmoji = "🌐",
                        features = listOf("Статус активности", "Контурные карточки", "Кнопка '+' для фото"),
                        color1 = VkBlue,
                        color2 = VkLightBlue,
                        color3 = if (isDark) VkDarkBg else VkLightBg,
                        isSelected = currentVariant == AppDesignVariant.VK,
                        modifier = Modifier.weight(1f),
                        onClick = { themeManager.setDesignVariant(AppDesignVariant.VK) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2-Column Grid (Row 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tile 3: Emerald
                    MessengerTile(
                        variant = AppDesignVariant.WHATSAPP,
                        title = "Изумрудный",
                        iconEmoji = "🟢",
                        features = listOf("Изумрудная шапка", "Обои чата", "Круглая кнопка Send"),
                        color1 = WhatsAppGreenHeader,
                        color2 = WhatsAppLightGreenAccent,
                        color3 = if (isDark) WhatsAppDarkBg else WhatsAppLightBg,
                        isSelected = currentVariant == AppDesignVariant.WHATSAPP,
                        modifier = Modifier.weight(1f),
                        onClick = { themeManager.setDesignVariant(AppDesignVariant.WHATSAPP) }
                    )

                    // Tile 4: Active Summary Status Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 145.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Текущий стиль",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val summaryText = when (currentVariant) {
                                    AppDesignVariant.TELEGRAM -> "Облачный (Aero): круглый FAB с карандашом, мягкие хвостики баблов и единая капсула ввода."
                                    AppDesignVariant.VK -> "Портальный (Connect): аккуратная карточная лента, статус активности, сервисные бейджи и кнопка '+'."
                                    AppDesignVariant.WHATSAPP -> "Изумрудный (Emerald): глубокая изумрудная шапка, обои диалога и отдельная круглая кнопка отправки."
                                }

                                Text(
                                    text = summaryText,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "АКТИВЕН",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Apply / Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Применить и закрыть",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val activeColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.15f) else (if (isDark) TelegramDarkSurfaceVariant else Color(0xFFF1F5F9)),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, activeColor) else null,
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MessengerTile(
    variant: AppDesignVariant,
    title: String,
    iconEmoji: String,
    features: List<String>,
    color1: Color,
    color2: Color,
    color3: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val activeBorderColor = color1

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color1.copy(alpha = 0.12f) else (if (isDark) TelegramDarkSurfaceVariant else Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) activeBorderColor else (if (isDark) Color(0xFF2B3746) else Color(0xFFE2E8F0))
        ),
        modifier = modifier
            .heightIn(min = 145.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top Row of the Tile: Emoji + Title + Selection Checkmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = iconEmoji, fontSize = 16.sp)
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(color1),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Выбрано",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Structural feature bullet points
                features.forEach { feat ->
                    Text(
                        text = "• $feat",
                        fontSize = 9.5.sp,
                        lineHeight = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Swatch Dots at the bottom
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color3.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color1))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color2))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color3))
            }
        }
    }
}
