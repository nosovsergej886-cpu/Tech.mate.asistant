package com.example.services

import android.content.Context
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HostSyncStatus(
    val isConfigured: Boolean = false,
    val serverUrl: String = "",
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val lastMessage: String = "",
    val isSuccess: Boolean = false
)

class CustomHostSyncService private constructor(private val context: Context) {

    companion object {
        const val DEFAULT_SERVER_URL = "https://tech-mate.ru/techmate_sync.php"
        const val DEFAULT_API_KEY = "techmate_secret_key"
        const val DEFAULT_CORPORATE_EMAIL = "noreply@tech-mate.ru"
        const val DEFAULT_SENDER_NAME = "Tech.Mate Сервис"

        @Volatile
        private var instance: CustomHostSyncService? = null

        fun getInstance(context: Context): CustomHostSyncService {
            return instance ?: synchronized(this) {
                instance ?: CustomHostSyncService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences("techmate_host_sync", Context.MODE_PRIVATE)
    private val dbService = DatabaseService.getInstance(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        // Автоматическая преднастройка сервера tech-mate.ru
        val currentUrl = prefs.getString("server_url", "") ?: ""
        if (currentUrl.isBlank()) {
            prefs.edit()
                .putString("server_url", DEFAULT_SERVER_URL)
                .putString("api_key", DEFAULT_API_KEY)
                .putBoolean("auto_sync_enabled", true)
                .putString("corporate_email", DEFAULT_CORPORATE_EMAIL)
                .putString("corporate_sender_name", DEFAULT_SENDER_NAME)
                .putBoolean("corporate_email_enabled", true)
                .apply()
        }
    }

    private val _syncStatus = MutableStateFlow(loadInitialStatus())
    val syncStatus: StateFlow<HostSyncStatus> = _syncStatus.asStateFlow()

    private fun loadInitialStatus(): HostSyncStatus {
        val url = prefs.getString("server_url", DEFAULT_SERVER_URL)?.ifBlank { DEFAULT_SERVER_URL } ?: DEFAULT_SERVER_URL
        val lastSync = prefs.getLong("last_sync_time", 0L)
        return HostSyncStatus(
            isConfigured = true,
            serverUrl = url,
            lastSyncTime = lastSync,
            lastMessage = "Хостинг tech-mate.ru подключен",
            isSuccess = true
        )
    }

    fun getServerUrl(): String = prefs.getString("server_url", DEFAULT_SERVER_URL)?.ifBlank { DEFAULT_SERVER_URL } ?: DEFAULT_SERVER_URL
    fun getApiKey(): String = prefs.getString("api_key", DEFAULT_API_KEY)?.ifBlank { DEFAULT_API_KEY } ?: DEFAULT_API_KEY
    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean("auto_sync_enabled", true)

    // Настройки корпоративной почты хостинга
    fun getCorporateEmail(): String = prefs.getString("corporate_email", DEFAULT_CORPORATE_EMAIL)?.ifBlank { DEFAULT_CORPORATE_EMAIL } ?: DEFAULT_CORPORATE_EMAIL
    fun getCorporateSenderName(): String = prefs.getString("corporate_sender_name", DEFAULT_SENDER_NAME)?.ifBlank { DEFAULT_SENDER_NAME } ?: DEFAULT_SENDER_NAME
    fun isCorporateEmailEnabled(): Boolean = prefs.getBoolean("corporate_email_enabled", true)

    // SMTP настройки для прямого подключения
    fun isSmtpEnabled(): Boolean = prefs.getBoolean("smtp_enabled", false)
    fun getSmtpHost(): String = prefs.getString("smtp_host", "") ?: ""
    fun getSmtpPort(): Int = prefs.getInt("smtp_port", 465)
    fun getSmtpUser(): String = prefs.getString("smtp_user", "") ?: ""
    fun getSmtpPass(): String = prefs.getString("smtp_pass", "") ?: ""
    fun getSmtpSecure(): String = prefs.getString("smtp_secure", "SSL") ?: "SSL"

    fun saveConfig(serverUrl: String, apiKey: String, autoSync: Boolean) {
        val cleanUrl = serverUrl.trim().removeSuffix("/")
        prefs.edit()
            .putString("server_url", cleanUrl)
            .putString("api_key", apiKey.trim())
            .putBoolean("auto_sync_enabled", autoSync)
            .apply()

        _syncStatus.value = _syncStatus.value.copy(
            isConfigured = cleanUrl.isNotBlank(),
            serverUrl = cleanUrl,
            lastMessage = if (cleanUrl.isNotBlank()) "Настройки сохранены" else "Хостинг отключен"
        )
    }

    fun saveCorporateEmailConfig(
        corporateEmail: String,
        senderName: String,
        enabled: Boolean,
        smtpEnabled: Boolean = false,
        smtpHost: String = "",
        smtpPort: Int = 465,
        smtpUser: String = "",
        smtpPass: String = "",
        smtpSecure: String = "SSL"
    ) {
        prefs.edit()
            .putString("corporate_email", corporateEmail.trim())
            .putString("corporate_sender_name", senderName.trim().ifBlank { "Tech.Mate Сервис" })
            .putBoolean("corporate_email_enabled", enabled)
            .putBoolean("smtp_enabled", smtpEnabled)
            .putString("smtp_host", smtpHost.trim())
            .putInt("smtp_port", smtpPort)
            .putString("smtp_user", smtpUser.trim())
            .putString("smtp_pass", smtpPass.trim())
            .putString("smtp_secure", smtpSecure.trim())
            .apply()
    }

    /**
     * Отправка 6-значного кода аутентификации через почтовый сервис хостинга
     */
    suspend fun sendOtpCodeViaHost(
        recipientEmail: String,
        code: String,
        overrideUrl: String? = null,
        overrideApiKey: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val url = overrideUrl ?: getServerUrl()
        val apiKey = overrideApiKey ?: getApiKey()
        val cleanUrl = url.trim().removeSuffix("/")

        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://"))) {
            return@withContext Pair(false, "URL хостинга не настроен для отправки почты")
        }

        val cleanRecipient = recipientEmail.trim()
        if (cleanRecipient.isBlank() || !cleanRecipient.contains("@")) {
            return@withContext Pair(false, "Некорректный email получателя")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("action", "send_otp")
                put("apiKey", apiKey)
                put("recipient", cleanRecipient)
                put("code", code)
                put("senderEmail", getCorporateEmail())
                put("senderName", getCorporateSenderName())

                val smtpObj = JSONObject().apply {
                    put("enabled", isSmtpEnabled())
                    put("host", getSmtpHost())
                    put("port", getSmtpPort())
                    put("user", getSmtpUser().ifBlank { getCorporateEmail() })
                    put("pass", getSmtpPass())
                    put("secure", getSmtpSecure())
                }
                put("smtp", smtpObj)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(cleanUrl)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val respJson = JSONObject(body)
                        val success = respJson.optBoolean("success", false)
                        val msg = respJson.optString("message", if (success) "Код успешно отправлен" else "Ошибка отправки")
                        if (success) {
                            Pair(true, msg)
                        } else {
                            val err = respJson.optString("error", msg)
                            Pair(false, err)
                        }
                    } catch (e: Exception) {
                        Pair(false, "Неверный формат ответа сервера: $body")
                    }
                } else {
                    Pair(false, "Ошибка сервера (HTTP ${response.code}): $body")
                }
            }
        } catch (e: Exception) {
            Log.e("HostSync", "sendOtpCodeViaHost error: ${e.message}", e)
            Pair(false, "Не удалось отправить письмо: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun testConnection(testUrl: String, testApiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanUrl = testUrl.trim().removeSuffix("/")
        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://"))) {
            return@withContext Pair(false, "Укажите корректный URL (например, https://ваш-сайт.рф/techmate_sync.php)")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("action", "ping")
                put("apiKey", testApiKey)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(cleanUrl)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val respJson = JSONObject(body)
                        if (respJson.optBoolean("success", false) || respJson.optString("status") == "ok") {
                            Pair(true, "Связь с хостингом установлена успешно! (Ответ: ${respJson.optString("message", "OK")})")
                        } else {
                            val err = respJson.optString("error", "Сервер вернул ошибку")
                            Pair(false, "Сервер ответил ошибкой: $err")
                        }
                    } catch (e: Exception) {
                        Pair(true, "Сервер ответил кодом 200 OK")
                    }
                } else {
                    Pair(false, "Ошибка сервера (HTTP ${response.code}): $body")
                }
            }
        } catch (e: Exception) {
            Log.e("HostSync", "Test connection error: ${e.message}", e)
            Pair(false, "Не удалось подключиться: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Выполняет полный обмен данными:
     * 1. Загружает на хостинг локальные посты, истории, базу знаний и комментарии
     * 2. Скачивает с хостинга новые записи от других мастеров
     */
    suspend fun performFullHostSync(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val url = getServerUrl()
        val apiKey = getApiKey()
        if (url.isBlank()) {
            return@withContext Pair(false, "URL хостинга не задан")
        }

        _syncStatus.value = _syncStatus.value.copy(isSyncing = true, lastMessage = "Синхронизация с сайтом...")

        try {
            // 1. Собираем локальные данные
            val localPosts = dbService.postDao.getAllPostsSync()
            val localStories = dbService.storyDao.getAllStoriesSync()
            val localKb = dbService.knowledgeDao.getAllEntriesSync()
            val localComments = dbService.postCommentDao.getAllCommentsSync()

            val postsArray = JSONArray()
            for (p in localPosts) {
                postsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("authorId", p.authorId)
                    put("authorName", p.authorName)
                    put("authorEmail", p.authorEmail)
                    put("authorServiceCenter", p.authorServiceCenter)
                    put("content", p.content)
                    put("mediaType", p.mediaType)
                    put("mediaUrl", p.mediaUrl)
                    put("mediaTitle", p.mediaTitle)
                    put("taggedDevice", p.taggedDevice)
                    put("likesCount", p.likesCount)
                    put("commentsCount", p.commentsCount)
                    put("viewsCount", p.viewsCount)
                    put("createdAt", p.createdAt)
                })
            }

            val storiesArray = JSONArray()
            for (s in localStories) {
                storiesArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("subtitle", s.subtitle)
                    put("content", s.content)
                    put("authorName", s.authorName)
                    put("authorEmail", s.authorEmail)
                    put("authorServiceCenter", s.authorServiceCenter)
                    put("scope", s.scope)
                    put("iconEmoji", s.iconEmoji)
                    put("warningLevel", s.warningLevel)
                    put("mediaType", s.mediaType)
                    put("mediaUrl", s.mediaUrl)
                    put("createdAt", s.createdAt)
                })
            }

            val kbArray = JSONArray()
            for (k in localKb) {
                kbArray.put(JSONObject().apply {
                    put("id", k.id)
                    put("brand", k.brand)
                    put("model", k.model)
                    put("problem", k.problem)
                    put("guideDataJson", k.guideDataJson)
                    put("addedBy", k.addedBy)
                    put("isSchematic", k.isSchematic)
                    put("addedDate", k.addedDate)
                })
            }

            val commentsArray = JSONArray()
            for (c in localComments) {
                commentsArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("postId", c.postId)
                    put("authorName", c.authorName)
                    put("authorEmail", c.authorEmail)
                    put("content", c.content)
                    put("createdAt", c.createdAt)
                })
            }

            val payload = JSONObject().apply {
                put("action", "sync_bidirectional")
                put("apiKey", apiKey)
                put("since", prefs.getLong("last_sync_time", 0L))
                put("posts", postsArray)
                put("stories", storiesArray)
                put("knowledge", kbArray)
                put("comments", commentsArray)
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respText = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val msg = "Сервер вернул код ${response.code}"
                    _syncStatus.value = _syncStatus.value.copy(isSyncing = false, lastMessage = msg, isSuccess = false)
                    return@withContext Pair(false, msg)
                }

                val respJson = JSONObject(respText)
                if (!respJson.optBoolean("success", true)) {
                    val err = respJson.optString("error", "Ошибка синхронизации")
                    _syncStatus.value = _syncStatus.value.copy(isSyncing = false, lastMessage = err, isSuccess = false)
                    return@withContext Pair(false, err)
                }

                // 2. Скачиваем удалённые посты от других мастеров
                var pulledPostsCount = 0
                val remotePosts = respJson.optJSONArray("remotePosts")
                if (remotePosts != null) {
                    for (i in 0 until remotePosts.length()) {
                        val obj = remotePosts.getJSONObject(i)
                        val id = obj.optString("id")
                        if (id.isNotBlank()) {
                            dbService.postDao.insertPost(
                                PostEntity(
                                    id = id,
                                    authorId = obj.optString("authorId", ""),
                                    authorName = obj.optString("authorName", "Мастер"),
                                    authorEmail = obj.optString("authorEmail", ""),
                                    authorServiceCenter = obj.optString("authorServiceCenter", "СЦ"),
                                    content = obj.optString("content", ""),
                                    mediaType = obj.optString("mediaType", "NONE"),
                                    mediaUrl = obj.optString("mediaUrl", ""),
                                    mediaTitle = obj.optString("mediaTitle", ""),
                                    taggedDevice = obj.optString("taggedDevice", ""),
                                    likesCount = obj.optInt("likesCount", 0),
                                    commentsCount = obj.optInt("commentsCount", 0),
                                    viewsCount = obj.optInt("viewsCount", 1),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                            pulledPostsCount++
                        }
                    }
                }

                // 3. Скачиваем удалённые истории
                var pulledStoriesCount = 0
                val remoteStories = respJson.optJSONArray("remoteStories")
                if (remoteStories != null) {
                    for (i in 0 until remoteStories.length()) {
                        val obj = remoteStories.getJSONObject(i)
                        val id = obj.optString("id")
                        if (id.isNotBlank()) {
                            dbService.storyDao.insertStory(
                                StoryEntity(
                                    id = id,
                                    title = obj.optString("title", "История"),
                                    subtitle = obj.optString("subtitle", ""),
                                    content = obj.optString("content", ""),
                                    authorName = obj.optString("authorName", "Мастер"),
                                    authorEmail = obj.optString("authorEmail", ""),
                                    authorServiceCenter = obj.optString("authorServiceCenter", "СЦ"),
                                    scope = obj.optString("scope", "LOCAL_SC"),
                                    iconEmoji = obj.optString("iconEmoji", "🛠️"),
                                    warningLevel = obj.optString("warningLevel", "NORMAL"),
                                    mediaType = obj.optString("mediaType", "PHOTO"),
                                    mediaUrl = obj.optString("mediaUrl", ""),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                            pulledStoriesCount++
                        }
                    }
                }

                // 4. Скачиваем удалённую базу знаний
                var pulledKbCount = 0
                val remoteKb = respJson.optJSONArray("remoteKnowledge")
                if (remoteKb != null) {
                    for (i in 0 until remoteKb.length()) {
                        val obj = remoteKb.getJSONObject(i)
                        val id = obj.optString("id")
                        if (id.isNotBlank()) {
                            dbService.knowledgeDao.insertEntry(
                                KnowledgeBaseEntryEntity(
                                    id = id,
                                    brand = obj.optString("brand", ""),
                                    model = obj.optString("model", ""),
                                    problem = obj.optString("problem", ""),
                                    guideDataJson = obj.optString("guideDataJson", ""),
                                    addedBy = obj.optString("addedBy", "Хостинг"),
                                    addedDate = obj.optLong("addedDate", System.currentTimeMillis()),
                                    isSchematic = obj.optBoolean("isSchematic", false)
                                )
                            )
                            pulledKbCount++
                        }
                    }
                }

                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_sync_time", now).apply()

                val summary = "Успешно синхронизировано с сайтом! (Получено: $pulledPostsCount постов, $pulledStoriesCount историй, $pulledKbCount статей)"
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    lastSyncTime = now,
                    lastMessage = summary,
                    isSuccess = true
                )
                Pair(true, summary)
            }
        } catch (e: Exception) {
            Log.e("HostSync", "Sync error: ${e.message}", e)
            val err = "Ошибка обмена: ${e.localizedMessage ?: e.message}"
            _syncStatus.value = _syncStatus.value.copy(isSyncing = false, lastMessage = err, isSuccess = false)
            Pair(false, err)
        }
    }

    /**
     * Моментальная автоматическая отправка нового поста на сайт
     */
    suspend fun autoPushPost(post: PostEntity) = withContext(Dispatchers.IO) {
        if (!isAutoSyncEnabled() || getServerUrl().isBlank()) return@withContext
        try {
            val payload = JSONObject().apply {
                put("action", "push_post")
                put("apiKey", getApiKey())
                put("post", JSONObject().apply {
                    put("id", post.id)
                    put("authorId", post.authorId)
                    put("authorName", post.authorName)
                    put("authorEmail", post.authorEmail)
                    put("authorServiceCenter", post.authorServiceCenter)
                    put("content", post.content)
                    put("mediaType", post.mediaType)
                    put("mediaUrl", post.mediaUrl)
                    put("mediaTitle", post.mediaTitle)
                    put("taggedDevice", post.taggedDevice)
                    put("likesCount", post.likesCount)
                    put("commentsCount", post.commentsCount)
                    put("viewsCount", post.viewsCount)
                    put("createdAt", post.createdAt)
                })
            }
            sendQuickPost(payload)
        } catch (e: Exception) {
            Log.w("HostSync", "autoPushPost error: ${e.message}")
        }
    }

    /**
     * Моментальная автоматическая отправка нового комментария на сайт
     */
    suspend fun autoPushComment(comment: PostCommentEntity) = withContext(Dispatchers.IO) {
        if (!isAutoSyncEnabled() || getServerUrl().isBlank()) return@withContext
        try {
            val payload = JSONObject().apply {
                put("action", "push_comment")
                put("apiKey", getApiKey())
                put("comment", JSONObject().apply {
                    put("id", comment.id)
                    put("postId", comment.postId)
                    put("authorName", comment.authorName)
                    put("authorEmail", comment.authorEmail)
                    put("content", comment.content)
                    put("createdAt", comment.createdAt)
                })
            }
            sendQuickPost(payload)
        } catch (e: Exception) {
            Log.w("HostSync", "autoPushComment error: ${e.message}")
        }
    }

    /**
     * Моментальная автоматическая отправка статьи базы знаний на сайт
     */
    suspend fun autoPushKnowledge(entry: KnowledgeBaseEntryEntity) = withContext(Dispatchers.IO) {
        if (!isAutoSyncEnabled() || getServerUrl().isBlank()) return@withContext
        try {
            val payload = JSONObject().apply {
                put("action", "push_knowledge")
                put("apiKey", getApiKey())
                put("entry", JSONObject().apply {
                    put("id", entry.id)
                    put("brand", entry.brand)
                    put("model", entry.model)
                    put("problem", entry.problem)
                    put("guideDataJson", entry.guideDataJson)
                    put("addedBy", entry.addedBy)
                    put("isSchematic", entry.isSchematic)
                    put("addedDate", entry.addedDate)
                })
            }
            sendQuickPost(payload)
        } catch (e: Exception) {
            Log.w("HostSync", "autoPushKnowledge error: ${e.message}")
        }
    }

    /**
     * Моментальная автоматическая отправка истории на сайт
     */
    suspend fun autoPushStory(story: StoryEntity) = withContext(Dispatchers.IO) {
        if (!isAutoSyncEnabled() || getServerUrl().isBlank()) return@withContext
        try {
            val payload = JSONObject().apply {
                put("action", "push_story")
                put("apiKey", getApiKey())
                put("story", JSONObject().apply {
                    put("id", story.id)
                    put("title", story.title)
                    put("subtitle", story.subtitle)
                    put("content", story.content)
                    put("authorName", story.authorName)
                    put("authorEmail", story.authorEmail)
                    put("authorServiceCenter", story.authorServiceCenter)
                    put("scope", story.scope)
                    put("iconEmoji", story.iconEmoji)
                    put("warningLevel", story.warningLevel)
                    put("isPermanent", story.isPermanent)
                    put("viewsCount", story.viewsCount)
                    put("taggedDeviceModel", story.taggedDeviceModel)
                    put("taggedCategory", story.taggedCategory)
                    put("mediaType", story.mediaType)
                    put("mediaUrl", story.mediaUrl)
                    put("createdAt", story.createdAt)
                })
            }
            sendQuickPost(payload)
        } catch (e: Exception) {
            Log.w("HostSync", "autoPushStory error: ${e.message}")
        }
    }

    private fun sendQuickPost(payload: JSONObject) {
        val url = getServerUrl()
        if (url.isBlank()) return
        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        httpClient.newCall(request).execute().close()
    }

    /**
     * Полный готовый код PHP-скрипта для загрузки на хостинг клиента
     */
    fun getPhpScriptTemplate(): String {
        return """
<?php
/**
 * Tech.Mate Hub — Автоматическая синхронизация с сайтом и хостингом (12 ГБ)
 * Разместите этот файл в папку сайта (например, public_html/techmate_sync.php)
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if (${'$'}_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// 1. Секретный ключ (должен совпадать с ключом в настройках приложения)
define('API_KEY', '${getApiKey()}');

// Папка для хранения данных (будет создана автоматически)
${'$'}dataDir = __DIR__ . '/techmate_data';
if (!is_dir(${'$'}dataDir)) {
    @mkdir(${'$'}dataDir, 0777, true);
}

${'$'}input = json_decode(file_get_contents('php://input'), true);
${'$'}key = ${'$'}input['apiKey'] ?? ${'$'}_GET['apiKey'] ?? '';

if (${'$'}key !== API_KEY) {
    http_response_code(403);
    echo json_encode(['success' => false, 'error' => 'Неверный API ключ синхронизации']);
    exit;
}

${'$'}action = ${'$'}input['action'] ?? ${'$'}_GET['action'] ?? 'ping';

// Функция чтения JSON-хранилища
function readStorage(${'$'}file) {
    global ${'$'}dataDir;
    ${'$'}path = ${'$'}dataDir . '/' . ${'$'}file;
    if (!file_exists(${'$'}path)) return [];
    ${'$'}content = @file_get_contents(${'$'}path);
    return json_decode(${'$'}content, true) ?: [];
}

// Функция записи JSON-хранилища
function writeStorage(${'$'}file, ${'$'}data) {
    global ${'$'}dataDir;
    ${'$'}path = ${'$'}dataDir . '/' . ${'$'}file;
    @file_put_contents(${'$'}path, json_encode(${'$'}data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
}

switch (${'$'}action) {
    case 'ping':
        echo json_encode([
            'success' => true,
            'status' => 'ok',
            'message' => 'Хостинг готов к работе. Место на диске доступно.',
            'timestamp' => time()
        ]);
        break;

    case 'push_post':
        ${'$'}posts = readStorage('posts.json');
        ${'$'}newPost = ${'$'}input['post'] ?? null;
        if (${'$'}newPost && !empty(${'$'}newPost['id'])) {
            ${'$'}posts[${'$'}newPost['id']] = ${'$'}newPost;
            writeStorage('posts.json', ${'$'}posts);
        }
        echo json_encode(['success' => true, 'count' => count(${'$'}posts)]);
        break;

    case 'push_comment':
        ${'$'}comments = readStorage('comments.json');
        ${'$'}newComment = ${'$'}input['comment'] ?? null;
        if (${'$'}newComment && !empty(${'$'}newComment['id'])) {
            ${'$'}comments[${'$'}newComment['id']] = ${'$'}newComment;
            writeStorage('comments.json', ${'$'}comments);
        }
        echo json_encode(['success' => true, 'count' => count(${'$'}comments)]);
        break;

    case 'push_knowledge':
        ${'$'}kb = readStorage('knowledge.json');
        ${'$'}newEntry = ${'$'}input['entry'] ?? null;
        if (${'$'}newEntry && !empty(${'$'}newEntry['id'])) {
            ${'$'}kb[${'$'}newEntry['id']] = ${'$'}newEntry;
            writeStorage('knowledge.json', ${'$'}kb);
        }
        echo json_encode(['success' => true, 'count' => count(${'$'}kb)]);
        break;

    case 'push_story':
        ${'$'}stories = readStorage('stories.json');
        ${'$'}newStory = ${'$'}input['story'] ?? null;
        if (${'$'}newStory && !empty(${'$'}newStory['id'])) {
            ${'$'}stories[${'$'}newStory['id']] = ${'$'}newStory;
            writeStorage('stories.json', ${'$'}stories);
        }
        echo json_encode(['success' => true, 'count' => count(${'$'}stories)]);
        break;

    case 'sync_bidirectional':
        // Объединение постов
        ${'$'}storedPosts = readStorage('posts.json');
        foreach ((${'$'}input['posts'] ?? []) as ${'$'}p) {
            if (!empty(${'$'}p['id'])) {
                ${'$'}storedPosts[${'$'}p['id']] = ${'$'}p;
            }
        }
        writeStorage('posts.json', ${'$'}storedPosts);

        // Объединение историй
        ${'$'}storedStories = readStorage('stories.json');
        foreach ((${'$'}input['stories'] ?? []) as ${'$'}s) {
            if (!empty(${'$'}s['id'])) {
                ${'$'}storedStories[${'$'}s['id']] = ${'$'}s;
            }
        }
        writeStorage('stories.json', ${'$'}storedStories);

        // Объединение базы знаний
        ${'$'}storedKb = readStorage('knowledge.json');
        foreach ((${'$'}input['knowledge'] ?? []) as ${'$'}k) {
            if (!empty(${'$'}k['id'])) {
                ${'$'}storedKb[${'$'}k['id']] = ${'$'}k;
            }
        }
        writeStorage('knowledge.json', ${'$'}storedKb);

        // Возвращаем все актуальные записи приложению
        echo json_encode([
            'success' => true,
            'remotePosts' => array_values(${'$'}storedPosts),
            'remoteStories' => array_values(${'$'}storedStories),
            'remoteKnowledge' => array_values(${'$'}storedKb)
        ]);
        break;

    case 'send_otp':
        ${'$'}recipient = trim(${'$'}input['recipient'] ?? '');
        ${'$'}code = trim(${'$'}input['code'] ?? '');
        ${'$'}senderEmail = trim(${'$'}input['senderEmail'] ?? '');
        ${'$'}senderName = trim(${'$'}input['senderName'] ?? 'Tech.Mate Сервис');

        if (empty(${'$'}recipient) || !filter_var(${'$'}recipient, FILTER_VALIDATE_EMAIL)) {
            echo json_encode(['success' => false, 'error' => 'Некорректный адрес email получателя']);
            break;
        }
        if (empty(${'$'}code)) {
            echo json_encode(['success' => false, 'error' => 'Код подтверждения не указан']);
            break;
        }

        if (empty(${'$'}senderEmail)) {
            ${'$'}domain = ${'$'}_SERVER['SERVER_NAME'] ?? 'mysite.ru';
            ${'$'}senderEmail = 'noreply@' . preg_replace('/^www\./', '', ${'$'}domain);
        }

        ${'$'}subject = "Код подтверждения Tech.Mate: " . ${'$'}code;
        ${'$'}html = '<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body style="margin:0;padding:20px;background:#f4f6f9;font-family:Arial,sans-serif;">'
              . '<div style="max-width:520px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 4px 12px rgba(0,0,0,0.06);">'
              . '<div style="background:linear-gradient(135deg,#0288D1 0%,#01579B 100%);padding:24px 20px;text-align:center;">'
              . '<h1 style="color:#fff;margin:0;font-size:24px;letter-spacing:0.5px;">Tech.Mate</h1>'
              . '<p style="color:#B3E5FC;margin:4px 0 0 0;font-size:13px;">Инженерный центр и база знаний мастеров</p>'
              . '</div>'
              . '<div style="padding:28px 24px;text-align:center;color:#333;">'
              . '<h2 style="font-size:18px;margin-top:0;color:#1e293b;">Ваш проверочный код</h2>'
              . '<p style="font-size:14px;color:#475569;line-height:1.5;margin:12px 0 20px 0;">'
              . 'Вы запросили вход в сервис Tech.Mate. Введите этот 6-значный код в приложении:'
              . '</p>'
              . '<div style="background:#f0f9ff;border:2px dashed #0288D1;border-radius:10px;padding:16px 24px;display:inline-block;margin-bottom:20px;">'
              . '<span style="font-size:34px;font-weight:bold;letter-spacing:8px;color:#0288D1;font-family:Consolas,monospace;">' . htmlspecialchars(${'$'}code) . '</span>'
              . '</div>'
              . '<p style="font-size:12px;color:#64748b;margin:0;">Код действителен в течение 10 минут. Если вы не запрашивали код, проигнорируйте письмо.</p>'
              . '</div>'
              . '<div style="background:#f8fafc;padding:14px 20px;text-align:center;border-top:1px solid #e2e8f0;">'
              . '<p style="margin:0;font-size:11px;color:#94a3b8;">Отправлено с корпоративного сервера ' . htmlspecialchars(${'$'}senderEmail) . '</p>'
              . '</div>'
              . '</div></body></html>';

        ${'$'}smtp = ${'$'}input['smtp'] ?? [];
        ${'$'}smtpSent = false;
        ${'$'}smtpError = '';

        if (!empty(${'$'}smtp['enabled']) && !empty(${'$'}smtp['host']) && !empty(${'$'}smtp['user'])) {
            ${'$'}host = ${'$'}smtp['host'];
            ${'$'}port = intval(${'$'}smtp['port'] ?? 465);
            ${'$'}user = ${'$'}smtp['user'];
            ${'$'}pass = ${'$'}smtp['pass'] ?? '';
            ${'$'}secure = strtoupper(${'$'}smtp['secure'] ?? 'SSL');

            ${'$'}prefix = (${'$'}secure === 'SSL' || ${'$'}port == 465) ? 'ssl://' : '';
            ${'$'}socket = @fsockopen(${'$'}prefix . ${'$'}host, ${'$'}port, ${'$'}errno, ${'$'}errstr, 10);
            if (${'$'}socket) {
                ${'$'}read = function() use (${'$'}socket) {
                    ${'$'}res = '';
                    while (${'$'}str = fgets(${'$'}socket, 515)) {
                        ${'$'}res .= ${'$'}str;
                        if (substr(${'$'}str, 3, 1) == " ") break;
                    }
                    return ${'$'}res;
                };
                ${'$'}write = function(${'$'}cmd) use (${'$'}socket) {
                    fputs(${'$'}socket, ${'$'}cmd . "\r\n");
                };

                ${'$'}read();
                ${'$'}write("EHLO " . (${'$'}_SERVER['SERVER_NAME'] ?? 'localhost'));
                ${'$'}read();
                ${'$'}write("AUTH LOGIN");
                ${'$'}read();
                ${'$'}write(base64_encode(${'$'}user));
                ${'$'}read();
                ${'$'}write(base64_encode(${'$'}pass));
                ${'$'}authRes = ${'$'}read();

                if (substr(${'$'}authRes, 0, 3) == '235') {
                    ${'$'}write("MAIL FROM: <" . ${'$'}user . ">");
                    ${'$'}read();
                    ${'$'}write("RCPT TO: <" . ${'$'}recipient . ">");
                    ${'$'}read();
                    ${'$'}write("DATA");
                    ${'$'}read();

                    ${'$'}headers = "MIME-Version: 1.0\r\n"
                                 . "Content-Type: text/html; charset=UTF-8\r\n"
                                 . "From: =?UTF-8?B?" . base64_encode(${'$'}senderName) . "?= <" . ${'$'}user . ">\r\n"
                                 . "To: <" . ${'$'}recipient . ">\r\n"
                                 . "Subject: =?UTF-8?B?" . base64_encode(${'$'}subject) . "?=\r\n"
                                 . "Date: " . date("r") . "\r\n";

                    ${'$'}write(${'$'}headers . "\r\n" . ${'$'}html . "\r\n.");
                    ${'$'}dataRes = ${'$'}read();
                    ${'$'}write("QUIT");
                    fclose(${'$'}socket);

                    if (substr(${'$'}dataRes, 0, 3) == '250') {
                        ${'$'}smtpSent = true;
                    } else {
                        ${'$'}smtpError = "SMTP Data: " . trim(${'$'}dataRes);
                    }
                } else {
                    ${'$'}write("QUIT");
                    fclose(${'$'}socket);
                    ${'$'}smtpError = "SMTP Auth error: " . trim(${'$'}authRes);
                }
            } else {
                ${'$'}smtpError = "Подключение к SMTP не удалось: " . ${'$'}errstr;
            }
        }

        if (${'$'}smtpSent) {
            echo json_encode([
                'success' => true,
                'message' => 'Код успешно отправлен через SMTP корпоративной почты (' . ${'$'}smtp['user'] . ')',
                'method' => 'smtp'
            ]);
            break;
        }

        // Отправка через локальную почтовую службу хостинга
        ${'$'}headers  = "MIME-Version: 1.0\r\n";
        ${'$'}headers .= "Content-Type: text/html; charset=UTF-8\r\n";
        ${'$'}headers .= "From: =?UTF-8?B?" . base64_encode(${'$'}senderName) . "?= <" . ${'$'}senderEmail . ">\r\n";
        ${'$'}headers .= "Reply-To: " . ${'$'}senderEmail . "\r\n";
        ${'$'}headers .= "X-Mailer: PHP/" . phpversion();

        ${'$'}encodedSubject = "=?UTF-8?B?" . base64_encode(${'$'}subject) . "?=";
        ${'$'}mailOk = @mail(${'$'}recipient, ${'$'}encodedSubject, ${'$'}html, ${'$'}headers, "-f" . ${'$'}senderEmail);
        if (!${'$'}mailOk) {
            ${'$'}mailOk = @mail(${'$'}recipient, ${'$'}encodedSubject, ${'$'}html, ${'$'}headers);
        }

        if (${'$'}mailOk) {
            echo json_encode([
                'success' => true,
                'message' => 'Код отправлен через корпоративную почту хостинга (' . ${'$'}senderEmail . ')' . (!empty(${'$'}smtpError) ? " [SMTP: " . ${'$'}smtpError . "]" : ""),
                'method' => 'hosting_mail'
            ]);
        } else {
            echo json_encode([
                'success' => false,
                'error' => 'Ошибка отправки почты хостингом.' . (!empty(${'$'}smtpError) ? " [SMTP: " . ${'$'}smtpError . "]" : "") . ' Проверьте поддержку mail() на хостинге или включите SMTP.'
            ]);
        }
        break;

    default:
        echo json_encode(['success' => false, 'error' => 'Неизвестное действие']);
        break;
}
""".trimIndent()
    }
}
