package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActivityLogEntity
import com.example.model.InvitationEntity
import com.example.model.PasswordResetRequestEntity
import com.example.model.ServiceCenterEntity
import com.example.model.SupportMessageEntity
import com.example.model.SupportTicketEntity
import com.example.model.UserEntity
import com.example.services.AppDesignVariant
import com.example.services.AuthService
import com.example.services.DatabaseService
import com.example.services.LanguageService
import com.example.services.ThemeManager
import com.example.ui.theme.*
import com.example.ui.util.filterDuplicateInput
import com.example.ui.widgets.GoogleServicesSyncDialog
import com.example.ui.widgets.ThemeSwitcherDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onNavigateAdmin: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val authService = remember { AuthService.getInstance(context) }
    val dbService = remember { DatabaseService.getInstance(context) }
    val themeManager = remember { ThemeManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val currentUser by authService.currentUser.collectAsState()
    val isGodMode = authService.isGodMode()
    val isServiceAdmin = authService.isServiceAdmin()
    val currentDesignVariant by themeManager.designVariant.collectAsState()

    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    var totalRequests by remember { mutableIntStateOf(0) }
    var solvedIssues by remember { mutableIntStateOf(0) }
    var knowledgeEntriesCount by remember { mutableIntStateOf(0) }

    var showSearchApiDialog by remember { mutableStateOf(false) }
    var showGoogleServicesDialog by remember { mutableStateOf(false) }
    var showDesignSwitcherDialog by remember { mutableStateOf(false) }
    var showAiVoiceDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showRedeemInviteDialog by remember { mutableStateOf(false) }

    val searchService = remember { com.example.services.GoogleCustomSearchService.getInstance(context) }
    var customSearchApiKey by remember { mutableStateOf(searchService.getApiKey()) }
    var customSearchCx by remember { mutableStateOf(searchService.getSearchEngineId()) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            editedName = currentUser!!.name
            totalRequests = dbService.messageDao.getTotalMessagesCount()
            solvedIssues = dbService.messageDao.getSolvedProblemsCount()
            knowledgeEntriesCount = dbService.knowledgeDao.getKnowledgeCount()
        }
    }

    val currentLangName = LanguageService.getString("lang_name")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TelegramHeader
                )
            )
        },
        containerColor = if (isDarkTheme) TelegramDarkBg else TechBackgroundLight
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Master Profile Header Card
            item {
                Surface(
                    color = if (isDarkTheme) TelegramDarkSurface else Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Master Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (isGodMode) TechGoldTestPoint else TelegramBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser?.name?.firstOrNull() ?: 'М').uppercase(),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isEditingName) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editedName,
                                    onValueChange = { editedName = filterDuplicateInput(editedName, it) },
                                    singleLine = true,
                                    modifier = Modifier.width(220.dp)
                                )
                                IconButton(onClick = {
                                    scope.launch {
                                        authService.updateCurrentUserName(editedName)
                                        isEditingName = false
                                    }
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = TechPrimaryBlue)
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentUser?.name ?: "Мастер по ремонту",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = { isEditingName = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TelegramTimestamp, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(
                            text = if (isGodMode) "👑 Главный Администратор" else "Инженер-электронщик • ${currentUser?.role ?: "master"}",
                            fontSize = 13.sp,
                            color = if (isGodMode) TechGoldTestPoint else TechPrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                        if (!currentUser?.email.isNullOrBlank()) {
                            Text(
                                text = currentUser!!.email!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Stats Cards
            item {
                Surface(
                    color = if (isDarkTheme) TelegramDarkSurface else Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(number = "$totalRequests", label = LanguageService.getString("stats_requests"))
                        StatItem(number = "$solvedIssues", label = LanguageService.getString("stats_solved"))
                        StatItem(number = "$knowledgeEntriesCount", label = LanguageService.getString("stats_added_kb"))
                    }
                }
            }

            // Settings Group 1: Appearance & AI Voice / Persona
            item {
                TelegramSettingsGroup(isDark = isDarkTheme) {
                    TelegramSettingsRow(
                        icon = Icons.Default.Palette,
                        iconBg = Color(0xFFE91E63),
                        title = "Стиль оформления (Дизайн)",
                        subtitle = currentDesignVariant.titleRu,
                        onClick = { showDesignSwitcherDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = TelegramDivider.copy(alpha = 0.2f))
                    TelegramSettingsRow(
                        icon = Icons.Default.DarkMode,
                        iconBg = Color(0xFF5C6BC0),
                        title = "Тёмная тема",
                        subtitle = if (isDarkTheme) "Включена" else "Выключена",
                        trailing = {
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onToggleTheme
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = TelegramDivider.copy(alpha = 0.2f))
                    TelegramSettingsRow(
                        icon = Icons.Default.RecordVoiceOver,
                        iconBg = Color(0xFF26A69A),
                        title = "Голос и имя ИИ-ассистента",
                        subtitle = "Имя: ${currentUser?.customAiName ?: "TechMate"} • Голос: ${if (currentUser?.aiVoiceGender == "FEMALE") "Женский" else "Мужской"}",
                        onClick = { showAiVoiceDialog = true }
                    )
                }
            }

            // Settings Group 2: Support, Invitations & Search
            item {
                TelegramSettingsGroup(isDark = isDarkTheme) {
                    TelegramSettingsRow(
                        icon = Icons.Default.HeadsetMic,
                        iconBg = Color(0xFF9C27B0),
                        title = "Поддержка",
                        subtitle = "Связь с разработчиком (nosovsergej886@gmail.com)",
                        onClick = { showSupportDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = TelegramDivider.copy(alpha = 0.2f))
                    TelegramSettingsRow(
                        icon = Icons.Default.VpnKey,
                        iconBg = TechGoldTestPoint,
                        title = "Активировать инвайт-код",
                        subtitle = "Повышение статуса до Администратора СЦ или Мастера",
                        onClick = { showRedeemInviteDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = TelegramDivider.copy(alpha = 0.2f))
                    TelegramSettingsRow(
                        icon = Icons.Default.Language,
                        iconBg = Color(0xFF29B6F6),
                        title = "Язык приложения",
                        subtitle = currentLangName,
                        onClick = { LanguageService.toggleLanguage() }
                    )
                    if (isGodMode) {
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = TelegramDivider.copy(alpha = 0.2f))
                        TelegramSettingsRow(
                            icon = Icons.Default.Key,
                            iconBg = Color(0xFF4CAF50),
                            title = "Google Custom Search API (Главный Админ)",
                            subtitle = if (customSearchApiKey.isNotBlank()) "API Ключ настроен (активен)" else "По умолчанию (встроенный поиск)",
                            onClick = {
                                customSearchApiKey = searchService.getApiKey()
                                customSearchCx = searchService.getSearchEngineId()
                                showSearchApiDialog = true
                            }
                        )
                    }
                }
            }

            // Settings Group 3: Google Services & Cloud Sync
            item {
                TelegramSettingsGroup(isDark = isDarkTheme) {
                    TelegramSettingsRow(
                        icon = Icons.Default.CloudSync,
                        iconBg = Color(0xFF4285F4),
                        title = "Службы и сервисы Google",
                        subtitle = "Синхронизация Firestore Spark (бесплатно) и бэкап на Google Диск",
                        onClick = { showGoogleServicesDialog = true }
                    )
                }
            }

            // Admin panel if admin / god mode / service admin
            val isAdminUser = isGodMode || isServiceAdmin || currentUser?.role in listOf("admin", "god", "service_admin")
            if (isAdminUser) {
                item {
                    TelegramSettingsGroup(isDark = isDarkTheme) {
                        TelegramSettingsRow(
                            icon = Icons.Default.AdminPanelSettings,
                            iconBg = if (isGodMode) TechGoldTestPoint else Color(0xFFD32F2F),
                            title = if (isGodMode) "👑 Панель Главного Администратора" else "🏢 Управление Сервисным Центром",
                            subtitle = "Инвайты, блокировка мастеров, тикеты поддержки",
                            onClick = onNavigateAdmin
                        )
                    }
                }
            }

            // Logout
            item {
                Surface(
                    color = if (isDarkTheme) TelegramDarkSurface else Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            authService.logout()
                            onLogout()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = LanguageService.getString("logout"),
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Footer Version
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TechMate Pro v3.0 • Инженерная платформа",
                        fontSize = 12.sp,
                        color = TelegramTimestamp
                    )
                }
            }
        }

        // Google Services Sync Dialog
        if (showGoogleServicesDialog) {
            GoogleServicesSyncDialog(
                onDismiss = { showGoogleServicesDialog = false },
                onOpenSearchConfig = {
                    customSearchApiKey = searchService.getApiKey()
                    customSearchCx = searchService.getSearchEngineId()
                    showSearchApiDialog = true
                }
            )
        }

        // Theme Switcher Dialog
        if (showDesignSwitcherDialog) {
            ThemeSwitcherDialog(
                themeManager = themeManager,
                onDismiss = { showDesignSwitcherDialog = false }
            )
        }

        // AI Voice and Persona Configuration Dialog
        if (showAiVoiceDialog) {
            AiVoiceSettingsDialog(
                currentName = currentUser?.customAiName ?: "TechMate",
                currentGender = currentUser?.aiVoiceGender ?: "FEMALE",
                isGodMode = isGodMode,
                onDismiss = { showAiVoiceDialog = false },
                onSave = { name, gender ->
                    scope.launch {
                        authService.updateAiPreferences(name, gender)
                        showAiVoiceDialog = false
                        Toast.makeText(context, "Настройки ИИ сохранены: $name ($gender)", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Support Chat Dialog
        if (showSupportDialog) {
            SupportChatDialog(
                currentUser = currentUser,
                onDismiss = { showSupportDialog = false }
            )
        }

        // Redeem Invite Code Dialog
        if (showRedeemInviteDialog) {
            RedeemInviteDialog(
                onDismiss = { showRedeemInviteDialog = false },
                onRedeem = { inviteCode ->
                    scope.launch {
                        val (success, message) = authService.promoteExistingUserWithInvite(inviteCode)
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        if (success) {
                            showRedeemInviteDialog = false
                        }
                    }
                }
            )
        }

        // Google Search API Configuration Dialog
        if (showSearchApiDialog) {
            AlertDialog(
                onDismissRequest = { showSearchApiDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TechPrimaryBlue)
                        Text("Google Search API")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Настройте Google Custom Search JSON API для получения реальных фотографий тестпоинтов и схем со всего интернета (GSMForum, 4PDA, UnlockBoot).",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = customSearchApiKey,
                            onValueChange = { customSearchApiKey = it },
                            label = { Text("Google Cloud API Key") },
                            placeholder = { Text("AIzaSy...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customSearchCx,
                            onValueChange = { customSearchCx = it },
                            label = { Text("Search Engine ID (CX)") },
                            placeholder = { Text(com.example.services.GoogleCustomSearchService.DEFAULT_CX) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            searchService.saveConfig(customSearchApiKey, customSearchCx)
                            showSearchApiDialog = false
                            Toast.makeText(context, "Настройки Google Search API сохранены!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue)
                    ) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSearchApiDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
fun RedeemInviteDialog(
    onDismiss: () -> Unit,
    onRedeem: (String) -> Unit
) {
    var inviteCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = TechGoldTestPoint)
                Text("Активация инвайт-кода", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Введите одноразовый секретный инвайт-код от Главного Администратора или вашего СЦ для моментального повышения роли в Firestore.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inviteCodeInput,
                    onValueChange = { inviteCodeInput = filterDuplicateInput(inviteCodeInput, it).uppercase() },
                    label = { Text("Инвайт-код (например: GOD-INV-7A39)") },
                    placeholder = { Text("GOD-INV-XXXX или TM-INV-XXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inviteCodeInput.isNotBlank()) {
                        isLoading = true
                        onRedeem(inviteCodeInput.trim())
                    }
                },
                enabled = inviteCodeInput.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = TechGoldTestPoint)
            ) {
                Text(if (isLoading) "Проверка..." else "Активировать", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AiVoiceSettingsDialog(
    currentName: String,
    currentGender: String,
    isGodMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, gender: String) -> Unit
) {
    val context = LocalContext.current
    val openAiVoiceService = remember { com.example.services.OpenAiVoiceService.getInstance(context) }

    var aiName by remember { mutableStateOf(currentName) }
    var selectedEngine by remember { mutableStateOf(openAiVoiceService.engineType.value) }
    var selectedVoiceId by remember { mutableStateOf(openAiVoiceService.selectedVoice.value) }
    var speed by remember { mutableFloatStateOf(openAiVoiceService.ttsSpeed.value) }
    var endpoint by remember { mutableStateOf(openAiVoiceService.apiEndpoint.value) }
    var customApiKey by remember { mutableStateOf(openAiVoiceService.customApiKey.value) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val isSpeaking by openAiVoiceService.isSpeaking.collectAsState()

    AlertDialog(
        onDismissRequest = {
            openAiVoiceService.stop()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = TelegramPrimary)
                Text("🎙️ OpenAI Голос & Whisper", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            val dialogScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(dialogScrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Пакет OpenAI Whisper (STT) + OpenAI TTS для живого и естественного голоса ассистента.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = aiName,
                    onValueChange = { aiName = filterDuplicateInput(aiName, it) },
                    label = { Text("Имя ассистента") },
                    placeholder = { Text("TechMate, Джарвис, Алиса...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Voice Engine Selector
                Text("Движок синтеза и распознавания:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedEngine == com.example.services.VoiceEngineType.OPENAI_WHISPER_TTS,
                        onClick = { selectedEngine = com.example.services.VoiceEngineType.OPENAI_WHISPER_TTS },
                        label = { Text("✨ OpenAI Live") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedEngine == com.example.services.VoiceEngineType.ANDROID_NATIVE,
                        onClick = { selectedEngine = com.example.services.VoiceEngineType.ANDROID_NATIVE },
                        label = { Text("📱 Локальный TTS") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // OpenAI Voices List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Выбор живого голоса OpenAI (${openAiVoiceService.openAiVoices.size} вариантов):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    openAiVoiceService.openAiVoices.forEach { voice ->
                        val isSelected = selectedVoiceId == voice.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVoiceId = voice.id
                                    openAiVoiceService.playSampleVoice(voice.id)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) TelegramPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TelegramPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = voice.nameRu,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) TelegramPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = voice.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AssistChip(
                                    onClick = {
                                        selectedVoiceId = voice.id
                                        openAiVoiceService.playSampleVoice(voice.id)
                                    },
                                    label = { Text("▶ Тест", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Speed slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Скорость воспроизведения:", fontSize = 12.sp)
                        Text(String.format(java.util.Locale.US, "%.2fx", speed), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TelegramPrimary)
                    }
                    Slider(
                        value = speed,
                        onValueChange = { speed = it },
                        valueRange = 0.75f..1.35f,
                        steps = 6
                    )
                }

                // Advanced endpoint toggle
                TextButton(
                    onClick = { showAdvancedSettings = !showAdvancedSettings },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (showAdvancedSettings) "▲ Скрыть кастомный endpoint" else "▼ Настроить кастомный endpoint / ключ",
                        fontSize = 11.sp,
                        color = TelegramPrimary
                    )
                }

                if (showAdvancedSettings) {
                    if (isGodMode) {
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            label = { Text("Endpoint (по умолчанию aitunnel / openai)") },
                            placeholder = { Text("https://api.aitunnel.ru/v1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { customApiKey = it },
                            label = { Text("Кастомный API Key (опционально)") },
                            placeholder = { Text("Оставьте пустым для встроенного ключа") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Изменение API ключей доступно только Главному Администратору (nosovsergej886@gmail.com).",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Live Audio Test Button
                Button(
                    onClick = {
                        if (isSpeaking) {
                            openAiVoiceService.stop()
                        } else {
                            openAiVoiceService.playSampleVoice(selectedVoiceId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeaking) Color.Red else TelegramPrimary.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSpeaking) "Остановить" else "▶ Прослушать образец (${selectedVoiceId.replaceFirstChar { it.uppercase() }})", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openAiVoiceService.stop()
                    if (isGodMode) {
                        openAiVoiceService.updateSettings(
                            engine = selectedEngine,
                            endpoint = endpoint,
                            apiKey = customApiKey,
                            voice = selectedVoiceId,
                            speed = speed
                        )
                    } else {
                        openAiVoiceService.updateSettings(
                            engine = selectedEngine,
                            voice = selectedVoiceId,
                            speed = speed
                        )
                    }
                    val gender = if (selectedVoiceId in listOf("echo", "onyx", "fable")) "MALE" else "FEMALE"
                    onSave(aiName.trim().ifBlank { "TechMate" }, gender)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TelegramPrimary)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                openAiVoiceService.stop()
                onDismiss()
            }) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun SupportChatDialog(
    currentUser: UserEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dbService = remember { DatabaseService.getInstance(context) }
    val syncService = remember { com.example.services.FirebaseSyncService.getInstance(context) }
    val scope = rememberCoroutineScope()

    var activeTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var supportText by remember { mutableStateOf("") }

    val ticketsFlow: Flow<List<SupportTicketEntity>> = remember(currentUser) {
        if (currentUser != null) {
            dbService.supportTicketDao.getTicketsForUser(currentUser.id)
        } else {
            flowOf(emptyList())
        }
    }
    val userTickets by ticketsFlow.collectAsState(initial = emptyList())

    LaunchedEffect(userTickets) {
        if (activeTicket == null && userTickets.isNotEmpty()) {
            activeTicket = userTickets.first()
        }
    }

    val messagesFlow: Flow<List<SupportMessageEntity>> = remember(activeTicket) {
        if (activeTicket != null) {
            dbService.supportMessageDao.getMessagesForTicket(activeTicket!!.id)
        } else {
            flowOf(emptyList())
        }
    }
    val ticketMessages by messagesFlow.collectAsState(initial = emptyList())

    val quickTemplates = listOf(
        "Нужна помощь по линии питания VPH_PWR",
        "Запрос схемы / тестпоинта",
        "Активация прав сервисного центра",
        "Вопрос главному администратору"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = TechGoldTestPoint)
                        Text("Поддержка", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Служба поддержки: Сергей Носов (nosovsergej886@gmail.com)",
                        fontSize = 11.sp,
                        color = TechGoldTestPoint
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Status Bar
                Surface(
                    color = TechGoldTestPoint.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(shape = CircleShape, color = TechSuccessGreen, modifier = Modifier.size(8.dp)) {}
                        Text(
                            text = "Канал маршрутизируется исключительно аккаунту Главного Администратора",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (activeTicket == null && userTickets.isEmpty() && ticketMessages.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = TechGoldTestPoint, modifier = Modifier.size(48.dp))
                            Text(
                                text = "Напишите ваш технический вопрос или запрос на расширение доступа. Главный администратор получит его в реальном времени через Firestore.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Quick Templates Chips
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                quickTemplates.forEach { template ->
                                    AssistChip(
                                        onClick = { supportText = template },
                                        label = { Text(template, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ticketMessages, key = { it.id }) { msg ->
                            val isMe = msg.senderId == currentUser?.id
                            val isGodSender = msg.senderRole == "god" || msg.senderEmail == "nosovsergej886@gmail.com"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isMe -> TelegramPrimary
                                        isGodSender -> Color(0xFF2C2411)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = if (isGodSender) androidx.compose.foundation.BorderStroke(1.dp, TechGoldTestPoint.copy(alpha = 0.6f)) else null,
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (isMe) "Вы" else "👑 Сергей Носов (Главный Админ)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMe) Color.White.copy(alpha = 0.8f) else TechGoldTestPoint
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = if (isMe || isGodSender) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = supportText,
                            onValueChange = { supportText = filterDuplicateInput(supportText, it) },
                            placeholder = { Text("Сообщение Главному Администратору...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(
                            onClick = {
                                if (supportText.isBlank() || currentUser == null) return@IconButton
                                val textToSend = supportText.trim()
                                supportText = ""
                                scope.launch {
                                    var currentTicket = activeTicket
                                    if (currentTicket == null) {
                                        val newTicketId = "ticket_${System.currentTimeMillis()}"
                                        val newTicket = SupportTicketEntity(
                                            id = newTicketId,
                                            userId = currentUser.id,
                                            userName = currentUser.name,
                                            userEmail = currentUser.email.ifBlank { currentUser.username },
                                            serviceCenterName = currentUser.serviceCenterName ?: "",
                                            subject = "Запрос от ${currentUser.name}",
                                            status = "open",
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        dbService.supportTicketDao.insertTicket(newTicket)
                                        syncService.syncSupportTicketToFirestore(newTicket)
                                        activeTicket = newTicket
                                        currentTicket = newTicket
                                    }
                                    val newMsg = SupportMessageEntity(
                                        id = "sup_msg_${System.currentTimeMillis()}",
                                        ticketId = currentTicket.id,
                                        senderId = currentUser.id,
                                        senderName = currentUser.name,
                                        senderEmail = currentUser.email.ifBlank { currentUser.username },
                                        senderRole = currentUser.role,
                                        text = textToSend,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    dbService.supportMessageDao.insertMessage(newMsg)
                                    syncService.sendSupportMessageToFirestore(newMsg)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = TechGoldTestPoint)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun GodSupportDeskDialog(
    currentUser: UserEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dbService = remember { DatabaseService.getInstance(context) }
    val syncService = remember { com.example.services.FirebaseSyncService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val allTicketsFlow = remember { dbService.supportTicketDao.getAllTickets() }
    val tickets by allTicketsFlow.collectAsState(initial = emptyList())

    var selectedTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var filterOpenOnly by remember { mutableStateOf(false) }

    val displayTickets = remember(tickets, filterOpenOnly) {
        if (filterOpenOnly) tickets.filter { it.status == "open" } else tickets
    }

    if (selectedTicket != null) {
        val ticket = selectedTicket!!
        val ticketMsgsFlow = remember(ticket.id) { dbService.supportMessageDao.getMessagesForTicket(ticket.id) }
        val ticketMsgs by ticketMsgsFlow.collectAsState(initial = emptyList())
        var replyText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedTicket = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "💬 Чат: ${ticket.userName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${ticket.serviceCenterName} • ${ticket.userEmail}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { selectedTicket = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    // Status row with quick action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (ticket.status == "open") Color.Red.copy(alpha = 0.15f) else TechSuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (ticket.status == "open") "🔴 Статус: Открыт" else "🟢 Статус: Решён",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ticket.status == "open") Color.Red else TechSuccessGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                val newStatus = if (ticket.status == "open") "resolved" else "open"
                                scope.launch {
                                    dbService.supportTicketDao.updateTicketStatus(ticket.id, newStatus)
                                    val updated = ticket.copy(status = newStatus, updatedAt = System.currentTimeMillis())
                                    syncService.syncSupportTicketToFirestore(updated)
                                    selectedTicket = updated
                                }
                            }
                        ) {
                            Text(
                                text = if (ticket.status == "open") "Отметить решённым" else "Открыть заново",
                                fontSize = 11.sp,
                                color = TelegramPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Messages list
                    if (ticketMsgs.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Сообщений пока нет", fontSize = 12.sp, color = TelegramTimestamp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ticketMsgs, key = { it.id }) { msg ->
                                val isGodSender = msg.senderRole == "god" || AuthService.isGodEmail(msg.senderEmail)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isGodSender) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isGodSender) Color(0xFF2C2411) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isGodSender) androidx.compose.foundation.BorderStroke(1.dp, TechGoldTestPoint.copy(alpha = 0.6f)) else null,
                                        modifier = Modifier.widthIn(max = 270.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = if (isGodSender) "👑 Вы (Администратор)" else msg.senderName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGodSender) TechGoldTestPoint else TelegramPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.text,
                                                fontSize = 13.sp,
                                                color = if (isGodSender) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reply row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Ответ мастеру...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(
                            onClick = {
                                if (replyText.isBlank()) return@IconButton
                                val textToSend = replyText.trim()
                                replyText = ""
                                scope.launch {
                                    val replyMsg = SupportMessageEntity(
                                        id = "sup_ans_${System.currentTimeMillis()}",
                                        ticketId = ticket.id,
                                        senderId = currentUser?.id ?: "god",
                                        senderName = currentUser?.name ?: "Главный Администратор",
                                        senderEmail = currentUser?.email?.ifBlank { currentUser?.username } ?: "tech.mateasistant@gmail.com",
                                        senderRole = "god",
                                        text = textToSend,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    dbService.supportMessageDao.insertMessage(replyMsg)
                                    syncService.sendSupportMessageToFirestore(replyMsg)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = TechGoldTestPoint)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTicket = null }) {
                    Text("Назад к списку")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = TechGoldTestPoint)
                        Column {
                            Text("🎧 Обращения мастеров", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Служба поддержки TechMate (Входящие)", fontSize = 11.sp, color = TechGoldTestPoint)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !filterOpenOnly,
                            onClick = { filterOpenOnly = false },
                            label = { Text("Все (${tickets.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterOpenOnly,
                            onClick = { filterOpenOnly = true },
                            label = { Text("Только открытые (${tickets.count { it.status == "open" }})", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (displayTickets.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Inbox, contentDescription = null, tint = TelegramTimestamp, modifier = Modifier.size(48.dp))
                                Text("Нет обращений от мастеров", color = TelegramTimestamp, fontSize = 14.sp)
                                Text("Все входящие вопросы мастеров появятся здесь.", color = TelegramTimestamp, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayTickets, key = { it.id }) { ticket ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTicket = ticket },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ticket.userName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (ticket.status == "open") Color.Red.copy(alpha = 0.2f) else TechSuccessGreen.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (ticket.status == "open") "Открыт" else "Решён",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (ticket.status == "open") Color.Red else TechSuccessGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "${ticket.serviceCenterName} • ${ticket.userEmail}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Тема: ${ticket.subject}", fontSize = 12.sp, color = TelegramPrimary, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
fun TelegramSettingsGroup(
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = if (isDark) TelegramDarkSurface else Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun TelegramSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TelegramTimestamp
                )
            }
        }

        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun StatItem(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = TechPrimaryBlue
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authService = remember { AuthService.getInstance(context) }
    val dbService = remember { DatabaseService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val currentUser by authService.currentUser.collectAsState()
    val isGodMode = authService.isGodMode()
    val isServiceAdmin = authService.isServiceAdmin()

    // 0 = Users/Masters, 1 = Invitations, 2 = Support Tickets, 3 = SC (if god) or Resets, 4 = Resets (if god)
    var selectedAdminTab by remember { mutableIntStateOf(0) }

    val usersFlow = remember { dbService.userDao.getAllUsers() }
    val allUsers by usersFlow.collectAsState(initial = emptyList())

    val serviceCentersFlow = remember { dbService.serviceCenterDao.getAllServiceCenters() }
    val serviceCenters by serviceCentersFlow.collectAsState(initial = emptyList())

    val invitationsFlow = remember(isGodMode, currentUser) {
        if (isGodMode) {
            dbService.invitationDao.getAllInvitations()
        } else {
            dbService.invitationDao.getInvitationsForServiceCenter(currentUser?.serviceCenterId ?: "")
        }
    }
    val invitations by invitationsFlow.collectAsState(initial = emptyList())

    val ticketsFlow = remember { dbService.supportTicketDao.getAllTickets() }
    val supportTickets by ticketsFlow.collectAsState(initial = emptyList())

    val passwordResetsFlow = remember(isGodMode, currentUser) {
        if (isGodMode) {
            dbService.passwordResetDao.getRequestsForGod()
        } else {
            dbService.passwordResetDao.getRequestsForServiceAdmin(currentUser?.serviceCenterId ?: "")
        }
    }
    val pendingResets by passwordResetsFlow.collectAsState(initial = emptyList())

    val activityLogsFlow = remember { dbService.activityLogDao.getAllLogs() }
    val activityLogs by activityLogsFlow.collectAsState(initial = emptyList())

    val currentCenter = remember(serviceCenters, currentUser) {
        serviceCenters.firstOrNull { it.id == currentUser?.serviceCenterId }
    }

    val displayUsers = remember(allUsers, currentUser, isGodMode) {
        if (isGodMode) {
            allUsers
        } else {
            allUsers.filter { it.serviceCenterId == currentUser?.serviceCenterId || it.id == currentUser?.id }
        }
    }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddCenterDialog by remember { mutableStateOf(false) }
    var showCreateInviteDialog by remember { mutableStateOf(false) }
    var activeSupportTicketToAnswer by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var newlyCreatedInviteCode by remember { mutableStateOf<String?>(null) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var invitationToDelete by remember { mutableStateOf<InvitationEntity?>(null) }
    var resetRequestToResolve by remember { mutableStateOf<PasswordResetRequestEntity?>(null) }
    var showEventThemeDialog by remember { mutableStateOf(false) }

    if (showEventThemeDialog) {
        com.example.ui.components.EventThemeAndAnimationDialog(
            onDismiss = { showEventThemeDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isGodMode) "👑 Главный Администратор" else "🏢 Панель: ${currentCenter?.name ?: "Сервисный Центр"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = currentUser?.email?.ifBlank { currentUser?.username } ?: "",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showEventThemeDialog = true }) {
                        Icon(Icons.Default.Celebration, contentDescription = "Праздники и ивенты", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TelegramHeader)
            )
        },
        floatingActionButton = {
            when {
                selectedAdminTab == 0 -> {
                    FloatingActionButton(
                        onClick = { showAddUserDialog = true },
                        containerColor = TechPrimaryBlue,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add User")
                    }
                }
                selectedAdminTab == 1 -> {
                    FloatingActionButton(
                        onClick = { showCreateInviteDialog = true },
                        containerColor = TelegramPrimary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = "Create Invitation")
                    }
                }
                selectedAdminTab == 3 && isGodMode -> {
                    FloatingActionButton(
                        onClick = { showAddCenterDialog = true },
                        containerColor = TechGoldTestPoint,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "Add Center")
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Festive Themes & Events Banner Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { showEventThemeDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎉", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Праздничные темы и события",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Новый год, 23 Февраля, 8 Марта, 9 Мая, Киберпанк и др.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showEventThemeDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Настроить", fontSize = 11.sp)
                    }
                }
            }

            // Admin Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedAdminTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TechPrimaryBlue,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedAdminTab == 0,
                    onClick = { selectedAdminTab = 0 },
                    text = { Text("👥 Мастера (${displayUsers.size})") }
                )
                Tab(
                    selected = selectedAdminTab == 1,
                    onClick = { selectedAdminTab = 1 },
                    text = { Text("🎫 Инвайты (${invitations.size})") }
                )
                Tab(
                    selected = selectedAdminTab == 2,
                    onClick = { selectedAdminTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🎧 Поддержка (${supportTickets.size})")
                            if (supportTickets.any { it.status == "open" }) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Red,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        }
                    }
                )
                if (isGodMode) {
                    Tab(
                        selected = selectedAdminTab == 3,
                        onClick = { selectedAdminTab = 3 },
                        text = { Text("🏢 СЦ (${serviceCenters.size})") }
                    )
                }
                val resetTabIndex = if (isGodMode) 4 else 3
                Tab(
                    selected = selectedAdminTab == resetTabIndex,
                    onClick = { selectedAdminTab = resetTabIndex },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔑 Сбросы")
                            if (pendingResets.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "${pendingResets.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                )
                if (isGodMode) {
                    Tab(
                        selected = selectedAdminTab == 5,
                        onClick = { selectedAdminTab = 5 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📜 Логи (${activityLogs.size})")
                            }
                        }
                    )
                }
            }

            when (selectedAdminTab) {
                0 -> {
                    // Masters List with Block/Promote/Delete
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayUsers, key = { it.id }) { user ->
                            val userCenter = serviceCenters.firstOrNull { it.id == user.serviceCenterId }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (user.isBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = user.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val roleLabel = when (user.role) {
                                                "god" -> "👑 GOD"
                                                "service_admin", "admin" -> "🏢 Админ СЦ"
                                                else -> "🔧 Мастер"
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (user.role == "god") TechGoldTestPoint.copy(alpha = 0.2f) else TechPrimaryBlue.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = roleLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (user.role == "god") TechGoldTestPoint else TechPrimaryBlue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (user.isBlocked) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.Red.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "🚫 Заблокирован",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Red,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Email: ${user.email.ifBlank { user.username }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (userCenter != null) {
                                            Text(
                                                text = "СЦ: ${userCenter.name} (${userCenter.city})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TechPrimaryBlue
                                            )
                                        }
                                    }

                                    if (user.role != "god" && user.id != currentUser?.id) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Toggle Block Button
                                            IconButton(onClick = {
                                                scope.launch {
                                                    authService.setUserBlocked(user.id, !user.isBlocked)
                                                    Toast.makeText(context, if (!user.isBlocked) "Пользователь заблокирован" else "Пользователь разблокирован", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = if (user.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                                    contentDescription = "Block",
                                                    tint = if (user.isBlocked) TechSuccessGreen else Color(0xFFE65100)
                                                )
                                            }
                                            // Delete User Button
                                            IconButton(onClick = { userToDelete = user }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Invitations List
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(invitations, key = { it.id }) { invite ->
                            val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                            val isExpired = invite.expiresAt < System.currentTimeMillis()
                            val isUsedUp = invite.isUsed
                            val isActive = !isExpired && !isUsedUp

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = invite.inviteCode,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = if (isActive) TechPrimaryBlue else TelegramTimestamp
                                            )
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", invite.inviteCode))
                                                    Toast.makeText(context, "Код скопирован: ${invite.inviteCode}", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TechPrimaryBlue, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when {
                                                !isActive -> Color.Gray.copy(alpha = 0.15f)
                                                invite.targetRole == "service_admin" -> TechGoldTestPoint.copy(alpha = 0.2f)
                                                else -> TechPrimaryBlue.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = when {
                                                    isUsedUp -> "Использован"
                                                    isExpired -> "Истёк"
                                                    invite.targetRole == "service_admin" -> "Админ СЦ"
                                                    else -> "Мастер"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    !isActive -> Color.Gray
                                                    invite.targetRole == "service_admin" -> TechGoldTestPoint
                                                    else -> TechPrimaryBlue
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Действует до: ${dateFormat.format(Date(invite.expiresAt))} • Статус: ${if (isUsedUp) "Активирован" else if (isExpired) "Истёк" else "Готов к выдаче"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (invite.usedByUserName != null) {
                                        Text(
                                            text = "Активировал: ${invite.usedByUserName} (${invite.usedByUserEmail ?: ""})",
                                            fontSize = 12.sp,
                                            color = TechPrimaryBlue,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { invitationToDelete = invite },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Удалить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Support Tickets List
                    if (supportTickets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = TelegramTimestamp, modifier = Modifier.size(48.dp))
                                Text("Нет обращений в поддержку", color = TelegramTimestamp, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(supportTickets, key = { it.id }) { ticket ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSupportTicketToAnswer = ticket },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ticket.userName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (ticket.status == "open") Color.Red.copy(alpha = 0.2f) else TechSuccessGreen.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (ticket.status == "open") "Открыт" else "Решён",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (ticket.status == "open") Color.Red else TechSuccessGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Email: ${ticket.userEmail}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Тема: ${ticket.subject}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TechPrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    if (isGodMode) {
                        // Service Centers List
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(serviceCenters, key = { it.id }) { center ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(text = center.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = "Город: ${center.city}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Лимит мастеров: ${center.maxMastersLimit}", fontSize = 12.sp, color = TechPrimaryBlue)
                                    }
                                }
                            }
                        }
                    } else {
                        PasswordResetsListView(pendingResets = pendingResets, onResolve = { resetRequestToResolve = it })
                    }
                }

                4 -> {
                    PasswordResetsListView(pendingResets = pendingResets, onResolve = { resetRequestToResolve = it })
                }

                5 -> {
                    // God Mode: Activity Logs
                    ActivityLogsListView(
                        logs = activityLogs,
                        onClearLogs = {
                            scope.launch {
                                dbService.activityLogDao.clearLogs()
                            }
                        }
                    )
                }
            }
        }

        // Dialog: Create Invitation
        if (showCreateInviteDialog) {
            val syncService = remember { com.example.services.FirebaseSyncService.getInstance(context) }
            CreateInviteDialog(
                isGodMode = isGodMode,
                serviceCenters = serviceCenters,
                currentCenterId = currentUser?.serviceCenterId,
                onDismiss = { showCreateInviteDialog = false },
                onCreate = { code, role, centerId ->
                    scope.launch {
                        val newInvite = InvitationEntity(
                            id = "inv_${System.currentTimeMillis()}",
                            inviteCode = code,
                            serviceCenterId = centerId ?: currentUser?.serviceCenterId ?: "",
                            serviceCenterName = serviceCenters.firstOrNull { it.id == centerId }?.name ?: currentUser?.serviceCenterName ?: "Сервисный Центр",
                            createdByAdminEmail = currentUser?.email?.ifBlank { currentUser?.username } ?: "",
                            createdByAdminName = currentUser?.name ?: "Администратор",
                            targetRole = role,
                            expiresAt = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000), // 7 days
                            isUsed = false
                        )
                        dbService.invitationDao.insertInvitation(newInvite)
                        syncService.syncInvitationToFirestore(newInvite)
                        newlyCreatedInviteCode = code
                        showCreateInviteDialog = false
                    }
                }
            )
        }

        // Dialog: Answer Support Ticket
        if (activeSupportTicketToAnswer != null) {
            val syncService = remember { com.example.services.FirebaseSyncService.getInstance(context) }
            val ticket = activeSupportTicketToAnswer!!
            var replyText by remember { mutableStateOf("") }
            val ticketMsgsFlow: Flow<List<SupportMessageEntity>> = remember(ticket) { dbService.supportMessageDao.getMessagesForTicket(ticket.id) }
            val ticketMsgs by ticketMsgsFlow.collectAsState(initial = emptyList())

            AlertDialog(
                onDismissRequest = { activeSupportTicketToAnswer = null },
                title = { Text("Ответ на тикет: ${ticket.userName}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(ticketMsgs, key = { it.id }) { m ->
                                Text(
                                    text = "${m.senderName}: ${m.text}",
                                    fontSize = 13.sp,
                                    color = if (m.senderRole == "god") TechGoldTestPoint else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Ответ оператора...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (replyText.isBlank()) return@Button
                            scope.launch {
                                val reply = SupportMessageEntity(
                                    id = "sup_ans_${System.currentTimeMillis()}",
                                    ticketId = ticket.id,
                                    senderId = currentUser?.id ?: "god",
                                    senderName = currentUser?.name ?: "Главный Администратор",
                                    senderEmail = currentUser?.email?.ifBlank { currentUser?.username } ?: "nosovsergej886@gmail.com",
                                    senderRole = "god",
                                    text = replyText.trim(),
                                    timestamp = System.currentTimeMillis()
                                )
                                dbService.supportMessageDao.insertMessage(reply)
                                syncService.sendSupportMessageToFirestore(reply)
                                replyText = ""
                            }
                        }
                    ) {
                        Text("Отправить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeSupportTicketToAnswer = null }) {
                        Text("Закрыть")
                    }
                }
            )
        }

        // Delete User Confirmation
        if (userToDelete != null) {
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = { Text("Удалить пользователя?") },
                text = { Text("Вы действительно хотите удалить ${userToDelete?.name} (${userToDelete?.email}) из системы?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                userToDelete?.let { dbService.userDao.deleteUser(it.id) }
                                userToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToDelete = null }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Delete Invite Confirmation
        if (invitationToDelete != null) {
            AlertDialog(
                onDismissRequest = { invitationToDelete = null },
                title = { Text("Удалить инвайт?") },
                text = { Text("Код ${invitationToDelete?.inviteCode} станет недействительным.") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                invitationToDelete?.let { dbService.invitationDao.deleteInvitation(it.id) }
                                invitationToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { invitationToDelete = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
fun CreateInviteDialog(
    isGodMode: Boolean,
    serviceCenters: List<ServiceCenterEntity>,
    currentCenterId: String?,
    onDismiss: () -> Unit,
    onCreate: (code: String, role: String, centerId: String?) -> Unit
) {
    fun generateRandomCode(role: String): String {
        val prefix = if (role == "service_admin") "ADM" else "MST"
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val codeBody = (1..5).map { chars.random() }.joinToString("")
        return "$prefix-$codeBody"
    }

    var targetRole by remember { mutableStateOf("master") }
    var inviteCode by remember { mutableStateOf(generateRandomCode(targetRole)) }
    var selectedCenterId by remember { mutableStateOf(currentCenterId ?: serviceCenters.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎫 Создать инвайт-код", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = filterDuplicateInput(inviteCode, it).uppercase() },
                    label = { Text("Инвайт-код") },
                    trailingIcon = {
                        IconButton(onClick = { inviteCode = generateRandomCode(targetRole) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Сгенерировать код", tint = TelegramPrimary)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = { inviteCode = generateRandomCode(targetRole) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = TelegramPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🎲 Сгенерировать случайный код", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                Text("Роль для нового участника:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetRole == "master",
                        onClick = {
                            targetRole = "master"
                            inviteCode = generateRandomCode("master")
                        },
                        label = { Text("Мастер") }
                    )
                    if (isGodMode) {
                        FilterChip(
                            selected = targetRole == "service_admin",
                            onClick = {
                                targetRole = "service_admin"
                                inviteCode = generateRandomCode("service_admin")
                            },
                            label = { Text("Админ СЦ") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(inviteCode.trim(), targetRole, selectedCenterId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TelegramPrimary)
            ) {
                Text("Создать инвайт")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun PasswordResetsListView(
    pendingResets: List<PasswordResetRequestEntity>,
    onResolve: (PasswordResetRequestEntity) -> Unit
) {
    if (pendingResets.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TechSuccessGreen, modifier = Modifier.size(48.dp))
                Text(text = "Нет ожидающих запросов на сброс", color = TelegramTimestamp, fontSize = 15.sp)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(pendingResets, key = { it.id }) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Запрос: ${req.userName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Email: ${req.userEmail}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onResolve(req) },
                            colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Подтвердить сброс")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogsListView(
    logs: List<ActivityLogEntity>,
    onClearLogs: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = TelegramTimestamp, modifier = Modifier.size(54.dp))
                Text(text = "Журнал аудита пуст", fontWeight = FontWeight.Bold, color = TelegramTimestamp, fontSize = 16.sp)
                Text(text = "Все действия мастеров (входы, генерация инвайтов, смена API ключей) будут фиксироваться здесь.", color = TelegramTimestamp, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Всего записей: ${logs.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onClearLogs,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Очистить журнал", fontSize = 12.sp)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs, key = { it.id }) { log ->
                    val (icon, badgeColor, badgeLabel) = when (log.eventType) {
                        "LOGIN", "AUTH_LOGIN" -> Triple(Icons.Default.Login, TechSuccessGreen, "Вход")
                        "INVITE_CREATE" -> Triple(Icons.Default.AddLink, TelegramPrimary, "Инвайт")
                        "API_KEY_CHANGE" -> Triple(Icons.Default.Key, TechGoldTestPoint, "API Ключ")
                        "SECURITY_ALERT" -> Triple(Icons.Default.Warning, Color.Red, "Безопасность")
                        else -> Triple(Icons.Default.Info, TechPrimaryBlue, log.eventType)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.userEmail.ifBlank { log.userName },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.details,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        fontSize = 10.sp,
                                        color = TelegramTimestamp
                                    )
                                    Text(
                                        text = log.eventType,
                                        fontSize = 10.sp,
                                        color = TelegramTimestamp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
