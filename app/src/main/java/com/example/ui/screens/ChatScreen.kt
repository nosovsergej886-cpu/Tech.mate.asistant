package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.services.AppDesignVariant
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TestPointItem
import com.example.data.TestPointRepository
import com.example.model.Cause
import com.example.model.ChatEntity
import com.example.model.GuideData
import com.example.model.KnowledgeBaseEntryEntity
import com.example.model.MessageEntity
import com.example.model.Step
import com.example.model.UserEntity
import com.example.services.AiService
import com.example.services.AuthService
import com.example.services.DatabaseService
import com.example.services.LanguageService
import com.example.services.OpenAiVoiceService
import com.example.services.VoiceEngineType
import com.example.services.OpenAiVoiceOption
import com.example.ui.theme.*
import com.example.ui.util.filterDuplicateInput
import com.example.ui.widgets.ChatBubble
import com.example.ui.widgets.RepairAlgorithmDialog
import com.example.ui.widgets.TypingIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

import com.example.ui.dialogs.ClientReportDialog
import com.example.ui.dialogs.TechCalculatorDialog
import com.example.ui.dialogs.VoiceRecorderDialog
import com.example.ui.widgets.GoogleCustomSearchDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dbService = remember { DatabaseService.getInstance(context) }
    val aiService = remember { AiService.getInstance(context) }
    val authService = remember { AuthService.getInstance(context) }
    val currentUser by authService.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current
    val tokens = LocalDesignTokens.current

    var chatTitle by remember { mutableStateOf("Чат с ИИ") }

    LaunchedEffect(chatId) {
        val chat = dbService.chatDao.getChatById(chatId)
        if (chat != null) {
            chatTitle = chat.title
        }
    }

    val messagesFlow = remember(chatId) {
        dbService.messageDao.getMessagesForChat(chatId)
    }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var isMicroscopeAttachment by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var showGoogleSearchDialog by remember { mutableStateOf(false) }
    var showRepairAlgorithmDialog by remember { mutableStateOf(false) }

    // OpenAI Lively Voice Engine (TTS) & Whisper (STT)
    val openAiVoiceService = remember { OpenAiVoiceService.getInstance(context) }
    val isSpeaking by openAiVoiceService.isSpeaking.collectAsState()
    val isTranscribing by openAiVoiceService.isTranscribing.collectAsState()
    var speakingMessageId by remember { mutableStateOf<String?>(null) }
    var isVoiceAnswerEnabled by remember { mutableStateOf(true) } // AI voice response toggle
    var showOpenAiVoiceDialog by remember { mutableStateOf(false) }

    fun stopSpeech() {
        openAiVoiceService.stop()
        speakingMessageId = null
    }

    fun speakText(textToSpeak: String, messageId: String? = null) {
        speakingMessageId = messageId
        openAiVoiceService.speak(textToSpeak) {
            if (speakingMessageId == messageId) {
                speakingMessageId = null
            }
        }
    }

    // Direct In-App Speech Recognition (No Google popup windows!)
    var recordedVoiceText by remember { mutableStateOf("") }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(context) {
        val recognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null

        speechRecognizer = recognizer
        onDispose {
            recognizer?.destroy()
        }
    }

    // Telegram Voice Recording State
    var isVoiceRecording by remember { mutableStateOf(false) }
    var voiceRecordSeconds by remember { mutableIntStateOf(0) }

    fun startDirectVoiceRecording() {
        isVoiceRecording = true
        recordedVoiceText = ""
        voiceRecordSeconds = 0

        openAiVoiceService.startAudioRecording()

        speechRecognizer?.let { recognizer ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {}
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        recordedVoiceText = matches[0]
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        recordedVoiceText = matches[0]
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            try {
                recognizer.startListening(intent)
            } catch (e: Exception) {
                // Background speech fallback
            }
        }
    }

    // Audio Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startDirectVoiceRecording()
        } else {
            Toast.makeText(context, "Для записи голосовых сообщений необходим доступ к микрофону", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestVoiceRecordingPermissionAndStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startDirectVoiceRecording()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isVoiceRecording) {
        if (isVoiceRecording) {
            voiceRecordSeconds = 0
            while (isVoiceRecording) {
                delay(1000)
                voiceRecordSeconds++
            }
        }
    }

    // Dialog visibility states
    var showAttachBottomSheet by remember { mutableStateOf(false) }
    var showSolutionQuery by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showVoiceRecorderDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Image Pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            selectedImageBase64 = convertUriToBase64(context, uri)
        }
    }

    var showPermissionsDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val resized = resizeBitmap(bitmap, 1024)
            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            selectedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            selectedImageUri = Uri.parse("data:image/jpeg;base64,$selectedImageBase64")
        }
    }

    fun launchCameraSafe(isMicroscope: Boolean = false) {
        if (com.example.services.PermissionHelper.hasCameraPermission(context)) {
            isMicroscopeAttachment = isMicroscope
            cameraLauncher.launch(null)
        } else {
            showPermissionsDialog = true
        }
    }

    fun handleSendMessage(textToSend: String, isVoice: Boolean = false, durationSec: Int = 0) {
        if (textToSend.isBlank() && selectedImageBase64 == null) return

        val actualSec = if (durationSec > 0) durationSec else (textToSend.length / 10).coerceIn(3, 59)
        val formattedText = if (isVoice && !textToSend.contains("[VOICE")) {
            "🎙️ [VOICE:${actualSec}s] $textToSend"
        } else textToSend

        val userMessage = MessageEntity(
            chatId = chatId,
            role = "user",
            text = formattedText,
            imageUrl = selectedImageUri?.toString()
        )

        val imageForAi = selectedImageBase64

        inputText = ""
        selectedImageUri = null
        selectedImageBase64 = null
        isVoiceRecording = false

        scope.launch {
            dbService.messageDao.insertMessage(userMessage)

            // Auto-update title if default
            if (chatTitle.startsWith("Чат с ИИ #") && textToSend.isNotBlank()) {
                val newTitle = textToSend.take(25)
                chatTitle = newTitle
                val existing = dbService.chatDao.getChatById(chatId)
                if (existing != null) {
                    dbService.chatDao.updateChat(existing.copy(title = newTitle, lastMessage = textToSend))
                }
            } else {
                val existing = dbService.chatDao.getChatById(chatId)
                if (existing != null) {
                    dbService.chatDao.updateChat(existing.copy(lastMessage = textToSend, lastMessageTime = System.currentTimeMillis()))
                }
            }

            isThinking = true
            try {
                val promptForAi = if (isVoice) {
                    "Голосовой запрос мастера по ремонту электроники: \"$textToSend\". Дай четкий, полезный и лаконичный ответ разговорным языком без лишних технических вводных словечек."
                } else {
                    textToSend
                }

                val (aiResponseText, guideData) = aiService.sendMessage(
                    chatId = chatId,
                    userText = promptForAi,
                    imageBase64 = imageForAi
                )

                val aiMessage = MessageEntity(
                    chatId = chatId,
                    role = "ai",
                    text = aiResponseText,
                    guideJson = if (guideData != null) dbService.toJson(guideData) else null
                )

                dbService.messageDao.insertMessage(aiMessage)

                val existing = dbService.chatDao.getChatById(chatId)
                if (existing != null) {
                    dbService.chatDao.updateChat(
                        existing.copy(
                            lastMessage = aiResponseText.take(50),
                            lastMessageTime = System.currentTimeMillis()
                        )
                    )
                }

                // Speak AI response out loud ONLY IF user sent a voice message AND voice answer mode is enabled
                if (isVoice && isVoiceAnswerEnabled) {
                    speakText(aiResponseText, aiMessage.id)
                }

                showSolutionQuery = true
            } catch (e: Exception) {
                val errorMsg = MessageEntity(
                    chatId = chatId,
                    role = "ai",
                    text = "⚠️ Ошибка ИИ: ${e.message}"
                )
                dbService.messageDao.insertMessage(errorMsg)
            } finally {
                isThinking = false
            }
        }
    }

    val handleMarkSolved: () -> Unit = {
        scope.launch {
            showSolutionQuery = false
            val summary = aiService.generateKnowledgeBaseSummary(chatId)
            val entry = KnowledgeBaseEntryEntity(
                brand = summary.device.split(" ").firstOrNull() ?: "Другое",
                model = summary.device,
                problem = summary.problem,
                guideDataJson = dbService.toJson(summary),
                addedBy = "Чат Ремонта"
            )
            dbService.knowledgeDao.insertEntry(entry)

            val confirmMsg = MessageEntity(
                chatId = chatId,
                role = "ai",
                text = "Инструкция решена и сохранена в базу знаний!",
                guideJson = dbService.toJson(summary),
                isSaved = true
            )
            dbService.messageDao.insertMessage(confirmMsg)

            Toast.makeText(context, "Сохранено в базу знаний!", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = chatTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = tokens.topBarContentColor,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            if (tokens.variant == AppDesignVariant.VK) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        val statusText = when (tokens.variant) {
                            AppDesignVariant.TELEGRAM -> "бот • в сети"
                            AppDesignVariant.VK -> "отвечает мгновенно"
                            AppDesignVariant.WHATSAPP -> "в сети"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (tokens.variant != AppDesignVariant.VK) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (tokens.variant == AppDesignVariant.WHATSAPP) Color(0xFF25D366) else Color(0xFF4CAF50))
                                )
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tokens.variant == AppDesignVariant.WHATSAPP) Color(0xFF25D366) else tokens.topBarContentColor.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Extra WhatsApp action buttons: Video Call & Audio Call
                    if (tokens.variant == AppDesignVariant.WHATSAPP) {
                        IconButton(onClick = { Toast.makeText(context, "ИИ голосовой вызов в разработке", Toast.LENGTH_SHORT).show() }) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = tokens.topBarContentColor
                            )
                        }
                        IconButton(onClick = { Toast.makeText(context, "ИИ звонок в разработке", Toast.LENGTH_SHORT).show() }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = tokens.topBarContentColor
                            )
                        }
                    }

                    // 12 Steps Repair Algorithm Checklist
                    IconButton(onClick = { showRepairAlgorithmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Repair Algorithm",
                            tint = TechGoldTestPoint
                        )
                    }

                    // OpenAI Voice Selector Button (🎙️)
                    IconButton(onClick = { showOpenAiVoiceDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "OpenAI Voice Settings",
                            tint = TelegramPrimary
                        )
                    }

                    // AI Voice Answer Toggle Button (🔊)
                    IconButton(onClick = {
                        isVoiceAnswerEnabled = !isVoiceAnswerEnabled
                        val statusText = if (isVoiceAnswerEnabled) "🔊 Озвучка OpenAI ИИ включена" else "🔇 Озвучка ИИ отключена"
                        Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                        if (!isVoiceAnswerEnabled) stopSpeech()
                    }) {
                        Icon(
                            imageVector = if (isVoiceAnswerEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Voice Response Toggle",
                            tint = if (isVoiceAnswerEnabled) TechGoldTestPoint else Color.White.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(onClick = { showCalculatorDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Report",
                            tint = Color.White
                        )
                    }
                    TextButton(
                        onClick = handleMarkSolved,
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "Решено",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.topBarBackground,
                    titleContentColor = tokens.topBarContentColor,
                    navigationIconContentColor = tokens.topBarContentColor,
                    actionIconContentColor = tokens.topBarContentColor
                )
            )
        },
        containerColor = tokens.chatBackground
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Active Voice Playback Banner
            AnimatedVisibility(visible = isSpeaking) {
                Surface(
                    color = TechPrimaryBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("🔊 ИИ мастер говорит...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { stopSpeech() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                        }
                    }
                }
            }

            // Chat Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = tokens.primaryAccent.copy(alpha = if (isDark) 0.20f else 0.10f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.35f)),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "👋",
                                        fontSize = 42.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Здесь пока нет сообщений...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 17.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Поздоровайтесь или начните общение смайликом:",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Telegram-style quick starter emojis
                            val starterEmojis = listOf(
                                "👋 Привет!" to "Привет! Нужна подсказка по диагностике.",
                                "🤝 Салют" to "Салют! Подскажи по ремонту устройства.",
                                "🛠️ Нужен совет" to "Привет! Нужен совет инженера по плате.",
                                "⚡ Замеры цепи" to "Привет! Помоги проанализировать замеры питания.",
                                "📱 Вопрос по плате" to "Привет! Есть вопрос по материнской плате."
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    starterEmojis.forEach { (label, prompt) ->
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isDark) tokens.primaryAccent.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                                            ),
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .clickable { handleSendMessage(prompt) }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDark) Color.White else Color(0xFF1E293B),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                items(messages, key = { it.id }) { message ->
                    val guideData = remember(message.guideJson) {
                        dbService.parseGuide(message.guideJson)
                    }

                    ChatBubble(
                        message = message,
                        parsedGuide = guideData,
                        onSaveGuideToKb = { guideToSave ->
                            scope.launch {
                                val entry = KnowledgeBaseEntryEntity(
                                    brand = guideToSave.device.split(" ").firstOrNull() ?: "Другое",
                                    model = guideToSave.device,
                                    problem = guideToSave.problem,
                                    guideDataJson = dbService.toJson(guideToSave),
                                    addedBy = "Чат Ремонта"
                                )
                                dbService.knowledgeDao.insertEntry(entry)
                                dbService.messageDao.updateSavedStatus(message.id, true)
                                Toast.makeText(context, "Сохранено в базу знаний!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSaveTestPointToKb = { tpToSave ->
                            scope.launch {
                                val (normBrand, normModel) = TestPointRepository.normalizeBrand(tpToSave.brand, tpToSave.model)
                                val normalizedTp = tpToSave.copy(brand = normBrand, model = normModel)
                                TestPointRepository.addTestPoint(normalizedTp)
                                val guideData = GuideData(
                                    device = "$normBrand $normModel",
                                    problem = "FRP / TestPoint: ${normalizedTp.cpuType}",
                                    difficulty = "Средняя",
                                    timeEstimate = "5-10 мин",
                                    tools = normalizedTp.toolsNeeded,
                                    causes = listOf(
                                        Cause("Блокировка FRP / EDL", 100, normalizedTp.description, "Замкнут TestPoint", "Сброс в UnlockTool / Chimera")
                                    ),
                                    steps = normalizedTp.frpGuide.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
                                        Step(idx + 1, "Шаг ${idx + 1}", line)
                                    },
                                    proTip = "Обязательно отключайте аккумулятор перед замыканием тестпоинта!",
                                    risks = "Не повредите соседние SMD элементы металлическим пинцетом"
                                )
                                val entry = KnowledgeBaseEntryEntity(
                                    brand = normBrand,
                                    model = normModel,
                                    problem = "TestPoint / FRP (${normalizedTp.cpuType})",
                                    guideDataJson = dbService.toJson(guideData),
                                    addedBy = "Мастер (Тестпоинт)"
                                )
                                dbService.knowledgeDao.insertEntry(entry)
                                dbService.messageDao.updateSavedStatus(message.id, true)
                                Toast.makeText(context, "✅ Тестпоинт $normModel сохранен в категорию $normBrand!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onApproveCandidate = { candidate ->
                            scope.launch {
                                val updatedText = message.text.replace(
                                    "\"status\":\"PENDING\"",
                                    "\"status\":\"APPROVED\""
                                )
                                dbService.messageDao.updateMessage(message.copy(text = updatedText))

                                val confirmMsg = aiService.approveAndSaveCandidate(candidate)
                                val assistantEntity = MessageEntity(
                                    chatId = chatId,
                                    role = "assistant",
                                    text = confirmMsg,
                                    timestamp = System.currentTimeMillis()
                                )
                                dbService.messageDao.insertMessage(assistantEntity)
                                Toast.makeText(context, LanguageService.getString("tp_approved_saved"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRejectCandidate = { candidate ->
                            scope.launch {
                                val updatedText = message.text.replace(
                                    "\"status\":\"PENDING\"",
                                    "\"status\":\"REJECTED\""
                                )
                                dbService.messageDao.updateMessage(message.copy(text = updatedText))

                                Toast.makeText(context, LanguageService.getString("tp_searching_next"), Toast.LENGTH_SHORT).show()
                                val (responseText, _) = aiService.searchAlternativeCandidate(
                                    model = candidate.model,
                                    rejectedUrls = setOf(candidate.imageUrl),
                                    attempt = candidate.attempt + 1
                                )
                                val nextAssistantEntity = MessageEntity(
                                    chatId = chatId,
                                    role = "assistant",
                                    text = responseText,
                                    timestamp = System.currentTimeMillis()
                                )
                                dbService.messageDao.insertMessage(nextAssistantEntity)
                            }
                        },
                        onRatingChange = { newRating ->
                            scope.launch {
                                dbService.messageDao.updateRating(message.id, newRating)
                                val feedbackText = when (newRating) {
                                    1 -> "👍 Положительный отзыв учтен"
                                    -1 -> "👎 Отзыв учтен"
                                    else -> "Отзыв сброшен"
                                }
                                Toast.makeText(context, feedbackText, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSpeakMessage = { textToSpeak ->
                            speakText(textToSpeak, message.id)
                        },
                        isSpeaking = isSpeaking && speakingMessageId == message.id,
                        onStopSpeech = { stopSpeech() }
                    )
                }

                if (isThinking) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // After Solution Query Banner
            AnimatedVisibility(visible = showSolutionQuery) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDark) TelegramAiBubbleDark else TelegramAiBubbleLight,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Проблема решена?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = handleMarkSolved,
                                colors = ButtonDefaults.buttonColors(containerColor = TechGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Да, в базу", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { showSolutionQuery = false },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Продолжить", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Dynamic Contextual Smart Autocomplete Suggestion Chips
            val smartSuggestions = remember(inputText, messages.lastOrNull()?.text) {
                val lastAiText = messages.lastOrNull { it.role == "ai" }?.text?.lowercase() ?: ""
                val trimmedInput = inputText.trim().lowercase()

                when {
                    lastAiText.contains("как давно") || lastAiText.contains("когда") || lastAiText.contains("сколько") -> {
                        listOf("Сегодня", "2 дня назад", "После зарядки в авто", "Вчера вечером")
                    }
                    lastAiText.contains("напряжени") || lastAiText.contains("ток") || lastAiText.contains("замер") || lastAiText.contains("vbus") -> {
                        listOf("5V 0.00A (Нет тока)", "5V 0.45A (Медленно)", "9V 1.5A (Быстрая)", "0.02A Короткое замыкание")
                    }
                    trimmedInput.startsWith("у") || trimmedInput.startsWith("уп") -> {
                        listOf("Упал / Падение", "Упал в воду", "Удар по корпусу", "Упал, нет изображения")
                    }
                    trimmedInput.startsWith("н") || trimmedInput.startsWith("не") -> {
                        listOf("Не включается", "Не заряжается", "Не видит SIM", "Не работает тачскрин")
                    }
                    trimmedInput.startsWith("з") || trimmedInput.startsWith("за") -> {
                        listOf("Залитик / Вода", "Заряжается 0.00A", "Зависает на логотипе", "Замена дисплея")
                    }
                    trimmedInput.startsWith("в") || trimmedInput.startsWith("во") -> {
                        listOf("Вода попадание", "Восстановление прошивки", "Выключается на 20%")
                    }
                    else -> {
                        listOf("Попала вода", "Удар / падение", "Не включается", "Не заряжается", "0.00A потребление")
                    }
                }
            }

            // Master Quick Action Chips Bar
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { showRepairAlgorithmDialog = true },
                        label = { Text("📋 12 шагов алгоритма", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null, tint = if (isDark) Color(0xFFFDE047) else TechGoldTestPoint, modifier = Modifier.size(15.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isDark) TechGoldTestPoint.copy(alpha = 0.28f) else TechGoldTestPoint.copy(alpha = 0.15f),
                            labelColor = if (isDark) Color(0xFFFDE047) else Color(0xFFB45309)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) TechGoldTestPoint.copy(alpha = 0.65f) else TechGoldTestPoint.copy(alpha = 0.35f))
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            inputText = "⚡ Замеры линий питания: VBUS=...V, VBAT=...V, Диодная прозвонка линий D+/D-: ... mV. Подскажи норму."
                        },
                        label = { Text("⚡ Замеры / КЗ", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isDark) tokens.primaryAccent.copy(alpha = 0.28f) else tokens.primaryAccent.copy(alpha = 0.12f),
                            labelColor = if (isDark) Color.White else tokens.primaryAccent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) tokens.primaryAccent.copy(alpha = 0.65f) else tokens.primaryAccent.copy(alpha = 0.35f))
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            val currentQuery = inputText.ifBlank { chatTitle.ifBlank { "Xiaomi Redmi Note 12" } }
                            inputText = "Покажи фото и схему TestPoint для $currentQuery (EDL / BROM режим)"
                        },
                        label = { Text("📍 TestPoint", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isDark) tokens.primaryAccent.copy(alpha = 0.28f) else tokens.primaryAccent.copy(alpha = 0.12f),
                            labelColor = if (isDark) Color.White else tokens.primaryAccent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) tokens.primaryAccent.copy(alpha = 0.65f) else tokens.primaryAccent.copy(alpha = 0.35f))
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            launchCameraSafe(isMicroscope = true)
                        },
                        label = { Text("🔬 Микроскоп", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isDark) tokens.primaryAccent.copy(alpha = 0.28f) else tokens.primaryAccent.copy(alpha = 0.12f),
                            labelColor = if (isDark) Color.White else tokens.primaryAccent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) tokens.primaryAccent.copy(alpha = 0.65f) else tokens.primaryAccent.copy(alpha = 0.35f))
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            Toast.makeText(context, "💡 Главное правило: Сначала понять причину — потом паять!", Toast.LENGTH_LONG).show()
                        },
                        label = { Text("💡 Правило мастера", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isDark) Color(0x3322C55E) else Color(0x2222C55E),
                            labelColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x8822C55E) else Color(0x4422C55E))
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(smartSuggestions) { reply ->
                    AssistChip(
                        onClick = {
                            inputText = if (inputText.isBlank()) reply else "$inputText $reply"
                        },
                        label = { Text(reply, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isDark) Color(0xFF243247) else Color(0xFFF1F5F9),
                            labelColor = if (isDark) Color.White else Color(0xFF0F172A)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) TechGoldTestPoint.copy(alpha = 0.65f) else Color(0xFFCBD5E1))
                    )
                }
            }

            // Image Preview Thumbnail
            if (selectedImageUri != null) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                selectedImageUri = null
                                selectedImageBase64 = null
                                isMicroscopeAttachment = false
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    if (isMicroscopeAttachment) {
                        AssistChip(
                            onClick = {},
                            label = { Text("🔬 Микроскоп", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = TechGoldTestPoint.copy(alpha = 0.2f), labelColor = TechGoldTestPoint)
                        )
                    }
                }
            }

            // Messenger-Specific Dynamic Input Bar
            Surface(
                color = tokens.chatBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isVoiceRecording) {
                    // TELEGRAM VOICE RECORDING BAR MODE (ROUNDED PILL WITH ANIMATED WAVEFORM)
                    val transition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by transition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                        label = "pulseAlpha"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = if (isDark) Color(0xFF17212B) else Color(0xFFFFFFFF),
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Red Pulsing Record Dot & Mic
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f * pulseAlpha + 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Recording",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "0:%02d".format(voiceRecordSeconds),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (recordedVoiceText.isNotBlank()) recordedVoiceText else "Говорите...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Live Animated Waveform Bars
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    val bar1 by transition.animateFloat(
                                        initialValue = 6f, targetValue = 22f,
                                        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "b1"
                                    )
                                    val bar2 by transition.animateFloat(
                                        initialValue = 16f, targetValue = 8f,
                                        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
                                        label = "b2"
                                    )
                                    val bar3 by transition.animateFloat(
                                        initialValue = 8f, targetValue = 24f,
                                        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "b3"
                                    )
                                    val bar4 by transition.animateFloat(
                                        initialValue = 20f, targetValue = 10f,
                                        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse),
                                        label = "b4"
                                    )
                                    val bar5 by transition.animateFloat(
                                        initialValue = 10f, targetValue = 22f,
                                        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "b5"
                                    )

                                    listOf(bar1, bar2, bar3, bar4, bar5, bar2, bar1).forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(h.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(tokens.primaryAccent)
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cancel / Delete voice note
                                IconButton(
                                    onClick = {
                                        try { speechRecognizer?.stopListening() } catch (e: Exception) {}
                                        openAiVoiceService.cancelRecording()
                                        isVoiceRecording = false
                                        recordedVoiceText = ""
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Cancel Voice",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Send Voice Message FAB
                                IconButton(
                                    onClick = {
                                        try { speechRecognizer?.stopListening() } catch (e: Exception) {}
                                        val recSeconds = if (voiceRecordSeconds > 0) voiceRecordSeconds else 3
                                        val fallbackText = recordedVoiceText.trim()
                                        isVoiceRecording = false

                                        openAiVoiceService.stopAndTranscribe(
                                            promptHint = "Диагностика смартфонов, тестпоинт, EDL, BROM, короткое замыкание, замеры, пайка",
                                            onSuccess = { recognizedText ->
                                                val textToSend = if (recognizedText.isNotBlank()) recognizedText else if (fallbackText.isNotBlank()) fallbackText else "Голосовой запрос"
                                                handleSendMessage(textToSend, isVoice = true, durationSec = recSeconds)
                                                recordedVoiceText = ""
                                            },
                                            onError = { _ ->
                                                val textToSend = if (fallbackText.isNotBlank()) fallbackText else if (inputText.isNotBlank()) inputText else "Голосовое сообщение"
                                                handleSendMessage(textToSend, isVoice = true, durationSec = recSeconds)
                                                recordedVoiceText = ""
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(TelegramBlue)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                } else {
                    // Normal Input Area per Messenger Structure
                    when (tokens.variant) {
                        AppDesignVariant.WHATSAPP -> {
                            // WhatsApp Iconic Detached Floating Input Capsule + Detached Action Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(25.dp),
                                    color = if (isDark) Color(0xFF1F2C34) else Color.White,
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SentimentSatisfied,
                                                contentDescription = "Emoji",
                                                tint = if (isDark) Color(0xFF8696A0) else Color(0xFF54656F),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        TextField(
                                            value = inputText,
                                            onValueChange = { inputText = filterDuplicateInput(inputText, it) },
                                            placeholder = {
                                                Text(
                                                    "Сообщение",
                                                    fontSize = 16.sp,
                                                    color = if (isDark) Color(0xFF8696A0) else Color(0xFF8696A0)
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                cursorColor = WhatsAppLightGreenAccent
                                            ),
                                            maxLines = 4
                                        )

                                        IconButton(
                                            onClick = { showAttachBottomSheet = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = "Attach",
                                                tint = if (isDark) Color(0xFF8696A0) else Color(0xFF54656F),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                launchCameraSafe(isMicroscope = false)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Camera",
                                                tint = if (isDark) Color(0xFF8696A0) else Color(0xFF54656F),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                val hasContent = inputText.isNotBlank() || selectedImageBase64 != null
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = if (isDark) WhatsAppLightGreenAccent else WhatsAppTeal,
                                    shadowElevation = 3.dp
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (hasContent) {
                                                handleSendMessage(inputText)
                                            } else {
                                                requestVoiceRecordingPermissionAndStart()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = if (hasContent) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                            contentDescription = if (hasContent) "Send" else "Voice Note",
                                            tint = if (isDark && !hasContent) Color.Black else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AppDesignVariant.VK -> {
                            // VK Structural Layout: Circular + button, message field with camera inside, VK Blue Send button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF2C2D2E) else Color(0xFFEBEDF0)
                                ) {
                                    IconButton(
                                        onClick = { showAttachBottomSheet = true },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "VK Attach",
                                            tint = VkBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isDark) VkDarkSurface else Color(0xFFF2F3F5),
                                    border = if (!isDark) androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFD3D5D8)) else null
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        TextField(
                                            value = inputText,
                                            onValueChange = { inputText = filterDuplicateInput(inputText, it) },
                                            placeholder = {
                                                Text(
                                                    "Ваше сообщение...",
                                                    fontSize = 15.sp,
                                                    color = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                cursorColor = VkBlue
                                            ),
                                            maxLines = 4
                                        )

                                        IconButton(
                                            onClick = {
                                                launchCameraSafe(isMicroscope = false)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Camera",
                                                tint = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                val hasContent = inputText.isNotBlank() || selectedImageBase64 != null
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = VkBlue
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (hasContent) {
                                                handleSendMessage(inputText)
                                            } else {
                                                requestVoiceRecordingPermissionAndStart()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = if (hasContent) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                            contentDescription = if (hasContent) "Send" else "Voice",
                                            tint = Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AppDesignVariant.TELEGRAM -> {
                            // Telegram Classic Single Pill + Blue Circular FAB
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(26.dp),
                                    color = if (isDark) TelegramInputDark else TelegramInputLight,
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showAttachBottomSheet = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = "Attach",
                                                tint = TelegramTimestamp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        TextField(
                                            value = inputText,
                                            onValueChange = { inputText = filterDuplicateInput(inputText, it) },
                                            placeholder = {
                                                Text(
                                                    "Сообщение...",
                                                    fontSize = 15.sp,
                                                    color = TelegramTimestamp
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                cursorColor = tokens.primaryAccent
                                            ),
                                            maxLines = 4
                                        )

                                        IconButton(
                                            onClick = {
                                                launchCameraSafe(isMicroscope = false)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Camera",
                                                tint = TelegramTimestamp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                if (inputText.isNotBlank() || selectedImageBase64 != null) {
                                    IconButton(
                                        onClick = { handleSendMessage(inputText) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(TelegramBlue)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            requestVoiceRecordingPermissionAndStart()
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(TelegramBlue)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Telegram Voice Note",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Attachment Dialog
        if (showAttachBottomSheet) {
            AlertDialog(
                onDismissRequest = { showAttachBottomSheet = false },
                title = { Text("Прикрепить фото / микроскоп") },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                showAttachBottomSheet = false
                                showGoogleSearchDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TechPrimaryBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("🔎 Найти TestPoint в Google (API)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        TextButton(
                            onClick = {
                                showAttachBottomSheet = false
                                launchCameraSafe(isMicroscope = true)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Biotech, contentDescription = null, tint = TechGoldTestPoint)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("🔬 Снимок с микроскопа", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        TextButton(
                            onClick = {
                                showAttachBottomSheet = false
                                launchCameraSafe(isMicroscope = false)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = tokens.primaryAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Обычная камера", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        TextButton(
                            onClick = {
                                showAttachBottomSheet = false
                                isMicroscopeAttachment = false
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = tokens.primaryAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Галерея плат и схем", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAttachBottomSheet = false }) {
                        Text("Отмена", color = tokens.primaryAccent)
                    }
                }
            )
        }

        // Permissions Dialog fallback
        if (showPermissionsDialog) {
            com.example.ui.components.PermissionsDialog(
                onDismiss = { showPermissionsDialog = false }
            )
        }

        // Dialog: 12 Steps Repair Algorithm
        if (showRepairAlgorithmDialog) {
            RepairAlgorithmDialog(
                onDismiss = { showRepairAlgorithmDialog = false },
                onInsertStepToChat = { stepPrompt ->
                    handleSendMessage(stepPrompt)
                }
            )
        }

        // Dialog: OpenAI Voice & Whisper Engine Studio
        if (showOpenAiVoiceDialog) {
            AiVoiceSettingsDialog(
                currentName = currentUser?.customAiName ?: "TechMate",
                currentGender = currentUser?.aiVoiceGender ?: "FEMALE",
                onDismiss = { showOpenAiVoiceDialog = false },
                onSave = { newName, newGender ->
                    showOpenAiVoiceDialog = false
                    scope.launch {
                        currentUser?.let { user ->
                            val updated = user.copy(customAiName = newName, aiVoiceGender = newGender)
                            dbService.userDao.insertUser(updated)
                        }
                    }
                }
            )
        }

        // Dialog: Google Custom Search for TestPoints
        if (showGoogleSearchDialog) {
            GoogleCustomSearchDialog(
                initialQuery = inputText.ifBlank { "Honor X7a" },
                onDismiss = { showGoogleSearchDialog = false },
                onSelectImageForChat = { imageUrl, caption ->
                    handleSendMessage("📍 **Тестпоинт из Google Search**: $caption\n\n![$caption]($imageUrl)")
                }
            )
        }

        // Dialog 1: Master Calculator
        if (showCalculatorDialog) {
            TechCalculatorDialog(
                onDismiss = { showCalculatorDialog = false },
                onInsertToChat = { result -> handleSendMessage(result) }
            )
        }

        // Dialog 2: Client Repair Report
        if (showReportDialog) {
            ClientReportDialog(
                chatTitle = chatTitle,
                lastAiMessage = messages.lastOrNull { it.role == "ai" }?.text,
                onDismiss = { showReportDialog = false },
                onInsertToChat = { report -> handleSendMessage(report) }
            )
        }

        // Dialog 3: Voice Note Recorder
        if (showVoiceRecorderDialog) {
            VoiceRecorderDialog(
                onDismiss = { showVoiceRecorderDialog = false },
                onSendVoiceNote = { voiceNoteText -> handleSendMessage(voiceNoteText, isVoice = true) }
            )
        }
    }
}

private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap
    val ratio = width.toFloat() / height.toFloat()
    val (newWidth, newHeight) = if (width > height) {
        maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
    } else {
        (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
    }
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

private fun convertUriToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream) ?: return null
        val resized = resizeBitmap(original, 1024)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}
