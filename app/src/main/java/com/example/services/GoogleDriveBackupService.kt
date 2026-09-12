package com.example.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class RestoreResult(
    val success: Boolean,
    val ordersRestored: Int = 0,
    val kbEntriesRestored: Int = 0,
    val storiesRestored: Int = 0,
    val message: String = ""
)

class GoogleDriveBackupService private constructor(private val context: Context) {

    private val dbService = DatabaseService.getInstance(context)

    companion object {
        @Volatile
        private var instance: GoogleDriveBackupService? = null

        fun getInstance(context: Context): GoogleDriveBackupService {
            return instance ?: synchronized(this) {
                instance ?: GoogleDriveBackupService(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Generates a complete JSON backup of the local database:
     * - Repair orders
     * - Knowledge base repair guides
     * - Stories & diagnostic alerts
     * - Boardview records
     */
    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // 1. Metadata
        val meta = JSONObject()
        meta.put("version", "3.0")
        meta.put("timestamp", System.currentTimeMillis())
        meta.put("appName", "TechMate Pro")
        meta.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("metadata", meta)

        // 2. Repair Orders
        val orders = dbService.repairOrderDao.getAllOrdersSync()
        val ordersArray = JSONArray()
        for (order in orders) {
            val o = JSONObject()
            o.put("id", order.id)
            o.put("serviceCenterId", order.serviceCenterId)
            o.put("deviceType", order.deviceType)
            o.put("brand", order.brand)
            o.put("model", order.model)
            o.put("serialNumber", order.serialNumber)
            o.put("clientName", order.clientName)
            o.put("clientPhone", order.clientPhone)
            o.put("declaredDefect", order.declaredDefect)
            o.put("diagnosticNotes", order.diagnosticNotes)
            o.put("repairStatus", order.repairStatus)
            o.put("urgency", order.urgency)
            o.put("estimatedCost", order.estimatedCost)
            o.put("finalCost", order.finalCost)
            o.put("assignedMasterId", order.assignedMasterId)
            o.put("assignedMasterName", order.assignedMasterName)
            o.put("createdAt", order.createdAt)
            o.put("updatedAt", order.updatedAt)
            ordersArray.put(o)
        }
        root.put("repair_orders", ordersArray)

        // 3. Knowledge Base
        val kbEntries = dbService.knowledgeDao.getAllEntriesSync()
        val kbArray = JSONArray()
        for (kb in kbEntries) {
            val k = JSONObject()
            k.put("id", kb.id)
            k.put("brand", kb.brand)
            k.put("model", kb.model)
            k.put("problem", kb.problem)
            k.put("guideDataJson", kb.guideDataJson)
            k.put("addedBy", kb.addedBy)
            k.put("addedDate", kb.addedDate)
            k.put("isSchematic", kb.isSchematic)
            kbArray.put(k)
        }
        root.put("knowledge_base", kbArray)

        // 4. Stories
        val stories = dbService.storyDao.getAllStoriesSync()
        val storiesArray = JSONArray()
        for (st in stories) {
            val s = JSONObject()
            s.put("id", st.id)
            s.put("title", st.title)
            s.put("subtitle", st.subtitle)
            s.put("content", st.content)
            s.put("authorName", st.authorName)
            s.put("authorEmail", st.authorEmail)
            s.put("authorServiceCenter", st.authorServiceCenter)
            s.put("authorServiceCenterId", st.authorServiceCenterId)
            s.put("scope", st.scope)
            s.put("isModeratedPublic", st.isModeratedPublic)
            s.put("iconEmoji", st.iconEmoji)
            s.put("warningLevel", st.warningLevel)
            s.put("isPermanent", st.isPermanent)
            s.put("viewsCount", st.viewsCount)
            s.put("taggedDeviceModel", st.taggedDeviceModel)
            s.put("taggedCategory", st.taggedCategory)
            s.put("mediaType", st.mediaType)
            s.put("mediaUrl", st.mediaUrl)
            s.put("createdAt", st.createdAt)
            storiesArray.put(s)
        }
        root.put("stories", storiesArray)

        // 5. Posts
        val posts = dbService.postDao.getAllPostsSync()
        val postsArray = JSONArray()
        for (post in posts) {
            val p = JSONObject()
            p.put("id", post.id)
            p.put("authorName", post.authorName)
            p.put("authorEmail", post.authorEmail)
            p.put("authorServiceCenter", post.authorServiceCenter)
            p.put("authorServiceCenterId", post.authorServiceCenterId)
            p.put("content", post.content)
            p.put("mediaType", post.mediaType)
            p.put("mediaUrl", post.mediaUrl)
            p.put("mediaTitle", post.mediaTitle)
            p.put("taggedDevice", post.taggedDevice)
            p.put("likesCount", post.likesCount)
            p.put("commentsCount", post.commentsCount)
            p.put("viewsCount", post.viewsCount)
            p.put("createdAt", post.createdAt)
            postsArray.put(p)
        }
        root.put("posts", postsArray)

        // 6. Post Comments
        val comments = dbService.postCommentDao.getAllCommentsSync()
        val commentsArray = JSONArray()
        for (c in comments) {
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("postId", c.postId)
            cObj.put("authorName", c.authorName)
            cObj.put("authorEmail", c.authorEmail)
            cObj.put("authorServiceCenter", c.authorServiceCenter)
            cObj.put("content", c.content)
            cObj.put("createdAt", c.createdAt)
            commentsArray.put(cObj)
        }
        root.put("post_comments", commentsArray)

        // 7. Offline Saved Knowledge Items
        val savedKbItems = dbService.savedKnowledgeDao.getAllItemsSync()
        val savedKbArray = JSONArray()
        for (item in savedKbItems) {
            val sk = JSONObject()
            sk.put("id", item.id)
            sk.put("query", item.query)
            sk.put("aiResponse", item.aiResponse)
            sk.put("timestamp", item.timestamp)
            sk.put("tags", item.tags)
            savedKbArray.put(sk)
        }
        root.put("saved_knowledge_items", savedKbArray)

        root.toString(2)
    }

    /**
     * Creates an automated persistent local backup file on device storage.
     */
    suspend fun createAutoBackupFile(): File = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val file = File(backupDir, "TechMate_AutoBackup.json")
        file.writeText(json)
        file
    }

    /**
     * Writes the backup JSON into a shareable cache file and opens Android's system share picker,
     * allowing the user to pick "Google Диск" (Google Drive) for free cloud storage.
     */
    suspend fun exportToGoogleDrive(context: Context): Uri = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "TechMate_GoogleDrive_Backup_$dateFormat.json"
        val backupFile = File(context.cacheDir, fileName)
        backupFile.writeText(json)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
        uri
    }

    /**
     * Safely exports and opens Google Drive upload on application context.
     */
    fun performSafeDriveExport(context: Context, onError: (String) -> Unit = {}) {
        FirebaseSyncService.getInstance(context).appScope.launch {
            try {
                val uri = exportToGoogleDrive(context)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    launchGoogleDriveShareIntent(context, uri)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError(e.message ?: "Ошибка экспорта")
                }
            }
        }
    }

