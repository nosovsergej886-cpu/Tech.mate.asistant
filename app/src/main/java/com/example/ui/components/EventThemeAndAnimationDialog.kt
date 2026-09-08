package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.services.AuthService
import com.example.services.EventTheme
import com.example.services.FirebaseSyncService
import com.example.services.SplashAnimationType
import com.example.services.ThemeManager
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechPrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun EventThemeAndAnimationDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeManager = remember { ThemeManager.getInstance(context) }
    val authService = remember { AuthService.getInstance(context) }
    val currentUser by authService.currentUser.collectAsState()

    val currentSplash by themeManager.splashAnimation.collectAsState()
    val currentEvent by themeManager.eventTheme.collectAsState()

    val userEmail = currentUser?.email?.ifBlank { currentUser?.username } ?: ""
    val isGodAdmin = authService.isGodMode() || themeManager.canUserModifyEvents(userEmail)

    var selectedSplash by remember { mutableStateOf(currentSplash) }
    var selectedEvent by remember { mutableStateOf(currentEvent) }
    var isPublishingToCloud by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141A24),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon & Status
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isGodAdmin) TechGoldTestPoint.copy(alpha = 0.2f) else TechPrimaryBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isGodAdmin) "👑" else "🎨", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Дизайн ивентов и Запуск",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGodAdmin) TechGoldTestPoint.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (isGodAdmin) TechGoldTestPoint.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isGodAdmin) Icons.Default.Check else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isGodAdmin) TechGoldTestPoint else Color.LightGray,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGodAdmin) "Главный Администратор • Управление активно" else "Мастер СЦ • Режим просмотра ивента",
                                color = if (isGodAdmin) TechGoldTestPoint else Color.LightGray,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!isGodAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔒 Смена глобального дизайна и анимации запуска доступна только Главному Администратору (Сергей Носов / nosovsergej886@gmail.com). Текущий ивент активен для всей сети.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 1: SPLASH ANIMATION SELECTOR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚀 Анимация запуска (Splash Screen):",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SplashAnimationType.entries.forEach { anim ->
                            val isSelected = selectedSplash == anim
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) TechPrimaryBlue.copy(alpha = 0.25f) else Color(0xFF1B222F),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) TechPrimaryBlue else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isGodAdmin) {
                                        selectedSplash = anim
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(anim.iconEmoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = anim.titleRu,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = anim.descriptionRu,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = TechPrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 2: EVENT THEME SELECTOR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎭 Тематический ивент приложения:",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EventTheme.entries.forEach { ev ->
                            val isSelected = selectedEvent == ev
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) TechGoldTestPoint.copy(alpha = 0.2f) else Color(0xFF1B222F),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) TechGoldTestPoint else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isGodAdmin) {
                                        selectedEvent = ev
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ev.iconEmoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ev.titleRu,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ev.descriptionRu,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Баннер: \"${ev.bannerText}\"",
                                            color = TechGoldTestPoint.copy(alpha = 0.9f),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = TechGoldTestPoint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // ACTION BUTTONS
                    if (isGodAdmin) {
                        Button(
                            onClick = {
                                themeManager.setSplashAnimation(selectedSplash, userEmail)
                                themeManager.setEventTheme(selectedEvent, userEmail)
                                isPublishingToCloud = true
                                scope.launch {
                                    try {
                                        val syncService = FirebaseSyncService.getInstance(context)
                                        syncService.pushEventTheme(selectedEvent.name, selectedSplash.name)
                                        Toast.makeText(context, "Ивент и анимация успешно применены и сохранены в Google Cloud!", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Применено локально: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isPublishingToCloud = false
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = !isPublishingToCloud,
                            colors = ButtonDefaults.buttonColors(containerColor = TechGoldTestPoint),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isPublishingToCloud) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Публикация в Google Cloud...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Применить и опубликовать в Google Cloud", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Закрыть", fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}
