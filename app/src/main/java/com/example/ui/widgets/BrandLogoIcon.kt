package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrandLogoIcon(
    brand: String,
    modifier: Modifier = Modifier.size(28.dp)
) {
    val b = brand.lowercase().trim()
    when {
        b.contains("apple") || b.contains("iphone") || b.contains("ipad") -> {
            Surface(
                modifier = modifier,
                shape = CircleShape,
                color = Color(0xFF1C1C1E)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        b.contains("samsung") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1428A0)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "SAMSUNG",
                        color = Color.White,
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Normal
                    )
                }
            }
        }
        b.contains("xiaomi") || b.contains("mi") || b.contains("redmi") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFF6900)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = size.width * 0.22f
                        val path = Path().apply {
                            moveTo(stroke / 2, size.height)
                            lineTo(stroke / 2, stroke / 2)
                            lineTo(size.width - stroke / 2, stroke / 2)
                            lineTo(size.width - stroke / 2, size.height)
                        }
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(
                                width = stroke,
                                cap = StrokeCap.Square
                            )
                        )
                    }
                }
            }
        }
        b.contains("huawei") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFCF0A2C)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "HUA",
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        b.contains("honor") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF00A2E8)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "HONOR",
                        color = Color.White,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        b.contains("poco") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFFD700)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "POCO",
                        color = Color.Black,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        b.contains("realme") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFFC107)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "realme",
                        color = Color.Black,
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        b.contains("vivo") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0066FF)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "vivo",
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
        b.contains("oppo") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF00875A)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "oppo",
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        b.contains("google") || b.contains("pixel") -> {
            Surface(
                modifier = modifier,
                shape = CircleShape,
                color = Color(0xFF4285F4)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "G",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        b.contains("infinix") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF5B2C6F)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Infinix",
                        color = Color.White,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        b.contains("tecno") -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF003399)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "TECNO",
                        color = Color.White,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        else -> {
            Surface(
                modifier = modifier,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = brand.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

