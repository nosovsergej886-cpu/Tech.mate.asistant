package com.example.services

import android.content.Context
import android.util.Log
import com.example.model.InvitationEntity
import com.example.model.PasswordResetRequestEntity
import com.example.model.ServiceCenterEntity
import com.example.model.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth

class AuthService private constructor(private val context: Context) {

    private val dbService = DatabaseService.getInstance(context)
    private val userDao = dbService.userDao
    private val invitationDao = dbService.invitationDao
    private val serviceCenterDao = dbService.serviceCenterDao
    private val passwordResetDao = dbService.passwordResetDao
    private val otpDao = dbService.otpVerificationDao

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("AuthService", "FirebaseAuth initialization failed: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // --- OTP Verification System (Google Account & Email/Phone 6-Digit Code) ---
    suspend fun generateAndSendOtp(recipient: String, authType: String = "email_confirm"): String {
        val cleanRecipient = recipient.trim().lowercase()
        val randomCode = (100000..999999).random().toString()
        val otpEntity = com.example.model.OtpVerificationEntity(
            recipient = cleanRecipient,
            code = randomCode,
            authType = authType,
            expiresAt = System.currentTimeMillis() + (10L * 60 * 1000)
        )
        otpDao.insertOtp(otpEntity)

        // If recipient is an email address, trigger Corporate Hosting email delivery / Resend direct delivery + Firebase fallback
        if (cleanRecipient.contains("@")) {
            try {
                EmailOtpService.sendOtpCode(cleanRecipient, randomCode, context)
            } catch (e: Exception) {
                Log.w("AuthService", "OTP email dispatch exception: ${e.message}")
            }

            try {
                firebaseAuth?.let { auth ->
                    auth.sendPasswordResetEmail(cleanRecipient)
                        .addOnSuccessListener {
                            Log.i("AuthService", "Firebase Auth email trigger sent successfully to $cleanRecipient")
                        }
                        .addOnFailureListener { err ->
                            Log.w("AuthService", "Firebase Auth email delivery note: ${err.message}")
                        }
                }
            } catch (e: Exception) {
                Log.w("AuthService", "Firebase email dispatch skipped: ${e.message}")
            }
        }

        return randomCode
    }

    suspend fun verifyOtp(recipient: String, inputCode: String): Boolean {
        val cleanRecipient = recipient.trim().lowercase()
        val cleanCode = inputCode.trim()
        val latest = otpDao.getLatestOtpForRecipient(cleanRecipient) ?: return false
        if (latest.expiresAt < System.currentTimeMillis()) return false
        if (latest.code == cleanCode) {
            otpDao.markOtpVerified(latest.id)
            return true
        }
        return false
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        val cleanEmail = email.trim().lowercase()
        return userDao.getUserByEmail(cleanEmail) ?: userDao.getUserByUsername(cleanEmail)
    }

    suspend fun loginExistingUserDirectly(email: String): UserEntity {
        val cleanEmail = email.trim().lowercase()
        val isGod = isGodEmail(cleanEmail)
        var user = userDao.getUserByEmail(cleanEmail) ?: userDao.getUserByUsername(cleanEmail)
        if (user == null) {
            user = UserEntity(
                id = UUID.randomUUID().toString(),
                username = cleanEmail,
                name = if (isGod) "Сергей Носов (Главный Администратор)" else cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = cleanEmail,
                role = if (isGod) "god" else "master",
                serviceCenterName = if (isGod) "Центральный офис TechMate" else "СЦ «ТехноМастер»",
                serviceCenterId = if (isGod) "god_sc" else "sc_default"
            )
            userDao.insertUser(user)
        }
        _currentUser.value = user
        return user
    }

    suspend fun getUserCount(): Int = userDao.getUserCount()

    fun isGodMode(): Boolean {
        val user = _currentUser.value ?: return false
        return user.role == "god" || isGodEmail(user.email) || isGodEmail(user.username)
    }

    fun isServiceAdmin(): Boolean {
        val user = _currentUser.value ?: return false
        return user.role == "service_admin" || user.role == "admin" || isGodMode()
    }

    // --- NEW STREAMLINED EMAIL-ONLY AUTHENTICATION (NO PASSWORD) ---
    suspend fun loginOrRegisterWithEmailOtp(
        email: String,
        name: String = "",
        city: String = "",
        serviceCenterName: String = "",
        inviteCode: String? = null
    ): UserEntity {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            throw Exception("Укажите корректный адрес электронной почты")
        }

        val isGod = isGodEmail(cleanEmail)
        var user = userDao.getUserByEmail(cleanEmail) ?: userDao.getUserByUsername(cleanEmail)

        if (user != null) {
            // Existing user
            if (user.isBlocked) {
                throw Exception("Ваш аккаунт заблокирован главным администратором.")
            }
            val updatedUser = user.copy(
                role = if (isGod) "god" else user.role,
                name = if (name.isNotBlank()) name.trim() else user.name,
                city = if (city.isNotBlank()) city.trim() else user.city,
                serviceCenterName = if (serviceCenterName.isNotBlank()) serviceCenterName.trim() else user.serviceCenterName
            )
            userDao.insertUser(updatedUser)
            _currentUser.value = updatedUser
            try {
                FirebaseSyncService.getInstance(context).recordActivityLog(
                    eventType = "LOGIN",
                    userEmail = updatedUser.email,
                    userName = updatedUser.name,
                    details = "Успешная авторизация по Email коду (${updatedUser.role})"
                )
            } catch (e: Exception) {
                Log.w("AuthService", "Log error: ${e.message}")
            }
            return updatedUser
        }

        // Check if there is an invite code applied
        var assignedRole = if (isGod) "god" else "master"
        var scId: String? = null
        var scName: String? = serviceCenterName.trim().ifBlank { null }

        if (!isGod && !inviteCode.isNullOrBlank()) {
            val inv = validateInviteCode(inviteCode)
            if (inv != null) {
                assignedRole = inv.targetRole
                scId = inv.serviceCenterId
                scName = inv.serviceCenterName
            }
        }

        val newUser = UserEntity(
            username = cleanEmail,
            email = cleanEmail,
            passwordHash = "", // No passwords required!
            role = assignedRole,
            name = name.trim().ifBlank { if (isGod) "Сергей Носов (Гендиректор)" else cleanEmail.substringBefore("@") },
            serviceCenterId = scId,
            serviceCenterName = scName ?: "Технический отдел",
            city = city.trim(),
            authProvider = "email_code",
            isBlocked = false
        )

        userDao.insertUser(newUser)

        // If invite was used, mark it
        if (!inviteCode.isNullOrBlank()) {
            val inv = invitationDao.getInvitationByCode(inviteCode.trim().uppercase())
            if (inv != null && !inv.isUsed) {
                invitationDao.markAsUsed(
                    id = inv.id,
                    userId = newUser.id,
                    userName = newUser.name,
                    userEmail = newUser.email
                )
            }
        }

        _currentUser.value = newUser
        try {
            FirebaseSyncService.getInstance(context).recordActivityLog(
                eventType = "LOGIN",
                userEmail = newUser.email,
                userName = newUser.name,
                details = "Первичная регистрация и вход по Email коду (${newUser.role})"
            )
        } catch (e: Exception) {
            Log.w("AuthService", "Log error: ${e.message}")
        }
        return newUser
    }

