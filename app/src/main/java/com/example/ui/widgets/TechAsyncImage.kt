package com.example.ui.widgets

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TechGoldTestPoint
import com.example.ui.theme.TechPrimaryBlue
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ImageLoaderProvider {
    @Volatile
    private var customLoader: ImageLoader? = null

    fun getLoader(context: Context): ImageLoader {
        return customLoader ?: synchronized(this) {
            customLoader ?: ImageLoader.Builder(context.applicationContext)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .writeTimeout(20, TimeUnit.SECONDS)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build()
                }
                .crossfade(true)
                .build().also { customLoader = it }
        }
    }
}

@Composable
fun TechAsyncImage(
    imageUrl: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null,
    title: String? = null,
    sourceUrl: String? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    var retryTrigger by remember { mutableIntStateOf(0) }

    // Direct Bitmap decoding if Base64
    val base64Bitmap = remember(imageUrl) {
        val trimmed = imageUrl.trim()
        if (trimmed.startsWith("data:image/") && trimmed.contains(";base64,")) {
            try {
                val data = trimmed.substringAfter(";base64,")
                val bytes = Base64.decode(data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else if (trimmed.length > 200 && !trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("content://") && !trimmed.startsWith("file://")) {
            try {
                val bytes = Base64.decode(trimmed, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (base64Bitmap != null) {
        Box(
            modifier = modifier
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = base64Bitmap.asImageBitmap(),
                contentDescription = contentDescription ?: title ?: "Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                colorFilter = colorFilter
            )
        }
        return
    }

    val imageLoader = remember { ImageLoaderProvider.getLoader(context) }

    val imageRequest = remember(imageUrl, retryTrigger) {
        val trimmedUrl = imageUrl.trim()
        val dataTarget: Any = if (trimmedUrl.startsWith("content://") || trimmedUrl.startsWith("file://")) {
            Uri.parse(trimmedUrl)
        } else if (retryTrigger > 0 && trimmedUrl.startsWith("http")) {
            // Hotlink bypass proxy
            "https://wsrv.nl/?url=" + Uri.encode(trimmedUrl) + "&output=webp"
        } else {
            trimmedUrl
        }

        ImageRequest.Builder(context)
            .data(dataTarget)
            .crossfade(true)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .addHeader("Referer", "https://www.google.com/")
            .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .build()
    }

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            imageLoader = imageLoader,
            contentDescription = contentDescription ?: title ?: "Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            colorFilter = colorFilter
        ) {
            val state = painter.state
            when (state) {
                is coil.compose.AsyncImagePainter.State.Loading -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "shimmerAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isDark) Color(0xFF1E293B).copy(alpha = alpha)
                                else Color(0xFFE2E8F0).copy(alpha = alpha)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = TechPrimaryBlue,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Загрузка схемы платы...",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                is coil.compose.AsyncImagePainter.State.Error -> {
                    // Fallback to crisp digital vector Boardview Motherboard
                    Box(modifier = Modifier.fillMaxSize()) {
                        TechBoardviewCanvas(
                            deviceTitle = title ?: "Схема TestPoint платы",
                            modifier = Modifier.fillMaxSize(),
                            isDark = isDark
                        )

                        // Top right quick action controls
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Retry with Proxy Button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xCC0F172A),
                                modifier = Modifier.clickable { retryTrigger++ }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry Proxy",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Фото",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                            }

                            // Open in Browser
                            val effectiveUrl = sourceUrl ?: imageUrl
                            if (effectiveUrl.startsWith("http")) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xCC0F172A),
                                    modifier = Modifier.clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveUrl)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Browser",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Веб",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

