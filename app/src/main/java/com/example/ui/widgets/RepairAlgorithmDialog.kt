package com.example.ui.widgets

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechPrimaryBlue

data class RepairStep(
    val stepNumber: Int,
    val icon: String,
    val title: String,
    val description: String
)

data class QCCheckItem(
    val id: String,
    val icon: String,
    val title: String,
    val subtitle: String
)

val standardRepairSteps = listOf(
    RepairStep(1, "📞", "История поломки", "Сбор анамнеза: падение, вода, неоригинальное ЗУ, скачок напряжения, после другого СЦ"),
    RepairStep(2, "🔍", "Внешний осмотр", "Осмотр корпуса, геометрии рамы, разъёмов Type-C/Lightning, кнопок, дисплея, АКБ"),
    RepairStep(3, "✔️", "Подтверждение дефекта", "Проверка заявленной неисправности, ток USB-тестера, поведение на ЛБП до и после PWR"),
    RepairStep(4, "📋", "Проверка доп. неисправностей", "Проверка АКБ, камер, динамиков/микрофонов, сети, Wi-Fi, датчиков FaceID/TouchID"),
    RepairStep(5, "🌐", "Поиск типовых проблем", "Поиск типовых болячек ревизии платы / процессора (отвал CPU, прогар контроллера зарядки)"),
    RepairStep(6, "🛠️", "Разборка устройства", "Термопрогрев сепаратора, аккуратное снятие крышки/модуля, защита шлейфов, винтовая карта"),
    RepairStep(7, "🔬", "Осмотр под микроскопом", "Осмотр платы: компаунд, окислы от влаги, сколы кристаллов, трещины BGA, сбитые SMD"),
    RepairStep(8, "⚡", "Диагностика и поиск причины", "Диодная прозвонка линий (Красный на GND), замеры VBUS, VBAT, дежурок, поиск КЗ тепловизором"),
    RepairStep(9, "🤝", "Согласование ремонта", "Расчет стоимости запчастей, трудоемкости и согласование цены с клиентом"),
    RepairStep(10, "✏️", "Ремонтные работы", "Пайка, реболл BGA микросхем, замена элементов, восстановление пятаков, прошивка"),
    RepairStep(11, "🧹", "Профилактика и чистка", "Замена термопасты/термопрокладок, отмывка в УЗ-ванне / изопропиловым спиртом, сушка"),
    RepairStep(12, "📋", "Проверка всех функций", "Полный тест-лист под нагрузкой, проверка зарядки, энергопотребления в спящем режиме")
)

val standardQCList = listOf(
    QCCheckItem("qc_power", "⚡", "Зарядка и АКБ", "Ток 5V/9V/12V на USB-тестере, индикация % и спящий режим (< 0.01A)"),
    QCCheckItem("qc_display", "📱", "Дисплей и тачскрин", "Отклик сенсора по всей сетке/углам, автояркость, тест *#0*#"),
    QCCheckItem("qc_cellular", "📶", "Связь и звонки", "Регистрация 4G LTE/5G, тестовый звонок 112/оператору"),
    QCCheckItem("qc_audio", "🔊", "Динамики и микрофоны", "Слуховой/полифонический динамик, нижний и верхний микрофоны шумодава"),
    QCCheckItem("qc_wireless", "📡", "Беспроводные модули", "Wi-Fi 2.4/5GHz, Bluetooth аудио, GPS навигатор, NFC оплата"),
    QCCheckItem("qc_camera", "📷", "Камеры и вспышка", "Основная, широкоугольная, селфи, автофокусировка и фонарик"),
    QCCheckItem("qc_biometrics", "🔒", "Биометрия и датчики", "Датчик приближения при звонке, Touch ID / Face ID, гироскоп"),
    QCCheckItem("qc_buttons", "🔘", "Кнопки и тактильность", "Кнопки Power, Vol +/-, переключатель Mute, вибромотор"),
    QCCheckItem("qc_stress", "🌡️", "Стресс-тест под нагрузкой", "10 минут видео Full HD / 3D нагрузки без троттлинга и выключений")
)