    suspend fun setUserBlocked(userId: String, isBlocked: Boolean) {
        userDao.updateBlockedStatus(userId, isBlocked)
        if (_currentUser.value?.id == userId && isBlocked) {
            _currentUser.value = null
        }
    }

    suspend fun setUserRole(userId: String, newRole: String) {
        userDao.updateRole(userId, newRole)
        val current = _currentUser.value
        if (current?.id == userId) {
            _currentUser.value = current.copy(role = newRole)
        }
    }

    suspend fun updateAiPreferences(aiName: String, voiceGender: String) {
        val current = _currentUser.value ?: return
        userDao.updateAiPreferences(current.id, aiName, voiceGender)
        _currentUser.value = current.copy(customAiName = aiName, aiVoiceGender = voiceGender)
        try {
            VoiceService.getInstance(context).saveVoicePreferences(aiName, voiceGender)
        } catch (e: Exception) {
            Log.w("AuthService", "VoiceService save error: ${e.message}")
        }
    }


    // --- INVITATION WORKFLOW (Admin-to-Technician & God Mode Promotions) ---

    suspend fun createInvitation(
        targetRole: String = "master",
        validDays: Int = 7,
        customCenterId: String? = null,
        customCenterName: String? = null
    ): InvitationEntity {
        val current = _currentUser.value ?: throw Exception("Необходимо авторизоваться")
        if (!isServiceAdmin()) {
            throw Exception("Только Администратор СЦ или Главный Администратор (God Mode) может создавать приглашения")
        }

        val scId = customCenterId ?: current.serviceCenterId ?: current.id
        val scName = customCenterName ?: current.serviceCenterName ?: "Сервисный Центр"

        // Generate secure unique cryptographic one-time invite code e.g. "GOD-INV-8A4F" or "TM-INV-9382"
        val prefix = if (isGodMode()) "GOD-INV" else "TM-INV"
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val randomSuffix = (1..6).map { chars.random() }.joinToString("")
        val inviteCode = "$prefix-$randomSuffix"

        val invitation = InvitationEntity(
            inviteCode = inviteCode,
            serviceCenterId = scId,
            serviceCenterName = scName,
            createdByAdminEmail = current.email.ifBlank { current.username },
            createdByAdminName = current.name,
            targetRole = targetRole,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (validDays.toLong() * 24 * 60 * 60 * 1000),
            isUsed = false
        )

        invitationDao.insertInvitation(invitation)

        // Upload to Firestore in real-time
        try {
            val syncService = FirebaseSyncService.getInstance(context)
            syncService.syncInvitationToFirestore(invitation)
            syncService.recordActivityLog(
                eventType = "INVITE_GENERATED",
                userEmail = current.email.ifBlank { current.username },
                userName = current.name,
                details = "Сгенерирован инвайт-код $inviteCode для роли $targetRole ($scName)"
            )
        } catch (e: Exception) {
            Log.w("AuthService", "Could not upload invite to Firestore: ${e.message}")
        }

        return invitation
    }

