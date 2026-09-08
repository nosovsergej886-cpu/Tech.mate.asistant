package com.example.data.db

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE serviceCenterId = :serviceCenterId ORDER BY createdAt ASC")
    fun getUsersByServiceCenter(serviceCenterId: String): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT COUNT(*) FROM users WHERE serviceCenterId = :serviceCenterId")
    suspend fun getMastersCountForService(serviceCenterId: String): Int

    @Query("SELECT * FROM users WHERE username = :username OR email = :username OR phoneOrTelegram = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email OR phoneOrTelegram = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneOrTelegram = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String)

    @Query("UPDATE users SET isBlocked = :isBlocked WHERE id = :id")
    suspend fun updateBlockedStatus(id: String, isBlocked: Boolean)

    @Query("UPDATE users SET role = :role WHERE id = :id")
    suspend fun updateRole(id: String, role: String)

    @Query("UPDATE users SET customAiName = :aiName, aiVoiceGender = :voiceGender WHERE id = :id")
    suspend fun updateAiPreferences(id: String, aiName: String, voiceGender: String)

    @Query("UPDATE users SET passwordHash = :newHash WHERE id = :id")
    suspend fun updatePasswordHash(id: String, newHash: String)

    @Query("UPDATE users SET passwordHash = :newHash WHERE email = :email OR username = :email")
    suspend fun updatePasswordByEmail(email: String, newHash: String)

    @Query("UPDATE users SET maxMastersLimit = :limit WHERE id = :id")
    suspend fun updateMastersLimit(id: String, limit: Int)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)
}

@Dao
interface ServiceCenterDao {
    @Query("SELECT * FROM service_centers ORDER BY createdAt DESC")
    fun getAllServiceCenters(): Flow<List<ServiceCenterEntity>>

    @Query("SELECT * FROM service_centers WHERE id = :id LIMIT 1")
    suspend fun getServiceCenterById(id: String): ServiceCenterEntity?

    @Query("SELECT * FROM service_centers WHERE adminEmail = :email LIMIT 1")
    suspend fun getServiceCenterByAdminEmail(email: String): ServiceCenterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceCenter(sc: ServiceCenterEntity)

    @Query("UPDATE service_centers SET maxMastersLimit = :limit WHERE id = :id")
    suspend fun updateLimit(id: String, limit: Int)

    @Query("DELETE FROM service_centers WHERE id = :id")
    suspend fun deleteServiceCenter(id: String)
}

@Dao
interface PasswordResetDao {
    @Query("SELECT * FROM password_reset_requests ORDER BY requestDate DESC")
    fun getAllRequests(): Flow<List<PasswordResetRequestEntity>>

    @Query("SELECT * FROM password_reset_requests WHERE targetAudience = 'god' ORDER BY requestDate DESC")
    fun getRequestsForGod(): Flow<List<PasswordResetRequestEntity>>

    @Query("SELECT * FROM password_reset_requests WHERE serviceCenterId = :serviceCenterId ORDER BY requestDate DESC")
    fun getRequestsForServiceAdmin(serviceCenterId: String): Flow<List<PasswordResetRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: PasswordResetRequestEntity)

    @Query("UPDATE password_reset_requests SET status = :status, newTempPassword = :tempPassword WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, tempPassword: String?)

    @Query("DELETE FROM password_reset_requests WHERE id = :id")
    suspend fun deleteRequest(id: String)
}

@Dao
interface BoardviewDao {
    @Query("SELECT * FROM boardview_photos ORDER BY addedDate DESC")
    fun getAllBoardviews(): Flow<List<BoardviewPhotoEntity>>

    @Query("SELECT * FROM boardview_photos WHERE brand = :brand AND model = :model ORDER BY side ASC, addedDate DESC")
    fun getBoardviewsForModel(brand: String, model: String): Flow<List<BoardviewPhotoEntity>>

    @Query("SELECT DISTINCT brand FROM boardview_photos")
    fun getDistinctBrands(): Flow<List<String>>

