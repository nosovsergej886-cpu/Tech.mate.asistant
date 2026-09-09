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
import com.example.model.ChatEntity
import com.example.services.DatabaseService
import com.example.services.ThemeManager
import com.example.ui.theme.*
import com.example.ui.widgets.RepairAlgorithmDialog
import com.example.ui.widgets.ThemeSwitcherDialog
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

    var showThemeSwitcherDialog by remember { mutableStateOf(false) }
    var showRepairAlgorithmDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // VK: Thin, sleek bottom bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // REMOVED navigationBarsPadding() here to fix the black bar at the bottom!
                    // Scaffold will automatically inset it if we don't consume it.
                    .background(if (isDark) VkDarkSurface else Color.White)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = if (isDark) Color(0xFF2C2D2E) else Color(0xFFE1E3E6)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Triple(0, "Лента", if (selectedTab == 0) Icons.Default.DynamicFeed else Icons.Outlined.DynamicFeed),
                        Triple(1, "Чаты", if (selectedTab == 1) Icons.Default.Forum else Icons.Outlined.Forum),
                        Triple(2, "Сервисы", if (selectedTab == 2) Icons.Default.Widgets else Icons.Outlined.Widgets),
                        Triple(3, "Профиль", if (selectedTab == 3) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle)
                    ).forEach { (tabIdx, label, icon) ->
                        val isSelected = selectedTab == tabIdx
                        val activeColor = VkBlue
                        val inactiveColor = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedTab = tabIdx },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (tabIdx == 1) { // Chat Tab Badge
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = VkBlue,
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
                                    fontSize = 11.sp,
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