    fun getInvitationsFlow(): kotlinx.coroutines.flow.Flow<List<InvitationEntity>> {
        val current = _currentUser.value
        return if (isGodMode()) {
            invitationDao.getAllInvitations()
        } else {
            val scId = current?.serviceCenterId ?: current?.id ?: ""
            invitationDao.getInvitationsForServiceCenter(scId)
        }
    }

    suspend fun validateInviteCode(inviteCode: String): InvitationEntity? {
        val cleanCode = inviteCode.trim().uppercase()
        // Check local DB
        var inv = invitationDao.getInvitationByCode(cleanCode)
        if (inv == null) {
            // Check Firestore
            inv = FirebaseSyncService.getInstance(context).fetchAndValidateInviteFromFirestore(cleanCode)
        }
        if (inv == null) return null
        if (inv.isUsed) return null
        if (System.currentTimeMillis() > inv.expiresAt) return null
        return inv
    }

    /**
     * Allows an existing logged-in user to promote their account using a valid one-time invite code.
     */
    suspend fun promoteExistingUserWithInvite(inviteCode: String): Pair<Boolean, String> {
        val current = _currentUser.value ?: return Pair(false, "Вы не авторизованы")
        val cleanCode = inviteCode.trim().uppercase()
        val invitation = validateInviteCode(cleanCode) ?: return Pair(false, "Недействительный или уже использованный код приглашения")

        val newRole = invitation.targetRole
        val scId = invitation.serviceCenterId
        val scName = invitation.serviceCenterName

        // Update local DB
        userDao.updateRole(current.id, newRole)
        invitationDao.markAsUsed(
            id = invitation.id,
            userId = current.id,
            userName = current.name,
            userEmail = current.email.ifBlank { current.username }
        )

        // Update current user state
        _currentUser.value = current.copy(
            role = newRole,
            serviceCenterId = scId,
            serviceCenterName = scName
        )

        // Sync to Firestore
        try {
            val syncService = FirebaseSyncService.getInstance(context)
            syncService.promoteUserInFirestore(current.id, newRole, scId, scName)
            syncService.markInviteUsedInFirestore(invitation.id, current.id, current.name, current.email)
            syncService.recordActivityLog(
                eventType = "INVITE_REDEEMED",
                userEmail = current.email.ifBlank { current.username },
                userName = current.name,
                details = "Активирован инвайт-код $cleanCode, повышена роль до $newRole ($scName)"
            )
        } catch (e: Exception) {
            Log.w("AuthService", "Error syncing promotion to Firestore: ${e.message}")
        }

        val roleDisplayName = when (newRole) {
            "service_admin" -> "Администратор СЦ"
            "admin" -> "Администратор системы"
            "god" -> "Главный Администратор (God Mode)"
            else -> "Старший Инженер-Схемотехник"
        }

        return Pair(true, "Поздравляем! Ваш статус повышен до: $roleDisplayName ($scName)")
    }