    /**
     * Launches the system intent chooser to upload/save to Google Drive or share.
     */
    fun launchGoogleDriveShareIntent(context: Context, fileUri: Uri) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Резервная копия TechMate (Google Диск)")
            putExtra(Intent.EXTRA_TEXT, "Резервная копия базы данных ремонтов и историй TechMate Pro")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Сохранить в Google Диск или отправить")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Parses and restores data from a JSON string.
     */
    suspend fun restoreFromJson(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var ordersRestored = 0
            var kbRestored = 0
            var storiesRestored = 0

            // 1. Restore Orders
            if (root.has("repair_orders")) {
                val ordersArray = root.getJSONArray("repair_orders")
                for (i in 0 until ordersArray.length()) {
                    val o = ordersArray.getJSONObject(i)
                    val order = RepairOrderEntity(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        serviceCenterId = o.optString("serviceCenterId", "default"),
                        deviceType = o.optString("deviceType", "Смартфон"),
                        brand = o.optString("brand", ""),
                        model = o.optString("model", ""),
                        serialNumber = o.optString("serialNumber", ""),
                        clientName = o.optString("clientName", ""),
                        clientPhone = o.optString("clientPhone", ""),
                        declaredDefect = o.optString("declaredDefect", ""),
                        diagnosticNotes = o.optString("diagnosticNotes", ""),
                        repairStatus = o.optString("repairStatus", "new"),
                        urgency = o.optString("urgency", "normal"),
                        estimatedCost = o.optDouble("estimatedCost", 0.0),
                        finalCost = o.optDouble("finalCost", 0.0),
                        assignedMasterId = o.optString("assignedMasterId", ""),
                        assignedMasterName = o.optString("assignedMasterName", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                    dbService.repairOrderDao.insertOrder(order)
                    ordersRestored++
                }
            }

            // 2. Restore Knowledge Base
            if (root.has("knowledge_base")) {
                val kbArray = root.getJSONArray("knowledge_base")
                for (i in 0 until kbArray.length()) {
                    val k = kbArray.getJSONObject(i)
                    val entry = KnowledgeBaseEntryEntity(
                        id = k.optString("id", UUID.randomUUID().toString()),
                        brand = k.optString("brand", ""),
                        model = k.optString("model", ""),
                        problem = k.optString("problem", ""),
                        guideDataJson = k.optString("guideDataJson", "{}"),
                        addedBy = k.optString("addedBy", "Мастер"),
                        addedDate = k.optLong("addedDate", System.currentTimeMillis()),
                        isSchematic = k.optBoolean("isSchematic", false)
                    )
                    dbService.knowledgeDao.insertEntry(entry)
                    kbRestored++
                }
            }

            // 3. Restore Stories
            if (root.has("stories")) {
                val storiesArray = root.getJSONArray("stories")
                for (i in 0 until storiesArray.length()) {
                    val s = storiesArray.getJSONObject(i)
                    val story = StoryEntity(
                        id = s.optString("id", UUID.randomUUID().toString()),
                        title = s.optString("title", "Запись"),
                        subtitle = s.optString("subtitle", ""),
                        content = s.optString("content", ""),
                        authorName = s.optString("authorName", "Мастер"),
                        authorEmail = s.optString("authorEmail", ""),
                        authorServiceCenter = s.optString("authorServiceCenter", "СЦ"),
                        authorServiceCenterId = s.optString("authorServiceCenterId", "default_sc"),
                        scope = s.optString("scope", "LOCAL_SC"),
                        isModeratedPublic = s.optBoolean("isModeratedPublic", true),
                        iconEmoji = s.optString("iconEmoji", "🛠️"),
                        warningLevel = s.optString("warningLevel", "NORMAL"),
                        isPermanent = s.optBoolean("isPermanent", false),
                        viewsCount = s.optInt("viewsCount", 0),
                        taggedDeviceModel = s.optString("taggedDeviceModel", ""),
                        taggedCategory = s.optString("taggedCategory", ""),
                        mediaType = s.optString("mediaType", "PHOTO"),
                        mediaUrl = s.optString("mediaUrl", ""),
                        createdAt = s.optLong("createdAt", System.currentTimeMillis())
                    )
                    dbService.storyDao.insertStory(story)
                    storiesRestored++
                }
            }

            // 4. Restore Posts
            var postsRestored = 0
            if (root.has("posts")) {
                val postsArray = root.getJSONArray("posts")
                for (i in 0 until postsArray.length()) {
                    val p = postsArray.getJSONObject(i)
                    val post = PostEntity(
                        id = p.optString("id", UUID.randomUUID().toString()),
                        authorName = p.optString("authorName", "Мастер"),
                        authorEmail = p.optString("authorEmail", ""),
                        authorServiceCenter = p.optString("authorServiceCenter", "СЦ"),
                        authorServiceCenterId = p.optString("authorServiceCenterId", "default"),
                        content = p.optString("content", ""),
                        mediaType = p.optString("mediaType", "NONE"),
                        mediaUrl = p.optString("mediaUrl", ""),
                        mediaTitle = p.optString("mediaTitle", ""),
                        taggedDevice = p.optString("taggedDevice", ""),
                        likesCount = p.optInt("likesCount", 0),
                        commentsCount = p.optInt("commentsCount", 0),
                        viewsCount = p.optInt("viewsCount", 1),
                        createdAt = p.optLong("createdAt", System.currentTimeMillis())
                    )
                    dbService.postDao.insertPost(post)
                    postsRestored++
                }
            }

            // 5. Restore Saved Knowledge
            var savedKbRestored = 0
            if (root.has("saved_knowledge_items")) {
                val savedKbArray = root.getJSONArray("saved_knowledge_items")
                for (i in 0 until savedKbArray.length()) {
                    val sk = savedKbArray.getJSONObject(i)
                    val item = SavedKnowledgeItem(
                        id = sk.optString("id", UUID.randomUUID().toString()),
                        query = sk.optString("query", ""),
                        aiResponse = sk.optString("aiResponse", ""),
                        timestamp = sk.optLong("timestamp", System.currentTimeMillis()),
                        tags = sk.optString("tags", "")
                    )
                    dbService.savedKnowledgeDao.insertItem(item)
                    savedKbRestored++
                }
            }

            RestoreResult(
                success = true,
                ordersRestored = ordersRestored,
                kbEntriesRestored = kbRestored,
                storiesRestored = storiesRestored,
                message = "Восстановлено: заказов ($ordersRestored), постов ($postsRestored), инструкций ($kbRestored), базы знаний ($savedKbRestored)"
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "Ошибка разбора файла бэкапа: ${e.message}"
            )
        }
    }
}
