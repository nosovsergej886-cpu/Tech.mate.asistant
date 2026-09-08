package com.example.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechPrimaryBlue

/**
 * Ultra-crisp interactive Vector Boardview & Motherboard Component.
 * Automatically renders a high-precision digital schematic with:
 * - FR-4 Solder Mask textured circuit board
 * - Silk-screen copper routing and SMD pads
 * - BGA Main Processor (CPU), Flash Memory (eMMC/UFS), Power PMIC
 * - Battery FPC Connector with VBAT / GND pins
 * - Pulsing Golden TestPoint Target (TP1) and GND Ground Probe route
 */
@Composable
fun TechBoardviewCanvas(
    deviceTitle: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    showInteractiveProbe: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val cleanTitle = remember(deviceTitle) {
        deviceTitle.replace("Тестпоинт", "")
            .replace("TestPoint", "")
            .replace("Фото", "")
            .trim()
            .ifBlank { "Mobile PCB Boardview" }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0D1520) else Color(0xFF1E293B))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. PCB Base Board Outline (FR-4 Dark Teal/Green Solder Mask)
            val pcbMargin = 16f
            val pcbWidth = w - pcbMargin * 2
            val pcbHeight = h - pcbMargin * 2

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = if (isDark) listOf(Color(0xFF0F262C), Color(0xFF08191E), Color(0xFF0A1E24))
                    else listOf(Color(0xFF133E48), Color(0xFF0D2B32)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                topLeft = Offset(pcbMargin, pcbMargin),
                size = Size(pcbWidth, pcbHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Board Edge Ground Copper Pour Border
            drawRoundRect(
                color = Color(0xFF1E555C).copy(alpha = 0.6f),
                topLeft = Offset(pcbMargin + 4f, pcbMargin + 4f),
                size = Size(pcbWidth - 8f, pcbHeight - 8f),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 2f)
            )

            // 2. Silk-Screen Coordinate Grid / Electronic Scale
            val gridStep = 40f
            val gridColor = Color(0xFF00E5FF).copy(alpha = 0.08f)
            var x = pcbMargin + 10f
            while (x < w - pcbMargin) {
                drawLine(gridColor, Offset(x, pcbMargin), Offset(x, h - pcbMargin), strokeWidth = 1f)
                x += gridStep
            }
            var y = pcbMargin + 10f
            while (y < h - pcbMargin) {
                drawLine(gridColor, Offset(pcbMargin, y), Offset(w - pcbMargin, y), strokeWidth = 1f)
                y += gridStep
            }

            // 3. Copper Trace Tracks (BGA Routing to Test Points)
            val copperColor = Color(0xFF2A8B9A).copy(alpha = 0.5f)
            val tracePath = Path().apply {
                moveTo(w * 0.45f, h * 0.45f)
                lineTo(w * 0.45f, h * 0.32f)
                lineTo(w * 0.62f, h * 0.32f)
                lineTo(w * 0.62f, h * 0.28f)

                moveTo(w * 0.48f, h * 0.55f)
                lineTo(w * 0.48f, h * 0.68f)
                lineTo(w * 0.30f, h * 0.68f)
                lineTo(w * 0.30f, h * 0.76f)

                moveTo(w * 0.60f, h * 0.50f)
                lineTo(w * 0.78f, h * 0.50f)
                lineTo(w * 0.78f, h * 0.65f)
            }
            drawPath(tracePath, copperColor, style = Stroke(width = 2.5f))

            // 4. Large CPU / AP BGA Package (Center)
            val cpuX = w * 0.32f
            val cpuY = h * 0.38f
            val cpuW = w * 0.36f
            val cpuH = h * 0.24f

            drawRoundRect(
                color = Color(0xFF060B0E),
                topLeft = Offset(cpuX, cpuY),
                size = Size(cpuW, cpuH),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                topLeft = Offset(cpuX, cpuY),
                size = Size(cpuW, cpuH),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1.5f)
            )

            // Pin 1 Index Dot
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 3.5f,
                center = Offset(cpuX + 10f, cpuY + 10f)
            )

            // 5. PMIC / Power Management IC (Top Left)
            val pmicX = w * 0.15f
            val pmicY = h * 0.22f
            val pmicW = w * 0.20f
            val pmicH = h * 0.14f

            drawRoundRect(
                color = Color(0xFF0A1218),
                topLeft = Offset(pmicX, pmicY),
                size = Size(pmicW, pmicH),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFF64748B),
                topLeft = Offset(pmicX, pmicY),
                size = Size(pmicW, pmicH),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 1f)
            )

            // Surrounding Power Chokes / Inductors
            for (i in 0..2) {
                val chokeX = pmicX + pmicW + 8f
                val chokeY = pmicY + (i * 18f)
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(chokeX, chokeY),
                    size = Size(16f, 12f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                // Silver solder terminals
                drawRect(Color(0xFFCBD5E1), Offset(chokeX, chokeY), Size(3f, 12f))
                drawRect(Color(0xFFCBD5E1), Offset(chokeX + 13f, chokeY), Size(3f, 12f))
            }

            // 6. Battery / Sub-board FPC Connector (Bottom)
            val fpcX = w * 0.25f
            val fpcY = h * 0.80f
            val fpcW = w * 0.50f
            val fpcH = h * 0.08f

            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(fpcX, fpcY),
                size = Size(fpcW, fpcH),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFFF59E0B).copy(alpha = 0.7f),
                topLeft = Offset(fpcX, fpcY),
                size = Size(fpcW, fpcH),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 1.2f)
            )

            // Gold Pins inside FPC
            var pinX = fpcX + 10f
            while (pinX < fpcX + fpcW - 10f) {
                drawRect(Color(0xFFFFD700), Offset(pinX, fpcY + 4f), Size(3f, fpcH - 8f))
                pinX += 10f
            }

            // 7. Ground Shield Test Points / Mass Pads (GND)
            val gndX = w * 0.72f
            val gndY = h * 0.28f
            drawCircle(Color(0xFF94A3B8), radius = 9f, center = Offset(gndX, gndY))
            drawCircle(Color(0xFF0F172A), radius = 5f, center = Offset(gndX, gndY))

            // 8. CRITICAL: Golden Pulsing Test Point (TP1 - EDL / BROM)
            val tp1X = w * 0.62f
            val tp1Y = h * 0.28f

            // Pulse wave animation ring
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = Offset(tp1X, tp1Y),
                style = Stroke(width = 2.5f)
            )

            // Outer Target Reticle
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 11f,
                center = Offset(tp1X, tp1Y),
                style = Stroke(width = 1.8f)
            )
            // Center Gold Pad
            drawCircle(
                color = Color(0xFFFFC107),
                radius = 6.5f,
                center = Offset(tp1X, tp1Y)
            )
            drawCircle(
                color = Color(0xFFFFF9C4),
                radius = 2.5f,
                center = Offset(tp1X, tp1Y)
            )

            // Dashed Tweezer / Probe Connection Line from TP1 to GND
            val probeLine = Path().apply {
                moveTo(tp1X, tp1Y)
                lineTo(gndX, gndY)
            }
            drawPath(
                path = probeLine,
                color = Color(0xFFEF4444).copy(alpha = 0.9f),
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )
        }

        // Overlay Labels and Technical Silkscreen Badges
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xDD0B192C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "📐 BOARDVIEW", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(text = "•", color = Color.Gray, fontSize = 10.sp)
                        Text(text = cleanTitle, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xDD1E293B)
                ) {
                    Text(
                        text = "EDL 9008 / BROM",
                        color = TechGoldTestPoint,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Bottom Tweezer / Probe Pinpoint Instruction Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xF007131F),
                border = androidx.compose.foundation.BorderStroke(1.dp, TechGoldTestPoint.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = "Testpoint Target",
                        tint = TechGoldTestPoint,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📍 ТОЧКА ТЕСТПОИНТА (TP1)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TechGoldTestPoint
                        )
                        Text(
                            text = "Замкните пинцетом золотую точку TP1 на корпус/GND и подключите USB",
                            fontSize = 10.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}