    suspend fun redeemInvitation(
        inviteCode: String,
        username: String,
        password: String,
        name: String,
        email: String = ""
    ): UserEntity {
        val cleanCode = inviteCode.trim().uppercase()
        val invitation = invitationDao.getInvitationByCode(cleanCode)
            ?: throw Exception("Код приглашения не найден")

        if (invitation.isUsed) {
            throw Exception("Этот код приглашения уже был использован")
        }

        if (System.currentTimeMillis() > invitation.expiresAt) {
            throw Exception("Срок действия кода приглашения истек")
        }

        val cleanUser = username.trim().lowercase()
        val cleanEmail = email.trim().lowercase()

        val existing = userDao.getUserByUsername(cleanUser) ?: (if (cleanEmail.isNotBlank()) userDao.getUserByEmail(cleanEmail) else null)
        if (existing != null) {
            throw Exception("Пользователь с таким логином/email уже зарегистрирован")
        }

        val newMaster = UserEntity(
            username = cleanUser,
            email = cleanEmail,
            passwordHash = hashPassword(password),
            role = invitation.targetRole,
            name = name.trim().ifBlank { cleanUser },
            serviceCenterId = invitation.serviceCenterId,
            serviceCenterName = invitation.serviceCenterName,
            city = ""
        )

        userDao.insertUser(newMaster)
        invitationDao.markAsUsed(
            id = invitation.id,
            userId = newMaster.id,
            userName = newMaster.name,
            userEmail = newMaster.email.ifBlank { newMaster.username }
        )

        _currentUser.value = newMaster
        return newMaster
    }

    suspend fun revokeInvitation(invitationId: String) {
        invitationDao.deleteInvitation(invitationId)
    }

    suspend fun createServiceCenter(name: String, city: String, address: String = ""): ServiceCenterEntity {
        val scId = "sc_" + System.currentTimeMillis()
        val sc = ServiceCenterEntity(
            id = scId,
            name = name.trim(),
            city = city.trim(),
            phoneOrTelegram = address.trim(),
            adminEmail = _currentUser.value?.email ?: "",
            adminName = _currentUser.value?.name ?: "",
            maxMastersLimit = 10
        )
        serviceCenterDao.insertServiceCenter(sc)
        return sc
    }