@Composable
fun RepairAlgorithmDialog(
    onDismiss: () -> Unit,
    onInsertStepToChat: (String) -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = 12 Правил, 1 = Чек-лист после ремонта
    var checkedSteps by remember { mutableStateOf(setOf<Int>()) }
    var checkedQC by remember { mutableStateOf(setOf<String>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = if (isDark) Color(0xFF101923) else Color(0xFFF8FAFC),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1E3A5F), Color(0xFF0F2744), Color(0xFF229ED9))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🛠️", fontSize = 22.sp)
                                Column {
                                    Text(
                                        text = "СТАНДАРТ РЕМОНТА МАСТЕРА",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "12 правил диагностики + Чек-лист проверки",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Golden Master Rule Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0A121A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TechGoldTestPoint)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "⚠️", fontSize = 18.sp)
                                Column {
                                    Text(
                                        text = "ГЛАВНОЕ ПРАВИЛО МАСТЕРА:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TechGoldTestPoint
                                    )
                                    Text(
                                        text = "СНАЧАЛА ПОНЯТЬ ПРИЧИНУ — ПОТОМ ПАЯТЬ!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Tabs: 12 Rules vs Post-Repair QC
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = if (isDark) Color(0xFF131D2A) else Color(0xFFEEF2F6),
                    contentColor = TechPrimaryBlue
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "12 Правил мастера (${checkedSteps.size}/12)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Чек-лист QC (${checkedQC.size}/${standardQCList.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                if (selectedTab == 0) {
                    // Progress Bar
                    val progress = checkedSteps.size.toFloat() / standardRepairSteps.size
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = TechGoldTestPoint,
                        trackColor = Color(0xFF1E293B)
                    )

                    // List of 12 steps
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(standardRepairSteps) { _, step ->
                            val isChecked = checkedSteps.contains(step.stepNumber)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isChecked) {
                                    if (isDark) Color(0xFF132D20) else Color(0xFFDCFCE7)
                                } else {
                                    if (isDark) Color(0xFF182330) else Color.White
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isChecked) Color(0xFF22C55E) else if (isDark) Color(0xFF2B3D52) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checkedSteps = if (isChecked) {
                                            checkedSteps - step.stepNumber
                                        } else {
                                            checkedSteps + step.stepNumber
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Step Number Badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (isChecked) Color(0xFF22C55E) else TechGoldTestPoint,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isChecked) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text(
                                                text = "${step.stepNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = step.icon, fontSize = 14.sp)
                                            Text(
                                                text = step.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDark) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                        Text(
                                            text = step.description,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            lineHeight = 14.sp
                                        )
                                    }

                                    // Quick Ask Button
                                    IconButton(
                                        onClick = {
                                            onInsertStepToChat("Помоги выполнить шаг ${step.stepNumber}: ${step.title}. На что обратить внимание и как правильно протестировать?")
                                            onDismiss()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = "Ask AI",
                                            tint = TechPrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Handover Step
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF251A0A) else Color(0xFFFEF3C7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TechGoldTestPoint.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎁", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "ВЫДАЧА КЛИЕНТУ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TechGoldTestPoint
                                        )
                                        Text(
                                            text = "Оформление гарантийного талона, рекомендации по зарядке и эксплуатации",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF78350F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Post-Repair QC Checklist View
                    val qcProgress = checkedQC.size.toFloat() / standardQCList.size
                    LinearProgressIndicator(
                        progress = { qcProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF22C55E),
                        trackColor = Color(0xFF1E293B)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(standardQCList) { _, qcItem ->
                            val isChecked = checkedQC.contains(qcItem.id)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isChecked) {
                                    if (isDark) Color(0xFF132D20) else Color(0xFFDCFCE7)
                                } else {
                                    if (isDark) Color(0xFF182330) else Color.White
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isChecked) Color(0xFF22C55E) else if (isDark) Color(0xFF2B3D52) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checkedQC = if (isChecked) {
                                            checkedQC - qcItem.id
                                        } else {
                                            checkedQC + qcItem.id
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            checkedQC = if (isChecked) {
                                                checkedQC - qcItem.id
                                            } else {
                                                checkedQC + qcItem.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF22C55E),
                                            uncheckedColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                        )
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = qcItem.icon, fontSize = 14.sp)
                                            Text(
                                                text = qcItem.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDark) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                        Text(
                                            text = qcItem.subtitle,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Surface
                Surface(
                    color = if (isDark) Color(0xFF0F1723) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTab == 0) {
                            Text(
                                text = "Шагов: ${checkedSteps.size} из 12",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (checkedSteps.size == 12) Color(0xFF22C55E) else TechPrimaryBlue
                            )
                        } else {
                            Text(
                                text = "Проверено: ${checkedQC.size} из ${standardQCList.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (checkedQC.size == standardQCList.size) Color(0xFF22C55E) else Color(0xFF22C55E)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedTab == 1 && checkedQC.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        val report = buildString {
                                            append("📋 **ЧЕК-ЛИСТ ПРОВЕРКИ РАБОТОСПОСОБНОСТИ (${checkedQC.size}/${standardQCList.size})**:\n")
                                            standardQCList.forEach { item ->
                                                val done = checkedQC.contains(item.id)
                                                append(if (done) "✅ " else "⬜ ")
                                                append("${item.icon} **${item.title}**: ${item.subtitle}\n")
                                            }
                                        }
                                        onInsertStepToChat(report)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("В чат", fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue)
                            ) {
                                Text("Готово", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
