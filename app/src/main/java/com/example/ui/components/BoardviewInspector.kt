package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BoardviewPhotoEntity
import com.example.services.DatabaseService
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TechPrimaryBlue
import com.example.ui.util.filterDuplicateInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardviewInspectorScreen(
    brand: String,
    model: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val scope = rememberCoroutineScope()
    val dbService = remember { DatabaseService.getInstance(context) }

    val boardviewsFlow = remember(brand, model) {
        dbService.boardviewDao.getBoardviewsForModel(brand, model)
    }
    val boardviews by boardviewsFlow.collectAsState(initial = emptyList())

    var selectedSide by remember { mutableStateOf("front") } // "front", "back", "subboard", "schematic"
    var showAddPhotoDialog by remember { mutableStateOf(false) }

    val currentPhotos = remember(boardviews, selectedSide) {
        boardviews.filter { it.side == selectedSide }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$brand $model",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⚡ Схема и фото материнской платы (PCB Boardview)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TechPrimaryBlue
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPhotoDialog = true }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Добавить фото платы", tint = TechPrimaryBlue)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Side Selector Chips (Front, Back, Sub-board, Schematic)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sides = listOf(
                    Triple("front", "📷 Лицевая сторона (Top PCB)", "Лицо"),
                    Triple("back", "📷 Обратная сторона (Bottom PCB)", "Оборот"),
                    Triple("subboard", "🔌 Субплата Type-C", "Субплата"),
                    Triple("schematic", "📄 Принципиальная схема", "Схема")
                )
                items(sides) { (sideKey, fullLabel, shortLabel) ->
                    val isSelected = selectedSide == sideKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSide = sideKey },
                        label = { Text(shortLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            if (sideKey.startsWith("front") || sideKey.startsWith("back")) {
                                Icon(Icons.Default.DeveloperBoard, contentDescription = null, modifier = Modifier.size(16.dp))
                            } else if (sideKey == "subboard") {
                                Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Schema, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            if (currentPhotos.isEmpty()) {
                // Empty Boardview Placeholder with quick actions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TechPrimaryBlue.copy(alpha = 0.15f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DeveloperBoard,
                                        contentDescription = null,
                                        tint = TechPrimaryBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Фото платы пока не загружено",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Для $brand $model ещё никто не добавил фото ${if (selectedSide == "front") "лицевой стороны" else if (selectedSide == "back") "обратной стороны" else "схемы"}. Вы можете загрузить фото первым!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { showAddPhotoDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📷 Загрузить фото платы", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val query = "$brand $model motherboard boardview schematics photo"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}&tbm=isch"))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Поиск фото в Google")
                            }
                        }
                    }
                }
            } else {
                // Display Boardview with Interactive Zoom & Components Callouts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(currentPhotos) { photo ->
                        BoardviewPhotoCard(
                            photo = photo,
                            onDelete = {
                                scope.launch {
                                    dbService.boardviewDao.deleteBoardview(photo.id)
                                    Toast.makeText(context, "Фото удалено", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // Standard Reference Voltage Drops & Diagnostic Points
                    item {
                        ReferenceVoltageCard(brand = brand, model = model)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showAddPhotoDialog) {
        AddBoardviewPhotoDialog(
            brand = brand,
            model = model,
            initialSide = selectedSide,
            onDismiss = { showAddPhotoDialog = false },
            onSave = { title, side, url, desc ->
                scope.launch {
                    val entity = BoardviewPhotoEntity(
                        brand = brand,
                        model = model,
                        title = title,
                        side = side,
                        imageUrl = url,
                        description = desc,
                        addedBy = "Мастер"
                    )
                    dbService.boardviewDao.insertBoardview(entity)
                    showAddPhotoDialog = false
                    Toast.makeText(context, "Фото платы успешно сохранено!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun BoardviewPhotoCard(
    photo: BoardviewPhotoEntity,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = photo.title.ifBlank { "Плата: ${photo.side}" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Добавил: ${photo.addedBy}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zoomable Interactive Photo Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .transformable(state = transformState),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photo.imageUrl,
                    contentDescription = photo.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )

                if (scale > 1.05f) {
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.ZoomOutMap, contentDescription = "Сброс зума", tint = Color.White)
                    }
                }
            }

            if (photo.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📌 ${photo.description}",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReferenceVoltageCard(brand: String, model: String) {
    val isDark = LocalIsDarkTheme.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2638) else Color(0xFFEEF2FF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚡", fontSize = 16.sp)
                Text(
                    text = "Контрольные точки напряжений и падений (Diode Drop)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TechPrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val points = listOf(
                Pair("VBUS 5.0V (Type-C / Lightning)", "0.520 - 0.600 V drop (5.0V активен)"),
                Pair("VBAT / VDD_MAIN", "0.380 - 0.450 V drop (3.8V - 4.2V)"),
                Pair("PMIC 1.8V Always-On (VDD_IO)", "0.410 - 0.480 V drop (1.80V)"),
                Pair("CPU Core / LDO Rail", "0.220 - 0.310 V drop (0.85V - 1.1V)")
            )

            points.forEach { (rail, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = rail, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = value, fontSize = 11.sp, color = TechPrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddBoardviewPhotoDialog(
    brand: String,
    model: String,
    initialSide: String,
    onDismiss: () -> Unit,
    onSave: (title: String, side: String, url: String, desc: String) -> Unit
) {
    var title by remember { mutableStateOf("Плата $brand $model") }
    var side by remember { mutableStateOf(initialSide) }
    var imageUrl by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📷 Добавить фото платы / схему") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = filterDuplicateInput(title, it) },
                    label = { Text("Название / Описание платы") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Side selection
                Text("Сторона платы:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sides = listOf(
                        Pair("front", "Лицевая"),
                        Pair("back", "Обратная"),
                        Pair("subboard", "Субплата"),
                        Pair("schematic", "Схема")
                    )
                    sides.forEach { (key, label) ->
                        FilterChip(
                            selected = side == key,
                            onClick = { side = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = filterDuplicateInput(imageUrl, it) },
                    label = { Text("Прямая ссылка на фото (URL)") },
                    placeholder = { Text("https://... или путь") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = filterDuplicateInput(desc, it) },
                    label = { Text("Заметки (расположение КП, CPU, тестпоинтов)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (imageUrl.isBlank()) return@Button
                    onSave(title, side, imageUrl, desc)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
