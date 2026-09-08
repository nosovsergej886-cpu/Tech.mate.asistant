package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatEntity
import com.example.services.AppDesignVariant
import com.example.services.DatabaseService
import com.example.services.LanguageService
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

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val isDark = LocalIsDarkTheme.current
    val designVariant = LocalDesignVariant.current

    var showThemeSwitcherDialog by remember { mutableStateOf(false) }
    var showRepairAlgorithmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // WHATSAPP: Navigation is located at the top (replaces bottom bar)
            if (designVariant == AppDesignVariant.WHATSAPP) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) WhatsAppDarkSurface else WhatsAppTeal)
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "WhatsApp",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 20.sp
                            )
                        },
                        actions = {
                            IconButton(onClick = { showThemeSwitcherDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Сменить оформление",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { showRepairAlgorithmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Engineering,
                                    contentDescription = "12 шагов алгоритма",
                                    tint = WhatsAppLightGreenAccent
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (isDark) WhatsAppDarkSurface else WhatsAppTeal,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )

                    // WhatsApp Iconic Top Tab Row: [ЧАТЫ] [БАЗА ЗНАНИЙ] [НАСТРОЙКИ] [ЗВОНКИ]
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = if (isDark) WhatsAppDarkSurface else WhatsAppTeal,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = WhatsAppLightGreenAccent,
                                    height = 3.5.dp
                                )
                            }
                        },
                        divider = {}
                    ) {
                        // 0: CHATS Tab
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "ЧАТЫ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedTab == 0) WhatsAppLightGreenAccent else Color.White.copy(alpha = 0.8f)
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = WhatsAppLightGreenAccent
                                    ) {
                                        Text(
                                            text = "1",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00382B),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        )
                        // 1: KNOWLEDGE BASE Tab
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = "БАЗА ЗНАНИЙ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == 1) WhatsAppLightGreenAccent else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        )
                        // 2: SETTINGS Tab
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Text(
                                    text = "НАСТРОЙКИ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == 2) WhatsAppLightGreenAccent else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        )
                        // 3: CALLS Tab
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = {
                                Text(
                                    text = "ЗВОНКИ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 3) WhatsAppLightGreenAccent else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            when (designVariant) {
                AppDesignVariant.TELEGRAM -> {
                    // TELEGRAM: Floating rounded capsule with 2-3mm bottom margin and 5mm side margins, 1-1.5cm thickness
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(27.dp),
                            color = if (isDark) TelegramDarkSurface else Color.White,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF2B3A4A) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    Triple(0, "Чаты", if (selectedTab == 0) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline),
                                    Triple(1, "База знаний", if (selectedTab == 1) Icons.Default.MenuBook else Icons.Outlined.MenuBook),
                                    Triple(2, "Настройки", if (selectedTab == 2) Icons.Default.Settings else Icons.Outlined.Settings)
                                ).forEach { (tabIdx, label, icon) ->
                                    val isSelected = selectedTab == tabIdx
                                    val activeColor = TelegramBlue
                                    val inactiveColor = if (isDark) Color(0xFF7A8B9E) else Color(0xFF8E8E93)

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
                                            if (tabIdx == 0) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = TelegramBlue,
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
                                                        modifier = Modifier.size(21.dp)
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = if (isSelected) activeColor else inactiveColor,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) activeColor else inactiveColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                AppDesignVariant.VK -> {
                    // VK: Thin, sleek bottom bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .background(if (isDark) VkDarkSurface else Color.White)
                    ) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = if (isDark) Color(0xFF2C2D2E) else Color(0xFFE1E3E6)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Triple(0, "Мессенджер", if (selectedTab == 0) Icons.Default.Forum else Icons.Outlined.Forum),
                                Triple(1, "Сервисы", if (selectedTab == 1) Icons.Default.Widgets else Icons.Outlined.Widgets),
                                Triple(2, "Профиль", if (selectedTab == 2) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle)
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
                                        if (tabIdx == 0) {
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
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) activeColor else inactiveColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) activeColor else inactiveColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                AppDesignVariant.WHATSAPP -> {
                    // WhatsApp has NO bottom bar (navigation moved to top)
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
                    0 -> ChatsScreen(
                        onOpenChat = onOpenChat,
                        onNavigateToKnowledge = { selectedTab = 1 },
                        onNavigateToProfile = { selectedTab = 2 },
                        hideTopBar = (designVariant == AppDesignVariant.WHATSAPP)
                    )
                    1 -> KnowledgeScreen(
                        onBack = null
                    )
                    2 -> ProfileScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        onNavigateAdmin = onNavigateAdmin,
                        onLogout = onLogout,
                        onBack = null
                    )
                    3 -> if (designVariant == AppDesignVariant.WHATSAPP) {
                        WhatsAppCallsView(onOpenChat = onOpenChat)
                    } else {
                        ChatsScreen(
                            onOpenChat = onOpenChat,
                            onNavigateToKnowledge = { selectedTab = 1 },
                            onNavigateToProfile = { selectedTab = 2 }
                        )
                    }
                }
            }
        }
    }

    if (showThemeSwitcherDialog) {
        ThemeSwitcherDialog(
            themeManager = themeManager,
            onDismiss = { showThemeSwitcherDialog = false }
        )
    }
    if (showRepairAlgorithmDialog) {
        RepairAlgorithmDialog(
            onDismiss = { showRepairAlgorithmDialog = false },
            onInsertStepToChat = { stepPrompt ->
                showRepairAlgorithmDialog = false
                val newChat = ChatEntity(
                    title = "Шаг ремонта: " + stepPrompt.take(25),
                    lastMessage = stepPrompt,
                    lastMessageTime = System.currentTimeMillis()
                )
                scope.launch {
                    dbService.chatDao.insertChat(newChat)
                    onOpenChat(newChat.id)
                }
            }
        )
    }
}
