package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.services.AppDesignVariant
import com.example.model.ChatEntity
import com.example.services.DatabaseService
import com.example.services.ThemeManager
import com.example.ui.theme.*
import com.example.ui.widgets.RepairAlgorithmDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onOpenChat: (String) -> Unit,
    onNavigateAdmin: () -> Unit,
    onLogout: () -> Unit,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeManager = remember { ThemeManager.getInstance(context) }
    val dbService = remember { DatabaseService.getInstance(context) }

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val isDark = LocalIsDarkTheme.current

    var showRepairAlgorithmDialog by remember { mutableStateOf(false) }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val tokens = LocalDesignTokens.current
    val isPremium = tokens.variant == AppDesignVariant.PREMIUM_TECHMATE

    // Intercept hardware/gesture back button so user does not exit app accidentally
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Adaptive Bottom Bar matching the active design variant (Flagship Tech.mate Glassmorphism by default)
            val barBg = when (tokens.variant) {
                AppDesignVariant.PREMIUM_TECHMATE -> if (isDark) Color(0xFF0B0F19).copy(alpha = 0.96f) else Color(0xFFFFFFFF)
                AppDesignVariant.TELEGRAM -> if (isDark) TelegramDarkSurface else Color.White
                AppDesignVariant.VK -> if (isDark) VkDarkSurface else Color.White
                AppDesignVariant.WHATSAPP -> if (isDark) WhatsAppDarkSurface else Color.White
            }
            val dividerColor = when (tokens.variant) {
                AppDesignVariant.PREMIUM_TECHMATE -> if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
                AppDesignVariant.TELEGRAM -> if (isDark) Color(0xFF232E3C) else Color(0xFFE2E8F0)
                AppDesignVariant.VK -> if (isDark) Color(0xFF2C2D2E) else Color(0xFFE1E3E6)
                AppDesignVariant.WHATSAPP -> if (isDark) Color(0xFF1F2C34) else Color(0xFFE2E8F0)
            }
            val activeAccent = when (tokens.variant) {
                AppDesignVariant.PREMIUM_TECHMATE -> TechMateIndigo
                AppDesignVariant.TELEGRAM -> TelegramBlue
                AppDesignVariant.VK -> VkBlue
                AppDesignVariant.WHATSAPP -> WhatsAppLightGreenAccent
            }
            val inactiveAccent = when (tokens.variant) {
                AppDesignVariant.PREMIUM_TECHMATE -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                AppDesignVariant.TELEGRAM -> TelegramTextSecondary
                AppDesignVariant.VK -> if (isDark) VkTextSecondaryDark else VkTextSecondaryLight
                AppDesignVariant.WHATSAPP -> if (isDark) WhatsAppTextSecondaryDark else WhatsAppTextSecondaryLight
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBg)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(
                    thickness = if (isPremium) 1.dp else 0.5.dp,
                    color = dividerColor
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Triple(0, "Лента", if (selectedTab == 0) Icons.Default.DynamicFeed else Icons.Outlined.DynamicFeed),
                        Triple(1, "Чаты", if (selectedTab == 1) Icons.Default.Forum else Icons.Outlined.Forum),
                        Triple(2, "Знания", if (selectedTab == 2) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder),
                        Triple(3, "Профиль", if (selectedTab == 3) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle)
                    ).forEach { (tabIdx, label, icon) ->
                        val isSelected = selectedTab == tabIdx
                        val activeColor = activeAccent
                        val inactiveColor = inactiveAccent

                        val pillBg = if (isSelected && isPremium) {
                            TechMateIndigo.copy(alpha = if (isDark) 0.16f else 0.10f)
                        } else Color.Transparent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(pillBg)
                                .clickable {
                                    if (selectedTab != tabIdx) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        selectedTab = tabIdx
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                if (tabIdx == 1) { // Chat Tab Badge
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = activeAccent,
                                                contentColor = Color.White
                                            ) {
                                                Text("1", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) activeColor else inactiveColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) activeColor else inactiveColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else inactiveColor
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(targetState = selectedTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    0 -> PostsScreen(isDarkTheme = isDarkTheme)
                    1 -> ChatsScreen(
                        onOpenChat = onOpenChat,
                        onNavigateToKnowledge = { selectedTab = 2 },
                        onNavigateToProfile = { selectedTab = 3 },
                        hideTopBar = false
                    )
                    2 -> KnowledgeScreen(
                        onBack = null
                    )
                    3 -> ProfileScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        onNavigateAdmin = onNavigateAdmin,
                        onLogout = onLogout,
                        onBack = null
                    )
                }
            }
        }
    }
}
