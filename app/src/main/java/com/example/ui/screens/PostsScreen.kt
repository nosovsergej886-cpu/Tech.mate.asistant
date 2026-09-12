package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.PostCommentEntity
import com.example.model.PostEntity
import com.example.model.PostLikeEntity
import com.example.model.StoryEntity
import com.example.services.AppDesignVariant
import com.example.services.AuthService
import com.example.services.DatabaseService
import com.example.services.StoryService
import com.example.ui.components.StoryOverlay
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbService = remember { DatabaseService.getInstance(context) }
    val authService = remember { AuthService.getInstance(context) }
    val storyService = remember { StoryService.getInstance(context) }
    val syncService = remember { com.example.services.FirebaseSyncService.getInstance(context) }
    val syncReport by syncService.syncStatus.collectAsState()
    var isManualSyncing by remember { mutableStateOf(false) }

    val currentUser by authService.currentUser.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val tokens = LocalDesignTokens.current
    val haptic = LocalHapticFeedback.current
    val isPremium = tokens.variant == AppDesignVariant.PREMIUM_TECHMATE

    val surfaceColor = if (isDark) {
        if (isPremium) TechMateDarkSurface else VkDarkSurface
    } else Color.White
    val bgColor = if (isDark) {
        if (isPremium) TechMateDarkBg else VkDarkBg
    } else if (isPremium) Color(0xFFF8FAFC) else Color(0xFFEDEEF0)
    val textColor = tokens.topBarContentColor
    val secondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentColor = tokens.primaryAccent
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    // Posts & Likes flows
    val allPosts by dbService.postDao.getAllPosts().collectAsState(initial = emptyList())
    val userLikes by dbService.postLikeDao.getLikedPostIds(currentUser?.id ?: currentUser?.email ?: "guest").collectAsState(initial = emptyList())

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog states
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var activeCommentsPost by remember { mutableStateOf<PostEntity?>(null) }
    var activeMasterProfile by remember { mutableStateOf<PostEntity?>(null) }
    var activeMediaPreview by remember { mutableStateOf<Pair<String, String>?>(null) } // type to url

    val filteredPosts = remember(allPosts, searchQuery) {
        if (searchQuery.isBlank()) {
            allPosts
        } else {
            val q = searchQuery.trim().lowercase()
            allPosts.filter { post ->
                post.content.lowercase().contains(q) ||
                post.authorName.lowercase().contains(q) ||
                post.taggedDevice.lowercase().contains(q) ||
                post.authorServiceCenter.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск по постам, устройствам, мастерам...", fontSize = 14.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = surfaceColor,
                        titleContentColor = textColor
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Лента мастеров", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    if (isPremium) "💎 TECH.MATE" else "ВК",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = surfaceColor,
                        titleContentColor = textColor
                    ),
                    actions = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCreatePostDialog = true
                        }) {
                            Icon(Icons.Outlined.AddBox, contentDescription = "Добавить пост", tint = accentColor)
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isManualSyncing = true
                                syncService.appScope.launch {
                                    val res = syncService.performFullSync()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isManualSyncing = false
                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isManualSyncing
                        ) {
                            if (isManualSyncing || syncReport.state == com.example.services.SyncState.SYNCING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = accentColor
                                )
                            } else {
                                Icon(Icons.Outlined.CloudSync, contentDescription = "Синхронизация", tint = textColor)
                            }
                        }
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Поиск", tint = textColor)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Stories Carousel at top of feed (Premium Stories with Snap & Glassmorphism)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = surfaceColor
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        com.example.ui.components.PremiumStoriesRow()
                    }
                }
            }

            // 2. "What's new" post creation card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCreatePostDialog = true
                        },
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(TechMateIndigo, TechMatePurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (currentUser?.name?.firstOrNull() ?: 'М').uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Что у вас нового? Поделитесь кейсом ремонта...",
                            color = secondaryColor,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showCreatePostDialog = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = "Фото кейса", tint = accentColor)
                        }
                    }
                }
            }

            // If empty search results
            if (filteredPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Article,
                                contentDescription = null,
                                tint = secondaryColor,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) "По запросу «$searchQuery» ничего не найдено" else "В ленте пока нет постов",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryColor
                            )
                            if (searchQuery.isNotEmpty()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("Сбросить поиск", color = VkBlue)
                                }
                            }
                        }
                    }
                }
            } else {
                // Real Reactive Posts Feed
                items(filteredPosts, key = { it.id }) { post ->
                    val isLiked = userLikes.contains(post.id)
                    FeedPostCard(
                        post = post,
                        isLiked = isLiked,
                        isDark = isDark,
                        onLikeClick = {
                            scope.launch {
                                val userId = currentUser?.id ?: currentUser?.email ?: "guest"
                                if (isLiked) {
                                    dbService.postLikeDao.deleteLike(post.id, userId)
                                    dbService.postDao.updateLikesCount(post.id, (post.likesCount - 1).coerceAtLeast(0))
                                } else {
                                    dbService.postLikeDao.insertLike(PostLikeEntity(postId = post.id, userId = userId))
                                    dbService.postDao.updateLikesCount(post.id, post.likesCount + 1)
                                }
                            }
                        },
                        onCommentClick = {
                            // Increment views on open
                            scope.launch { dbService.postDao.incrementViews(post.id) }
                            activeCommentsPost = post
                        },
                        onAuthorClick = {
                            activeMasterProfile = post
                        },
                        onMediaClick = { type, url ->
                            activeMediaPreview = type to url
                        },
                        onShareClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Пост мастера", "${post.authorName} (${post.taggedDevice}):\n${post.content}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Текст поста скопирован в буфер", Toast.LENGTH_SHORT).show()
                        },
                        onAiStoriesDuplicate = {
                            scope.launch {
                                val story = StoryEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = post.taggedDevice.ifBlank { post.authorName },
                                    subtitle = post.authorServiceCenter,
                                    content = post.content,
                                    authorName = post.authorName,
                                    authorEmail = post.authorEmail,
                                    authorServiceCenter = post.authorServiceCenter,
                                    authorServiceCenterId = post.authorServiceCenterId,
                                    scope = "GLOBAL_ALERT",
                                    mediaType = post.mediaType,
                                    mediaUrl = post.mediaUrl,
                                    taggedDeviceModel = post.taggedDevice
                                )
                                dbService.storyDao.insertStory(story)
                                try {
                                    com.example.services.CustomHostSyncService.getInstance(context).autoPushStory(story)
                                } catch (_: Exception) {}
                                dbService.postDao.updatePost(post.copy(isAiAnalyzed = true))
                                Toast.makeText(context, "✨ Пост опубликован в Stories для всех мастеров!", Toast.LENGTH_LONG).show()
                            }
                        },
                        canDelete = authService.isGodMode() || authService.isServiceAdmin() || post.authorEmail == currentUser?.email,
                        onDeletePost = {
                            scope.launch {
                                dbService.postDao.deletePost(post.id)
                                syncService.deletePostFromFirestore(post.id)
                                Toast.makeText(context, "Запись удалена из ленты", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialog: Create Post
    if (showCreatePostDialog) {
        CreatePostDialog(
            currentUser = currentUser,
            onDismiss = { showCreatePostDialog = false },
            onPostCreated = { newPost, autoPublishToStories ->
                scope.launch {
                    dbService.postDao.insertPost(newPost)
                    syncService.syncPostToFirestore(newPost)
                    if (autoPublishToStories) {
                        val story = StoryEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            title = newPost.taggedDevice.ifBlank { newPost.authorName },
                            subtitle = newPost.authorServiceCenter,
                            content = newPost.content,
                            authorName = newPost.authorName,
                            authorEmail = newPost.authorEmail,
                            authorServiceCenter = newPost.authorServiceCenter,
                            authorServiceCenterId = newPost.authorServiceCenterId,
                            scope = if (newPost.isPublic) "GLOBAL_ALERT" else "LOCAL_SC",
                            mediaType = newPost.mediaType,
                            mediaUrl = newPost.mediaUrl,
                            taggedDeviceModel = newPost.taggedDevice
                        )
                        dbService.storyDao.insertStory(story)
                        try {
                            com.example.services.CustomHostSyncService.getInstance(context).autoPushStory(story)
                        } catch (_: Exception) {}
                    }
                    showCreatePostDialog = false
                    Toast.makeText(context, "Пост опубликован в Ленте!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // VK BottomSheet: Comments Sheet
    if (activeCommentsPost != null) {
        PostCommentsBottomSheet(
            post = activeCommentsPost!!,
            currentUser = currentUser,
            onDismiss = { activeCommentsPost = null }
        )
    }

    // Dialog: Master Profile
    if (activeMasterProfile != null) {
        MasterProfileDialog(
            post = activeMasterProfile!!,
            allPosts = allPosts,
            onDismiss = { activeMasterProfile = null }
        )
    }

    // Dialog: Media Preview (Photo / Video)
    if (activeMediaPreview != null) {
        val (mediaType, mediaUrl) = activeMediaPreview!!
        Dialog(
            onDismissRequest = { activeMediaPreview = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { activeMediaPreview = null },
                contentAlignment = Alignment.Center
            ) {
                if (mediaType == "VIDEO") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayCircleFilled,
                                    contentDescription = "Воспроизведение",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    "▶ Воспроизведение видеоматериала ремонта",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Видеозапись диагностики и замера линии питания",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { activeMediaPreview = null },
                                colors = ButtonDefaults.buttonColors(containerColor = VkBlue)
                            ) {
                                Text("Закрыть плеер")
                            }
                        }
                    }
                } else {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Фото",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun FeedPostCard(
    post: PostEntity,
    isLiked: Boolean,
    isDark: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onMediaClick: (String, String) -> Unit,
    onShareClick: () -> Unit,
    onAiStoriesDuplicate: () -> Unit,
    canDelete: Boolean = false,
    onDeletePost: (() -> Unit)? = null
) {
    val tokens = LocalDesignTokens.current
    val haptic = LocalHapticFeedback.current
    val isPremium = tokens.variant == AppDesignVariant.PREMIUM_TECHMATE

    val surfaceColor = if (isDark) {
        if (isPremium) Color(0xFF111827).copy(alpha = 0.88f) else VkDarkSurface
    } else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val accentColor = tokens.primaryAccent

    var showPostMenu by remember { mutableStateOf(false) }

    val formattedTime = remember(post.createdAt) {
        val diff = System.currentTimeMillis() - post.createdAt
        when {
            diff < 60 * 1000 -> "Только что"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} мин. назад"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} ч. назад"
            else -> SimpleDateFormat("d MMMM в HH:mm", Locale("ru")).format(Date(post.createdAt))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header: Author Avatar, Name, Service Center, Device Tag
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable Avatar with gradient
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAuthorClick()
                        },
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(TechMateIndigo, TechMatePurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            post.authorName.firstOrNull()?.uppercase() ?: "М",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAuthorClick() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            post.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified Master",
                            tint = VkBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(formattedTime, fontSize = 12.sp, color = secondaryColor)
                        Text("•", fontSize = 12.sp, color = secondaryColor)
                        Text(
                            post.authorServiceCenter,
                            fontSize = 12.sp,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tagged Device Badge
                if (post.taggedDevice.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            post.taggedDevice,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF93C5FD) else VkBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Three-dot menu
                Box {
                    IconButton(
                        onClick = { showPostMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = secondaryColor)
                    }

                    DropdownMenu(
                        expanded = showPostMenu,
                        onDismissRequest = { showPostMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("✨ Создать Stories из поста") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TechGoldTestPoint) },
                            onClick = {
                                showPostMenu = false
                                onAiStoriesDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📋 Скопировать ссылку") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showPostMenu = false
                                onShareClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("👤 Профиль мастера") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = {
                                showPostMenu = false
                                onAuthorClick()
                            }
                        )
                        if (canDelete && onDeletePost != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("🗑️ Удалить запись", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showPostMenu = false
                                    onDeletePost()
                                }
                            )
                        }
                    }
                }
            }

            // Badges row: Public vs SC / AI Stories status
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (post.isPublic) VkBlue.copy(alpha = 0.12f) else Color(0x2222C55E)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (post.isPublic) Icons.Default.Public else Icons.Default.Business,
                            contentDescription = null,
                            tint = if (post.isPublic) VkBlue else Color(0xFF16A34A),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (post.isPublic) "Для всех мастеров" else "Мой СЦ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (post.isPublic) VkBlue else Color(0xFF16A34A)
                        )
                    }
                }

                if (post.isAiAnalyzed) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TechGoldTestPoint.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✨ В Stories", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                    }
                }
            }

            // Post Text
            Text(
                text = post.content,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // Media attachment (Photo / Video)
            if (post.mediaUrl.isNotBlank()) {
                if (post.mediaType == "VIDEO") {
                    // VK-like Video Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .clickable { onMediaClick("VIDEO", post.mediaUrl) },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=800&q=80",
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        // Play button
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = VkBlue
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        // Video title & duration badge
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                post.mediaTitle.ifBlank { "Видео ремонта" },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    "01:45",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else if (post.mediaType == "PHOTO") {
                    // Full photo view
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onMediaClick("PHOTO", post.mediaUrl) }
                    ) {
                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription = post.mediaTitle.ifBlank { "Фото ремонта" },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // VK-style Action Bar: Like, Comments, Views, Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pillBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)

                // Like Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isLiked) Color(0xFFFFEBEB) else pillBg
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLikeClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Нравится",
                        tint = if (isLiked) Color(0xFFEF4444) else secondaryColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        post.likesCount.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLiked) Color(0xFFEF4444) else secondaryColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Comment Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(pillBg)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCommentClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Комментарии",
                        tint = secondaryColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        post.commentsCount.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Share Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(pillBg)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShareClick()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "Поделиться",
                        tint = secondaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Views Counter (VK eye icon)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Visibility,
                        contentDescription = "Просмотры",
                        tint = secondaryColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        post.viewsCount.toString(),
                        fontSize = 12.sp,
                        color = secondaryColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

// Dialog for writing and publishing a new post
@Composable
fun CreatePostDialog(
    currentUser: com.example.model.UserEntity?,
    onDismiss: () -> Unit,
    onPostCreated: (PostEntity, Boolean) -> Unit
) {
    var contentText by remember { mutableStateOf("") }
    var taggedDevice by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("NONE") } // "NONE", "PHOTO", "VIDEO"
    var mediaUrl by remember { mutableStateOf("") }
    var isPublicForMasters by remember { mutableStateOf(true) }
    var autoPublishToStories by remember { mutableStateOf(true) }

    val presetPhotos = listOf(
        "Микроскоп" to "https://images.unsplash.com/photo-1597740985671-2a8a3b80532e?w=800&q=80",
        "Плата CPU" to "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80",
        "Инструменты" to "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=800&q=80"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Новая публикация", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = { Text("Опишите поломку, замеры, замененные чипы или задайте вопрос коллегам...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 5
                )

                OutlinedTextField(
                    value = taggedDevice,
                    onValueChange = { taggedDevice = it },
                    label = { Text("Устройство (модель)") },
                    placeholder = { Text("Например: iPhone 13 Pro, Xiaomi 12") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Media Attachment selector
                Text("Прикрепить медиа (Фото / Видео):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = mediaType == "NONE",
                        onClick = {
                            mediaType = "NONE"
                            mediaUrl = ""
                        },
                        label = { Text("Без медиа", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = mediaType == "PHOTO",
                        onClick = {
                            mediaType = "PHOTO"
                            mediaUrl = presetPhotos.first().second
                        },
                        label = { Text("📷 Фото платы", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = mediaType == "VIDEO",
                        onClick = {
                            mediaType = "VIDEO"
                            mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                        },
                        label = { Text("🎥 Видео ремонта", fontSize = 11.sp) }
                    )
                }

                // Public / SC Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isPublicForMasters,
                        onCheckedChange = { isPublicForMasters = it }
                    )
                    Text("🌐 Видно всем мастерам сообщества", fontSize = 13.sp)
                }

                // AI Stories Duplicate Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = autoPublishToStories,
                        onCheckedChange = { autoPublishToStories = it }
                    )
                    Text("✨ ИИ анализ и дублирование в Stories", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contentText.isNotBlank()) {
                        val newPost = PostEntity(
                            authorName = currentUser?.name ?: "Мастер",
                            authorEmail = currentUser?.email ?: "master@techmate.pro",
                            authorServiceCenter = currentUser?.serviceCenterId?.let { "СЦ #$it" } ?: "СЦ «ТехноМастер»",
                            authorServiceCenterId = currentUser?.serviceCenterId ?: "default_sc",
                            content = contentText.trim(),
                            taggedDevice = taggedDevice.trim(),
                            mediaType = mediaType,
                            mediaUrl = mediaUrl,
                            isPublic = isPublicForMasters,
                            isAiAnalyzed = autoPublishToStories
                        )
                        onPostCreated(newPost, autoPublishToStories)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VkBlue),
                enabled = contentText.isNotBlank()
            ) {
                Text("Опубликовать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// VK-Style Modal Bottom Sheet for Post Comments
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCommentsBottomSheet(
    post: PostEntity,
    currentUser: com.example.model.UserEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbService = remember { DatabaseService.getInstance(context) }
    val authService = remember { AuthService.getInstance(context) }
    val syncService = remember { com.example.services.FirebaseSyncService.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val comments by dbService.postCommentDao.getCommentsForPost(post.id).collectAsState(initial = emptyList())

    var commentInput by remember { mutableStateOf("") }
    var replyingToName by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF222222) else Color.White,
        tonalElevation = 6.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header: "Комментарии" title + count badge + close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Комментарии",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF19191A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF0F2F5)
                    ) {
                        Text(
                            comments.size.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VkBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )
                }
            }

            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                thickness = 0.8.dp
            )

            // Post mini summary banner
            Surface(
                color = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF7F8FA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (post.authorName.firstOrNull() ?: 'М').uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            post.authorName,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF19191A)
                        )
                        Text(
                            post.content,
                            fontSize = 11.5.sp,
                            color = if (isDark) Color.Gray else Color(0xFF656565),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                thickness = 0.5.dp
            )

            // Comments List
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Комментариев пока нет",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF19191A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Будьте первым, кто оставит экспертное мнение по ремонту!",
                            fontSize = 12.5.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        val canDeleteThisComment = authService.isGodMode() ||
                                authService.isServiceAdmin() ||
                                comment.authorEmail == currentUser?.email

                        VkCommentItem(
                            comment = comment,
                            isDark = isDark,
                            canDelete = canDeleteThisComment,
                            onReply = {
                                replyingToName = comment.authorName
                                commentInput = "${comment.authorName}, "
                            },
                            onDelete = {
                                scope.launch {
                                    dbService.postCommentDao.deleteComment(comment.id)
                                    dbService.postDao.updateCommentsCount(
                                        post.id,
                                        (post.commentsCount - 1).coerceAtLeast(0)
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Комментарий удален", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // Reply banner if replying to someone
            if (replyingToName != null) {
                Surface(
                    color = VkBlue.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Ответ для $replyingToName",
                            fontSize = 12.sp,
                            color = VkBlue,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = {
                                replyingToName = null
                                commentInput = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить", tint = VkBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                thickness = 0.8.dp
            )

            // VK Bottom Sticky Input Bar
            Surface(
                color = if (isDark) Color(0xFF222222) else Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current User Avatar
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (currentUser?.name?.firstOrNull() ?: 'М').uppercase(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // VK style pill TextField
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (isDark) Color(0xFF333333) else Color(0xFFF0F2F5),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                placeholder = {
                                    Text(
                                        if (replyingToName != null) "Ваш ответ..." else "Написать комментарий...",
                                        fontSize = 13.5.sp,
                                        color = if (isDark) Color.Gray else Color(0xFF828282)
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 3,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Round Send Button
                    IconButton(
                        onClick = {
                            if (commentInput.isNotBlank()) {
                                scope.launch {
                                    val textToSend = commentInput.trim()
                                    val newComment = PostCommentEntity(
                                        postId = post.id,
                                        authorName = currentUser?.name ?: "Мастер",
                                        authorEmail = currentUser?.email ?: "master@techmate.pro",
                                        authorServiceCenter = currentUser?.serviceCenterId ?: "СЦ #1",
                                        content = textToSend
                                    )
                                    dbService.postCommentDao.insertComment(newComment)
                                    dbService.postDao.incrementCommentsCount(post.id)
                                    syncService.syncCommentToFirestore(newComment)
                                    commentInput = ""
                                    replyingToName = null
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        enabled = commentInput.isNotBlank(),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (commentInput.isNotBlank()) VkBlue else Color.Gray.copy(alpha = 0.3f)
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Элемент комментария в стиле VK
 */
@Composable
fun VkCommentItem(
    comment: PostCommentEntity,
    isDark: Boolean,
    canDelete: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d MMM в HH:mm", Locale("ru")) }
    val formattedDate = remember(comment.createdAt) { dateFormat.format(Date(comment.createdAt)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Author avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2688EB)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (comment.authorName.firstOrNull() ?: 'М').uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    comment.authorName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF70B2FF) else Color(0xFF2A5885)
                )

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Удалить комментарий",
                            tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = comment.content,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = if (isDark) Color.White.copy(alpha = 0.95f) else Color(0xFF19191A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Ответить",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF70B2FF) else Color(0xFF2A5885),
                    modifier = Modifier.clickable { onReply() }
                )
            }
        }
    }
}

// Dialog: Master Profile & All their posts
@Composable
fun MasterProfileDialog(
    post: PostEntity,
    allPosts: List<PostEntity>,
    onDismiss: () -> Unit
) {
    val masterPosts = remember(allPosts, post.authorEmail, post.authorName) {
        allPosts.filter { it.authorEmail == post.authorEmail || it.authorName == post.authorName }
    }

    val totalLikes = remember(masterPosts) { masterPosts.sumOf { it.likesCount } }
    val totalComments = remember(masterPosts) { masterPosts.sumOf { it.commentsCount } }
    val totalViews = remember(masterPosts) { masterPosts.sumOf { it.viewsCount } }
    val popularityScore = remember(totalLikes, totalViews, totalComments) {
        (totalLikes * 3 + totalComments * 2 + (totalViews / 10)).coerceAtLeast(10)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = VkBlue
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            post.authorName.firstOrNull()?.uppercase() ?: "М",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VkBlue, modifier = Modifier.size(16.dp))
                    }
                    Text(post.authorServiceCenter, fontSize = 12.sp, color = Color.Gray)
                    Text("Инженер-электронщик • BGA специалист", fontSize = 11.sp, color = TechGoldTestPoint, fontWeight = FontWeight.Medium)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // Master Stats Row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = VkBlue.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${masterPosts.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VkBlue)
                            Text("Постов", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalLikes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFEF4444))
                            Text("Лайков", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalViews", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                            Text("Просмотров", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡ $popularityScore", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF59E0B))
                            Text("Рейтинг", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Все публикации мастера (${masterPosts.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(masterPosts) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (p.taggedDevice.isNotBlank()) {
                                    Text("📱 ${p.taggedDevice}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VkBlue)
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    p.content,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("❤️ ${p.likesCount}", fontSize = 11.sp, color = Color.Gray)
                                    Text("💬 ${p.commentsCount}", fontSize = 11.sp, color = Color.Gray)
                                    Text("👁️ ${p.viewsCount}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VkBlue)
            ) {
                Text("Закрыть")
            }
        }
    )
}
