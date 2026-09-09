package com.example.services

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

enum class AppDesignVariant(val titleRu: String, val descriptionRu: String, val iconEmoji: String) {
    TELEGRAM(
        titleRu = "Облачный стиль (Aero)",
        descriptionRu = "Минималистичный интерфейс: лазурно-синие акценты, плавающий круглый FAB, мягкие пузыри сообщений с хвостиками",
        iconEmoji = "☁️"
    ),
    VK(
        titleRu = "Портальный стиль (Connect)",
        descriptionRu = "Сервисная платформа: глубокий синий #0077FF, контурные карточки ленты, статус активности и быстрая кнопка '+' для вложений",
        iconEmoji = "🌐"
    ),
    WHATSAPP(
        titleRu = "Изумрудный стиль (Emerald)",
        descriptionRu = "Корпоративный мессенджер: глубокий темно-изумрудный заголовок, фисташковые пузыри и отдельная круглая кнопка отправки",
        iconEmoji = "🟢"
    )
}

enum class SplashAnimationType(val titleRu: String, val descriptionRu: String, val iconEmoji: String) {
    CIRCUIT_TRACE(
        titleRu = "Электро-схема (Tech Circuit)",
        descriptionRu = "Медные проводящие дорожки, тестпоинты и пульсирующий чипсет",
        iconEmoji = "⚡"
    ),
    CYBER_SCAN(
        titleRu = "Кибер-сканер (Neon Scanline)",
        descriptionRu = "Лазерное сканирование кремниевого кристалла и неоновые лучи",
        iconEmoji = "🟢"
    ),
    BGA_LASER(
        titleRu = "BGA Реболлинг (Laser Reball)",
        descriptionRu = "Сборка матрицы шариков BGA и термо-лазерная вспышка",
        iconEmoji = "🔬"
    ),
    FESTIVE_NEW_YEAR(
        titleRu = "Новогодний ивент (Festive Sparkle)",
        descriptionRu = "Золотые искры, праздничная подсветка дорожек и новогодний стиль",
        iconEmoji = "✨"
    ),
    BIOS_TERMINAL(
        titleRu = "BIOS Терминал (Matrix Boot)",
        descriptionRu = "Инженерная проверка оборудования, самодиагностика системных шин",
        iconEmoji = "💻"
    )
}

enum class EventTheme(val titleRu: String, val descriptionRu: String, val iconEmoji: String, val bannerText: String) {
    STANDARD(
        titleRu = "Стандартный TechMate",
        descriptionRu = "Классический профессиональный стиль инженерного центра",
        iconEmoji = "🛠️",
        bannerText = "TechMate • Сервисный центр"
    ),
    NEW_YEAR_FESTIVE(
        titleRu = "Новогодний ивент 2026",
        descriptionRu = "Праздничное оформление, золотые акценты и новогодние бонусы",
        iconEmoji = "🎄",
        bannerText = "🎄 С Новым Годом и Рождеством мастеров! ❄️"
    ),
    CYBERPUNK_NEON(
        titleRu = "Киберпанк 2077 Неон",
        descriptionRu = "Неоновый циановый и электро-фиолетовый кибер-стиль будущего",
        iconEmoji = "👾",
        bannerText = "⚡ CYBER_REPAIR // MATRIX_ONLINE"
    ),
    PRO_ENGINEER(
        titleRu = "Про-Инженер (Янтарный Титан)",
        descriptionRu = "Тёмный титан, янтарные индикаторы паяльных станций Weller/JBC",
        iconEmoji = "⚙️",
        bannerText = "🛠️ Hardware Engineering Division"
    )
}

class ThemeManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("techmate_theme_prefs", Context.MODE_PRIVATE)
    
    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name)
        } catch (e: Exception) {
            AppThemeMode.DARK
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _designVariant = MutableStateFlow(
        try {
            AppDesignVariant.VK
        } catch (e: Exception) {
            AppDesignVariant.VK
        }
    )
    val designVariant: StateFlow<AppDesignVariant> = _designVariant.asStateFlow()

    private val _splashAnimation = MutableStateFlow(
        try {
            SplashAnimationType.valueOf(prefs.getString(KEY_SPLASH_ANIMATION, SplashAnimationType.CIRCUIT_TRACE.name) ?: SplashAnimationType.CIRCUIT_TRACE.name)
        } catch (e: Exception) {
            SplashAnimationType.CIRCUIT_TRACE
        }
    )
    val splashAnimation: StateFlow<SplashAnimationType> = _splashAnimation.asStateFlow()

    private val _eventTheme = MutableStateFlow(
        try {
            EventTheme.valueOf(prefs.getString(KEY_EVENT_THEME, EventTheme.STANDARD.name) ?: EventTheme.STANDARD.name)
        } catch (e: Exception) {
            EventTheme.STANDARD
        }
    )
    val eventTheme: StateFlow<EventTheme> = _eventTheme.asStateFlow()

    fun canUserModifyEvents(userEmail: String?): Boolean {
        if (userEmail == null) return false
        val normalized = userEmail.trim().lowercase()
        return normalized == "nosovsergej886@gmail.com" || normalized == "admin" || normalized == "god"
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun toggleDarkTheme(isDark: Boolean) {
        setThemeMode(if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT)
    }

    fun setDesignVariant(variant: AppDesignVariant) {
        _designVariant.value = variant
        prefs.edit().putString(KEY_DESIGN_VARIANT, variant.name).apply()
    }

    fun setSplashAnimation(anim: SplashAnimationType, userEmail: String?): Boolean {
        if (!canUserModifyEvents(userEmail)) {
            return false
        }
        _splashAnimation.value = anim
        prefs.edit().putString(KEY_SPLASH_ANIMATION, anim.name).apply()
        return true
    }

    fun setEventTheme(event: EventTheme, userEmail: String?): Boolean {
        if (!canUserModifyEvents(userEmail)) {
            return false
        }
        _eventTheme.value = event
        prefs.edit().putString(KEY_EVENT_THEME, event.name).apply()
        return true
    }

    // Force apply from cloud sync (e.g. pulled from Firestore for all devices)
    fun applyRemoteEventConfig(eventName: String?, splashName: String?) {
        if (eventName != null) {
            try {
                _eventTheme.value = EventTheme.valueOf(eventName)
                prefs.edit().putString(KEY_EVENT_THEME, eventName).apply()
            } catch (_: Exception) {}
        }
        if (splashName != null) {
            try {
                _splashAnimation.value = SplashAnimationType.valueOf(splashName)
                prefs.edit().putString(KEY_SPLASH_ANIMATION, splashName).apply()
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_DESIGN_VARIANT = "app_design_variant"
        private const val KEY_SPLASH_ANIMATION = "app_splash_animation"
        private const val KEY_EVENT_THEME = "app_event_theme"

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

