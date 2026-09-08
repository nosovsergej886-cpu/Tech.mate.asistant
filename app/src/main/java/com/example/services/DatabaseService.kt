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
