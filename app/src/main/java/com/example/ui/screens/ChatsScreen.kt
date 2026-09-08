package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatEntity
import com.example.services.AuthService
import com.example.services.DatabaseService
import com.example.services.LanguageService
import com.example.services.ThemeManager
import com.example.services.AppDesignVariant
import com.example.ui.components.StoriesBar
import com.example.ui.components.StoryOverlay
import com.example.ui.theme.*
import com.example.ui.util.filterDuplicateInput
import com.example.ui.widgets.RepairAlgorithmDialog
import com.example.ui.widgets.ThemeSwitcherDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val avatarColors = listOf(
    Color(0xFF527DA3),
    Color(0xFF50A2E9),
    Color(0xFF42A5F5),
    Color(0xFF26A69A),
    Color(0xFFAB47BC),
    Color(0xFFFFA726)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onOpenChat: (String) -> Unit,
    onNavigateToKnowledge: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    hideTopBar: Boolean = false
) {
    val context = LocalContext.current
    val dbService = remember { DatabaseService.getInstance(context) }
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current
    val tokens = LocalDesignTokens.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val chatsFlow = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            dbService.chatDao.getAllChats()
        } else {
            dbService.chatDao.searchChats(searchQuery)
        }
    }

    val chats by chatsFlow.collectAsState(initial = emptyList())

    val authService = remember { AuthService.getInstance(context) }
    val currentUser by authService.currentUser.collectAsState()

    val pinnedChats = remember(chats) { chats.filter { it.isPinned } }
    val unpinnedChats = remember(chats) { chats.filter { !it.isPinned } }

    // Ensure pinned support chat and pinned team chat exist
    LaunchedEffect(currentUser) {
        val supportId = "support_official_channel"
        val existingSupport = dbService.chatDao.getChatById(supportId)
        if (existingSupport == null) {
            dbService.chatDao.insertChat(
                ChatEntity(
                    id = supportId,
                    title = "Поддержка",
                    lastMessage = "Служба поддержки Tech.Mate онлайн. Напишите ваш вопрос!",
                    lastMessageTime = System.currentTimeMillis(),
                    isPinned = true
                )
            )
        } else if (!existingSupport.isPinned || existingSupport.title != "Поддержка") {
            dbService.chatDao.updateChat(
                existingSupport.copy(title = "Поддержка", isPinned = true)
            )
        }

        // Pinned chat for employees of the current service center
        val scId = currentUser?.serviceCenterId?.ifBlank { "default_sc" } ?: "default_sc"
        val scName = currentUser?.serviceCenterName?.ifBlank { "СЦ «ТехноМастер»" } ?: "СЦ «ТехноМастер»"
        val scChatId = "sc_team_chat_$scId"
        val scChatTitle = "Команда: $scName"
        val existingScChat = dbService.chatDao.getChatById(scChatId)
        if (existingScChat == null) {
            dbService.chatDao.insertChat(
                ChatEntity(
                    id = scChatId,
                    title = scChatTitle,
                    lastMessage = "Рабочий чат сотрудников $scName. Обсуждение ремонтов, запчастей и задач.",
                    lastMessageTime = System.currentTimeMillis() - 1000L,
                    isPinned = true
                )
            )
        } else if (!existingScChat.isPinned || existingScChat.title != scChatTitle) {
            dbService.chatDao.updateChat(
                existingScChat.copy(title = scChatTitle, isPinned = true)
            )
        }
    }

    var chatToDelete by remember { mutableStateOf<ChatEntity?>(null) }
    var selectedChatForMenu by remember { mutableStateOf<ChatEntity?>(null) }
    var showSupportDeskDialog by remember { mutableStateOf(false) }
    var showMasterSupportChatDialog by remember { mutableStateOf(false) }
    val themeManager = remember { ThemeManager.getInstance(context) }
    var showThemeSwitcherDialog by remember { mutableStateOf(false) }
    var showRepairAlgorithmDialog by remember { mutableStateOf(false) }

    // Telegram structural folder tabs
    var telegramSelectedFolder by remember { mutableIntStateOf(0) }
    val telegramFolders = listOf("Все", "Смартфоны", "Тестпоинты", "Боты")

    // VK structural filter chips
    var vkSelectedFilter by remember { mutableIntStateOf(0) }
    val vkFilters = listOf("Все", "Непрочитанные", "Техника", "Архив")

    // WhatsApp structural top tabs: 0: Camera, 1: CHATS, 2: STATUS, 3: CALLS
    var whatsAppSelectedTab by remember { mutableIntStateOf(1) }

    var fabPressed by remember { mutableStateOf(false) }
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.88f else 1f,
        label = "fabScale"
    )

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.topBarBackground)
                ) {
                    TopAppBar(
                    navigationIcon = {
                        when (tokens.variant) {
                            AppDesignVariant.TELEGRAM -> null
                            AppDesignVariant.VK -> {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 12.dp, end = 4.dp)
                                        .size(36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(VkBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("М", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // VK Online Dot on profile
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(if (isDark) VkDarkSurface else Color.White)
                                            .padding(1.5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color(0xFF4BB34B))
                                        )
                                    }
                                }
                            }
                            AppDesignVariant.WHATSAPP -> null
                        }
                    },
                    title = {
                        AnimatedContent(
                            targetState = isSearching,
                            label = "searchBarAnimation"
                        ) { searching ->
                            if (searching) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = filterDuplicateInput(searchQuery, it) },
                                    placeholder = {
                                        Text(
                                            "Поиск чатов...",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 16.sp
                                        )
                                    },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 16.sp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                when (tokens.variant) {
                                    AppDesignVariant.TELEGRAM -> {
                                        Text(
                                            text = "Tech.Mate",
                                            fontWeight = FontWeight.Bold,
                                            color = tokens.topBarContentColor,
                                            fontSize = 20.sp
                                        )
                                    }
                                    AppDesignVariant.VK -> {
                                        Text(
                                            text = "Чаты",
                                            fontWeight = FontWeight.Bold,
                                            color = tokens.topBarContentColor,
                                            fontSize = 20.sp
                                        )
                                    }
                                    AppDesignVariant.WHATSAPP -> {
                                        Text(
                                            text = "WhatsApp",
                                            fontWeight = FontWeight.Bold,
                                            color = tokens.topBarContentColor,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        // VK Create Chat Icon
                        if (tokens.variant == AppDesignVariant.VK) {
                            IconButton(onClick = {
                                scope.launch {
                                    val newChat = ChatEntity(
                                        title = "Диалог с мастером #${(100..999).random()}",
                                        lastMessage = "Чем могу помочь?",
                                        lastMessageTime = System.currentTimeMillis()
                                    )
                                    dbService.chatDao.insertChat(newChat)
                                    onOpenChat(newChat.id)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Создать беседу",
                                    tint = tokens.topBarContentColor
                                )
                            }
                        }

                        // WhatsApp Camera Icon
                        if (tokens.variant == AppDesignVariant.WHATSAPP) {
                            IconButton(onClick = { whatsAppSelectedTab = 0 }) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Камера",
                                    tint = tokens.topBarContentColor
                                )
                            }
                        }

                        // Theme Switcher Button
                        IconButton(onClick = { showThemeSwitcherDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Сменить оформление",
                                tint = tokens.topBarContentColor
                            )
                        }

                        IconButton(onClick = {
                            isSearching = !isSearching
                            if (!isSearching) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = tokens.topBarContentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = tokens.topBarBackground,
                        titleContentColor = tokens.topBarContentColor,
                        actionIconContentColor = tokens.topBarContentColor
                    )
                )

                // VARIANT SPECIFIC ARCHITECTURAL HEADER TABS
                when (tokens.variant) {
                    AppDesignVariant.TELEGRAM -> {
                        // Telegram Story Overlay with circular progress-indicated icons
                        StoryOverlay(
                            onOpenAlgorithm = { showRepairAlgorithmDialog = true }
                        )
                    }
                    AppDesignVariant.WHATSAPP -> {
                        // WhatsApp Top Navigation Bar: [ЧАТЫ] [БАЗА ЗНАНИЙ] [НАСТРОЙКИ] [ЗВОНКИ]
                        TabRow(
                            selectedTabIndex = when (whatsAppSelectedTab) {
                                1 -> 0
                                3 -> 3
                                else -> 0
                            },
                            containerColor = tokens.topBarBackground,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                val activeIdx = when (whatsAppSelectedTab) {
                                    1 -> 0
                                    3 -> 3
                                    else -> 0
                                }
                                if (activeIdx < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[activeIdx]),
                                        color = WhatsAppLightGreenAccent,
                                        height = 3.5.dp
                                    )
                                }
                            },
                            divider = {}
                        ) {
                            // 0: CHATS Tab
                            Tab(
                                selected = whatsAppSelectedTab == 1,
                                onClick = { whatsAppSelectedTab = 1 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "ЧАТЫ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (whatsAppSelectedTab == 1) WhatsAppLightGreenAccent else Color.White.copy(alpha = 0.8f)
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = WhatsAppLightGreenAccent
                                        ) {
                                            Text(
                                                text = chats.size.coerceAtLeast(1).toString(),
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
                                selected = false,
                                onClick = onNavigateToKnowledge,
                                text = {
                                    Text(
                                        text = "БАЗА ЗНАНИЙ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            )
                            // 2: SETTINGS Tab
                            Tab(
                                selected = false,
                                onClick = onNavigateToProfile,
                                text = {
                                    Text(
                                        text = "НАСТРОЙКИ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            )
                            // 3: CALLS Tab
                            Tab(
                                selected = whatsAppSelectedTab == 3,
                                onClick = { whatsAppSelectedTab = 3 },
                                text = {
                                    Text(
                                        text = "ЗВОНКИ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (whatsAppSelectedTab == 3) WhatsAppLightGreenAccent else Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            )
                        }
                    }
                    AppDesignVariant.VK -> {
                        // VK uses in-feed Stories & Filter Pills
                    }
                }
            }
        }
    },
    floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    fabPressed = true
                    scope.launch {
                        val newChat = ChatEntity(
                            title = "Чат с ИИ #${(100..999).random()}",
                            lastMessage = "Чем могу помочь в ремонте?",
                            lastMessageTime = System.currentTimeMillis()
                        )
                        dbService.chatDao.insertChat(newChat)
                        fabPressed = false
                        onOpenChat(newChat.id)
                    }
                },
                containerColor = tokens.fabColor,
                contentColor = if (tokens.isOled) Color.Black else Color.White,
                shape = tokens.actionButtonShape,
                modifier = Modifier
                    .size(56.dp)
                    .scale(fabScale)
            ) {
                val fabIcon = when (tokens.variant) {
                    AppDesignVariant.TELEGRAM -> Icons.Default.Edit
                    AppDesignVariant.VK -> Icons.Default.Edit
                    AppDesignVariant.WHATSAPP -> Icons.Default.Chat
                }
                Icon(
                    imageVector = fabIcon,
                    contentDescription = "New AI Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        containerColor = tokens.chatBackground
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WHATSAPP STATUS VIEW
            if (tokens.variant == AppDesignVariant.WHATSAPP && whatsAppSelectedTab == 2) {
                WhatsAppStatusView(onOpenChat = onOpenChat)
            }
            // WHATSAPP CALLS VIEW
            else if (tokens.variant == AppDesignVariant.WHATSAPP && whatsAppSelectedTab == 3) {
                WhatsAppCallsView(onOpenChat = onOpenChat)
            }
            // WHATSAPP CAMERA TAB VIEW
            else if (tokens.variant == AppDesignVariant.WHATSAPP && whatsAppSelectedTab == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Камера микроскопа и диагностика плат", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { whatsAppSelectedTab = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent)
                        ) {
                            Text("Вернуться к чатам", color = Color.White)
                        }
                    }
                }
            }
            // STANDARD MESSENGER CHAT LIST
            else if (chats.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (tokens.variant == AppDesignVariant.VK || (tokens.variant == AppDesignVariant.WHATSAPP && whatsAppSelectedTab == 1)) {
                        StoryOverlay(
                            onOpenAlgorithm = { showRepairAlgorithmDialog = true }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = tokens.primaryAccent.copy(alpha = 0.15f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = tokens.primaryAccent,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Чатов нет",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Нажмите кнопку справа, чтобы создать новый чат",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // VK Stories Carousel & Filter Chips
                    if (tokens.variant == AppDesignVariant.VK) {
                        item {
                            StoryOverlay(
                                onOpenAlgorithm = { showRepairAlgorithmDialog = true }
                            )
                        }
                        item {
                            VkFilterChipsBar(
                                selectedFilter = vkSelectedFilter,
                                filters = vkFilters,
                                onSelectFilter = { vkSelectedFilter = it }
                            )
                        }
                    }

                    // WhatsApp Stories Bar
                    if (tokens.variant == AppDesignVariant.WHATSAPP && (whatsAppSelectedTab == 1 || hideTopBar)) {
                        item {
                            StoryOverlay(
                                onOpenAlgorithm = { showRepairAlgorithmDialog = true }
                            )
                        }
                    }

                    // Pinned Chats Section
                    if (pinnedChats.isNotEmpty()) {
                        item {
                            Surface(
                                color = if (isDark) Color(0xFF181818) else Color(0xFFF2F2F7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ЗАКРЕПЛЁННЫЕ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else tokens.primaryAccent,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        items(pinnedChats, key = { it.id }) { chat ->
                            MessengerChatItem(
                                variant = tokens.variant,
                                chat = chat,
                                isPinned = true,
                                onClick = {
                                    val isSupportChat = chat.id == "support_official_channel" || chat.title == "Поддержка"
                                    if (isSupportChat) {
                                        val isGod = authService.isGodMode() || AuthService.isGodEmail(currentUser?.email)
                                        if (isGod) {
                                            showSupportDeskDialog = true
                                        } else {
                                            showMasterSupportChatDialog = true
                                        }
                                    } else {
                                        onOpenChat(chat.id)
                                    }
                                },
                                onLongClick = { selectedChatForMenu = chat }
                            )
                        }
                    }

                    // Regular Chats Section
                    if (unpinnedChats.isNotEmpty()) {
                        if (pinnedChats.isNotEmpty()) {
                            item {
                                Surface(
                                    color = if (isDark) Color(0xFF181818) else Color(0xFFF2F2F7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "ВСЕ ЧАТЫ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        items(unpinnedChats, key = { it.id }) { chat ->
                            MessengerChatItem(
                                variant = tokens.variant,
                                chat = chat,
                                isPinned = false,
                                onClick = {
                                    val isSupportChat = chat.id == "support_official_channel" || chat.title == "Поддержка"
                                    if (isSupportChat) {
                                        val isGod = authService.isGodMode() || AuthService.isGodEmail(currentUser?.email)
                                        if (isGod) {
                                            showSupportDeskDialog = true
                                        } else {
                                            showMasterSupportChatDialog = true
                                        }
                                    } else {
                                        onOpenChat(chat.id)
                                    }
                                },
                                onLongClick = { selectedChatForMenu = chat }
                            )
                        }
                    } else if (pinnedChats.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Чатов нет",
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Long Press Menu Modal / Dropdown Dialog
        if (selectedChatForMenu != null) {
            val chat = selectedChatForMenu!!
            AlertDialog(
                onDismissRequest = { selectedChatForMenu = null },
                title = { Text(chat.title, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    dbService.chatDao.updateChat(chat.copy(isPinned = !chat.isPinned))
                                    selectedChatForMenu = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (chat.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = tokens.primaryAccent
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (chat.isPinned) "Открепить" else "Закрепить",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                chatToDelete = chat
                                selectedChatForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Удалить",
                                    color = Color.Red
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedChatForMenu = null }) {
                        Text("Отмена", color = tokens.primaryAccent)
                    }
                }
            )
        }

        // Theme / Design Switcher Dialog
        if (showThemeSwitcherDialog) {
            ThemeSwitcherDialog(
                themeManager = themeManager,
                onDismiss = { showThemeSwitcherDialog = false }
            )
        }

        // Support Desk Dialog for Head Admin
        if (showSupportDeskDialog) {
            GodSupportDeskDialog(
                currentUser = currentUser,
                onDismiss = { showSupportDeskDialog = false }
            )
        }

        // Support Chat Dialog for Master
        if (showMasterSupportChatDialog) {
            SupportChatDialog(
                currentUser = currentUser,
                onDismiss = { showMasterSupportChatDialog = false }
            )
        }

        // 12 Steps Repair Algorithm Dialog
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

        // Confirmation Delete Dialog
        if (chatToDelete != null) {
            AlertDialog(
                onDismissRequest = { chatToDelete = null },
                title = { Text("Удалить чат?") },
                text = { Text("Вы уверены, что хотите удалить этот чат и всю историю сообщений?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = chatToDelete!!.id
                            scope.launch {
                                dbService.messageDao.deleteMessagesForChat(id)
                                dbService.chatDao.deleteChat(id)
                                chatToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Удалить", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chatToDelete = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
fun MessengerChatItem(
    variant: AppDesignVariant,
    chat: ChatEntity,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    when (variant) {
        AppDesignVariant.TELEGRAM -> TelegramChatItemRow(chat, isPinned, onClick, onLongClick)
        AppDesignVariant.VK -> VkChatItemRow(chat, isPinned, onClick, onLongClick)
        AppDesignVariant.WHATSAPP -> WhatsAppChatItemRow(chat, isPinned, onClick, onLongClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VkChatItemRow(
    chat: ChatEntity,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = formatter.format(Date(chat.lastMessageTime))
    val firstLetter = chat.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
    val avatarBg = VkBlue

    val rowBg = when {
        isPinned -> VkBlue.copy(alpha = if (isDark) 0.15f else 0.06f)
        isDark -> VkDarkSurface
        else -> Color.White
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = rowBg
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 48dp Avatar with VK online badge
                Box(modifier = Modifier.size(48.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firstLetter,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    // VK Green Online Dot in bottom right
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(if (isDark) VkDarkSurface else Color.White)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF4BB34B))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = chat.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // VK Verified Badge
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = VkBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeStr,
                            fontSize = 12.sp,
                            color = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chat.lastMessage,
                            fontSize = 14.sp,
                            color = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = if (isDark) Color(0xFF2C2D2E) else Color(0xFFE1E3E6)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WhatsAppChatItemRow(
    chat: ChatEntity,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = formatter.format(Date(chat.lastMessageTime))
    val firstLetter = chat.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
    val avatarBg = WhatsAppTeal

    val rowBg = when {
        isPinned -> WhatsAppLightGreenAccent.copy(alpha = if (isDark) 0.12f else 0.08f)
        isDark -> WhatsAppDarkSurface
        else -> Color.White
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = rowBg
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 11.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 50dp Circular Avatar
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = firstLetter,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chat.title,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WhatsAppLightGreenAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "✓✓ ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF53BDEB)
                            )
                            Text(
                                text = chat.lastMessage,
                                fontSize = 14.sp,
                                color = if (isDark) WhatsAppTextSecondaryDark else WhatsAppTextSecondaryLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // WhatsApp Green Unread Badge
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(WhatsAppLightGreenAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                thickness = 0.5.dp,
                color = if (isDark) Color(0xFF202C33) else Color(0xFFE2E8F0)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramChatItemRow(
    chat: ChatEntity,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = formatter.format(Date(chat.lastMessageTime))

    val firstLetter = chat.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T"
    val avatarColorIndex = (chat.title.hashCode() and 0x7FFFFFFF) % avatarColors.size
    val avatarBg = avatarColors[avatarColorIndex]

    val rowBg = when {
        isPinned -> TelegramHeader.copy(alpha = if (isDark) 0.15f else 0.08f)
        isDark -> TelegramChatBgDark
        else -> Color.White
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = rowBg
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 48dp Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = firstLetter,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chat.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeStr,
                            fontSize = 12.sp,
                            color = TelegramTimestamp
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = TelegramBlue,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = chat.lastMessage,
                            fontSize = 14.sp,
                            color = TelegramTimestamp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Divider 1px inset from left 76dp
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = TelegramDivider.copy(alpha = if (isDark) 0.2f else 0.8f)
            )
        }
    }
}

// ---------------------------------------------------------------------------------
// VK SPECIFIC STRUCTURAL COMPONENTS: STORIES CAROUSEL & FILTER PILLS
// ---------------------------------------------------------------------------------

@Composable
fun VkStoriesCarousel(
    onOpenChat: (String) -> Unit,
    onOpenAlgorithm: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val stories = listOf(
        Triple("➕", "История", {}),
        Triple("🤖", "ИИ-Мастер", { onOpenAlgorithm() }),
        Triple("⚡", "TestPoint", { onOpenAlgorithm() }),
        Triple("🔬", "Микроскоп", {}),
        Triple("📋", "Алгоритм", { onOpenAlgorithm() }),
        Triple("💡", "Советы", {})
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) VkDarkSurface else Color.White)
            .padding(vertical = 10.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(stories.size) { index ->
                val (emoji, label, action) = stories[index]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { action() }
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier.size(58.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // VK Authentic Gradient Ring
                        val ringBrush = if (index == 0) {
                            Brush.linearGradient(listOf(Color(0xFF818C99), Color(0xFF818C99)))
                        } else {
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF334B),
                                    Color(0xFFE02476),
                                    Color(0xFF7044FF),
                                    Color(0xFF0077FF),
                                    Color(0xFFFF334B)
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(ringBrush)
                                .padding(2.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }

                        // Add badge for story 0
                        if (index == 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(VkBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFE1E3E6) else Color(0xFF2C2D2E),
                        maxLines = 1
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = if (isDark) Color(0xFF2C2D2E) else Color(0xFFE1E3E6),
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
fun VkFilterChipsBar(
    selectedFilter: Int,
    filters: List<String>,
    onSelectFilter: (Int) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) VkDarkSurface else Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters.size) { index ->
            val isSelected = selectedFilter == index
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isSelected -> VkBlue
                    isDark -> Color(0xFF2C2D2E)
                    else -> Color(0xFFF2F3F5)
                },
                modifier = Modifier.clickable { onSelectFilter(index) }
            ) {
                Text(
                    text = filters[index],
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> Color.White
                        isDark -> Color(0xFFE1E3E6)
                        else -> Color(0xFF6D7885)
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// WHATSAPP SPECIFIC STRUCTURAL PAGES: STATUS & CALLS VIEWS
// ---------------------------------------------------------------------------------

@Composable
fun WhatsAppStatusView(onOpenChat: (String) -> Unit) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) WhatsAppDarkBg else Color(0xFFF0F2F5))
    ) {
        // My Status Row
        item {
            Surface(
                color = if (isDark) WhatsAppDarkSurface else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        android.widget.Toast.makeText(context, "Статус: Нажмите камеру для фото платы", android.widget.Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(52.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(WhatsAppTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 24.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(WhatsAppLightGreenAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Мой статус",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Нажмите, чтобы добавить статус ремонта",
                            fontSize = 13.5.sp,
                            color = WhatsAppTextSecondaryLight
                        )
                    }
                }
            }
        }

        // Section: Recent Updates
        item {
            Text(
                text = "НЕДАВНИЕ ОБНОВЛЕНИЯ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) WhatsAppLightGreenAccent else WhatsAppTeal,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        val updates = listOf(
            Triple("Мастер Алексей", "15 минут назад", "iPhone 13 Pro: Замена КП успешно выполнена"),
            Triple("ИИ-Лаборатория Tech.Mate", "45 минут назад", "Обновлена база TestPoint для Xiaomi BROM"),
            Triple("Сервис Пайки & Микроскоп", "Сегодня 11:20", "Локализован пробой по шине VBUS (4.98V)")
        )

        items(updates.size) { index ->
            val (name, time, caption) = updates[index]
            Surface(
                color = if (isDark) WhatsAppDarkSurface else Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // WhatsApp Green Status Ring
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(WhatsAppLightGreenAccent)
                                .padding(2.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (index) {
                                        0 -> "🛠️"
                                        1 -> "🤖"
                                        else -> "🔬"
                                    },
                                    fontSize = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$time • $caption",
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF8696A0) else Color(0xFF667781),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 82.dp),
                        thickness = 0.5.dp,
                        color = if (isDark) Color(0xFF202C33) else Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
fun WhatsAppCallsView(onOpenChat: (String) -> Unit) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) WhatsAppDarkBg else Color(0xFFF0F2F5))
    ) {
        // Create Call Link Row
        item {
            Surface(
                color = if (isDark) WhatsAppDarkSurface else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        android.widget.Toast.makeText(context, "Ссылка на видеоконсультацию скопирована", android.widget.Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(WhatsAppLightGreenAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Создать ссылку на звонок",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Поделитесь ссылкой для видеодиагностики платы",
                            fontSize = 13.sp,
                            color = WhatsAppTextSecondaryLight
                        )
                    }
                }
            }
        }

        // Recent Calls Header
        item {
            Text(
                text = "НЕДАВНИЕ ЗВОНКИ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) WhatsAppLightGreenAccent else WhatsAppTeal,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        val calls = listOf(
            Triple("Мастер ИИ Tech.Mate", "Входящий • Сегодня 14:30", true),
            Triple("Аудиодиагностика цепи VBUS", "Исходящий (4 мин) • Вчера 19:15", false),
            Triple("Дежурный инженер поддержки", "Пропущенный • 2 сентября 11:00", true)
        )

        items(calls.size) { index ->
            val (name, info, isIncoming) = calls[index]
            Surface(
                color = if (isDark) WhatsAppDarkSurface else Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WhatsAppTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (index) {
                                    0 -> "🤖"
                                    1 -> "⚡"
                                    else -> "🛠️"
                                },
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isIncoming) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                                    contentDescription = null,
                                    tint = if (index == 2) Color.Red else Color(0xFF25D366),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = info,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF8696A0) else Color(0xFF667781)
                                )
                            }
                        }

                        IconButton(onClick = {
                            android.widget.Toast.makeText(context, "Вызов мастера $name...", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Позвонить",
                                tint = WhatsAppLightGreenAccent
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 78.dp),
                        thickness = 0.5.dp,
                        color = if (isDark) Color(0xFF202C33) else Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}