    suspend fun addMasterToCenter(
        username: String,
        password: String,
        name: String,
        email: String = "",
        centerId: String? = null
    ): UserEntity {
        val current = _currentUser.value
        val scId = centerId ?: current?.serviceCenterId ?: "default_sc"
        val sc = if (scId != "default_sc") serviceCenterDao.getServiceCenterById(scId) else null

        val clean = username.trim().lowercase()
        val existing = userDao.getUserByUsername(clean) ?: (if (email.isNotBlank()) userDao.getUserByEmail(email.trim().lowercase()) else null)
        if (existing != null) {
            throw Exception("Пользователь с таким логином или email уже зарегистрирован")
        }

        val newMaster = UserEntity(
            username = clean,
            email = email.trim().lowercase(),
            passwordHash = hashPassword(password),
            role = "master",
            name = name.trim().ifBlank { clean },
            serviceCenterId = scId,
            serviceCenterName = sc?.name ?: current?.serviceCenterName ?: "Сервисный Центр",
            city = sc?.city ?: current?.city ?: ""
        )

        userDao.insertUser(newMaster)
        return newMaster
    }

    // Google Sign-In / Fast Auth & Linking
    suspend fun loginWithGoogle(
        email: String,
        fullName: String,
        avatarUrl: String? = null,
        serviceCenterCode: String? = null
    ): UserEntity {
        val cleanEmail = email.trim().lowercase()
        val isGod = isGodEmail(cleanEmail)

        var existingUser = userDao.getUserByEmail(cleanEmail) ?: userDao.getUserByUsername(cleanEmail)
        
        if (existingUser != null) {
            val updatedUser = existingUser.copy(
                role = if (isGod) "god" else existingUser.role,
                name = if (existingUser.name.isBlank()) fullName else existingUser.name,
                avatarUrl = avatarUrl ?: existingUser.avatarUrl,
                authProvider = "google"
            )
            userDao.insertUser(updatedUser)
            _currentUser.value = updatedUser
            try {
                FirebaseSyncService.getInstance(context).recordActivityLog(
                    eventType = "LOGIN",
                    userEmail = updatedUser.email,
                    userName = updatedUser.name,
                    details = "Успешная авторизация через Google аккаунт (${updatedUser.role})"
                )
            } catch (e: Exception) {
                Log.w("AuthService", "Log error: ${e.message}")
            }
            return updatedUser
        }

        // New user registered via Google
        val initialRole = if (isGod) "god" else "master"
        val newUser = UserEntity(
            username = cleanEmail,
            email = cleanEmail,
            passwordHash = hashPassword(UUID.randomUUID().toString()),
            role = initialRole,
            name = fullName.ifBlank { if (isGod) "Сергей Носов (Гендиректор)" else cleanEmail.substringBefore("@") },
            authProvider = "google",
            avatarUrl = avatarUrl
        )

        userDao.insertUser(newUser)
        _currentUser.value = newUser
        try {
            FirebaseSyncService.getInstance(context).recordActivityLog(
                eventType = "LOGIN",
                userEmail = newUser.email,
                userName = newUser.name,
                details = "Первичная регистрация и вход через Google аккаунт (${newUser.role})"
            )
        } catch (e: Exception) {
            Log.w("AuthService", "Log error: ${e.message}")
        }
        return newUser
    }

    suspend fun registerWithPhone(
        phone: String,
        name: String,
        password: String
    ): UserEntity {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        if (cleanPhone.length < 5) {
            throw Exception("Укажите корректный номер телефона")
        }

        val existingUser = userDao.getUserByPhone(cleanPhone) ?: userDao.getUserByUsername(cleanPhone)
        if (existingUser != null) {
            throw Exception("Пользователь с таким номером телефона уже зарегистрирован")
        }

        val newUser = UserEntity(
            username = cleanPhone,
            email = "",
            phoneOrTelegram = cleanPhone,
            passwordHash = hashPassword(password),
            role = "master",
            name = name.ifBlank { "Мастер $cleanPhone" },
            authProvider = "phone"
        )

        userDao.insertUser(newUser)
        _currentUser.value = newUser
        return newUser
    }

