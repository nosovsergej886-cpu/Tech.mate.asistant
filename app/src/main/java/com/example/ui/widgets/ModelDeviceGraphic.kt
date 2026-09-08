package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModelDeviceGraphic(
    brand: String,
    model: String,
    modifier: Modifier = Modifier.size(70.dp, 100.dp)
) {
    val b = brand.lowercase()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw realistic phone frame
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF3A3E4D), Color(0xFF181A22))
                    ),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Screen display area
                val margin = 4f
                val screenWidth = width - (margin * 2)
                val screenHeight = height - (margin * 2)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                    ),
                    topLeft = Offset(margin, margin),
                    size = Size(screenWidth, screenHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                if (b.contains("apple") || b.contains("iphone")) {
                    // Apple notch / Dynamic island
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(width / 2f - 10f, margin + 2f),
                        size = Size(20f, 5f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    // Triple camera arrangement accent on back
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 2f,
                        center = Offset(margin + 6f, margin + 6f)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 2f,
                        center = Offset(margin + 6f, margin + 14f)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 2f,
                        center = Offset(margin + 12f, margin + 10f)
                    )
                } else if (b.contains("samsung")) {
                    // Hole punch camera
                    drawCircle(
                        color = Color.Black,
                        radius = 3f,
                        center = Offset(width / 2f, margin + 6f)
                    )
                    // Edge curve highlights
                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = 0.5f),
                        start = Offset(margin, margin + 10f),
                        end = Offset(margin, height - margin - 10f),
                        strokeWidth = 2f
                    )
                } else {
                    // Xiaomi / Android punch camera or waterdrop
                    drawCircle(
                        color = Color.Black,
                        radius = 2.5f,
                        center = Offset(width / 2f, margin + 5f)
                    )
                    // Camera module
                    drawRoundRect(
                        color = Color(0xFF334155),
                        topLeft = Offset(width - margin - 10f, margin + 4f),
                        size = Size(8f, 16f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }

            // Small watermark or model label inside screen
            Text(
                text = model.take(8),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
            )
        }
    }
}
