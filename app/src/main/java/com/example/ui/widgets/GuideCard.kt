package com.example.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GuideData
import com.example.services.LanguageService
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechGreen
import com.example.ui.theme.TelegramHeader

@Composable
fun GuideCard(
    guide: GuideData,
    onSaveToKb: (() -> Unit)? = null,
    isAlreadySaved: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevronRotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Telegram blue left border 3dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(TelegramHeader)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                // Header with Device & Problem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val detectedBrand = guide.device.split(" ").firstOrNull() ?: "Device"
                        BrandLogoIcon(
                            brand = detectedBrand,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = guide.device,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = guide.problem,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TelegramHeader
                            )
                        }
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TelegramHeader,
                            modifier = Modifier.rotate(chevronRotation)
                        )
                    }
                }

                // Badges row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Difficulty Badge
                    val diffColor = when (guide.difficulty.lowercase()) {
                        "легко", "easy" -> Color(0xFF388E3C)
                        "сложно", "hard" -> Color(0xFFD32F2F)
                        else -> Color(0xFFF57C00)
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = diffColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚠️ ${guide.difficulty}",
                            style = MaterialTheme.typography.labelSmall,
                            color = diffColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Time Estimate
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TelegramHeader.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "⏱ ${guide.timeEstimate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TelegramHeader,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Tools Section
                        if (guide.tools.isNotEmpty()) {
                            Text(
                                text = "📋 " + LanguageService.getString("tools"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.padding(top = 4.dp, start = 4.dp)) {
                                guide.tools.forEach { tool ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = TechGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = tool,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Causes Section
                        if (guide.causes.isNotEmpty()) {
                            Text(
                                text = "🎯 " + LanguageService.getString("causes"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            guide.causes.forEachIndexed { index, cause ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}️⃣ ${cause.description}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${cause.probability}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TelegramHeader,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { (cause.probability / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .padding(vertical = 2.dp),
                                            color = TelegramHeader,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            text = "🔍 Проверка: ${cause.checkMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "⚡ Норма: ${cause.normalValue}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "🛠 Решение: ${cause.fixMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TechGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Steps Section
                        if (guide.steps.isNotEmpty()) {
                            Text(
                                text = "🔧 " + LanguageService.getString("steps"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            guide.steps.forEach { step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(TelegramHeader),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${step.stepNumber}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = step.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = step.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (!step.imageUrl.isNullOrBlank()) {
                                            var showStepZoom by remember { mutableStateOf(false) }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TechAsyncImage(
                                                imageUrl = step.imageUrl,
                                                contentDescription = step.title,
                                                title = step.title,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { showStepZoom = true },
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                            if (showStepZoom) {
                                                InteractiveImageViewerDialog(
                                                    imageUrl = step.imageUrl,
                                                    title = step.title,
                                                    subtitle = "${guide.device} • Шаг ${step.stepNumber}",
                                                    onDismiss = { showStepZoom = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Pro Tip
                        val tip = guide.proTip
                        if (!tip.isNullOrEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = TechGoldTestPoint.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("💡", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = LanguageService.getString("pro_tip"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = tip,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // Save to KB Button
                        if (onSaveToKb != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onSaveToKb,
                                enabled = !isAlreadySaved,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAlreadySaved) TechGreen else TelegramHeader
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAlreadySaved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAlreadySaved) "Сохранено в базу" else LanguageService.getString("save_to_kb")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
