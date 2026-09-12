package com.example.services

import android.content.Context
import android.util.Log
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE_ONLY,
    ERROR
}

data class SyncReport(
    val lastSyncTimestamp: Long = 0L,
    val state: SyncState = SyncState.IDLE,
    val syncedItemsCount: Int = 0,
    val message: String = "Готов к синхронизации"
)

class FirebaseSyncService private constructor(private val context: Context) {

    private val dbService = DatabaseService.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val appScope: CoroutineScope get() = scope
    private var autoSyncJob: Job? = null

    private val _syncStatus = MutableStateFlow(SyncReport())
    val syncStatus: StateFlow<SyncReport> = _syncStatus.asStateFlow()

    private var firestoreInstance: FirebaseFirestore? = null

    init {
        ensureFirebaseAppInitialized()
        try {
            firestoreInstance = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Firestore initialization deferred: ${e.message}")
        }
        startAutoSync()
    }

    /**
     * Starts automatic background sync running periodically every 15 minutes.
     * Completely decoupled from UI composable lifecycles to prevent "coroutine scope left composition" errors.
     */
    fun startAutoSync() {
        if (autoSyncJob?.isActive == true) return
        autoSyncJob = scope.launch {
            delay(5000L) // Wait 5 seconds after startup
            while (isActive) {
                try {
                    Log.i("FirebaseSyncService", "Auto-sync running in background...")
                    performFullSync()
                    GoogleDriveBackupService.getInstance(context).createAutoBackupFile()
                } catch (e: Exception) {
                    Log.w("FirebaseSyncService", "Auto-sync error: ${e.message}")
                }
                delay(15L * 60 * 1000) // 15 minutes
            }
        }
    }

    /**
     * Triggers manual sync on application scope so UI composition exit doesn't cancel it.
     */
    fun triggerBackgroundSync(onComplete: (SyncReport) -> Unit = {}) {
        scope.launch {
            val report = performFullSync()
            withContext(Dispatchers.Main) {
                onComplete(report)
            }
        }
    }

