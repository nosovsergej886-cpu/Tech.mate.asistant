package com.example.services

import android.content.Context
import com.example.model.StoryEntity
import com.example.model.StoryScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class StoryService private constructor(private val context: Context) {

    private val dbService = DatabaseService.getInstance(context)
    private val storyDao = dbService.storyDao

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // Remove hardcoded iPhone alert template if present in local database
            storyDao.deleteStory("alert_iphone_ios_brick")
            seedDefaultStories()
        }
    }

    fun getStories(serviceCenterId: String): Flow<List<StoryEntity>> {
        // Return all stories for social media-style unified feed
        return storyDao.getAllStories()
    }

    fun getAllStories(): Flow<List<StoryEntity>> {
        return storyDao.getAllStories()
    }

    suspend fun incrementViews(storyId: String) {
        storyDao.incrementViews(storyId)
    }

    suspend fun deleteStory(storyId: String) {
        storyDao.deleteStory(storyId)
    }

    /**
     * Social Stories Publishing:
     * - Publishes stories into the unified public feed so all team members and masters see them.
     * - Flags critical hardware/software alerts with prominent indicators.
     */
    suspend fun publishStory(
        title: String,
        subtitle: String,
        content: String,
        authorName: String,
        authorEmail: String,
        authorServiceCenter: String,
        authorServiceCenterId: String,
        requestedScope: StoryScope,
        iconEmoji: String = "📸",
        taggedDeviceModel: String = "",
        taggedCategory: String = "",
        mediaType: String = "PHOTO",
        mediaUrl: String = ""
    ): StoryEntity {
        val lowerText = (title + " " + subtitle + " " + content).lowercase()

        val hasGlobalFailureKeywords = listOf(
            "кирпич", "окирпич", "не обновляй", "не шей", "сбой", "отвал",
            "bootloop", "вечный перезапуск", "брак", "перегрев", "кз",
            "прошивк", "ios", "redmi", "xiaomi", "hyperos", "miui",
            "baseband", "модем", "отвал модема", "edl 9008", "brom"
        ).any { lowerText.contains(it) }

        val finalScope: StoryScope
        val warningLevel: String

        if (requestedScope == StoryScope.GLOBAL_ALERT || hasGlobalFailureKeywords) {
            finalScope = StoryScope.GLOBAL_ALERT
            warningLevel = "CRITICAL"
        } else if (requestedScope == StoryScope.ALGORITHM) {
            finalScope = StoryScope.ALGORITHM
            warningLevel = "NORMAL"
        } else {
            // Social feed stories are visible globally to all colleagues
            finalScope = StoryScope.GLOBAL_UPDATE
            warningLevel = "NORMAL"
        }

        val entity = StoryEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            subtitle = subtitle.trim(),
            content = content.trim(),
            authorName = authorName,
            authorEmail = authorEmail,
            authorServiceCenter = authorServiceCenter,
            authorServiceCenterId = authorServiceCenterId,
            scope = finalScope.name,
            isModeratedPublic = true,
            iconEmoji = iconEmoji,
            warningLevel = warningLevel,
            isPermanent = false,
            taggedDeviceModel = taggedDeviceModel,
            taggedCategory = taggedCategory,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            createdAt = System.currentTimeMillis()
        )

        storyDao.insertStory(entity)
        try {
            CustomHostSyncService.getInstance(context).autoPushStory(entity)
        } catch (_: Exception) {}
        return entity
    }

    private suspend fun seedDefaultStories() {
        val permanentAlgorithm = StoryEntity(
            id = "perm_repair_algorithm",
            title = "Этапы ремонта",
            subtitle = "12 шагов регламента",
            content = """
                📋 Официальный 12-этапный регламент компонентного ремонта Tech.Mate:
                
                1️⃣ Первичный осмотр: оптика, следы влаги, компаунд, сколы кристаллов BGA.
                2️⃣ Замер VBUS: входной ток, пульсации, детекция протоколов PD / QC.
                3️⃣ Проверка первичной цепи VBAT/VPH_PWR: поиск КЗ мультиметром в диодной прозвонке.
                4️⃣ Тепловизионная локализация: подача пониженного напряжения, поиск греющихся керамических конденсаторов.
                5️⃣ Подключение к ЛБП: анализ графика старта (зависание на 80мА — ОЗУ/CPU, 180мА — eMMC/UFS).
                6️⃣ Диагностика Buck/LDO контроллера питания (PMIC): замеры VDD_CORE, VDD_CPU, VDD_MODEM.
                7️⃣ Осциллографирование шин данных I2C / SPI / SPMI: поиск подтяжек и ответов ведомых микросхем.
                8️⃣ Проверка генерации опорных кварцев: 32.768 кГц (часы) и 38.4 МГц (радиочастотный блок).
                9️⃣ Аварийные интерфейсы BROM / EDL 9008 через замыкание TestPoint: проверка ответа процессора.
                🔟 Пайка и реболлинг BGA: очистка контактных площадок сплавом Розе, накатка шаров по трафарету прямого нагрева.
                1️⃣1️⃣ Контрольные замеры после монтажа: проверка сопротивлений перед первым включением.
                1️⃣2️⃣ Финальное стресс-тестирование: проверка сети, заряда, камер, датчиков и отдача клиенту.
            """.trimIndent(),
            authorName = "Tech.Mate Стандарт",
            authorEmail = "nosovsergej886@gmail.com",
            authorServiceCenter = "Сертифицированный регламент",
            authorServiceCenterId = "all",
            scope = StoryScope.ALGORITHM.name,
            isModeratedPublic = true,
            iconEmoji = "📋",
            warningLevel = "NORMAL",
            isPermanent = true,
            createdAt = 1000L
        )
        storyDao.insertStory(permanentAlgorithm)

        // Global Alert 2: Redmi HyperOS mass bootloop
        val redmiAlert = StoryEntity(
            id = "alert_redmi_hyperos_bootloop",
            title = "Redmi: Bootloop HyperOS",
            subtitle = "Массовый сбой ПО Redmi",
            content = """
                ⚡ ВНИМАНИЕ: МАССОВЫЙ СБОЙ ПО НА СМАРТФОНАХ REDMI!
                
                Ночное автоматическое обновление HyperOS на Redmi Note 11/12 вызывает циклическую перезагрузку (Bootloop) в Mi-Recovery.
                
                🛑 КАТЕГОРИЧЕСКИ НЕ ДЕЛАТЬ Wipe Data / Factory Reset — данные пользователя можно сохранить!
                
                Порядок восстановления:
                1. Перевод устройства в аварийный режим EDL (Qualcomm 9008) через TestPoint или BROM (MediaTek).
                2. Прошивка сервисного ядра (boot.img / vendor_boot) через авторизованный протокол.
                3. Сохранение раздела nvram / nvdata для предотвращения затирания IMEI.
            """.trimIndent(),
            authorName = "Инженер-диагност BROM",
            authorEmail = "master@repair.tech",
            authorServiceCenter = "Лаборатория прошивок",
            authorServiceCenterId = "all",
            scope = StoryScope.GLOBAL_ALERT.name,
            isModeratedPublic = true,
            iconEmoji = "⚡",
            warningLevel = "CRITICAL",
            isPermanent = false,
            createdAt = System.currentTimeMillis() - 7200000L
        )
        storyDao.insertStory(redmiAlert)

        // Global Update from Developer: Tech.Mate Updates
        val updateStory = StoryEntity(
            id = "update_techmate_v25",
            title = "Обновление Tech.Mate v2.5",
            subtitle = "Новые функции для всех СЦ",
            content = """
                🚀 Официальный релиз Tech.Mate v2.5 для всех мастеров:
                
                Что нового:
                • Интерактивная модерация историй: глобальные предупреждения о сбоях ПО и окирпичивании техники теперь видны всем мастерам платформы в реальном времени!
                • Локальные истории для своего СЦ: обменивайтесь информацией об оборудовании и донорах внутри вашей мастерской.
                • Новый интерактивный просмотрщик регламента 12 шагов ремонта.
                • Оптимизация навигации под Облачный (Aero), Портальный (Connect) и Изумрудный (Emerald) стили интерфейса.
                
                Благодарим за использование Tech.Mate!
            """.trimIndent(),
            authorName = "Сергей Носов (Автор)",
            authorEmail = "nosovsergej886@gmail.com",
            authorServiceCenter = "Разработка Tech.Mate",
            authorServiceCenterId = "all",
            scope = StoryScope.GLOBAL_UPDATE.name,
            isModeratedPublic = true,
            iconEmoji = "🚀",
            warningLevel = "IMPORTANT",
            isPermanent = false,
            createdAt = System.currentTimeMillis() - 10800000L
        )
        storyDao.insertStory(updateStory)

        // Local SC Story: Equipment & donor boards
        val localScStory = StoryEntity(
            id = "local_sc_donors",
            title = "Доноры и BGA-трафареты",
            subtitle = "Для мастеров нашего СЦ",
            content = """
                🏢 Внимание мастерам нашего СЦ:
                
                На склад поступила свежая партия расходников и доноров:
                • Донорские платы: Xiaomi Poco X3 Pro (КП PM7150), iPhone 11 (целый аудиокодек).
                • Трафареты прямого нагрева BGA для PMIC Qualcomm / MTK.
                • Калиброван тепловизор UNI-T на диагностическом месте №2.
                
                Расходники брать с отметкой в журнале учета.
            """.trimIndent(),
            authorName = "Старший мастер СЦ",
            authorEmail = "lead@techno.local",
            authorServiceCenter = "СЦ «ТехноМастер» #1",
            authorServiceCenterId = "default_sc",
            scope = StoryScope.LOCAL_SC.name,
            isModeratedPublic = false,
            iconEmoji = "🏢",
            warningLevel = "NORMAL",
            isPermanent = false,
            createdAt = System.currentTimeMillis() - 14400000L
        )
        storyDao.insertStory(localScStory)
    }

    companion object {
        @Volatile
        private var INSTANCE: StoryService? = null

        fun getInstance(context: Context): StoryService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StoryService(context).also { INSTANCE = it }
            }
        }
    }
}
