package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StoryOverlay
import coil.compose.AsyncImage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(
    isDarkTheme: Boolean
) {
    val isDark = LocalIsDarkTheme.current
    val surfaceColor = if (isDark) VkDarkSurface else Color.White
    val bgColor = if (isDark) VkDarkBg else Color(0xFFEDEEF0)
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryColor = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Лента мастеров", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor
                ),
                actions = {
                    IconButton(onClick = { /* Add Post */ }) {
                        Icon(Icons.Outlined.AddBox, contentDescription = "Добавить пост", tint = VkBlue)
                    }
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Поиск", tint = textColor)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "What's new" input card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = VkBlue.copy(alpha = 0.2f)
                        ) {}
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Что у вас нового?",
                            color = secondaryColor,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = "Фото", tint = secondaryColor)
                        }
                    }
                }
            }

            // Post 1
            item {
                FeedPostCard(
                    authorName = "Alexey (iPhone Specialist)",
                    time = "10 минут назад",
                    text = "Сегодня восстановил Face ID на iPhone 13 Pro после сильного залития. Шлейф был полностью корродирован, пришлось восстанавливать дороги под микроскопом. Клиент счастлив! 🛠️",
                    likes = 14,
                    comments = 3,
                    isDark = isDark
                )
            }

            // Post 2
            item {
                FeedPostCard(
                    authorName = "Сервисный Центр 'FixIt'",
                    time = "2 часа назад",
                    text = "Заехал интересный аппарат - Samsung S23 Ultra с диагнозом 'не заряжается'. Оказалось, проблема была не в разъеме, а в контроллере питания. Схемотехника у них в этом году усложнилась. Кто сталкивался?",
                    likes = 42,
                    comments = 15,
                    isDark = isDark
                )
            }

            // Post 3
            item {
                FeedPostCard(
                    authorName = "Дмитрий С.",
                    time = "Вчера в 18:45",
                    text = "Ребята, подскажите надежного поставщика оригинальных дисплеев на Xiaomi 12? Везде китайские копии с плохим олеофобным...",
                    likes = 5,
                    comments = 22,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
fun FeedPostCard(
    authorName: String,
    time: String,
    text: String,
    likes: Int,
    comments: Int,
    isDark: Boolean
) {
    val surfaceColor = if (isDark) VkDarkSurface else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryColor = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight
    val dividerColor = if (isDark) Color(0xFF2C2D2E) else Color(0xFFE1E3E6)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = VkBlue.copy(alpha = 0.2f)
                ) {}
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(authorName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textColor)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(time, fontSize = 12.sp, color = secondaryColor)
                }
                IconButton(onClick = { }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = secondaryColor)
                }
            }

            // Body
            Text(
                text = text,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // Footer / Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF2C2D2E) else Color(0xFFF2F3F5))
                        .clickable { }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Нравится", tint = secondaryColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(likes.toString(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = secondaryColor)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF2C2D2E) else Color(0xFFF2F3F5))
                        .clickable { }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Комментарии", tint = secondaryColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(comments.toString(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = secondaryColor)
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = "Поделиться", tint = secondaryColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
