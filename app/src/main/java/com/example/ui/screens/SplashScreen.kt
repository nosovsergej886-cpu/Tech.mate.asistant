package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.SplashAnimationType
import com.example.services.ThemeManager
import com.example.ui.theme.TechCopperTrace
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechPrimaryBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val splashType by themeManager.splashAnimation.collectAsState()

    LaunchedEffect(Unit) {
        delay(2200)
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C)),
        contentAlignment = Alignment.Center
    ) {
        when (splashType) {
            SplashAnimationType.CIRCUIT_TRACE -> CircuitTraceAnimation()
            SplashAnimationType.CYBER_SCAN -> CyberScanAnimation()
            SplashAnimationType.BGA_LASER -> BgaLaserAnimation()
            SplashAnimationType.FESTIVE_NEW_YEAR -> FestiveNewYearAnimation()
            SplashAnimationType.BIOS_TERMINAL -> BiosTerminalAnimation()
        }
    }
}

@Composable
private fun CircuitTraceAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chipPulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawLine(
            color = TechCopperTrace.copy(alpha = 0.4f),
            start = Offset(0f, height * 0.3f),
            end = Offset(width * 0.4f, height * 0.3f),
            strokeWidth = 4f
        )
        drawLine(
            color = TechCopperTrace.copy(alpha = 0.4f),
            start = Offset(width * 0.4f, height * 0.3f),
            end = Offset(width * 0.5f, height * 0.4f),
            strokeWidth = 4f
        )
        drawLine(
            color = TechCopperTrace.copy(alpha = 0.4f),
            start = Offset(width, height * 0.7f),
            end = Offset(width * 0.6f, height * 0.7f),
            strokeWidth = 4f
        )
        drawLine(
            color = TechCopperTrace.copy(alpha = 0.4f),
            start = Offset(width * 0.6f, height * 0.7f),
            end = Offset(width * 0.5f, height * 0.6f),
            strokeWidth = 4f
        )

        drawCircle(color = TechGoldTestPoint, radius = 10f, center = Offset(width * 0.2f, height * 0.3f))
        drawCircle(color = TechGoldTestPoint, radius = 10f, center = Offset(width * 0.8f, height * 0.7f))
        drawCircle(color = TechGoldTestPoint, radius = 12f, center = Offset(width * 0.5f, height * 0.25f))
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .size(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E2838))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = TechPrimaryBlue.copy(alpha = glowAlpha),
                style = Stroke(width = 6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
            )
        }

        LogoContent()
    }
}

@Composable
private fun CyberScanAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanline"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Cyber Grid
        for (i in 1..8) {
            drawLine(
                color = Color(0xFF00FFCC).copy(alpha = 0.08f),
                start = Offset(0f, h * (i / 10f)),
                end = Offset(w, h * (i / 10f)),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF00FFCC).copy(alpha = 0.08f),
                start = Offset(w * (i / 10f), 0f),
                end = Offset(w * (i / 10f), h),
                strokeWidth = 1f
            )
        }

        // Active laser scanline
        val currentScan = h * scanY
        drawLine(
            color = Color(0xFF00FFCC),
            start = Offset(0f, currentScan),
            end = Offset(w, currentScan),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF00FFCC).copy(alpha = 0.25f),
            start = Offset(0f, currentScan - 12f),
            end = Offset(w, currentScan - 12f),
            strokeWidth = 8f
        )
    }

    Box(
        modifier = Modifier
            .size(164.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF071B1E))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xFF00FFCC),
                style = Stroke(width = 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
            )
        }
        LogoContent(accentColor = Color(0xFF00FFCC))
    }
}

@Composable
private fun BgaLaserAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "bga")
    val laserRot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing)
        ),
        label = "rot"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // BGA Solder Balls Matrix 6x6
        val step = 32f
        val startX = cx - (step * 2.5f)
        val startY = cy - (step * 2.5f)

        for (r in 0..5) {
            for (c in 0..5) {
                val bx = startX + c * step
                val by = startY + r * step
                drawCircle(
                    color = if ((r + c) % 2 == 0) TechGoldTestPoint.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                    radius = 4f,
                    center = Offset(bx, by)
                )
            }
        }

        // Concentric Laser alignment rings
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.4f),
            radius = 120f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
        drawCircle(
            color = TechGoldTestPoint.copy(alpha = 0.6f),
            radius = 160f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
    }

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF18151D))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        LogoContent(accentColor = TechGoldTestPoint)
    }
}

@Composable
private fun FestiveNewYearAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "festive")
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "festivePulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Golden Sparkles
        val stars = listOf(
            Offset(w * 0.15f, h * 0.2f),
            Offset(w * 0.85f, h * 0.25f),
            Offset(w * 0.25f, h * 0.75f),
            Offset(w * 0.8f, h * 0.8f),
            Offset(w * 0.5f, h * 0.12f)
        )
        stars.forEach { pos ->
            drawCircle(TechGoldTestPoint.copy(alpha = 0.8f), radius = 6f, center = pos)
            drawLine(TechGoldTestPoint.copy(alpha = 0.5f), Offset(pos.x - 14f, pos.y), Offset(pos.x + 14f, pos.y), strokeWidth = 2f)
            drawLine(TechGoldTestPoint.copy(alpha = 0.5f), Offset(pos.x, pos.y - 14f), Offset(pos.x, pos.y + 14f), strokeWidth = 2f)
        }
    }

    Box(
        modifier = Modifier
            .scale(sparkleScale)
            .size(164.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF231A0E))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = TechGoldTestPoint,
                style = Stroke(width = 5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎄", fontSize = 28.sp)
            LogoContent(accentColor = TechGoldTestPoint)
        }
    }
}

@Composable
private fun BiosTerminalAnimation() {
    var lineCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        delay(350)
        lineCount = 2
        delay(350)
        lineCount = 3
        delay(350)
        lineCount = 4
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(">> TECHMATE HARDWARE BIOS v3.2.0 <<", color = Color(0xFF4ADE80), fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (lineCount >= 1) {
            Text("[OK] CPU CORE: 8-Core ARM BGA Detected", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
        }
        if (lineCount >= 2) {
            Text("[OK] TESTPOINT_BUS: I2C/SPI Online (0x7F)", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
        }
        if (lineCount >= 3) {
            Text("[OK] CLOUD_SYNC: Google Firebase Spark [READY]", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
        }
        if (lineCount >= 4) {
            Text("[OK] MASTER_WORKSPACE: Initialized.", color = TechGoldTestPoint, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            LogoContent(accentColor = Color(0xFF4ADE80))
        }
    }
}

@Composable
private fun LogoContent(accentColor: Color = TechPrimaryBlue) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "T",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(TechGoldTestPoint)
            )
            Text(
                text = "M",
                color = accentColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Tech.Mate",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
