package com.example.ui.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.services.CustomHostSyncService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomHostSyncDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hostSyncService = remember { CustomHostSyncService.getInstance(context) }
    val syncStatus by hostSyncService.syncStatus.collectAsState()
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(hostSyncService.getServerUrl()) }
    var apiKey by remember { mutableStateOf(hostSyncService.getApiKey()) }
    var isAutoSync by remember { mutableStateOf(hostSyncService.isAutoSyncEnabled()) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showPhpScriptDialog by remember { mutableStateOf(false) }

    // Настройки корпоративной почты / SMTP
    var corporateEmail by remember { mutableStateOf(hostSyncService.getCorporateEmail()) }
    var corporateSenderName by remember { mutableStateOf(hostSyncService.getCorporateSenderName()) }
    var isCorporateEmailEnabled by remember { mutableStateOf(hostSyncService.isCorporateEmailEnabled()) }

    var isSmtpExpanded by remember { mutableStateOf(false) }
    var isSmtpEnabled by remember { mutableStateOf(hostSyncService.isSmtpEnabled()) }
    var smtpHost by remember { mutableStateOf(hostSyncService.getSmtpHost()) }
    var smtpPort by remember { mutableStateOf(hostSyncService.getSmtpPort().toString()) }
    var smtpUser by remember { mutableStateOf(hostSyncService.getSmtpUser()) }
    var smtpPass by remember { mutableStateOf(hostSyncService.getSmtpPass()) }
    var smtpSecure by remember { mutableStateOf(hostSyncService.getSmtpSecure()) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var testEmailRecipient by remember { mutableStateOf("") }
    var isSendingTestEmail by remember { mutableStateOf(false) }
    var testEmailResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0288D1).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Свой сайт / Хостинг",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "12 ГБ места на вашем сервере",
                                fontSize = 12.sp,
                                color = Color(0xFF0288D1)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Пояснительная карточка
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0288D1).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF0288D1).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Автоматическая синхронизация для всех",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Как только любой мастер публикует пост, историю или статью базы знаний — данные сразу летят на ваш хостинг и становятся видны всем устройствам в сети.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Поле URL хостинга
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        testResult = null
                    },
                    label = { Text("URL скрипта на вашем сайте") },
                    placeholder = { Text(com.example.services.CustomHostSyncService.DEFAULT_SERVER_URL) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF0288D1))
                    },
                    trailingIcon = {
                        if (serverUrl != com.example.services.CustomHostSyncService.DEFAULT_SERVER_URL) {
                            TextButton(
                                onClick = {
                                    serverUrl = com.example.services.CustomHostSyncService.DEFAULT_SERVER_URL
                                    apiKey = com.example.services.CustomHostSyncService.DEFAULT_API_KEY
                                }
                            ) {
                                Text("По умолч.", fontSize = 11.sp, color = Color(0xFF0288D1))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Секретный ключ
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        testResult = null
                    },
                    label = { Text("Секретный ключ (API Key)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFFFA000))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Переключатель автоматической синхронизации
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isAutoSync = !isAutoSync }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Мгновенная авто-отправка на сайт",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Публиковать новые посты и решения сразу на ваш хостинг",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isAutoSync,
                        onCheckedChange = { isAutoSync = it }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Проверка связи (Ping)
                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            testResult = null
                            scope.launch {
                                val res = hostSyncService.testConnection(serverUrl, apiKey)
                                isTestingConnection = false
                                testResult = res
                            }
                        },
                        enabled = serverUrl.isNotBlank() && !isTestingConnection,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Проверить связь", fontSize = 12.sp)
                        }
                    }

                    // Сохранить настройки
                    Button(
                        onClick = {
                            hostSyncService.saveConfig(serverUrl, apiKey, isAutoSync)
                            val port = smtpPort.toIntOrNull() ?: 465
                            hostSyncService.saveCorporateEmailConfig(
                                corporateEmail = corporateEmail,
                                senderName = corporateSenderName,
                                enabled = isCorporateEmailEnabled,
                                smtpEnabled = isSmtpEnabled,
                                smtpHost = smtpHost,
                                smtpPort = port,
                                smtpUser = smtpUser,
                                smtpPass = smtpPass,
                                smtpSecure = smtpSecure
                            )
                            Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить", fontSize = 12.sp)
                    }
                }

                // Результат проверки соединения
                testResult?.let { (success, message) ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (success) Color(0xFF4CAF50).copy(alpha = 0.12f) else Color(0xFFEF5350).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (success) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color(0xFFEF5350).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (success) Color(0xFF388E3C) else Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                fontSize = 12.sp,
                                color = if (success) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка принудительного полного обмена сейчас
                Button(
                    onClick = {
                        scope.launch {
                            hostSyncService.saveConfig(serverUrl, apiKey, isAutoSync)
                            val (success, msg) = hostSyncService.performFullHostSync()
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = serverUrl.isNotBlank() && !syncStatus.isSyncing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (syncStatus.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Синхронизация...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Синхронизировать всё сейчас", fontWeight = FontWeight.Bold)
                    }
                }

                // Статус последней синхронизации
                if (syncStatus.lastSyncTime > 0) {
                    val dateFormat = remember { SimpleDateFormat("d MMMM HH:mm:ss", Locale("ru")) }
                    val dateStr = remember(syncStatus.lastSyncTime) { dateFormat.format(Date(syncStatus.lastSyncTime)) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Последняя синхронизация: $dateStr\n${syncStatus.lastMessage}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // ====================================================
                // СЕКЦИЯ: КОРПОРАТИВНАЯ ПОЧТА ХОСТИНГА ДЛЯ КОДОВ ВХОДА
                // ====================================================
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0288D1).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF0288D1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Корпоративная почта",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Коды авторизации с вашего домена",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF0288D1)
                                    )
                                }
                            }

                            Switch(
                                checked = isCorporateEmailEnabled,
                                onCheckedChange = {
                                    isCorporateEmailEnabled = it
                                    val port = smtpPort.toIntOrNull() ?: 465
                                    hostSyncService.saveCorporateEmailConfig(
                                        corporateEmail = corporateEmail,
                                        senderName = corporateSenderName,
                                        enabled = it,
                                        smtpEnabled = isSmtpEnabled,
                                        smtpHost = smtpHost,
                                        smtpPort = port,
                                        smtpUser = smtpUser,
                                        smtpPass = smtpPass,
                                        smtpSecure = smtpSecure
                                    )
                                }
                            )
                        }

                        Text(
                            text = "Коды подтверждения будут отправляться через почтовую службу вашего хостинга (с адреса вашего домена).",
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = corporateEmail,
                            onValueChange = { corporateEmail = it },
                            label = { Text("Корпоративный Email на хостинге") },
                            placeholder = { Text(com.example.services.CustomHostSyncService.DEFAULT_CORPORATE_EMAIL) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = corporateSenderName,
                            onValueChange = { corporateSenderName = it },
                            label = { Text("Имя отправителя") },
                            placeholder = { Text("Tech.Mate Сервис") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Раскрывающийся блок SMTP (если хостинг требует авторизацию)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isSmtpExpanded = !isSmtpExpanded },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Параметры SMTP (по желанию)",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Icon(
                                        if (isSmtpExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }

                                AnimatedVisibility(visible = isSmtpExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Использовать прямой SMTP:", fontSize = 12.sp)
                                            Switch(
                                                checked = isSmtpEnabled,
                                                onCheckedChange = { isSmtpEnabled = it }
                                            )
                                        }

                                        if (isSmtpEnabled) {
                                            OutlinedTextField(
                                                value = smtpHost,
                                                onValueChange = { smtpHost = it },
                                                label = { Text("SMTP Сервер") },
                                                placeholder = { Text("mail.ваш-домен.рф") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = smtpPort,
                                                    onValueChange = { smtpPort = it },
                                                    label = { Text("Порт") },
                                                    placeholder = { Text("465") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = smtpSecure,
                                                    onValueChange = { smtpSecure = it },
                                                    label = { Text("Шифрование") },
                                                    placeholder = { Text("SSL") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }

                                            OutlinedTextField(
                                                value = smtpUser,
                                                onValueChange = { smtpUser = it },
                                                label = { Text("SMTP Логин / Email") },
                                                placeholder = { Text("support@ваш-домен.рф") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )

                                            OutlinedTextField(
                                                value = smtpPass,
                                                onValueChange = { smtpPass = it },
                                                label = { Text("SMTP Пароль от почты") },
                                                singleLine = true,
                                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                trailingIcon = {
                                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                        Icon(
                                                            if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Кнопка сохранения параметров почты
                        Button(
                            onClick = {
                                val port = smtpPort.toIntOrNull() ?: 465
                                hostSyncService.saveCorporateEmailConfig(
                                    corporateEmail = corporateEmail,
                                    senderName = corporateSenderName,
                                    enabled = isCorporateEmailEnabled,
                                    smtpEnabled = isSmtpEnabled,
                                    smtpHost = smtpHost,
                                    smtpPort = port,
                                    smtpUser = smtpUser,
                                    smtpPass = smtpPass,
                                    smtpSecure = smtpSecure
                                )
                                Toast.makeText(context, "Настройки корпоративной почты сохранены", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранить параметры почты", fontSize = 12.5.sp)
                        }

                        // Блок отправки тестового кода
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Проверка отправки кода на email",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = testEmailRecipient,
                                    onValueChange = { testEmailRecipient = it },
                                    placeholder = { Text("Куда отправить код...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Button(
                                    onClick = {
                                        val clean = testEmailRecipient.trim()
                                        if (clean.isBlank() || !clean.contains("@")) {
                                            Toast.makeText(context, "Введите корректный email", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        scope.launch {
                                            isSendingTestEmail = true
                                            testEmailResult = null
                                            val port = smtpPort.toIntOrNull() ?: 465
                                            hostSyncService.saveCorporateEmailConfig(
                                                corporateEmail = corporateEmail,
                                                senderName = corporateSenderName,
                                                enabled = isCorporateEmailEnabled,
                                                smtpEnabled = isSmtpEnabled,
                                                smtpHost = smtpHost,
                                                smtpPort = port,
                                                smtpUser = smtpUser,
                                                smtpPass = smtpPass,
                                                smtpSecure = smtpSecure
                                            )
                                            hostSyncService.saveConfig(serverUrl, apiKey, isAutoSync)

                                            val testCode = (100000..999999).random().toString()
                                            val res = hostSyncService.sendOtpCodeViaHost(clean, testCode)
                                            testEmailResult = res
                                            isSendingTestEmail = false
                                        }
                                    },
                                    enabled = !isSendingTestEmail && serverUrl.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                                ) {
                                    if (isSendingTestEmail) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Тест", fontSize = 12.sp)
                                    }
                                }
                            }

                            testEmailResult?.let { (success, msg) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (success) Color(0xFF4CAF50).copy(alpha = 0.12f) else Color(0xFFEF5350).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (success) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color(0xFFEF5350).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (success) Color(0xFF388E3C) else Color(0xFFD32F2F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = msg,
                                            fontSize = 11.5.sp,
                                            color = if (success) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Кнопка показа готового PHP-скрипта
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPhpScriptDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF673AB7).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = Color(0xFF673AB7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Получить готовый PHP-скрипт",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Скопируйте 1 файл на ваш хостинг за 1 минуту",
                                fontSize = 11.5.sp,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Модальное окно с готовым PHP-скриптом для хостинга
    if (showPhpScriptDialog) {
        val phpScriptCode = remember(apiKey) { hostSyncService.getPhpScriptTemplate() }

        Dialog(
            onDismissRequest = { showPhpScriptDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF673AB7))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Скрипт techmate_sync.php",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(onClick = { showPhpScriptDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Инструкция (1 минута):\n" +
                               "1. Откройте панель хостинга (cPanel / Beget / TimeWeb / ISPmanager).\n" +
                               "2. В папке сайта (public_html) создайте файл techmate_sync.php.\n" +
                               "3. Вставьте этот код и нажмите «Сохранить».\n" +
                               "4. Скрипт сам создаст папки на ваших 12 ГБ диска и будет хранить всё автоматически!",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Блок кода
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = phpScriptCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color(0xFF81C784)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("techmate_sync.php", phpScriptCode))
                            Toast.makeText(context, "Код скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Скопировать весь PHP-код", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