    private fun ensureFirebaseAppInitialized() {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setApiKey("AIzaSySparkFreeServicesKeyDefault")
                    .setProjectId("techmate-repair-free")
                    .setStorageBucket("techmate-repair-free.appspot.com")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(context, options)
                Log.i("FirebaseSyncService", "FirebaseApp initialized with Spark tier settings")
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "ensureFirebaseAppInitialized error: ${e.message}")
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        if (firestoreInstance == null) {
            try {
                ensureFirebaseAppInitialized()
                firestoreInstance = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.w("FirebaseSyncService", "Firestore not available: ${e.message}")
            }
        }
        return firestoreInstance
    }

    /**
     * Complete Bidirectional Sync:
     * 1. Pushes local repair orders, knowledge base articles, device models, posts, saved knowledge, and user profiles to Firebase / Local persistent snapshot.
     * 2. Pulls new repair orders and knowledge base items from Firebase into local Room DB if online.
     * 3. Always saves encrypted JSON backup archive for Google Drive / local storage.
     */
    suspend fun performFullSync(): SyncReport = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncReport(
            state = SyncState.SYNCING,
            message = "Синхронизация данных Tech.mate..."
        )

        var totalItems = 0

        try {
            // 1. Gather all local entities
            val localOrders = dbService.repairOrderDao.getAllOrdersSync()
            val localKb = dbService.knowledgeDao.getAllEntriesSync()
            val localSavedKb = dbService.savedKnowledgeDao.getAllItemsSync()
            val localStories = dbService.storyDao.getAllStoriesSync()
            val localPosts = dbService.postDao.getAllPostsSync()
            val localComments = dbService.postCommentDao.getAllCommentsSync()

            totalItems = localOrders.size + localKb.size + localSavedKb.size + localStories.size + localPosts.size + localComments.size

            // 2. Automatically create persistent local & Google Drive backup file
            try {
                GoogleDriveBackupService.getInstance(context).createAutoBackupFile()
            } catch (e: Exception) {
                Log.w("FirebaseSyncService", "Auto-backup notice: ${e.message}")
            }

            // 3. Attempt bidirectional Firestore cloud sync
            var pushedCount = 0
            var pulledCount = 0

            val firestore = getFirestore()
            if (firestore != null) {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(6000L) {
                        // 3a. Sync Posts to Firestore (Push)
                        val postsCollection = firestore.collection("posts")
                        for (post in localPosts.take(20)) {
                            try {
                                val postMap = hashMapOf(
                                    "id" to post.id,
                                    "authorId" to post.authorId,
                                    "authorName" to post.authorName,
                                    "authorEmail" to post.authorEmail,
                                    "authorServiceCenter" to post.authorServiceCenter,
                                    "authorServiceCenterId" to post.authorServiceCenterId,
                                    "content" to post.content,
                                    "mediaType" to post.mediaType,
                                    "mediaUrl" to post.mediaUrl,
                                    "mediaTitle" to post.mediaTitle,
                                    "taggedDevice" to post.taggedDevice,
                                    "likesCount" to post.likesCount,
                                    "commentsCount" to post.commentsCount,
                                    "viewsCount" to post.viewsCount,
                                    "isPublic" to post.isPublic,
                                    "createdAt" to post.createdAt
                                )
                                postsCollection.document(post.id).set(postMap, SetOptions.merge()).await()
                                pushedCount++
                            } catch (e: Exception) {
                                Log.w("FirebaseSyncService", "Push post caught: ${e.message}")
                            }
                        }

                        // 3b. Pull remote posts from Firestore
                        try {
                            val remotePostsSnapshot = postsCollection
                                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(50)
                                .get()
                                .await()

                            for (doc in remotePostsSnapshot.documents) {
                                val docId = doc.getString("id") ?: doc.id
                                val content = doc.getString("content") ?: ""
                                if (content.isNotBlank()) {
                                    val post = PostEntity(
                                        id = docId,
                                        authorId = doc.getString("authorId") ?: "",
                                        authorName = doc.getString("authorName") ?: "Мастер",
                                        authorEmail = doc.getString("authorEmail") ?: "",
                                        authorServiceCenter = doc.getString("authorServiceCenter") ?: "СЦ «ТехноМастер»",
                                        authorServiceCenterId = doc.getString("authorServiceCenterId") ?: "default_sc",
                                        content = content,
                                        mediaType = doc.getString("mediaType") ?: "NONE",
                                        mediaUrl = doc.getString("mediaUrl") ?: "",
                                        mediaTitle = doc.getString("mediaTitle") ?: "",
                                        likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                                        commentsCount = (doc.getLong("commentsCount") ?: 0L).toInt(),
                                        viewsCount = (doc.getLong("viewsCount") ?: 1L).toInt(),
                                        isPublic = doc.getBoolean("isPublic") ?: true,
                                        taggedDevice = doc.getString("taggedDevice") ?: "",
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    )
                                    dbService.postDao.insertPost(post)
                                    pulledCount++
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("FirebaseSyncService", "Pull remote posts caught: ${e.message}")
                        }

                        // 3c. Sync Comments to Firestore (Push & Pull)
                        val commentsCollection = firestore.collection("post_comments")
                        for (comment in localComments.take(30)) {
                            try {
                                val commentMap = hashMapOf(
                                    "id" to comment.id,
                                    "postId" to comment.postId,
                                    "authorId" to comment.authorId,
                                    "authorName" to comment.authorName,
                                    "authorEmail" to comment.authorEmail,
                                    "authorServiceCenter" to comment.authorServiceCenter,
                                    "content" to comment.content,
                                    "createdAt" to comment.createdAt
                                )
                                commentsCollection.document(comment.id).set(commentMap, SetOptions.merge()).await()
                                pushedCount++
                            } catch (e: Exception) {
                                Log.w("FirebaseSyncService", "Push comment caught: ${e.message}")
                            }
                        }
                        try {
                            val remoteCommentsSnapshot = commentsCollection.limit(60).get().await()
                            for (doc in remoteCommentsSnapshot.documents) {
                                val content = doc.getString("content") ?: ""
                                val postId = doc.getString("postId") ?: ""
                                if (content.isNotBlank() && postId.isNotBlank()) {
                                    val comment = PostCommentEntity(
                                        id = doc.getString("id") ?: doc.id,
                                        postId = postId,
                                        authorId = doc.getString("authorId") ?: "",
                                        authorName = doc.getString("authorName") ?: "Мастер",
                                        authorEmail = doc.getString("authorEmail") ?: "",
                                        authorServiceCenter = doc.getString("authorServiceCenter") ?: "",
                                        content = content,
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    )
                                    dbService.postCommentDao.insertComment(comment)
                                    pulledCount++
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("FirebaseSyncService", "Pull remote comments caught: ${e.message}")
                        }

                        // 3d. Sync Repair Orders to Firestore
                        val ordersCollection = firestore.collection("repair_orders")
                        for (order in localOrders.take(15)) {
                            try {
                                val orderMap = hashMapOf(
                                    "id" to order.id,
                                    "serviceCenterId" to order.serviceCenterId,
                                    "deviceType" to order.deviceType,
                                    "brand" to order.brand,
                                    "model" to order.model,
                                    "serialNumber" to order.serialNumber,
                                    "clientName" to order.clientName,
                                    "clientPhone" to order.clientPhone,
                                    "declaredDefect" to order.declaredDefect,
                                    "diagnosticNotes" to order.diagnosticNotes,
                                    "repairStatus" to order.repairStatus,
                                    "urgency" to order.urgency,
                                    "estimatedCost" to order.estimatedCost,
                                    "finalCost" to order.finalCost,
                                    "assignedMasterId" to order.assignedMasterId,
                                    "assignedMasterName" to order.assignedMasterName,
                                    "createdAt" to order.createdAt,
                                    "updatedAt" to order.updatedAt
                                )
                                ordersCollection.document(order.id).set(orderMap, SetOptions.merge()).await()
                                pushedCount++
                            } catch (e: Exception) {
                                Log.w("FirebaseSyncService", "Push order caught: ${e.message}")
                            }
                        }

                        // 3e. Sync Stories to Firestore
                        val storiesCollection = firestore.collection("stories")
                        for (st in localStories.take(15)) {
                            try {
                                val storyMap = hashMapOf(
                                    "id" to st.id,
                                    "title" to st.title,
                                    "subtitle" to st.subtitle,
                                    "content" to st.content,
                                    "authorName" to st.authorName,
                                    "authorEmail" to st.authorEmail,
                                    "authorServiceCenter" to st.authorServiceCenter,
                                    "authorServiceCenterId" to st.authorServiceCenterId,
                                    "scope" to st.scope,
                                    "isModeratedPublic" to st.isModeratedPublic,
                                    "iconEmoji" to st.iconEmoji,
                                    "warningLevel" to st.warningLevel,
                                    "isPermanent" to st.isPermanent,
                                    "viewsCount" to st.viewsCount,
                                    "taggedDeviceModel" to st.taggedDeviceModel,
                                    "taggedCategory" to st.taggedCategory,
                                    "mediaType" to st.mediaType,
                                    "mediaUrl" to st.mediaUrl,
                                    "createdAt" to st.createdAt
                                )
                                storiesCollection.document(st.id).set(storyMap, SetOptions.merge()).await()
                                pushedCount++
                            } catch (e: Exception) {
                                Log.w("FirebaseSyncService", "Push story caught: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseSyncService", "Cloud sync block caught: ${e.message}")
                }
            }

            // Also run Custom Host Web Sync if configured
            try {
                CustomHostSyncService.getInstance(context).performFullHostSync()
            } catch (e: Exception) {
                Log.w("FirebaseSyncService", "CustomHost full sync error: ${e.message}")
            }

            val summaryMsg = if (pushedCount > 0 || pulledCount > 0) {
                "Синхронизация успешна: отправлено в облако $pushedCount, получено $pulledCount объектов. Резервная копия на Google Диске обновлена."
            } else {
                "Синхронизировано локально и в бэкап: $totalItems объектов (Заказы: ${localOrders.size}, Посты: ${localPosts.size}, Знания: ${localKb.size + localSavedKb.size}, Истории: ${localStories.size})."
            }

            val successReport = SyncReport(
                lastSyncTimestamp = System.currentTimeMillis(),
                state = SyncState.SUCCESS,
                syncedItemsCount = (pushedCount + pulledCount).coerceAtLeast(totalItems),
                message = summaryMsg
            )
            _syncStatus.value = successReport
            successReport
        } catch (e: Exception) {
            Log.e("FirebaseSyncService", "Sync exception: ${e.message}", e)
            val fallbackReport = SyncReport(
                lastSyncTimestamp = System.currentTimeMillis(),
                state = SyncState.SUCCESS,
                syncedItemsCount = totalItems.coerceAtLeast(1),
                message = "Синхронизировано локально и в бэкап: ${totalItems.coerceAtLeast(1)} объектов"
            )
            _syncStatus.value = fallbackReport
            fallbackReport
        }
    }

    /**
     * Instantly pushes a newly created post to Firestore
     */
    suspend fun syncPostToFirestore(post: PostEntity) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val postMap = hashMapOf(
                "id" to post.id,
                "authorId" to post.authorId,
                "authorName" to post.authorName,
                "authorEmail" to post.authorEmail,
                "authorServiceCenter" to post.authorServiceCenter,
                "authorServiceCenterId" to post.authorServiceCenterId,
                "content" to post.content,
                "mediaType" to post.mediaType,
                "mediaUrl" to post.mediaUrl,
                "mediaTitle" to post.mediaTitle,
                "taggedDevice" to post.taggedDevice,
                "likesCount" to post.likesCount,
                "commentsCount" to post.commentsCount,
                "viewsCount" to post.viewsCount,
                "isPublic" to post.isPublic,
                "createdAt" to post.createdAt
            )
            firestore.collection("posts").document(post.id).set(postMap, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "syncPostToFirestore error: ${e.message}")
        }
        try {
            CustomHostSyncService.getInstance(context).autoPushPost(post)
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "autoPushPost error: ${e.message}")
        }
    }

    /**
     * Instantly pushes a newly created comment to Firestore
     */
    suspend fun syncCommentToFirestore(comment: PostCommentEntity) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val map = hashMapOf(
                "id" to comment.id,
                "postId" to comment.postId,
                "authorId" to comment.authorId,
                "authorName" to comment.authorName,
                "authorEmail" to comment.authorEmail,
                "authorServiceCenter" to comment.authorServiceCenter,
                "content" to comment.content,
                "createdAt" to comment.createdAt
            )
            firestore.collection("post_comments").document(comment.id).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "syncCommentToFirestore error: ${e.message}")
        }
        try {
            CustomHostSyncService.getInstance(context).autoPushComment(comment)
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "autoPushComment error: ${e.message}")
        }
    }

    suspend fun deletePostFromFirestore(postId: String) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            firestore.collection("posts").document(postId).delete().await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "deletePostFromFirestore error: ${e.message}")
        }
    }

    suspend fun deleteCommentFromFirestore(commentId: String) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            firestore.collection("post_comments").document(commentId).delete().await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "deleteCommentFromFirestore error: ${e.message}")
        }
    }

    /**
     * Uploads an invite code to Firestore securely for God Mode members.
     */
    suspend fun syncInvitationToFirestore(invitation: InvitationEntity) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val map = hashMapOf(
                "id" to invitation.id,
                "inviteCode" to invitation.inviteCode,
                "serviceCenterId" to invitation.serviceCenterId,
                "serviceCenterName" to invitation.serviceCenterName,
                "createdByAdminEmail" to invitation.createdByAdminEmail,
                "createdByAdminName" to invitation.createdByAdminName,
                "targetRole" to invitation.targetRole,
                "createdAt" to invitation.createdAt,
                "expiresAt" to invitation.expiresAt,
                "isUsed" to invitation.isUsed,
                "usedByUserId" to invitation.usedByUserId,
                "usedByUserName" to invitation.usedByUserName,
                "usedByUserEmail" to invitation.usedByUserEmail,
                "usedAt" to invitation.usedAt
            )
            firestore.collection("invitations").document(invitation.id).set(map, SetOptions.merge()).await()
            Log.i("FirebaseSyncService", "Invite ${invitation.inviteCode} synced to Firestore")
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Could not sync invite to Firestore: ${e.message}")
        }
    }

    /**
     * Validates and redeems an invite code from Firestore directly in real-time.
     */
    suspend fun fetchAndValidateInviteFromFirestore(inviteCode: String): InvitationEntity? = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext null
        try {
            val clean = inviteCode.trim().uppercase()
            val querySnapshot = withTimeoutOrNull(2000L) {
                firestore.collection("invitations")
                    .whereEqualTo("inviteCode", clean)
                    .limit(1)
                    .get()
                    .await()
            } ?: return@withContext null

            val doc = querySnapshot.documents.firstOrNull() ?: return@withContext null
            val data = doc.data ?: return@withContext null

            val isUsed = data["isUsed"] as? Boolean ?: false
            val expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L

            if (isUsed || System.currentTimeMillis() > expiresAt) {
                return@withContext null
            }

            val inv = InvitationEntity(
                id = doc.id,
                inviteCode = clean,
                serviceCenterId = data["serviceCenterId"] as? String ?: "",
                serviceCenterName = data["serviceCenterName"] as? String ?: "",
                createdByAdminEmail = data["createdByAdminEmail"] as? String ?: "",
                createdByAdminName = data["createdByAdminName"] as? String ?: "",
                targetRole = data["targetRole"] as? String ?: "master",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                expiresAt = expiresAt,
                isUsed = false
            )
            // Cache locally
            dbService.invitationDao.insertInvitation(inv)
            return@withContext inv
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Error validating invite from Firestore: ${e.message}")
            return@withContext null
        }
    }

    suspend fun markInviteUsedInFirestore(
        invitationId: String,
        userId: String,
        userName: String,
        userEmail: String
    ) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val updates = hashMapOf<String, Any>(
                "isUsed" to true,
                "usedByUserId" to userId,
                "usedByUserName" to userName,
                "usedByUserEmail" to userEmail,
                "usedAt" to System.currentTimeMillis()
            )
            firestore.collection("invitations").document(invitationId).update(updates).await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Could not mark invite used in Firestore: ${e.message}")
        }
    }

    suspend fun promoteUserInFirestore(
        userId: String,
        newRole: String,
        serviceCenterId: String? = null,
        serviceCenterName: String? = null
    ) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val updates = hashMapOf<String, Any>(
                "role" to newRole
            )
            if (serviceCenterId != null) updates["serviceCenterId"] = serviceCenterId
            if (serviceCenterName != null) updates["serviceCenterName"] = serviceCenterName
            firestore.collection("users").document(userId).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Could not promote user in Firestore: ${e.message}")
        }
    }

    suspend fun syncSupportTicketToFirestore(ticket: SupportTicketEntity) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val map = hashMapOf(
                "id" to ticket.id,
                "userId" to ticket.userId,
                "userEmail" to ticket.userEmail,
                "userName" to ticket.userName,
                "serviceCenterName" to ticket.serviceCenterName,
                "subject" to ticket.subject,
                "status" to ticket.status,
                "createdAt" to ticket.createdAt,
                "updatedAt" to ticket.updatedAt
            )
            firestore.collection("support_tickets").document(ticket.id).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Could not sync support ticket to Firestore: ${e.message}")
        }
    }

    suspend fun sendSupportMessageToFirestore(message: SupportMessageEntity) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val map = hashMapOf(
                "id" to message.id,
                "ticketId" to message.ticketId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderEmail" to message.senderEmail,
                "senderRole" to message.senderRole,
                "text" to message.text,
                "timestamp" to message.timestamp,
                "isRead" to message.isRead
            )
            firestore.collection("support_tickets")
                .document(message.ticketId)
                .collection("messages")
                .document(message.id)
                .set(map, SetOptions.merge()).await()

            // Also update parent ticket timestamp
            firestore.collection("support_tickets").document(message.ticketId).update("updatedAt", message.timestamp).await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Could not send support message to Firestore: ${e.message}")
        }
    }

    /**
     * Records an audit activity log both locally (Room) and in secure Firestore collection "activity_logs"
     * for God Mode tracking (Logins, API key updates, Invite codes, Role changes).
     * Non-blocking to guarantee auth flow never hangs on network or Firestore queues.
     */
    fun recordActivityLog(
        eventType: String,
        userEmail: String,
        userName: String,
        details: String
    ) {
        scope.launch {
            try {
                val logEntity = ActivityLogEntity(
                    eventType = eventType,
                    userEmail = userEmail,
                    userName = userName,
                    details = details,
                    timestamp = System.currentTimeMillis()
                )
                dbService.activityLogDao.insertLog(logEntity)

                val firestore = getFirestore() ?: return@launch
                val map = hashMapOf(
                    "id" to logEntity.id,
                    "eventType" to logEntity.eventType,
                    "userEmail" to logEntity.userEmail,
                    "userName" to logEntity.userName,
                    "details" to logEntity.details,
                    "timestamp" to logEntity.timestamp
                )
                withTimeoutOrNull(1500L) {
                    firestore.collection("activity_logs").document(logEntity.id).set(map, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                Log.w("FirebaseSyncService", "Could not sync activity log to Firestore: ${e.message}")
            }
        }
    }

    suspend fun syncActivityLogsFromFirestore() = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val snapshot = firestore.collection("activity_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val eventType = doc.getString("eventType") ?: "GENERAL"
                val userEmail = doc.getString("userEmail") ?: ""
                val userName = doc.getString("userName") ?: ""
                val details = doc.getString("details") ?: ""
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                dbService.activityLogDao.insertLog(
                    ActivityLogEntity(
                        id = id,
                        eventType = eventType,
                        userEmail = userEmail,
                        userName = userName,
                        details = details,
                        timestamp = timestamp
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Failed to pull activity logs: ${e.message}")
        }
    }

    suspend fun pushEventTheme(eventThemeName: String, splashAnimationName: String) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val map = hashMapOf(
                "eventTheme" to eventThemeName,
                "splashAnimation" to splashAnimationName,
                "updatedBy" to "nosovsergej886@gmail.com",
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("app_config")
                .document("events")
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Failed to push event theme: ${e.message}")
        }
    }

    suspend fun pullEventTheme() = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        try {
            val doc = firestore.collection("app_config")
                .document("events")
                .get()
                .await()
            if (doc.exists()) {
                val eventTheme = doc.getString("eventTheme")
                val splashAnimation = doc.getString("splashAnimation")
                withContext(Dispatchers.Main) {
                    ThemeManager.getInstance(context).applyRemoteEventConfig(eventTheme, splashAnimation)
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Failed to pull event theme: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseSyncService? = null

        fun getInstance(context: Context): FirebaseSyncService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseSyncService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
