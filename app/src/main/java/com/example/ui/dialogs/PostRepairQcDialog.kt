package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TechGreen
import com.example.ui.theme.TechPrimaryBlue
import com.example.ui.util.filterDuplicateInput

data class QcCheckItem(
    val title: String,
    val description: String,
    val icon: String,
    var isChecked: Boolean = false,
    var notes: String = ""
)

fun getDefaultQcChecklist(deviceName: String = "Устройство"): List<QcCheckItem> {
    return listOf(
        QcCheckItem(
            title = "Зарядка и АКБ (QC / PD)",
            description = "Ток зарядки по USB-тестеру (5V/9V), потребление в спящем режиме < 0.01A, отсутствие нагрева разъема.",
            icon = "⚡"
        ),
        QcCheckItem(
            title = "Дисплей и сетка тачскрина",
            description = "Проверка отклика по всем углам и клавиатуре, отсутствие полос, битых пикселей и фантомных нажатий.",
            icon = "📱"
        ),
        QcCheckItem(
            title = "Связь 112 и динамики",
            description = "Тестовый вызов на 112/оператору, проверка громкости слухового разговорного и полифонического динамиков.",
            icon = "📶"
        ),
        QcCheckItem(
            title = "Микрофоны и шумоподавление",
            description = "Запись голосового сообщения, проверка нижнего микрофона и верхнего микрофона шумоподавления (видеокамера).",
            icon = "🎙️"
        ),
        QcCheckItem(
            title = "Беспроводные модули (Wi-Fi / BT / GPS / NFC)",
            description = "Подключение к сетям 2.4/5GHz, поиск Bluetooth устройств, геолокация на карте, бесконтактная оплата.",
            icon = "📡"
        ),
        QcCheckItem(
            title = "Камеры и фонарик",
            description = "Основная камера (автофокус), широкоугольная, фронтальная селфи, срабатывание светодиодной вспышки.",
            icon = "📷"
        ),
        QcCheckItem(
            title = "Биометрия и датчики",
            description = "Датчик приближения при звонке (экран тухнет), сканер отпечатка пальца / Face ID, акселерометр/гироскоп.",
            icon = "🔒"
        ),
        QcCheckItem(
            title = "Физические кнопки и вибро",
            description = "Кнопки Power, Vol +/- (четкий щелчок), тактильный вибромотор Taptic Engine / линейный вибро.",
            icon = "🔘"
        ),
        QcCheckItem(
            title = "Стресс-тест под нагрузкой (10 мин)",
            description = "Воспроизведение видео в высоком разрешении, проверка температурной стабильности платы без троттлинга.",
            icon = "🌡️"
        )
    )
}

