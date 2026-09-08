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
     * 1. Pushes local repair orders, knowledge base articles, device models, and user profiles to Firebase.
     * 2. Pulls new repair orders and knowledge base items from Firebase into local Room DB.
     */
    suspend fun performFullSync(): SyncReport = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore == null) {
            val report = SyncReport(
                lastSyncTimestamp = System.currentTimeMillis(),
                state = SyncState.OFFLINE_ONLY,
                syncedItemsCount = 0,
                message = "Локальный режим (Google Services подключаются автоматически при публикации)"
            )
            _syncStatus.value = report
            return@withContext report
        }

        _syncStatus.value = SyncReport(
            state = SyncState.SYNCING,
            message = "Синхронизация с серверами Google Cloud / Firestore..."
        )

        var totalItems = 0

        try {
            // 1. Sync Repair Orders
            val localOrders = dbService.repairOrderDao.getAllOrdersSync()
            val ordersCollection = firestore.collection("repair_orders")

            for (order in localOrders) {
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
                totalItems++
            }

            // Pull cloud orders
            val remoteOrdersSnapshot = ordersCollection.limit(100).get().await()
            for (doc in remoteOrdersSnapshot.documents) {
                val data = doc.data ?: continue
                val remoteOrder = RepairOrderEntity(
                    id = doc.id,
                    serviceCenterId = data["serviceCenterId"] as? String ?: "default",
                    deviceType = data["deviceType"] as? String ?: "Устройство",
                    brand = data["brand"] as? String ?: "",
                    model = data["model"] as? String ?: "",
                    serialNumber = data["serialNumber"] as? String ?: "",
                    clientName = data["clientName"] as? String ?: "",
                    clientPhone = data["clientPhone"] as? String ?: "",
                    declaredDefect = data["declaredDefect"] as? String ?: "",
                    diagnosticNotes = data["diagnosticNotes"] as? String ?: "",
                    repairStatus = data["repairStatus"] as? String ?: "new",
                    urgency = data["urgency"] as? String ?: "normal",
                    estimatedCost = (data["estimatedCost"] as? Number)?.toDouble() ?: 0.0,
                    finalCost = (data["finalCost"] as? Number)?.toDouble() ?: 0.0,
                    assignedMasterId = data["assignedMasterId"] as? String ?: "",
                    assignedMasterName = data["assignedMasterName"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                dbService.repairOrderDao.insertOrder(remoteOrder)
            }

            // 2. Sync Knowledge Base Articles
            val localEntries = dbService.knowledgeDao.getAllEntriesSync()
            val kbCollection = firestore.collection("knowledge_base")

            for (entry in localEntries) {
                val entryMap = hashMapOf(
                    "id" to entry.id,
                    "brand" to entry.brand,
                    "model" to entry.model,
                    "problem" to entry.problem,
                    "guideDataJson" to entry.guideDataJson,
                    "addedBy" to entry.addedBy,
                    "addedDate" to entry.addedDate,
                    "isSchematic" to entry.isSchematic
                )
                kbCollection.document(entry.id).set(entryMap, SetOptions.merge()).await()
                totalItems++
            }

            // Pull cloud knowledge base entries
            val remoteKbSnapshot = kbCollection.limit(100).get().await()
            for (doc in remoteKbSnapshot.documents) {
                val data = doc.data ?: continue
                val remoteEntry = KnowledgeBaseEntryEntity(
                    id = doc.id,
                    brand = data["brand"] as? String ?: "",
                    model = data["model"] as? String ?: "",
                    problem = data["problem"] as? String ?: "",
                    guideDataJson = data["guideDataJson"] as? String ?: "",
                    addedBy = data["addedBy"] as? String ?: "Мастер",
                    addedDate = (data["addedDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSchematic = data["isSchematic"] as? Boolean ?: false
                )
                dbService.knowledgeDao.insertEntry(remoteEntry)
            }

            // 3. Sync User Profile (if logged in)
            val currentUser = AuthService.getInstance(context).currentUser.value
            if (currentUser != null) {
                val userMap = hashMapOf(
                    "id" to currentUser.id,
                    "username" to currentUser.username,
                    "email" to currentUser.email,
                    "name" to currentUser.name,
                    "role" to currentUser.role,
                    "serviceCenterId" to (currentUser.serviceCenterId ?: ""),
                    "serviceCenterName" to (currentUser.serviceCenterName ?: ""),
                    "avatarUrl" to (currentUser.avatarUrl ?: ""),
                    "lastActiveAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(currentUser.id)
                    .set(userMap, SetOptions.merge()).await()
                totalItems++
            }

            // 4. Sync Invitations
            val localInvitations = dbService.invitationDao.getAllInvitations()
            val invSnapshot = firestore.collection("invitations").limit(100).get().await()
            for (doc in invSnapshot.documents) {
                val data = doc.data ?: continue
                val remoteInv = InvitationEntity(
                    id = doc.id,
                    inviteCode = data["inviteCode"] as? String ?: "",
                    serviceCenterId = data["serviceCenterId"] as? String ?: "",
                    serviceCenterName = data["serviceCenterName"] as? String ?: "",
                    createdByAdminEmail = data["createdByAdminEmail"] as? String ?: "",
                    createdByAdminName = data["createdByAdminName"] as? String ?: "",
                    targetRole = data["targetRole"] as? String ?: "master",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 7 * 86400000L),
                    isUsed = data["isUsed"] as? Boolean ?: false,
                    usedByUserId = data["usedByUserId"] as? String,
                    usedByUserName = data["usedByUserName"] as? String,
                    usedByUserEmail = data["usedByUserEmail"] as? String,
                    usedAt = (data["usedAt"] as? Number)?.toLong()
                )
                dbService.invitationDao.insertInvitation(remoteInv)
                totalItems++
            }

            // 5. Sync Support Tickets & Messages
            val ticketsSnapshot = firestore.collection("support_tickets").limit(100).get().await()
            for (doc in ticketsSnapshot.documents) {
                val data = doc.data ?: continue
                val remoteTicket = SupportTicketEntity(
                    id = doc.id,
                    userId = data["userId"] as? String ?: "",
                    userEmail = data["userEmail"] as? String ?: "",
                    userName = data["userName"] as? String ?: "",
                    serviceCenterName = data["serviceCenterName"] as? String ?: "",
                    subject = data["subject"] as? String ?: "Запрос в поддержку",
                    status = data["status"] as? String ?: "open",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                dbService.supportTicketDao.insertTicket(remoteTicket)
                totalItems++
            }

            // 6. Pull Event Theme & Launch Animation
            pullEventTheme()

            // 7. Sync Stories (Diagnostic alerts & repair cases)
            val localStories = dbService.storyDao.getAllStoriesSync()
            val storiesCollection = firestore.collection("stories")

            for (st in localStories) {
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
                totalItems++
            }

            // Pull remote stories from Firestore
            val remoteStoriesSnapshot = storiesCollection.limit(50).get().await()
            for (doc in remoteStoriesSnapshot.documents) {
                val data = doc.data ?: continue
                val remoteStory = StoryEntity(
                    id = doc.id,
                    title = data["title"] as? String ?: "Запись",
                    subtitle = data["subtitle"] as? String ?: "",
                    content = data["content"] as? String ?: "",
                    authorName = data["authorName"] as? String ?: "Мастер",
                    authorEmail = data["authorEmail"] as? String ?: "",
                    authorServiceCenter = data["authorServiceCenter"] as? String ?: "СЦ",
                    authorServiceCenterId = data["authorServiceCenterId"] as? String ?: "default_sc",
                    scope = data["scope"] as? String ?: "LOCAL_SC",
                    isModeratedPublic = data["isModeratedPublic"] as? Boolean ?: true,
                    iconEmoji = data["iconEmoji"] as? String ?: "🛠️",
                    warningLevel = data["warningLevel"] as? String ?: "NORMAL",
                    isPermanent = data["isPermanent"] as? Boolean ?: false,
                    viewsCount = (data["viewsCount"] as? Number)?.toInt() ?: 0,
                    taggedDeviceModel = data["taggedDeviceModel"] as? String ?: "",
                    taggedCategory = data["taggedCategory"] as? String ?: "",
                    mediaType = data["mediaType"] as? String ?: "PHOTO",
                    mediaUrl = data["mediaUrl"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                dbService.storyDao.insertStory(remoteStory)
                totalItems++
            }

            val successReport = SyncReport(
                lastSyncTimestamp = System.currentTimeMillis(),
                state = SyncState.SUCCESS,
                syncedItemsCount = totalItems,
                message = "Успешно синхронизировано объектов: $totalItems"
            )
            _syncStatus.value = successReport
            successReport
        } catch (e: Exception) {
            Log.e("FirebaseSyncService", "Sync error: ${e.message}", e)
            val errReport = SyncReport(
                lastSyncTimestamp = System.currentTimeMillis(),
                state = SyncState.ERROR,
                syncedItemsCount = totalItems,
                message = "Ошибка синхронизации: ${e.message ?: "Сеть недоступна"}"
            )
            _syncStatus.value = errReport
            errReport
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
