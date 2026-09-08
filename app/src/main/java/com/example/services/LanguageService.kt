package com.example.services

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class AppLanguage {
    RUSSIAN, ENGLISH
}

object LanguageService {
    private var prefs: SharedPreferences? = null
    private const val PREF_KEY_LANGUAGE = "app_selected_language"

    var currentLanguage by mutableStateOf(AppLanguage.RUSSIAN)
        private set

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("techmate_lang_prefs", Context.MODE_PRIVATE)
            val savedLang = prefs?.getString(PREF_KEY_LANGUAGE, null)
            if (savedLang != null) {
                currentLanguage = try {
                    AppLanguage.valueOf(savedLang)
                } catch (e: Exception) {
                    AppLanguage.RUSSIAN
                }
            } else {
                // Auto-detect system language by default
                val sysLang = Locale.getDefault().language.lowercase()
                currentLanguage = if (sysLang in listOf("ru", "be", "uk", "kk", "ky", "uz", "tg")) {
                    AppLanguage.RUSSIAN
                } else {
                    AppLanguage.ENGLISH
                }
            }
        }
    }

    fun toggleLanguage() {
        val newLang = if (currentLanguage == AppLanguage.RUSSIAN) AppLanguage.ENGLISH else AppLanguage.RUSSIAN
        setLanguage(newLang)
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        prefs?.edit()?.putString(PREF_KEY_LANGUAGE, language.name)?.apply()
    }

    fun getString(key: String): String {
        val isRu = currentLanguage == AppLanguage.RUSSIAN
        return when (key) {
            "app_title" -> "Tech.Mate"
            "chats_title" -> if (isRu) "Ремонты" else "Repairs"
            "knowledge_title" -> if (isRu) "База знаний" else "Knowledge Base"
            "profile_title" -> if (isRu) "Профиль" else "Profile"
            "nav_chats" -> if (isRu) "Чаты" else "Chats"
            "nav_knowledge" -> if (isRu) "База знаний" else "Knowledge"
            "nav_profile" -> if (isRu) "Профиль" else "Profile"
            "admin_title" -> if (isRu) "Пользователи" else "User Management"
            "search_chats_hint" -> if (isRu) "Поиск ремонтов..." else "Search repairs..."
            "search_kb_hint" -> if (isRu) "Поиск моделей, проблем..." else "Search models, issues..."
            "empty_chats" -> if (isRu) "Нет ремонтов. Начните новый чат!" else "No repairs. Start a new chat!"
            "empty_kb" -> if (isRu) "Добавьте первое руководство" else "Add your first guide"
            "new_chat" -> if (isRu) "Новый ремонт" else "New Repair"
            "delete" -> if (isRu) "Удалить" else "Delete"
            "pin" -> if (isRu) "Закрепить" else "Pin"
            "unpin" -> if (isRu) "Открепить" else "Unpin"
            "delete_chat_confirm_title" -> if (isRu) "Удалить чат?" else "Delete chat?"
            "delete_chat_confirm_msg" -> if (isRu) "Вы уверены, что хотите удалить этот ремонт?" else "Are you sure you want to delete this repair?"
            "cancel" -> if (isRu) "Отмена" else "Cancel"
            "confirm" -> if (isRu) "Удалить" else "Delete"
            "input_hint" -> if (isRu) "Опишите проблему..." else "Describe the problem..."
            "send" -> if (isRu) "Отправить" else "Send"
            "quick_moisture" -> if (isRu) "💧 Влага" else "💧 Water damage"
            "quick_impact" -> if (isRu) "🔨 Удар" else "🔨 Drop / Impact"
            "quick_reboot" -> if (isRu) "🔄 Перезагрузка" else "🔄 Boot loop"
            "quick_other" -> if (isRu) "❓ Другое" else "❓ Other"
            "attach_camera" -> if (isRu) "Камера" else "Camera"
            "attach_gallery" -> if (isRu) "Галерея" else "Gallery"
            "save_to_kb" -> if (isRu) "Сохранить в базу" else "Save to Knowledge Base"
            "was_helpful" -> if (isRu) "Помогло?" else "Was this helpful?"
            "yes_solved" -> if (isRu) "✅ Да, решено" else "✅ Yes, solved"
            "no_continue" -> if (isRu) "❌ Нет, продолжить" else "❌ No, continue"
            "copy" -> if (isRu) "Копировать" else "Copy"
            "copied_to_clipboard" -> if (isRu) "Текст скопирован" else "Text copied"
            "tab_guides" -> if (isRu) "Руководства" else "Guides"
            "tab_schematics" -> if (isRu) "Схемы" else "Schematics"
            "add_guide" -> if (isRu) "Добавить руководство" else "Add Guide"
            "brand" -> if (isRu) "Бренд" else "Brand"
            "model" -> if (isRu) "Модель" else "Model"
            "problem" -> if (isRu) "Проблема" else "Issue"
            "difficulty" -> if (isRu) "Сложность" else "Difficulty"
            "time_estimate" -> if (isRu) "Время" else "Time estimate"
            "tools" -> if (isRu) "Инструменты" else "Tools"
            "causes" -> if (isRu) "Вероятные причины" else "Likely causes"
            "steps" -> if (isRu) "Пошаговая диагностика" else "Step-by-step diagnostics"
            "pro_tip" -> if (isRu) "Совет" else "Pro Tip"
            "risks" -> if (isRu) "Риски" else "Risks"
            "save" -> if (isRu) "Сохранить" else "Save"
            "stats_requests" -> if (isRu) "Всего запросов" else "Total Requests"
            "stats_solved" -> if (isRu) "Решено проблем" else "Solved Issues"
            "stats_added_kb" -> if (isRu) "Добавлено в базу" else "Added to KB"
            "dark_theme" -> if (isRu) "Тёмная тема" else "Dark Theme"
            "language" -> if (isRu) "Язык" else "Language"
            "lang_name" -> if (isRu) "Русский" else "English"
            "admin_panel" -> if (isRu) "Панель администратора" else "Admin Panel"
            "logout" -> if (isRu) "Выйти из аккаунта" else "Log Out"
            "version" -> if (isRu) "Версия приложения: v1.0.0" else "App Version: v1.0.0"
            "login_title" -> if (isRu) "Вход" else "Sign In"
            "register_admin_title" -> if (isRu) "Создать аккаунт администратора" else "Create Administrator Account"
            "username" -> if (isRu) "Имя пользователя" else "Username"
            "password" -> if (isRu) "Пароль" else "Password"
            "name_label" -> if (isRu) "ФИО / Имя мастера" else "Full Name / Tech Name"
            "login_button" -> if (isRu) "Войти" else "Log In"
            "register_button" -> if (isRu) "Зарегистрировать" else "Register"
            "user_limit_error" -> if (isRu) "Достигнут лимит: 10 пользователей" else "Limit reached: 10 users"
            "login_error" -> if (isRu) "Неверный логин или пароль" else "Invalid username or password"
            "user_role" -> if (isRu) "Роль" else "Role"
            "role_admin" -> if (isRu) "Администратор" else "Administrator"
            "role_master" -> if (isRu) "Мастер" else "Master Tech"
            "role_viewer" -> if (isRu) "Наблюдатель" else "Viewer"
            "add_user" -> if (isRu) "Добавить пользователя" else "Add User"
            "offline_banner" -> if (isRu) "Нет подключения" else "No Connection"
            "ai_error" -> if (isRu) "ИИ недоступен" else "AI unavailable"
            "retry" -> if (isRu) "Повторить" else "Retry"
            "ai_typing" -> if (isRu) "Tech.Mate думает..." else "Tech.Mate is thinking..."
            "forgot_password" -> if (isRu) "Забыли пароль?" else "Forgot password?"
            "reset_password_title" -> if (isRu) "Сброс пароля" else "Reset Password"
            "reset_password_desc" -> if (isRu) "Для безопасности приложения сброс пароля выполняет Администратор. Обратитесь к главному мастеру/администратору вашей мастерской для установки нового пароля." else "For security reasons, password resets are performed by an Administrator. Contact your workshop master/admin to set a new password."
            "mark_solved" -> if (isRu) "Решено!" else "Solved!"
            "solved_dialog_title" -> if (isRu) "Проблема решена!" else "Problem Solved!"
            "solved_dialog_desc" -> if (isRu) "Добавить краткую сводку этого ремонта в Базу Знаний для других мастеров?" else "Add a concise summary of this repair to the Knowledge Base for other technicians?"
            "solved_summary_added" -> if (isRu) "✅ Сводка ремонта успешно добавлена в Базу Знаний!" else "✅ Repair summary successfully added to Knowledge Base!"
            // Testpoint Verification strings
            "tp_verify_title" -> if (isRu) "Проверка тестпоинта" else "TestPoint Verification"
            "tp_is_this_board" -> if (isRu) "Эта плата?" else "Is this the correct motherboard?"
            "tp_is_this_board_desc" -> if (isRu) "Сверьте расположение контрольных точек и компонентов." else "Verify test point pads and component locations."
            "tp_yes_approve" -> if (isRu) "✅ Да, эта плата" else "✅ Yes, this motherboard"
            "tp_no_reject" -> if (isRu) "❌ Нет, другая плата" else "❌ No, different motherboard"
            "tp_approved_saved" -> if (isRu) "✅ Плата подтверждена и сохранена в базу" else "✅ Motherboard verified and saved to database"
            "tp_rejected_searching" -> if (isRu) "❌ Отклонено (ищем другой вариант)" else "❌ Rejected (searching alternative)"
            "tp_searching_next" -> if (isRu) "Ищу другое фото платы..." else "Searching another motherboard photo..."
            "tp_open_in_kb" -> if (isRu) "📖 В базе знаний" else "📖 View in KB"
            // Registration strings
            "auth_step_email" -> if (isRu) "Вход и регистрация мастера" else "Technician Login & Registration"
            "auth_step_email_desc" -> if (isRu) "Введите рабочий email сервисного центра" else "Enter your service center email"
            "auth_step_otp" -> if (isRu) "Код подтверждения" else "Verification Code"
            "auth_step_profile" -> if (isRu) "Профиль мастера" else "Technician Profile"
            "auth_step_security" -> if (isRu) "Защита устройства" else "Device Security"
            "auth_lang_toggle" -> if (isRu) "Язык: Русский" else "Language: English"
            else -> key
        }
    }
}
