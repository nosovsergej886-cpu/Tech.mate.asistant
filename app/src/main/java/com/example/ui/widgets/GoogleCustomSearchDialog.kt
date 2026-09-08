package com.example.ui.widgets

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.TestPointItem
import com.example.data.TestPointRepository
import com.example.services.CustomSearchImageItem
import com.example.services.DatabaseService
import com.example.services.GoogleCustomSearchService
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechPrimaryBlue
import com.example.ui.theme.TelegramHeader
import kotlinx.coroutines.launch

@Composable
fun GoogleCustomSearchDialog(
    initialQuery: String = "",
    onDismiss: () -> Unit,
    onSelectImageForChat: (imageUrl: String, caption: String) -> Unit,
    onSaveToKnowledgeBase: ((title: String, imageUrl: String, brand: String, model: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val searchService = remember { GoogleCustomSearchService.getInstance(context) }

    var searchQuery by remember { mutableStateOf(initialQuery.ifBlank { "Honor X7a" }) }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<CustomSearchImageItem>>(emptyList()) }
    var selectedImageForZoom by remember { mutableStateOf<CustomSearchImageItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun performSearch(queryToSearch: String) {
        if (queryToSearch.isBlank()) return
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val results = searchService.searchTestPointImages(queryToSearch)
                searchResults = results
                if (results.isEmpty()) {
                    errorMessage = "По запросу ничего не найдено. Попробуйте уточнить модель."
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка поиска: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (searchQuery.isNotBlank()) {
            performSearch(searchQuery)
        }
    }

    val quickModels = listOf("Honor X7a", "Poco X3 Pro", "Redmi 9T", "Samsung A51", "Honor X8", "Poco M3", "Xiaomi 11T")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TechPrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = TechPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Google Custom Search",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Поиск реальных фото тестпоинтов и схем",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Модель (например, Honor X7a test point)...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { performSearch(searchQuery) },
                            enabled = !isLoading && searchQuery.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = TechPrimaryBlue)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick model chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickModels.forEach { model ->
                        SuggestionChip(
                            onClick = {
                                searchQuery = model
                                performSearch(model)
                            },
                            label = { Text(model, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (searchQuery == model) TechPrimaryBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Results or Loading or Empty
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TechPrimaryBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ищем реальные схемы и тестпоинты в Google...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Введите модель устройства и нажмите Поиск",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Найдено фото в сети: ${searchResults.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechPrimaryBlue,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedImageForZoom = item }
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Box {
                                        AsyncImage(
                                            model = item.thumbnailLink ?: item.link,
                                            contentDescription = item.title,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Source domain badge
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Black.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = item.displayLink,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        lineHeight = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Attach to chat
                                        Button(
                                            onClick = {
                                                onSelectImageForChat(item.link, item.title)
                                                onDismiss()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue)
                                        ) {
                                            Text("В чат", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Save to Knowledge Base
                                        IconButton(
                                            onClick = {
                                                if (onSaveToKnowledgeBase != null) {
                                                    onSaveToKnowledgeBase(item.title, item.link, "Auto", searchQuery)
                                                } else {
                                                    scope.launch {
                                                        try {
                                                            val db = DatabaseService.getInstance(context)
                                                            val guide = com.example.model.GuideData(
                                                                device = searchQuery,
                                                                problem = "Тестпоинт (EDL / Brom / TestPoint)",
                                                                difficulty = "Легко",
                                                                timeEstimate = "5 минут",
                                                                tools = listOf("Пинцет", "USB кабель", "ПК с драйверами"),
                                                                causes = listOf(
                                                                    com.example.model.Cause(
                                                                        description = "Необходим перевод в аварийный режим EDL / MTK Brom для прошивки или снятия FRP",
                                                                        probability = 100,
                                                                        checkMethod = "Замкнуть указанную точку на GND и подключить кабель",
                                                                        normalValue = "Определение порта Qualcomm HS-USB QDLoader 9008 / MediaTek USB Port",
                                                                        fixMethod = "Замыкание пинцетом Test Point на экран или массу платы"
                                                                    )
                                                                ),
                                                                steps = listOf(
                                                                    com.example.model.Step(
                                                                        stepNumber = 1,
                                                                        title = "Расположение Test Point",
                                                                        description = item.title,
                                                                        imageUrl = item.link
                                                                    )
                                                                ),
                                                                proTip = "Найдено через Google Custom Search (${item.displayLink})",
                                                                risks = "Не повредите соседние SMD компоненты при замыкании пинцетом."
                                                            )

                                                            db.knowledgeDao.insertEntry(
                                                                com.example.model.KnowledgeBaseEntryEntity(
                                                                    brand = "TestPoint",
                                                                    model = searchQuery,
                                                                    problem = "Тестпоинт: ${item.title}",
                                                                    guideDataJson = db.toJson(guide),
                                                                    addedBy = "Google Search",
                                                                    isSchematic = true
                                                                )
                                                            )
                                                            Toast.makeText(context, "Сохранено в Базу Знаний!", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(TechGoldTestPoint.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(
                                                Icons.Default.BookmarkBorder,
                                                contentDescription = "Save to KB",
                                                tint = TechGoldTestPoint,
                                                modifier = Modifier.size(16.dp)
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
    }

    // Interactive Pinch-To-Zoom Dialog for selected image
    if (selectedImageForZoom != null) {
        val activeItem = selectedImageForZoom!!
        InteractiveImageViewerDialog(
            imageUrl = activeItem.link,
            title = activeItem.title,
            subtitle = "🌐 ${activeItem.displayLink} • Pinch to zoom",
            sourceUrl = activeItem.contextLink,
            onDismiss = { selectedImageForZoom = null },
            onSaveToKb = { title, url ->
                if (onSaveToKnowledgeBase != null) {
                    onSaveToKnowledgeBase(title, url, "Auto", searchQuery)
                } else {
                    scope.launch {
                        try {
                            val db = DatabaseService.getInstance(context)
                            val guide = com.example.model.GuideData(
                                device = searchQuery,
                                problem = "Тестпоинт (EDL / Brom / TestPoint)",
                                difficulty = "Легко",
                                timeEstimate = "5 минут",
                                tools = listOf("Пинцет", "USB кабель", "ПК с драйверами"),
                                causes = listOf(
                                    com.example.model.Cause(
                                        description = "Необходим перевод в аварийный режим EDL / MTK Brom для прошивки или снятия FRP",
                                        probability = 100,
                                        checkMethod = "Замкнуть указанную точку на GND и подключить кабель",
                                        normalValue = "Определение порта Qualcomm HS-USB QDLoader 9008 / MediaTek USB Port",
                                        fixMethod = "Замыкание пинцетом Test Point на экран или массу платы"
                                    )
                                ),
                                steps = listOf(
                                    com.example.model.Step(
                                        stepNumber = 1,
                                        title = "Расположение Test Point",
                                        description = activeItem.title,
                                        imageUrl = activeItem.link
                                    )
                                ),
                                proTip = "Найдено через Google Custom Search (${activeItem.displayLink})",
                                risks = "Не повредите соседние SMD компоненты при замыкании пинцетом."
                            )

                            db.knowledgeDao.insertEntry(
                                com.example.model.KnowledgeBaseEntryEntity(
                                    brand = "TestPoint",
                                    model = searchQuery,
                                    problem = "Тестпоинт: ${activeItem.title}",
                                    guideDataJson = db.toJson(guide),
                                    addedBy = "Google Search",
                                    isSchematic = true
                                )
                            )
                            Toast.makeText(context, "Сохранено в Базу Знаний!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}
