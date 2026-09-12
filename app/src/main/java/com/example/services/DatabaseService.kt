package com.example.services

import android.content.Context
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DatabaseService private constructor(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "techmate.db"
    ).fallbackToDestructiveMigration().build()

    val userDao = db.userDao()
    val invitationDao = db.invitationDao()
    val serviceCenterDao = db.serviceCenterDao()
    val passwordResetDao = db.passwordResetDao()
    val boardviewDao = db.boardviewDao()
    val chatDao = db.chatDao()
    val messageDao = db.messageDao()
    val knowledgeDao = db.knowledgeDao()
    val repairOrderDao = db.repairOrderDao()
    val otpVerificationDao = db.otpVerificationDao()
    val supportTicketDao = db.supportTicketDao()
    val supportMessageDao = db.supportMessageDao()
    val systemEventDao = db.systemEventDao()
    val activityLogDao = db.activityLogDao()
    val storyDao = db.storyDao()
    val postDao = db.postDao()
    val postCommentDao = db.postCommentDao()
    val postLikeDao = db.postLikeDao()
    val savedKnowledgeDao = db.savedKnowledgeDao()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val guideAdapter = moshi.adapter(GuideData::class.java)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialData()
        }
    }

    fun parseGuide(json: String?): GuideData? {
        if (json.isNullOrEmpty()) return null
        return try {
            guideAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun toJson(guide: GuideData): String {
        return guideAdapter.toJson(guide)
    }

    private suspend fun seedInitialData() {
        // Auto-provision God Mode users (tech.mateasistant@gmail.com, nosovsergej886@gmail.com) if not exists
        val godEmails = listOf(
            "tech.mateasistant@gmail.com" to "Главный Администратор (TechMate)",
            "nosovsergej886@gmail.com" to "Сергей Носов (Гендиректор)"
        )
        for ((email, displayName) in godEmails) {
            val existingGod = userDao.getUserByEmail(email) ?: userDao.getUserByUsername(email)
            if (existingGod == null) {
                userDao.insertUser(
                    UserEntity(
                        id = if (email.startsWith("tech")) "user_god_techmate" else "user_god_nosov",
                        username = email,
                        email = email,
                        passwordHash = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", // default "admin" or login via Google
                        role = "god",
                        name = displayName,
                        authProvider = "google",
                        maxMastersLimit = 999
                    )
                )
            } else if (existingGod.role != "god") {
                userDao.insertUser(existingGod.copy(role = "god"))
            }
        }

        // Seed initial public Master Feed posts
        if (postDao.getAllPostsSync().isEmpty()) {
            val samplePosts = listOf(
                PostEntity(
                    id = "sample_post_1",
                    authorName = "Алексей (iPhone BGA Master)",
                    authorEmail = "alexey.repair@techmate.pro",
                    authorServiceCenter = "СЦ «iMasterLab»",
                    authorServiceCenterId = "sc_imaster",
                    content = "🔥 Восстановил Face ID на iPhone 13 Pro после сильного залития морской водой! Шлейф Flood Illuminator был полностью корродирован, поднял под микроскопом 3 оборванные дорожки 0.02мм и перекатал датчик на бессвинцовый припой. Ток в норме (0.42A при включении проектора). Клиент счастлив! 🛠️⚡",
                    mediaType = "PHOTO",
                    mediaUrl = "https://images.unsplash.com/photo-1597740985671-2a8a3b80532e?w=800&q=80",
                    mediaTitle = "Восстановление дорожек Face ID под микроскопом",
                    taggedDevice = "iPhone 13 Pro",
                    likesCount = 38,
                    commentsCount = 3,
                    viewsCount = 215,
                    isPublic = true,
                    isAiAnalyzed = true,
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 25
                ),
                PostEntity(
                    id = "sample_post_2",
                    authorName = "Сервисный Центр «FixLab»",
                    authorEmail = "support@fixlab.ru",
                    authorServiceCenter = "СЦ «FixLab Москва»",
                    authorServiceCenterId = "sc_fixlab",
                    content = "⚡ Внимание коллегам по Samsung S23 / S23 Ultra! Заехал аппарат с диагнозом «0.00A на зарядке». Внешне разъём Type-C чистый, но под тепловизором греется вторичный контроллер питания IF-PMIC. Причина: пробит фильтрующий керамический конденсатор по линии VBUS_IN. Снял КЗ, ток зарядки поднялся до 9V 2.2A. Записал видео замера линии!",
                    mediaType = "VIDEO",
                    mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    mediaTitle = "Замер линии VBUS и снятие КЗ вторичного КП (Видео)",
                    taggedDevice = "Samsung S23 Ultra",
                    likesCount = 54,
                    commentsCount = 2,
                    viewsCount = 480,
                    isPublic = true,
                    isAiAnalyzed = true,
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 120
                ),
                PostEntity(
                    id = "sample_post_3",
                    authorName = "Дмитрий Смирнов",
                    authorEmail = "dmitry.smirnov@techmate.pro",
                    authorServiceCenter = "СЦ «ТехноМастер»",
                    authorServiceCenterId = "default_sc",
                    content = "💡 Коллеги, делюсь практикой по Xiaomi Poco X3 Pro (CPU Snapdragon 860). Типичный отвал процессора из-за перегрева. Сделал реболлинг процессора и ОЗУ «бутерброда» на припой ПОС-63 с хорошим флюсом Amtech. Плата ожила, все тесты прошел. Главное правило: не перегревать текстолит выше 240°C на преднагревателе!",
                    mediaType = "PHOTO",
                    mediaUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80",
                    mediaTitle = "Процессор после реболла перед посадкой",
                    taggedDevice = "Poco X3 Pro",
                    likesCount = 67,
                    commentsCount = 2,
                    viewsCount = 612,
                    isPublic = true,
                    isAiAnalyzed = true,
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 360
                )
            )

            for (p in samplePosts) {
                postDao.insertPost(p)
            }

            // Seed sample comments
            val sampleComments = listOf(
                PostCommentEntity(
                    id = "comment_1",
                    postId = "sample_post_1",
                    authorName = "Иван Морозов",
                    authorEmail = "ivan@techmate.pro",
                    authorServiceCenter = "СЦ «Профи-Чип»",
                    content = "Отличная работа! Какую маску использовал для фиксации дорожек — ультрафиолетовую механическую или зеленку?",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 18
                ),
                PostCommentEntity(
                    id = "comment_2",
                    postId = "sample_post_1",
                    authorName = "Алексей (iPhone BGA Master)",
                    authorEmail = "alexey.repair@techmate.pro",
                    authorServiceCenter = "СЦ «iMasterLab»",
                    content = "Использовал черную маску Mechanic с отверждением 395nm УФ-фонариком, держит температуру до 300 градусов отлично!",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 12
                ),
                PostCommentEntity(
                    id = "comment_3",
                    postId = "sample_post_1",
                    authorName = "Сергей Носов",
                    authorEmail = "nosovsergej886@gmail.com",
                    authorServiceCenter = "Tech.Mate СЦ",
                    content = "Полезнейший кейс для базы знаний мастеров 👍",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 5
                ),
                PostCommentEntity(
                    id = "comment_4",
                    postId = "sample_post_2",
                    authorName = "Артём BGA",
                    authorEmail = "artem@bga-service.ru",
                    authorServiceCenter = "BGA Lab",
                    content = "Спасибо за подсказку! Как раз вчера принесли такой же с нулевым потреблением. Пошел проверять конденсаторы по VBUS.",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 95
                ),
                PostCommentEntity(
                    id = "comment_5",
                    postId = "sample_post_2",
                    authorName = "Михаил",
                    authorEmail = "mikhail@techmate.pro",
                    authorServiceCenter = "СЦ «ТехноСервис»",
                    content = "Видео очень наглядное, тепловизор решает 90% времени диагностики.",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 40
                ),
                PostCommentEntity(
                    id = "comment_6",
                    postId = "sample_post_3",
                    authorName = "Виктор К.",
                    authorEmail = "victor@techmate.pro",
                    authorServiceCenter = "СЦ «МобиРем»",
                    content = "Какую температуру фена ставил при снятии компаунда по периметру?",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 300
                ),
                PostCommentEntity(
                    id = "comment_7",
                    postId = "sample_post_3",
                    authorName = "Дмитрий Смирнов",
                    authorEmail = "dmitry.smirnov@techmate.pro",
                    authorServiceCenter = "СЦ «ТехноМастер»",
                    content = "220 градусов тонкой изогнутой иглой, компаунд отходит мягко и без срыва пятаков.",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 250
                )
            )

            for (c in sampleComments) {
                postCommentDao.insertComment(c)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DatabaseService? = null

        fun getInstance(context: Context): DatabaseService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseService(context).also { INSTANCE = it }
            }
        }
    }
}
