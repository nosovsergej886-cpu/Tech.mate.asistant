package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val email: String = "",
    val passwordHash: String = "",
    val role: String, // "god" (Главный Администратор: nosovsergej886@gmail.com), "service_admin" (Админ СЦ), "master" (Мастер), "viewer" (Наблюдатель)
    val name: String,
    val serviceCenterId: String? = null,
    val serviceCenterName: String? = null,
    val city: String? = null,
    val phoneOrTelegram: String? = null,
    val maxMastersLimit: Int = 5,
    val authProvider: String = "email_code", // "email_code", "google", "password"
    val avatarUrl: String? = null,
    val isBlocked: Boolean = false,
    val customAiName: String = "TechMate", // Custom AI Assistant Name
    val aiVoiceGender: String = "FEMALE", // "FEMALE" (Алиса / Siri style) or "MALE" (Джарвис / GPT style)
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "service_center_invitations")
data class InvitationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val inviteCode: String, // e.g. "INV-TECH-8921"
    val serviceCenterId: String,
    val serviceCenterName: String,
    val createdByAdminEmail: String,
    val createdByAdminName: String,
    val targetRole: String = "master", // "master" (Technician), "viewer" (Trainee)
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000), // 7 days default
    val isUsed: Boolean = false,
    val usedByUserId: String? = null,
    val usedByUserName: String? = null,
    val usedByUserEmail: String? = null,
    val usedAt: Long? = null
)

@Entity(tableName = "service_centers")
data class ServiceCenterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val city: String,
    val adminEmail: String,
    val adminName: String,
    val phoneOrTelegram: String = "",
    val maxMastersLimit: Int = 5,
    val isApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "password_reset_requests")
data class PasswordResetRequestEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userEmail: String,
    val userName: String,
    val userRole: String, // "service_admin" or "master"
    val targetAudience: String, // "god" (for service admins) or serviceCenterId (for masters)
    val serviceCenterId: String? = null,
    val serviceCenterName: String? = null,
    val status: String = "pending", // "pending", "approved", "rejected"
    val newTempPassword: String? = null,
    val requestDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "boardview_photos")
data class BoardviewPhotoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val brand: String,
    val model: String,
    val title: String,
    val side: String = "front", // "front", "back", "subboard", "schematic"
    val imageUrl: String,
    val description: String = "",
    val componentsJson: String? = null,
    val voltagePointsJson: String? = null,
    val addedBy: String = "Мастер",
    val addedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val lastMessage: String,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val userId: String = ""
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val role: String, // "user" or "ai"
    val text: String,
    val imageUrl: String? = null,
    val guideJson: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val rating: Int = 0 // 0 = unrated, 1 = thumbs up, -1 = thumbs down
)

@Entity(tableName = "knowledge_base")
data class KnowledgeBaseEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val brand: String,
    val model: String,
    val problem: String,
    val guideDataJson: String,
    val addedBy: String,
    val addedDate: Long = System.currentTimeMillis(),
    val isSchematic: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Cause(
    val description: String,
    val probability: Int,
    val checkMethod: String,
    val normalValue: String,
    val fixMethod: String
)

@JsonClass(generateAdapter = true)
data class Step(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GuideData(
    val device: String,
    val problem: String,
    val difficulty: String, // "Легко", "Средне", "Сложно"
    val timeEstimate: String,
    val tools: List<String>,
    val causes: List<Cause>,
    val steps: List<Step>,
    val proTip: String? = null,
    val risks: String? = null,
    val links: List<String> = emptyList()
)

@Entity(tableName = "repair_orders")
data class RepairOrderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val serviceCenterId: String = "default",
    val deviceType: String = "Смартфон",
    val brand: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val declaredDefect: String = "",
    val diagnosticNotes: String = "",
    val repairStatus: String = "new", // "new", "in_progress", "waiting_parts", "ready", "delivered", "unrepairable"
    val urgency: String = "normal", // "normal", "urgent", "express"
    val estimatedCost: Double = 0.0,
    val finalCost: Double = 0.0,
    val assignedMasterId: String = "",
    val assignedMasterName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "otp_verifications")