    @Query("SELECT DISTINCT model FROM boardview_photos WHERE brand = :brand")
    fun getDistinctModelsForBrand(brand: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoardview(photo: BoardviewPhotoEntity)

    @Query("DELETE FROM boardview_photos WHERE id = :id")
    suspend fun deleteBoardview(id: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE title LIKE '%' || :query || '%' ORDER BY isPinned DESC, lastMessageTime DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 1")
    suspend fun getChatById(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChat(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentMessagesForContext(chatId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSavedStatus(id: String, isSaved: Boolean)

    @Query("UPDATE messages SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: String, rating: Int)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalMessagesCount(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE isSaved = 1")
    suspend fun getSolvedProblemsCount(): Int
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_base ORDER BY addedDate DESC")
    fun getAllEntries(): Flow<List<KnowledgeBaseEntryEntity>>

    @Query("SELECT * FROM knowledge_base WHERE isSchematic = :isSchematic ORDER BY addedDate DESC")
    fun getEntriesByType(isSchematic: Boolean): Flow<List<KnowledgeBaseEntryEntity>>

    @Query("SELECT * FROM knowledge_base WHERE (brand LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%' OR problem LIKE '%' || :query || '%') AND isSchematic = :isSchematic ORDER BY addedDate DESC")
    fun searchEntries(query: String, isSchematic: Boolean): Flow<List<KnowledgeBaseEntryEntity>>

    @Query("SELECT DISTINCT brand FROM knowledge_base WHERE brand != ''")
    fun getDistinctBrands(): Flow<List<String>>

    @Query("SELECT DISTINCT model FROM knowledge_base WHERE brand = :brand AND model != ''")
    fun getDistinctModelsForBrand(brand: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: KnowledgeBaseEntryEntity)

    @Query("DELETE FROM knowledge_base WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("SELECT COUNT(*) FROM knowledge_base")
    suspend fun getKnowledgeCount(): Int

    @Query("SELECT * FROM knowledge_base")
    suspend fun getAllEntriesSync(): List<KnowledgeBaseEntryEntity>
}

@Dao
interface RepairOrderDao {
    @Query("SELECT * FROM repair_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<RepairOrderEntity>>

    @Query("SELECT * FROM repair_orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersSync(): List<RepairOrderEntity>

    @Query("SELECT * FROM repair_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): RepairOrderEntity?

    @Query("SELECT * FROM repair_orders WHERE serviceCenterId = :serviceCenterId ORDER BY createdAt DESC")
    fun getOrdersForServiceCenter(serviceCenterId: String): Flow<List<RepairOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: RepairOrderEntity)

    @Update
    suspend fun updateOrder(order: RepairOrderEntity)

    @Query("DELETE FROM repair_orders WHERE id = :id")
    suspend fun deleteOrder(id: String)

    @Query("SELECT COUNT(*) FROM repair_orders")
    suspend fun getOrdersCount(): Int
}

@Dao
interface OtpVerificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtp(otp: OtpVerificationEntity)

    @Query("SELECT * FROM otp_verifications WHERE recipient = :recipient AND isVerified = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestOtpForRecipient(recipient: String): OtpVerificationEntity?

    @Query("UPDATE otp_verifications SET isVerified = 1 WHERE id = :id")
    suspend fun markOtpVerified(id: String)
}

@Dao
interface InvitationDao {
    @Query("SELECT * FROM service_center_invitations ORDER BY createdAt DESC")
    fun getAllInvitations(): Flow<List<InvitationEntity>>

    @Query("SELECT * FROM service_center_invitations WHERE serviceCenterId = :serviceCenterId ORDER BY createdAt DESC")
    fun getInvitationsForServiceCenter(serviceCenterId: String): Flow<List<InvitationEntity>>

    @Query("SELECT * FROM service_center_invitations WHERE inviteCode = :code LIMIT 1")
    suspend fun getInvitationByCode(code: String): InvitationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: InvitationEntity)

    @Query("UPDATE service_center_invitations SET isUsed = 1, usedByUserId = :userId, usedByUserName = :userName, usedByUserEmail = :userEmail, usedAt = :usedAt WHERE id = :id")
    suspend fun markAsUsed(id: String, userId: String, userName: String, userEmail: String, usedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM service_center_invitations WHERE id = :id")
    suspend fun deleteInvitation(id: String)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY updatedAt DESC")
    fun getAllTickets(): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getTicketsForUser(userId: String): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets WHERE id = :ticketId LIMIT 1")
    suspend fun getTicketById(ticketId: String): SupportTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicketEntity)

    @Query("UPDATE support_tickets SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTicketStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM support_tickets WHERE id = :id")
    suspend fun deleteTicket(id: String)
}

@Dao
interface SupportMessageDao {
    @Query("SELECT * FROM support_messages WHERE ticketId = :ticketId ORDER BY timestamp ASC")
    fun getMessagesForTicket(ticketId: String): Flow<List<SupportMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SupportMessageEntity)

    @Query("SELECT COUNT(*) FROM support_messages WHERE isRead = 0 AND senderRole != 'god'")
    fun getUnreadSupportCountForGod(): Flow<Int>
}

@Dao
interface SystemEventDao {
    @Query("SELECT * FROM system_events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<SystemEventEntity>>

    @Query("SELECT * FROM system_events WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveEvents(): Flow<List<SystemEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SystemEventEntity)

    @Query("UPDATE system_events SET isActive = :isActive WHERE id = :id")
    suspend fun setEventActive(id: String, isActive: Boolean)

    @Query("DELETE FROM system_events WHERE id = :id")
    suspend fun deleteEvent(id: String)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE eventType = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY isPermanent DESC, createdAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories ORDER BY isPermanent DESC, createdAt DESC")
    suspend fun getAllStoriesSync(): List<StoryEntity>

    @Query("SELECT * FROM stories WHERE isPermanent = 1 OR scope = 'GLOBAL_ALERT' OR scope = 'GLOBAL_UPDATE' OR authorServiceCenterId = :serviceCenterId ORDER BY isPermanent DESC, createdAt DESC")
    fun getStoriesForServiceCenter(serviceCenterId: String): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Query("UPDATE stories SET viewsCount = viewsCount + 1 WHERE id = :id")
    suspend fun incrementViews(id: String)

    @Query("DELETE FROM stories WHERE id = :id AND isPermanent = 0")
    suspend fun deleteStory(id: String)
}

@Database(
    entities = [
        UserEntity::class,
        InvitationEntity::class,
        ServiceCenterEntity::class,
        PasswordResetRequestEntity::class,
        BoardviewPhotoEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        KnowledgeBaseEntryEntity::class,
        RepairOrderEntity::class,
        OtpVerificationEntity::class,
        SupportTicketEntity::class,
        SupportMessageEntity::class,
        SystemEventEntity::class,
        ActivityLogEntity::class,
        StoryEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun invitationDao(): InvitationDao
    abstract fun serviceCenterDao(): ServiceCenterDao
    abstract fun passwordResetDao(): PasswordResetDao
    abstract fun boardviewDao(): BoardviewDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun repairOrderDao(): RepairOrderDao
    abstract fun otpVerificationDao(): OtpVerificationDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun supportMessageDao(): SupportMessageDao
    abstract fun systemEventDao(): SystemEventDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun storyDao(): StoryDao
}