    suspend fun registerWithEmail(
        email: String,
        username: String,
        name: String,
        password: String
    ): UserEntity {
        val cleanEmail = email.trim().lowercase()
        val cleanUsername = username.trim().lowercase().ifBlank { cleanEmail.substringBefore("@") }

        if (!cleanEmail.contains("@")) {
            throw Exception("Укажите корректный адрес электронной почты")
        }

        val isGod = isGodEmail(cleanEmail) || isGodEmail(cleanUsername)
        val existing = userDao.getUserByEmail(cleanEmail) ?: userDao.getUserByUsername(cleanUsername)
        if (existing != null) {
            throw Exception("Пользователь с таким email или логином уже существует")
        }

        val newUser = UserEntity(
            username = cleanUsername,
            email = cleanEmail,
            passwordHash = hashPassword(password),
            role = if (isGod) "god" else "master",
            name = name.ifBlank { if (isGod) "Главный Администратор" else cleanUsername },
            authProvider = "email"
        )

        userDao.insertUser(newUser)
        _currentUser.value = newUser
        return newUser
    }

    suspend fun login(usernameOrPhoneOrEmail: String, password: String): UserEntity {
        val clean = usernameOrPhoneOrEmail.trim().lowercase()
        val cleanPhone = clean.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        val hashed = hashPassword(password)
        val user = userDao.getUserByUsername(clean) 
            ?: userDao.getUserByEmail(clean) 
            ?: userDao.getUserByPhone(cleanPhone)
            ?: userDao.getUserByPhone(clean)
            ?: throw Exception("Пользователь не найден. Проверьте логин/телефон или зарегистрируйтесь.")

        if (user.passwordHash != hashed && user.authProvider != "google") {
            throw Exception("Неверный пароль")
        }

        val isGod = isGodEmail(user.email) || isGodEmail(user.username)
        val activeUser = if (isGod && user.role != "god") {
            val updated = user.copy(role = "god")
            userDao.insertUser(updated)
            updated
        } else {
            user
        }

        _currentUser.value = activeUser
        return activeUser
    }

    suspend fun registerAdmin(username: String, password: String, name: String, email: String = ""): UserEntity {
        val cleanUser = username.trim().lowercase()
        val cleanEmail = email.trim().lowercase().ifBlank { cleanUser }
        val isGod = isGodEmail(cleanEmail) || isGodEmail(cleanUser)

        val existing = userDao.getUserByUsername(cleanUser) ?: userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            throw Exception("Пользователь с таким логином или email уже существует")
        }

        val newUser = UserEntity(
            username = cleanUser,
            email = cleanEmail,
            passwordHash = hashPassword(password),
            role = if (isGod) "god" else "service_admin",
            name = name.ifBlank { if (isGod) "Сергей Носов (Гендиректор)" else "Администратор СЦ" }
        )