data class OtpVerificationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipient: String, // email or phone number
    val code: String, // 6-digit code e.g. "482910"
    val authType: String = "email_confirm", // "email_confirm", "google_confirm", "phone_confirm", "reset_password"
    val expiresAt: Long = System.currentTimeMillis() + (10L * 60 * 1000), // 10 minutes
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userEmail: String,
    val userName: String,
    val serviceCenterName: String = "",
    val subject: String,
    val status: String = "open", // "open", "in_progress", "resolved"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_messages")
data class SupportMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ticketId: String,
    val senderId: String,
    val senderName: String,
    val senderEmail: String,
    val senderRole: String, // "god" (operator/support) or "master" / "service_admin"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "system_events")
data class SystemEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventType: String, // "NEW_YEAR", "VICTORY_DAY", "SPRING_SALE", "CUSTOM_ANNOUNCEMENT"
    val title: String,
    val description: String,
    val bannerEmoji: String = "🎉",
    val isActive: Boolean = true,
    val createdBy: String = "nosovsergej886@gmail.com",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventType: String, // "LOGIN", "API_KEY_CHANGE", "INVITE_GENERATED", "INVITE_REDEEMED", "ROLE_CHANGED", "USER_BLOCKED", "SUPPORT_TICKET"
    val userEmail: String,
    val userName: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class StoryScope {
    ALGORITHM,       // 1-я постоянная сторис: 12 этапов ремонта
    GLOBAL_ALERT,    // Глобальная для всех: массовые сбои ПО, брак, окирпичивание
    GLOBAL_UPDATE,   // Глобальная для всех: официальные обновления от автора Tech.Mate
    LOCAL_SC         // Локальная для участников одного СЦ
}

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String = "",
    val content: String,
    val authorName: String = "Мастер",
    val authorEmail: String = "",
    val authorServiceCenter: String = "СЦ #1",
    val authorServiceCenterId: String = "default_sc",
    val scope: String = "LOCAL_SC", // "ALGORITHM", "GLOBAL_ALERT", "GLOBAL_UPDATE", "LOCAL_SC"
    val isModeratedPublic: Boolean = true,
    val iconEmoji: String = "🛠️",
    val warningLevel: String = "NORMAL", // "CRITICAL", "IMPORTANT", "NORMAL"
    val isPermanent: Boolean = false,
    val viewsCount: Int = 0,
    val taggedDeviceModel: String = "",
    val taggedCategory: String = "",
    val mediaType: String = "PHOTO", // "PHOTO", "VIDEO"
    val mediaUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class TestPointCandidate(
    val brand: String,
    val model: String,
    val cpu: String,
    val imageUrl: String,
    val source: String,
    val title: String,
    val attempt: Int = 0,
    val status: String = "PENDING" // "PENDING", "APPROVED", "REJECTED"
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val authorId: String = "",
    val authorName: String = "Мастер",
    val authorEmail: String = "",
    val authorServiceCenter: String = "СЦ «ТехноМастер»",
    val authorServiceCenterId: String = "default_sc",
    val authorAvatarUrl: String = "",
    val content: String,
    val mediaType: String = "NONE", // "NONE", "PHOTO", "VIDEO"
    val mediaUrl: String = "",
    val mediaTitle: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val viewsCount: Int = 1,
    val isPublic: Boolean = true,
    val isAiAnalyzed: Boolean = false,
    val taggedDevice: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "post_comments")
data class PostCommentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val postId: String,
    val authorId: String = "",
    val authorName: String = "Мастер",
    val authorEmail: String = "",
    val authorServiceCenter: String = "",
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "post_likes", primaryKeys = ["postId", "userId"])
data class PostLikeEntity(
    val postId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_knowledge_items")
data class SavedKnowledgeItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val query: String,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tags: String = ""
)




