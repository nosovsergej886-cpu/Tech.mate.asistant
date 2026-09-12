package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.example.model.KnowledgeBaseEntryEntity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SavedKnowledgeItem
import com.example.services.DatabaseService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * МОДУЛЬ 4: Offline Knowledge Base (Room DB + Экран сохраненных знаний)
 *
 * Особенности реализации:
 * - Подключение к Room DAO (`savedKnowledgeDao.getAllItemsFlow()`).
 * - Премиальные карточки базы знаний в стиле Glassmorphism (`BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))` для темной темы).
 * - Живой поиск по запросу (`query`) и тегам (`tags`).
 * - Фильтрация по популярным тегам инженеров СЦ.
 * - Быстрое копирование решения в буфер обмена с виброоткликом.
 * - Диалог сохранения из чата [SaveToKnowledgeBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val dbService = remember { DatabaseService.getInstance(context) }
    val savedItems by dbService.savedKnowledgeDao.getAllItemsFlow().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var selectedDetailItem by remember { mutableStateOf<SavedKnowledgeItem?>(null) }

    // Инициализация стартовыми данными при первом открытии, если база пустая
    LaunchedEffect(Unit) {
        if (savedItems.isEmpty()) {
            scope.launch {
                dbService.savedKnowledgeDao.insertItem(
                    SavedKnowledgeItem(
                        id = UUID.randomUUID().toString(),
                        query = "iPhone 12 не заряжается, ток 0.00А",
                        aiResponse = "1. Проверить шлейф разъема зарядки на обрыв шины CC1/CC2.\n2. Проверить защитные диоды TVS по линии VBUS.\n3. Замерить падение напряжения на выходе контроллера Hydra (U6300): норма 0.420В на диодной прозвонке.",
                        tags = "iPhone, Зарядка, Hydra, BGA",
                        timestamp = System.currentTimeMillis() - 86400000L
                    )
                )
                dbService.savedKnowledgeDao.insertItem(
                    SavedKnowledgeItem(
                        id = UUID.randomUUID().toString(),
                        query = "Xiaomi Redmi Note 10 Pro циклическая перезагрузка (Bootloop)",
                        aiResponse = "Типовая неисправность: отвал процессора Snapdragon 732G либо микротрещины в пайке КП PM6150. Требуется реболл процессора с преднагревом платы до 160°C и рабочей температурой пайки 285°C.",
                        tags = "Xiaomi, Процессор, Bootloop, Реболл",
                        timestamp = System.currentTimeMillis() - 172800000L
                    )
                )
            }
        }
    }

    // Фильтрация элементов по тексту поиска и выбранному тегу
    val filteredItems = remember(savedItems, searchQuery, selectedTagFilter) {
        savedItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.query.contains(searchQuery, ignoreCase = true) ||
                    item.aiResponse.contains(searchQuery, ignoreCase = true) ||
                    item.tags.contains(searchQuery, ignoreCase = true)

            val matchesTag = selectedTagFilter == null ||
                    item.tags.contains(selectedTagFilter!!, ignoreCase = true)

            matchesQuery && matchesTag
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "База знаний СЦ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Назад",
                                tint = if (isDark) Color.White else Color.Black
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showManualAddDialog = true
                },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.shadow(8.dp, CircleShape, spotColor = Color(0x666366F1))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить решение")
            }
        },
        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Поле поиска с поисковой строкой по query и tags
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(
                        elevation = if (isDark) 4.dp else 2.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0x226366F1)
                    ),
                placeholder = {
                    Text(
                        "Поиск по неисправности, модели или тегу...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Поиск",
                        tint = Color(0xFF6366F1)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                    focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                    unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White
                )
            )

            // Чипсы фильтрации по популярным тегам
            val quickTags = listOf("Все", "iPhone", "Xiaomi", "Зарядка", "Питание", "BGA", "Bootloop")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickTags) { tag ->
                    val isSelected = (tag == "Все" && selectedTagFilter == null) || (tag == selectedTagFilter)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF6366F1) else (if (isDark) Color(0xFF1E293B) else Color.White),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF6366F1) else (if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f))
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTagFilter = if (tag == "Все") null else tag
                            }
                    ) {
                        Text(
                            text = if (tag == "Все") "Все кейсы (${savedItems.size})" else "#$tag",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else (if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.75f)),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Список карточек знаний
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Ничего не найдено",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Сохраняйте проверенные решения из чата долгим нажатием на ответ ИИ",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        SavedKnowledgeTile(
                            item = item,
                            isDark = isDark,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedDetailItem = item
                            },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Knowledge Entry", "${item.query}\n\n${item.aiResponse}")
                                clipboard.setPrimaryClip(clip)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, "Решение скопировано в буфер!", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                scope.launch {
                                    dbService.savedKnowledgeDao.deleteItem(item.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Удалено из базы знаний", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Диалог ручного добавления решения
    if (showManualAddDialog) {
        ManualAddKnowledgeDialog(
            onDismiss = { showManualAddDialog = false },
            onSave = { query, response, tags ->
                scope.launch {
                    val newItem = SavedKnowledgeItem(
                        id = UUID.randomUUID().toString(),
                        query = query,
                        aiResponse = response,
                        tags = tags,
                        timestamp = System.currentTimeMillis()
                    )
                    dbService.savedKnowledgeDao.insertItem(newItem)

                    val kbEntry = KnowledgeBaseEntryEntity(
                        id = newItem.id,
                        brand = tags.split(",").firstOrNull()?.trim() ?: "Общее",
                        model = "",
                        problem = query,
                        guideDataJson = response,
                        addedBy = "Мастер",
                        addedDate = newItem.timestamp,
                        isSchematic = false
                    )
                    dbService.knowledgeDao.insertEntry(kbEntry)

                    try {
                        com.example.services.CustomHostSyncService.getInstance(context).autoPushKnowledge(kbEntry)
                    } catch (_: Exception) {}

                    showManualAddDialog = false
                    Toast.makeText(context, "Кейс сохранён и отправлен в синхронизацию!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Диалог детального просмотра решения из плитки
    selectedDetailItem?.let { item ->
        KnowledgeDetailDialog(
            item = item,
            isDark = isDark,
            onDismiss = { selectedDetailItem = null },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Knowledge Entry", "${item.query}\n\n${item.aiResponse}")
                clipboard.setPrimaryClip(clip)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Решение скопировано в буфер!", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                scope.launch {
                    dbService.savedKnowledgeDao.deleteItem(item.id)
                    selectedDetailItem = null
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, "Удалено из базы знаний", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * Премиальная карточка решения базы знаний в стиле Glassmorphism
 */
@Composable
fun SavedKnowledgeCard(
    item: SavedKnowledgeItem,
    isDark: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "knowledgeCardScale"
    )

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isDark) 4.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x336366F1)
            )
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок вопроса/запроса
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.query,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Текст ответа ИИ / алгоритма ремонта
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.75f) else Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
                )
            ) {
                Text(
                    text = item.aiResponse,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF334155),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Теги и дата создания
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Теги
                if (item.tags.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item.tags.split(",").take(3).forEach { tagRaw ->
                            val tag = tagRaw.trim()
                            if (tag.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF818CF8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Скопировать",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * BottomSheet для быстрого сохранения ответа ИИ в базу знаний по долгому нажатию
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToKnowledgeBottomSheet(
    query: String,
    aiResponse: String,
    onDismiss: () -> Unit,
    onSaved: (SavedKnowledgeItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var tagsInput by remember { mutableStateOf("iPhone, Ремонт, Алгоритм") }
    var editedQuery by remember { mutableStateOf(query) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Сохранить в базу знаний",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Запрос / Неисправность:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = editedQuery,
                onValueChange = { editedQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Теги для быстрого поиска (через запятую):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Например: iPhone 13, Tristar, Питание") },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Превью решения ИИ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = aiResponse,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(12.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        val item = SavedKnowledgeItem(
                            id = UUID.randomUUID().toString(),
                            query = editedQuery.trim(),
                            aiResponse = aiResponse.trim(),
                            tags = tagsInput.trim(),
                            timestamp = System.currentTimeMillis()
                        )
                        onSaved(item)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить в базу", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Диалог ручного добавления кейса
 */
@Composable
private fun ManualAddKnowledgeDialog(
    onDismiss: () -> Unit,
    onSave: (query: String, response: String, tags: String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var query by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Добавить кейс ремонта",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color.White else Color.Black
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Неисправность и модель") },
                    placeholder = { Text("iPhone 11 не включается, ток 0.04А") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    label = { Text("Алгоритм решения и контрольные замеры") },
                    placeholder = { Text("Замена КП или линии PP1V8_S2...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Теги (через запятую)") },
                    placeholder = { Text("iPhone 11, PMIC, BGA") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            if (query.isNotBlank() && response.isNotBlank()) {
                                onSave(query, response, tags)
                            }
                        },
                        enabled = query.isNotBlank() && response.isNotBlank(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

/**
 * Плитка базы знаний (SavedKnowledgeTile) в сетке 2x2.
 */
@Composable
fun SavedKnowledgeTile(
    item: SavedKnowledgeItem,
    isDark: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "knowledgeTileScale"
    )

    val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }
    val firstTag = remember(item.tags) { item.tags.split(",").firstOrNull()?.trim() ?: "" }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .scale(scale)
            .shadow(
                elevation = if (isDark) 4.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x256366F1)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.90f) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.06f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Верхняя строка: иконка + тег
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (firstTag.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "#$firstTag",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Заголовок кейса (вопрос/неисправность)
                Text(
                    text = item.query,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Выжимка решения
                Text(
                    text = item.aiResponse.replace(Regex("[#*`-]"), "").trim(),
                    fontSize = 11.sp,
                    lineHeight = 14.5.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            // Нижняя строка: Дата и быстрые действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 10.5.sp,
                    color = Color.Gray
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Скопировать",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Полноэкранный детальный просмотр решения из плитки
 */
@Composable
fun KnowledgeDetailDialog(
    item: SavedKnowledgeItem,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }
    val scrollState = rememberScrollState()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Верхняя панель: иконка, дата и закрытие
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "База знаний СЦ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                                Text(
                                    formattedDate,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Закрыть",
                                tint = if (isDark) Color.White else Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Заголовок кейса
                    Text(
                        text = item.query,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        color = if (isDark) Color.White else Color.Black
                    )

                    // Теги
                    if (item.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item.tags.split(",").forEach { tagRaw ->
                                val tag = tagRaw.trim()
                                if (tag.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF6366F1).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF818CF8),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Прокручиваемый текст решения
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.aiResponse,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопки действий: Копировать и Удалить
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF4444)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Удалить", fontSize = 13.sp)
                        }

                        androidx.compose.material3.Button(
                            onClick = onCopy,
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1)
                            ),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Скопировать", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