        userDao.insertUser(newUser)
        _currentUser.value = newUser
        return newUser
    }

    // GOD MODE: Add or register a Service Center & Service Admin
    suspend fun addServiceCenterAndAdmin(
        scName: String,
        city: String,
        adminEmail: String,
        adminName: String,
        password: String,
        maxMasters: Int = 5,
        telegramOrPhone: String = ""
    ): ServiceCenterEntity {
        val cleanEmail = adminEmail.trim().lowercase()
        val scId = "sc_" + System.currentTimeMillis()

        val sc = ServiceCenterEntity(
            id = scId,
            name = scName.trim(),
            city = city.trim(),
            adminEmail = cleanEmail,
            adminName = adminName.trim(),
            phoneOrTelegram = telegramOrPhone.trim(),
            maxMastersLimit = maxMasters
        )
        serviceCenterDao.insertServiceCenter(sc)

        // Create or update admin user
        val existing = userDao.getUserByEmail(cleanEmail) ?: userDao.getUserByUsername(cleanEmail)
        val adminUser = existing?.copy(
            role = "service_admin",
            serviceCenterId = scId,
            serviceCenterName = scName.trim(),
            city = city.trim(),
            maxMastersLimit = maxMasters,
            passwordHash = if (password.isNotBlank()) hashPassword(password) else existing.passwordHash
        ) ?: UserEntity(
            username = cleanEmail,
            email = cleanEmail,
            passwordHash = hashPassword(password.ifBlank { "123456" }),
            role = "service_admin",
            name = adminName.trim(),
            serviceCenterId = scId,
            serviceCenterName = scName.trim(),
            city = city.trim(),
            phoneOrTelegram = telegramOrPhone.trim(),
            maxMastersLimit = maxMasters
        )

        userDao.insertUser(adminUser)
        return sc
    }

    // SERVICE ADMIN: Add a Master to their Service Center group
    suspend fun addMasterToGroup(
        name: String,
        usernameOrEmail: String,
        password: String
    ): UserEntity {
        val current = _currentUser.value ?: throw Exception("Не авторизован")
        val scId = current.serviceCenterId ?: current.id

        val currentMastersCount = userDao.getMastersCountForService(scId)
        if (current.role != "god" && currentMastersCount >= current.maxMastersLimit) {
            throw Exception("Достигнут лимит группы: ${current.maxMastersLimit} мастеров. Обратитесь к главному администратору для расширения.")
        }

        val clean = usernameOrEmail.trim().lowercase()
        val existing = userDao.getUserByUsername(clean) ?: userDao.getUserByEmail(clean)
        if (existing != null) {
            throw Exception("Пользователь с таким логином/email уже зарегистрирован")
        }

        val newMaster = UserEntity(
            username = clean,
            email = if (clean.contains("@")) clean else "",
            passwordHash = hashPassword(password),
            role = "master",
            name = name.trim().ifBlank { clean },
            serviceCenterId = scId,
            serviceCenterName = current.serviceCenterName ?: "Сервисный Центр",
            city = current.city
        )

        userDao.insertUser(newMaster)
        return newMaster
    }

    // Password Reset Request
    suspend fun requestPasswordReset(email: String, userType: String, scName: String? = null): PasswordResetRequestEntity {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(cleanEmail) ?: userDao.getUserByUsername(cleanEmail)

        val target = if (userType == "service_admin" || user?.role == "service_admin") "god" else (user?.serviceCenterId ?: "service_admin")
        
        val request = PasswordResetRequestEntity(
            userEmail = cleanEmail,
            userName = user?.name ?: cleanEmail,
            userRole = user?.role ?: if (userType == "service_admin") "service_admin" else "master",
            targetAudience = target,
            serviceCenterId = user?.serviceCenterId,
            serviceCenterName = user?.serviceCenterName ?: scName,
            status = "pending"
        )
        passwordResetDao.insertRequest(request)
        return request
    }

    // Approve Password Reset
    suspend fun approvePasswordReset(requestId: String, tempPassword: String) {
        val req = passwordResetDao.getAllRequests()
        val hash = hashPassword(tempPassword)
        passwordResetDao.updateStatus(requestId, "approved", tempPassword)
    }

    suspend fun updateCurrentUserName(newName: String) {
        val current = _currentUser.value ?: return
        userDao.updateName(current.id, newName)
        _currentUser.value = current.copy(name = newName)
    }

    suspend fun deleteUser(userId: String) {
        userDao.deleteUser(userId)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = null
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        val GOD_EMAILS = setOf(
            "tech.mateasistant@gmail.com",
            "nosovsergej886@gmail.com",
            "admin",
            "god"
        )

        fun isGodEmail(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            val clean = email.trim().lowercase()
            return clean in GOD_EMAILS || clean.startsWith("tech.mateasistant") || clean.startsWith("nosovsergej886")
        }

        @Volatile
        private var INSTANCE: AuthService? = null

        fun getInstance(context: Context): AuthService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthService(context).also { INSTANCE = it }
            }
        }
    }
}
