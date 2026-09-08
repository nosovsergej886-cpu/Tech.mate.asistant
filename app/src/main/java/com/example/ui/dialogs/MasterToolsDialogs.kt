package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechPrimaryBlue
import com.example.ui.util.filterDuplicateInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Technician Master Calculator Dialog
 * Supports: Ohm's Law, Voltage Divider, Power, Standard Math Calculator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechCalculatorDialog(
    onDismiss: () -> Unit,
    onInsertToChat: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Закон Ома, 1 = Делитель, 2 = Мощность, 3 = Калькулятор, 4 = Диодная прозвонка

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = TechPrimaryBlue)
                Text("⚡ Инструменты мастера", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Закон Ома", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Делитель R", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Мощность P", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Обычный", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Прозвонка mV", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> OhmsLawCalculator(onInsertToChat = {
                            onInsertToChat(it)
                            onDismiss()
                        })
                        1 -> VoltageDividerCalculator(onInsertToChat = {
                            onInsertToChat(it)
                            onDismiss()
                        })
                        2 -> PowerCalculator(onInsertToChat = {
                            onInsertToChat(it)
                            onDismiss()
                        })
                        3 -> SimpleMathCalculator(onInsertToChat = {
                            onInsertToChat(it)
                            onDismiss()
                        })
                        4 -> DiodeModeReferenceSection(onInsertToChat = {
                            onInsertToChat(it)
                            onDismiss()
                        })
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
private fun DiodeModeReferenceSection(onInsertToChat: (String) -> Unit) {
    val references = listOf(
        "VBUS 5V Type-C" to "520 - 680 mV (КЗ при <100 mV, Обрыв >1200 mV)",
        "VBAT / VPH_PWR" to "380 - 450 mV (Норма)",
        "Type-C CC1 / CC2" to "580 - 640 mV (Защитные диоды OVP)",
        "USB D+ / D- Data" to "400 - 550 mV (Разница между линиями <15 mV)",
        "Шина I2C (SCL / SDA)" to "450 - 520 mV (В подтяжке 1.8V)",
        "Линии дисплея MIPI" to "320 - 420 mV (Одинаковое значение на всех парах)",
        "Сброс дисплея RESX" to "500 - 600 mV",
        "Питание Touch VDD 3.0V" to "480 - 550 mV"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("📐 Эталонные падения напряжения (Диодная прозвонка, Красный щуп на GND):", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

        references.forEach { (line, value) ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().clickable {
                    onInsertToChat("📐 [Диодная прозвонка] $line ➔ $value")
                }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TechPrimaryBlue)
                        Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(Icons.Default.Send, contentDescription = null, tint = TechPrimaryBlue, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OhmsLawCalculator(onInsertToChat: (String) -> Unit) {
    var voltageInput by remember { mutableStateOf("5.0") }
    var currentInput by remember { mutableStateOf("1.5") }
    var resistanceInput by remember { mutableStateOf("") }

    val v = voltageInput.toDoubleOrNull()
    val i = currentInput.toDoubleOrNull()
    val r = resistanceInput.toDoubleOrNull()

    val calculatedRes = remember(v, i, r) {
        when {
            v != null && i != null && i != 0.0 -> {
                val calcR = ((v / i) * 100).roundToInt() / 100.0
                "Сопротивление R = V / I = $calcR Ом"
            }
            v != null && r != null && r != 0.0 -> {
                val calcI = ((v / r) * 1000).roundToInt() / 1000.0
                "Ток I = V / R = $calcI А (${(calcI * 1000).toInt()} мА)"
            }
            i != null && r != null -> {
                val calcV = ((i * r) * 100).roundToInt() / 100.0
                "Напряжение V = I * R = $calcV В"
            }
            else -> "Заполните любые 2 параметра для расчёта третьего"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Расчёт V (В), I (А), R (Ом):", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        OutlinedTextField(
            value = voltageInput,
            onValueChange = { voltageInput = filterDuplicateInput(voltageInput, it) },
            label = { Text("Напряжение V (Вольт)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = currentInput,
            onValueChange = { currentInput = filterDuplicateInput(currentInput, it) },
            label = { Text("Ток I (Ампер)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = resistanceInput,
            onValueChange = { resistanceInput = filterDuplicateInput(resistanceInput, it) },
            label = { Text("Сопротивление R (Ом)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = TechPrimaryBlue.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Результат:", fontWeight = FontWeight.Bold, color = TechPrimaryBlue, fontSize = 12.sp)
                Text(calculatedRes, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Button(
            onClick = { onInsertToChat("📊 [Закон Ома] $calculatedRes") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Вставить результат в чат")
        }
    }
}

@Composable
private fun VoltageDividerCalculator(onInsertToChat: (String) -> Unit) {
    var vInInput by remember { mutableStateOf("5.0") }
    var r1Input by remember { mutableStateOf("10000") }
    var r2Input by remember { mutableStateOf("22000") }

    val vin = vInInput.toDoubleOrNull() ?: 0.0
    val r1 = r1Input.toDoubleOrNull() ?: 0.0
    val r2 = r2Input.toDoubleOrNull() ?: 0.0

    val vOut = if (r1 + r2 > 0) {
        ((vin * (r2 / (r1 + r2))) * 1000).roundToInt() / 1000.0
    } else 0.0

    val resText = "При Vin = $vin В, R1 = $r1 Ом, R2 = $r2 Ом ➔ Vout = $vOut В"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Формула: Vout = Vin * (R2 / (R1 + R2))", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        OutlinedTextField(
            value = vInInput,
            onValueChange = { vInInput = filterDuplicateInput(vInInput, it) },
            label = { Text("Входное Vin (Вольт)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = r1Input,
            onValueChange = { r1Input = filterDuplicateInput(r1Input, it) },
            label = { Text("Плечо R1 (Ом)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = r2Input,
            onValueChange = { r2Input = filterDuplicateInput(r2Input, it) },
            label = { Text("Плечо R2 на землю (Ом)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = TechGoldTestPoint.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Выходное напряжение Vout:", fontWeight = FontWeight.Bold, color = Color(0xFFD87000), fontSize = 12.sp)
                Text("$vOut В", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Button(
            onClick = { onInsertToChat("📐 [Делитель напряжения] $resText") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Вставить результат в чат")
        }
    }
}

@Composable
private fun PowerCalculator(onInsertToChat: (String) -> Unit) {
    var vInput by remember { mutableStateOf("5.0") }
    var iInput by remember { mutableStateOf("2.0") }

    val v = vInput.toDoubleOrNull() ?: 0.0
    val i = iInput.toDoubleOrNull() ?: 0.0
    val p = ((v * i) * 100).roundToInt() / 100.0

    val resText = "При V = $v В и I = $i А ➔ Мощность P = $p Вт (W)"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Мощность P = V * I (Ватт)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        OutlinedTextField(
            value = vInput,
            onValueChange = { vInput = filterDuplicateInput(vInput, it) },
            label = { Text("Напряжение V (В)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = iInput,
            onValueChange = { iInput = filterDuplicateInput(iInput, it) },
            label = { Text("Ток I (А)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Рассеиваемая мощность:", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 12.sp)
                Text("$p Ватт (W)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Button(
            onClick = { onInsertToChat("💡 [Расчёт мощности] $resText") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Вставить результат в чат")
        }
    }
}

@Composable
private fun SimpleMathCalculator(onInsertToChat: (String) -> Unit) {
    var expr by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }

    fun append(char: String) {
        expr += char
        try {
            val eval = evaluateExpression(expr)
            if (eval != null) result = eval.toString()
        } catch (_: Exception) {}
    }

    fun clear() {
        expr = ""
        result = "0"
    }

    fun backspace() {
        if (expr.isNotEmpty()) {
            expr = expr.substring(0, expr.length - 1)
            try {
                val eval = evaluateExpression(expr)
                if (eval != null) result = eval.toString() else if (expr.isEmpty()) result = "0"
            } catch (_: Exception) {}
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
                Text(text = expr.ifBlank { "0" }, fontSize = 14.sp, color = Color.Gray)
                Text(text = result, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        val buttons = listOf(
            listOf("C", "⌫", "/", "*"),
            listOf("7", "8", "9", "-"),
            listOf("4", "5", "6", "+"),
            listOf("1", "2", "3", "="),
            listOf("0", ".", "(", ")")
        )

        for (row in buttons) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (btn in row) {
                    Button(
                        onClick = {
                            when (btn) {
                                "C" -> clear()
                                "⌫" -> backspace()
                                "=" -> {
                                    val eval = evaluateExpression(expr)
                                    if (eval != null) {
                                        result = eval.toString()
                                        expr = result
                                    }
                                }
                                else -> append(btn)
                            }
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = if (btn in listOf("/", "*", "-", "+", "=")) ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue)
                        else if (btn in listOf("C", "⌫")) ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                        else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text(btn, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = { onInsertToChat("🧮 [Расчёт] $expr = $result") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Отправить вычисление")
        }
    }
}

private fun evaluateExpression(expr: String): Double? {
    if (expr.isBlank()) return null
    return try {
        val clean = expr.replace("×", "*").replace("÷", "/")
        val tokens = clean.split("+", "-", "*", "/")
        if (tokens.size == 1) return tokens[0].toDoubleOrNull()
        // Simple evaluation logic for basic math
        var res = tokens[0].trim().toDoubleOrNull() ?: 0.0
        var opIdx = 0
        for (i in 0 until clean.length) {
            val ch = clean[i]
            if (ch == '+' || ch == '-' || ch == '*' || ch == '/') {
                opIdx++
                val nextVal = tokens.getOrNull(opIdx)?.trim()?.toDoubleOrNull() ?: 0.0
                when (ch) {
                    '+' -> res += nextVal
                    '-' -> res -= nextVal
                    '*' -> res *= nextVal
                    '/' -> if (nextVal != 0.0) res /= nextVal
                }
            }
        }
        ((res * 1000).roundToInt() / 1000.0)
    } catch (_: Exception) {
        null
    }
}

/**
 * Client Repair Report Generator Dialog (Акт выполненных работ для клиента)
 */
@Composable
fun ClientReportDialog(
    chatTitle: String,
    lastAiMessage: String?,
    onDismiss: () -> Unit,
    onInsertToChat: (String) -> Unit
) {
    val context = LocalContext.current
    var clientName by remember { mutableStateOf("Клиент") }
    var orderNumber by remember { mutableStateOf("№" + (1000..9999).random()) }
    var deviceModel by remember { mutableStateOf(if (chatTitle.startsWith("Чат")) "Samsung A51" else chatTitle) }
    var problemDesc by remember { mutableStateOf("Не заряжается, просадка VBUS") }
    var workDone by remember { mutableStateOf("Восстановление межплатного коннектора, зачистка окислов Type-C, замена защитного диода.") }
    var partCost by remember { mutableStateOf("450") }
    var laborCost by remember { mutableStateOf("1500") }
    var warrantyDays by remember { mutableStateOf("30") }

    val totalCost = remember(partCost, laborCost) {
        (partCost.toIntOrNull() ?: 0) + (laborCost.toIntOrNull() ?: 0)
    }

    val generatedReport = remember(clientName, orderNumber, deviceModel, problemDesc, workDone, partCost, laborCost, totalCost, warrantyDays) {
        """
📋 **АКТ ВЫПОЛНЕННЫХ РАБОТ / ОТЧЁТ КЛИЕНТУ**
----------------------------------------
📌 Заказ: $orderNumber
👤 Заказчик: $clientName
📱 Устройство: $deviceModel
----------------------------------------
🛠 **Заявленная проблема:**
$problemDesc

✅ **Проведённые работы и диагностика:**
$workDone

💰 **Расчёт стоимости:**
• Детали / Компоненты: $partCost ₽
• Работа мастера: $laborCost ₽
🏷 **ИТОГО К ОПЛАТЕ: $totalCost ₽**

🛡 **Гарантия сервиса:** $warrantyDays дней
----------------------------------------
Спасибо за обращение в наш сервисный центр!
        """.trimIndent()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = TechPrimaryBlue)
                Text("📄 Отчёт для клиента", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = orderNumber,
                        onValueChange = { orderNumber = filterDuplicateInput(orderNumber, it) },
                        label = { Text("Заказ №") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = filterDuplicateInput(clientName, it) },
                        label = { Text("Клиент") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = filterDuplicateInput(deviceModel, it) },
                    label = { Text("Модель устройства") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = problemDesc,
                    onValueChange = { problemDesc = filterDuplicateInput(problemDesc, it) },
                    label = { Text("Неисправность") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = workDone,
                    onValueChange = { workDone = filterDuplicateInput(workDone, it) },
                    label = { Text("Выполненные работы") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = partCost,
                        onValueChange = { partCost = filterDuplicateInput(partCost, it) },
                        label = { Text("Запчасти (₽)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = laborCost,
                        onValueChange = { laborCost = filterDuplicateInput(laborCost, it) },
                        label = { Text("Работа (₽)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Итого к оплате:", fontWeight = FontWeight.Bold)
                        Text("$totalCost ₽", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TechPrimaryBlue)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Client Report", generatedReport))
                        Toast.makeText(context, "Отчёт скопирован!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Копировать")
                }

                Button(
                    onClick = {
                        onInsertToChat(generatedReport)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("В чат")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Voice Note Recorder Dialog
 */
@Composable
fun VoiceRecorderDialog(
    onDismiss: () -> Unit,
    onSendVoiceNote: (String) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }
    var voiceText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    val quickPhrases = listOf(
        "Проверь VBUS 5V на коннекторе",
        "Не заряжается после воды, прозвони межплатку",
        "Устройство греется возле процессора",
        "Пробовали подкидывать заведомо рабочий АКБ"
    )

    LaunchedEffect(isRecording) {
        if (isRecording) {
            secondsElapsed = 0
            while (isRecording) {
                delay(1000)
                secondsElapsed++
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Red)
                Text("🎙 Голосовая заметка", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Recording visualizer
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) Color.Red.copy(alpha = 0.2f * pulseScale) else TechPrimaryBlue.copy(alpha = 0.15f))
                        .clickable { isRecording = !isRecording },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = if (isRecording) Color.Red else TechPrimaryBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = if (isRecording) "Запись... 00:${secondsElapsed.toString().padStart(2, '0')}" else "Нажмите на микрофон для записи",
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = voiceText,
                    onValueChange = { voiceText = filterDuplicateInput(voiceText, it) },
                    placeholder = { Text("Текст голосовой заметки или надиктуйте...") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Text("Быстрые шаблоны голосом:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    quickPhrases.forEach { phrase ->
                        AssistChip(
                            onClick = { voiceText = phrase },
                            label = { Text(phrase, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMsg = if (voiceText.isNotBlank()) "🎙 [Голосовая заметка]: $voiceText" else "🎙 [Голосовая заметка 00:${secondsElapsed.toString().padStart(2, '0')}]"
                    onSendVoiceNote(finalMsg)
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Отправить в чат")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