@Composable
fun PostRepairQcCard(
    deviceName: String = "Устройство",
    onOpenFullChecklist: () -> Unit,
    onPasteToChat: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var checkedCount by remember { mutableIntStateOf(0) }
    val totalCount = 9

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF132F23) else Color(0xFFE8F5E9)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🏁", fontSize = 16.sp)
                    Column {
                        Text(
                            text = "Чек-лист проверки после ремонта",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                        )
                        Text(
                            text = "Обязательный контроль качества (QC)",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFBBF7D0) else Color(0xFF22C55E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { 1.0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF22C55E),
                    trackColor = if (isDark) Color(0xFF1F4D36) else Color(0xFFC8E6C9)
                )
                Text(
                    text = "9 узлов контроля",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenFullChecklist,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Интерактивный тест", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (onPasteToChat != null) {
                    OutlinedButton(
                        onClick = {
                            val report = buildString {
                                appendLine("🏁 **РЕЗУЛЬТАТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА ($deviceName)**:")
                                appendLine("✅ 1. Зарядка и АКБ: QC/PD норма, ток сна < 0.01A")
                                appendLine("✅ 2. Дисплей и тачскрин: Отклик по всей сетке 100%")
                                appendLine("✅ 3. Связь и звук: Звонок 112, микрофоны и динамики в норме")
                                appendLine("✅ 4. Wi-Fi / Bluetooth / GPS / NFC: Сигнал устойчивый")
                                appendLine("✅ 5. Камеры и фонарик: Фокусировка и вспышка OK")
                                appendLine("✅ 6. Датчики и биометрия: Приближение / FaceID OK")
                                appendLine("✅ 7. Стресс-тест 10 мин: Температурная норма")
                                appendLine("Готов к выдаче клиенту!")
                            }
                            onPasteToChat(report)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("📋 В акт", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PostRepairQcDialog(
    deviceName: String = "Устройство",
    onDismiss: () -> Unit,
    onInsertReportToChat: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val items = remember { mutableStateListOf<QcCheckItem>().apply { addAll(getDefaultQcChecklist(deviceName)) } }

    val checkedCount = items.count { it.isChecked }
    val totalCount = items.size
    val progress = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🏁", fontSize = 22.sp)
                        Column {
                            Text(
                                text = "Чек-лист проверки после ремонта",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = deviceName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TechPrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF132F23) else Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Прогресс проверки:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                            )
                            Text(
                                text = "$checkedCount / $totalCount выполнено (${(progress * 100).toInt()}%)",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (checkedCount == totalCount) Color(0xFF16A34A) else TechPrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (checkedCount == totalCount) Color(0xFF16A34A) else TechPrimaryBlue,
                            trackColor = if (isDark) Color(0xFF1F4D36) else Color(0xFFC8E6C9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick buttons: "Выбрать все" / "Сбросить"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            items.indices.forEach { idx ->
                                items[idx] = items[idx].copy(isChecked = true)
                            }
                        }
                    ) {
                        Text("✅ Отметить все", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }

                    TextButton(
                        onClick = {
                            items.indices.forEach { idx ->
                                items[idx] = items[idx].copy(isChecked = false)
                            }
                        }
                    ) {
                        Text("Сбросить отметки", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Checklist Items
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        var isExpanded by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (item.isChecked) {
                                if (isDark) Color(0xFF143324) else Color(0xFFF0FDF4)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    items[index] = item.copy(isChecked = !item.isChecked)
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked ->
                                            items[index] = item.copy(isChecked = checked)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E))
                                    )

                                    Text(text = item.icon, fontSize = 18.sp)

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (item.isChecked) {
                                                if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                                            } else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (item.notes.isNotBlank()) {
                                    Text(
                                        text = "📝 Примечание: ${item.notes}",
                                        fontSize = 11.sp,
                                        color = TechPrimaryBlue,
                                        modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val report = generateReportText(deviceName, items)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("QC Report", report))
                            Toast.makeText(context, "Чек-лист скопирован в буфер!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Копировать", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val report = generateReportText(deviceName, items)
                            onInsertReportToChat?.invoke(report)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Вставить в чат", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun generateReportText(deviceName: String, items: List<QcCheckItem>): String {
    val checked = items.count { it.isChecked }
    val total = items.size
    return buildString {
        appendLine("🏁 **АКТ ПРОВЕРКИ РАБОТОСПОСОБНОСТИ ПОСЛЕ РЕМОНТА**")
        appendLine("📱 **Устройство:** $deviceName")
        appendLine("📊 **Результат QC:** $checked из $total тестов пройдено")
        appendLine("---")
        items.forEach { item ->
            val mark = if (item.isChecked) "✅" else "⚠️"
            val noteStr = if (item.notes.isNotBlank()) " (${item.notes})" else ""
            appendLine("$mark ${item.icon} **${item.title}**: ${if (item.isChecked) "Исправно" else "Не проверено"}$noteStr")
        }
        appendLine("---")
        appendLine(if (checked == total) "✨ **Устройство полностью протестировано и готово к выдаче клиенту.**" else "⚠️ **Рекомендуется завершить непройденные тесты перед выдачей.**")
    }
}
