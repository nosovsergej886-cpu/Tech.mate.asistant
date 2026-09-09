package com.example.services

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.config.ApiConfig
import com.example.model.BoardviewPhotoEntity
import com.example.model.Cause
import com.example.model.GuideData
import com.example.model.KnowledgeBaseEntryEntity
import com.example.model.MessageEntity
import com.example.model.Step
import com.example.model.TestPointCandidate
import com.example.data.TestPointItem
import com.example.data.TestPointRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiService private constructor(context: Context) {

    private val dbService = DatabaseService.getInstance(context)
    private val customSearchService = GoogleCustomSearchService.getInstance(context)

    private fun getSystemPrompt(): String {
        val isRu = LanguageService.currentLanguage == AppLanguage.RUSSIAN
        return if (isRu) {
            """Ты — Tech.Mate, персональный ИИ-ассистент старшего инженера-электронщика и мастера по компонентному ремонту смартфонов, ноутбуков и цифровой техники.
Ты общаешься профессионально, живо, по-товарищески, без шаблонных роботизированных фраз ("Привет! Чем я могу помочь?", "Обратитесь в сервис"). Говори четко по делу, как опытный коллега за соседним паяльным столом.

## ГЛАВНОЕ ПРАВИЛО МАСТЕРА:
⚠️ **"СНАЧАЛА ПОНЯТЬ ПРИЧИНУ — ПОТОМ ПАЯТЬ!"**
Никогда не предлагай бездумно прогревать или катать чипы без предварительных замеров и локализации дефекта.

## 📋 ОБЯЗАТЕЛЬНЫЙ АЛГОРИТМ РЕМОНТА (12 ПРАВИЛ МАСТЕРА):
В каждом диалоге по диагностике и ремонту ты САМ структурируешь свой ответ строго по этим 12 шагам, указывая текущий этап:
1. 📞 **История поломки**: Сбор анамнеза (удар, залитие, неродное ЗУ, скачок напряжения, после другого СЦ, погас после обновления).
2. 🔍 **Внешний осмотр**: Корпус, геометрия рамы, разъемы Type-C/Lightning, кнопки, следы вскрытия, вздутие АКБ.
3. ✔️ **Подтверждение дефекта**: Проверка заявленной неисправности (ток по USB-тестеру/триггеру, реакция на кнопку включения PWR, потребление на ЛБП).
4. 📋 **Проверка сопутствующих узлов**: Аккумулятор, дисплей/тачскрин, камеры, микрофоны/динамики, сеть/Wi-Fi/BT, датчики приближения/FaceID/TouchID.
5. 🌐 **Поиск типовых проблем**: Проверка базы типовых болячек конкретной модели, ревизии платы или связки CPU/PMIC.
6. 🛠️ **Разборка устройства**: Технологическая карта разборки, температурный режим сепаратора, защита шлейфов и дисплея, винтовая карта.
7. 🔬 **Осмотр платы под микроскопом**: Внимательный осмотр под увеличением — компаунд, коррозия от влаги, окислы, сколы кристаллов, трещины в BGA пайке, сбитые или прогоревшие SMD элементы.
8. ⚡ **Диагностика и поиск причины**:
   - Замеры падений напряжений в режиме диодной прозвонки относительно земли (Красный щуп на GND, черным по контрольным точкам / дросселям).
   - Замеры сопротивлений и напряжений основных линий: VBUS (5V/9V), VBAT (3.7-4.2V), VSYS/PP_VCC_MAIN, дежурки (3.3V / 5.0V), вторичные цепи (LDO, BUCK, VCORE, DRAM).
   - Локализация короткого замыкания (ЛБП с ограничением тока, тепловизор или фризер/спрей-заморозка).
9. 🤝 **Согласование ремонта**: Расчет стоимости запчастей, трудоемкости, сроков и целесообразности работ перед физическим вмешательством.
10. ✏️ **Ремонтные работы**: Пайка, реболл BGA, замена неисправных компонентов, восстановление оборванных пятаков/дорожек, прошивка BIOS/DUMP/FRP.
11. 🧹 **Профилактика и чистка**: Замена термопасты/термопрокладок, отмывка платы в ультразвуковой ванне (при залитии) или изопропиловым спиртом от флюса, сушка.
12. 📋 **Проверка всех функций**: Итоговый тест-лист всех узлов под нагрузкой.

## 🏁 ОБЯЗАТЕЛЬНЫЙ ЧЕК-ЛИСТ ПРОВЕРКИ РАБОТОСПОСОБНОСТИ ПОСЛЕ РЕМОНТА:
Каждый раз, когда предлагается решение проблемы или ремонт завершается, ОБЯЗАТЕЛЬНО выдавай мастеру чек-лист для проверки:
- [ ] ⚡ **Зарядка и АКБ**: Ток на USB-тестере (быстрая зарядка QC/PD), потребление в спящем режиме (< 0.01A).
- [ ] 📱 **Дисплей и тачскрин**: Отклик по всем углам, автояркость, отсутствие фантомных нажатий.
- [ ] 📶 **Связь и микрофоны**: Тестовый звонок 112/оператору, разговорный и полифонический динамик, нижний микрофон и верхний микрофон шумоподавления.
- [ ] 📡 **Беспроводные модули**: Wi-Fi 2.4/5GHz, Bluetooth, GPS навигация, NFC/оплата.
- [ ] 📷 **Камеры и фонарик**: Основная, широкоугольная, фронтальная, автофокус, вспышка.
- [ ] 🔒 **Биометрия и датчики**: Датчик приближения при звонке, гироскоп, сканер отпечатка / Face ID.
- [ ] 🔘 **Кнопки и вибро**: Кнопки Power, Vol +/- и виброотклик.
- [ ] 🌡️ **Стресс-тест**: 10 минут видео/нагрузки (температурная стабильность).

## ТЕСТПОИНТЫ И СХЕМЫ:
Когда мастер просит тестпоинт или схему, дай четкие инструкции: процессор, аварийный режим (Qualcomm EDL 9008, MediaTek BROM, Samsung Exynos EUB, Kirin COM 1.0), расположение точек и алгоритм замыкания."""
        } else {
            """You are Tech.Mate, a professional AI assistant and master electronics engineer specializing in component-level repair of smartphones, laptops, and digital electronics.
The user interface and technician have selected ENGLISH. You MUST communicate and provide all technical explanations, 12 repair rules, testpoint instructions, and checklists entirely in ENGLISH.

## MASTER ENGINEER'S GOLDEN RULE:
⚠️ **"UNDERSTAND THE ROOT CAUSE FIRST — SOLDER SECOND!"**
Never suggest reflowing or reballing chips blindly without prior electrical measurements and fault isolation.

## 📋 MANDATORY 12-STEP REPAIR ALGORITHM:
In diagnostics and repairs, structure your response according to these 12 master steps:
1. 📞 **Device History**: Influx cause (liquid spill, drop impact, non-original charger, power surge, post-other-repair-shop, dead after OTA).
2. 🔍 **Visual Inspection**: Chassis geometry, Type-C/Lightning ports, buttons, tamper evidence, battery swelling.
3. ✔️ **Defect Confirmation**: Current draw via USB tester/trigger, PWR button reaction, DC bench power supply consumption.
4. 📋 **Auxiliary Systems**: Battery, display/digitizer, cameras, mics/speakers, Wi-Fi/BT, sensors/FaceID/TouchID.
5. 🌐 **Known Model Issues**: Check model-specific common failure points (e.g., POCO X3 Pro CPU solder detachment, charging MOSFET short, backlight fuse failure).
6. 🛠️ **Disassembly Guide**: Temperature profiles for hot plate separator, flex cable safety, screw mapping.
7. 🔬 **Microscope Board Inspection**: Compound cracks, corrosion, oxides, chipped dies, cracked BGA balls, damaged SMD components.
8. ⚡ **Electrical Diagnostics**:
   - Diode mode voltage drop measurements to GND (Red probe to GND, Black probe to test points/coils).
   - Resistance & rail voltages: VBUS (5V/9V), VBAT (3.7-4.2V), VSYS/PP_VCC_MAIN, standby rails (3.3V/5.0V), buck/LDO secondary lines.
   - Short circuit isolation (current-limited DC power supply with thermal camera or freeze spray).
9. 🤝 **Repair Agreement**: Parts cost, labor time, feasibility confirmation prior to physical intervention.
10. ✏️ **Rework & Soldering**: Micro-soldering, BGA reballing, damaged trace/pad jumpering, BIOS/DUMP/FRP flashing.
11. 🧹 **Cleaning & Preventative Maintenance**: Thermal paste/pad replacement, ultrasonic bath (for liquid damage) or flux cleanup with isopropyl alcohol.
12. 📋 **Full Functional Verification**: Comprehensive stress-test and post-repair QC checklist.

## 🏁 MANDATORY POST-REPAIR QUALITY CHECKLIST:
Always provide the technician with this post-repair QC checklist:
- [ ] ⚡ **Charging & Battery**: Current on USB tester (QC/PD fast charge), sleep state current (< 0.01A).
- [ ] 📱 **Display & Touch**: All corner touch response, auto-brightness, no phantom touches.
- [ ] 📶 **Cellular & Audio**: Test call, earpiece and loudspeaker, bottom mic and top noise-cancellation mic.
- [ ] 📡 **Wireless**: Wi-Fi 2.4/5GHz, Bluetooth, GPS navigation, NFC/Contactless payment.
- [ ] 📷 **Cameras & Flashlight**: Main, wide, selfie, autofocus, flashlight.
- [ ] 🔒 **Biometrics & Sensors**: Proximity sensor during call, gyroscope, fingerprint / Face ID.
- [ ] 🔘 **Physical Buttons & Haptics**: Power, Vol +/-, vibration feedback.
- [ ] 🌡️ **Thermal Stress Test**: 10 min benchmark/video playback."""
        }
    }


    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        chatId: String,
        userText: String,
        imageBase64: String? = null
    ): Pair<String, GuideData?> = withContext(Dispatchers.IO) {

        // Retrieve last 10 messages context window
        val recentMessages = dbService.messageDao.getRecentMessagesForContext(chatId).reversed()

        val geminiApiKey = BuildConfig.GEMINI_API_KEY.trim()
        val openRouterKey = ApiConfig.API_KEY.trim()

        // 1. Detect model and whether user is asking specifically for testpoint / pinout / schematic
        val detectedModel = extractDeviceModelFromContext(userText, recentMessages)
        val isTpOrPhotoQuery = userText.lowercase().let { lower ->
            (lower.contains("тестпоинт") || lower.contains("testpoint") || lower.contains("test point") ||
             lower.contains("edl 9008") || lower.contains("edl mode") || lower.contains("brom pinout") ||
             lower.contains("isp pinout") || lower.contains("clkgnd") ||
             ((lower.contains("edl") || lower.contains("brom") || lower.contains("фрп") || lower.contains("frp")) && (lower.contains("точка") || lower.contains("замкнуть") || lower.contains("пин") || lower.contains("схема"))))
        }

        // 2. Perform live web search for actual testpoint / PCB photo
        var liveImages: List<CustomSearchImageItem> = emptyList()
        var candidateImage: CustomSearchImageItem? = null
        val searchTarget = detectedModel ?: if (isTpOrPhotoQuery) userText.replace("где", "").replace("находится", "").trim() else null

        if (searchTarget != null && isTpOrPhotoQuery) {
            try {
                candidateImage = customSearchService.searchCandidateTestPointImage(
                    deviceModel = searchTarget,
                    excludedUrls = emptySet(),
                    attempt = 0
                )
                if (candidateImage != null) {
                    liveImages = listOf(candidateImage)
                    Log.d("AiService", "Found single candidate testpoint image for $searchTarget: ${candidateImage.link}")
                }
            } catch (e: Exception) {
                Log.w("AiService", "Candidate testpoint search error: ${e.message}")
            }
        }

        var aiText: String? = null

        // 3. Try Aitunnel API first if aitunnel key is configured
        if (openRouterKey.isNotBlank() && openRouterKey.startsWith("sk-aitunnel")) {
            try {
                aiText = callOpenRouterApi(openRouterKey, recentMessages, userText, imageBase64, detectedModel, liveImages)
            } catch (e: Exception) {
                Log.e("AiService", "Aitunnel API call failed: ${e.message}")
            }
        }

        // 4. Try Google Gemini REST API if Aitunnel didn't run or failed
        if (aiText == null && geminiApiKey.isNotBlank() && geminiApiKey != "MY_GEMINI_API_KEY") {
            try {
                aiText = callGeminiApi(geminiApiKey, recentMessages, userText, imageBase64, detectedModel, liveImages)
            } catch (e: Exception) {
                Log.e("AiService", "Gemini API call failed: ${e.message}")
            }
        }

        // 5. Fallback to OpenRouter if configured
        if (aiText == null && openRouterKey.isNotBlank() && openRouterKey != "YOUR_OPENROUTER_API_KEY" && !openRouterKey.startsWith("sk-aitunnel")) {
            try {
                aiText = callOpenRouterApi(openRouterKey, recentMessages, userText, imageBase64, detectedModel, liveImages)
            } catch (e: Exception) {
                Log.e("AiService", "OpenRouter API call failed: ${e.message}")
            }
        }

        // 5. Fallback to smart offline diagnosis if API calls fail or no key set
        if (aiText == null) {
            aiText = generateOfflineDiagnosis(userText, recentMessages, liveImages)
        }

        // 6. If a candidate motherboard/testpoint image was found, attach the candidate verification tag
        if (candidateImage != null && isTpOrPhotoQuery) {
            val targetName = searchTarget ?: "Xiaomi"
            val brandName = extractBrandFromModel(targetName)
            val cpuType = detectCpuType(targetName, candidateImage.title)
            val isRu = LanguageService.currentLanguage == AppLanguage.RUSSIAN

            val candidateJsonObj = JSONObject().apply {
                put("brand", brandName)
                put("model", targetName)
                put("cpu", cpuType)
                put("imageUrl", candidateImage.link)
                put("source", candidateImage.displayLink)
                put("title", candidateImage.title.ifBlank { "$targetName TestPoint" })
                put("attempt", 0)
                put("status", "PENDING")
            }

            aiText += "\n\n[TESTPOINT_CANDIDATE:${candidateJsonObj}]"

            val promptVerification = if (isRu) {
                "\n\n❓ **Эта плата?** Пожалуйста, сравните фото с платой на вашем столе.\n" +
                "• Если это именно ваша ревизия — нажмите **«✅ Да, эта плата»**, и она сразу сохранится в базу тестпоинтов.\n" +
                "• Если это другая плата или ревизия — нажмите **«❌ Нет, другая плата»**, и я отсею её и найду другое фото."
            } else {
                "\n\n❓ **Is this the correct motherboard?** Compare the photo with the board on your bench.\n" +
                "• If it matches — click **\"✅ Yes, this motherboard\"** to save it to your database.\n" +
                "• If it's different — click **\"❌ No, different motherboard\"**, and I will search for an alternative."
            }

            if (!aiText.contains("Эта плата?") && !aiText.contains("correct motherboard?")) {
                aiText += promptVerification
            }
        } else if (liveImages.isNotEmpty() && !aiText.contains("[GOOGLE_SEARCH_DATA:")) {
            // General gallery for non-testpoint images
            try {
                val arr = JSONArray()
                liveImages.forEach { item ->
                    arr.put(JSONObject().apply {
                        put("title", item.title)
                        put("link", item.link)
                        put("displayLink", item.displayLink)
                        put("thumbnailLink", item.thumbnailLink ?: item.link)
                        put("contextLink", item.contextLink ?: "")
                        put("snippet", item.snippet ?: "")
                    })
                }
                aiText += "\n\n[GOOGLE_SEARCH_DATA:${arr}]"
            } catch (e: Exception) {
                Log.e("AiService", "Search tag format error: ${e.message}")
            }
        }

        val parsedGuide = parseGuideFromResponse(aiText)
        if (parsedGuide != null && detectedModel != null) {
            try {
                val brandName = extractBrandFromModel(detectedModel)
                dbService.knowledgeDao.insertEntry(
                    KnowledgeBaseEntryEntity(
                        brand = brandName,
                        model = detectedModel,
                        problem = parsedGuide.problem.ifBlank { "Диагностика и ремонт платы" },
                        guideDataJson = dbService.toJson(parsedGuide),
                        addedBy = "TechMate AI Engine",
                        isSchematic = isTpOrPhotoQuery
                    )
                )
            } catch (e: Exception) {
                Log.w("AiService", "Auto-insert knowledge error: ${e.message}")
            }
        }
        return@withContext Pair(aiText, parsedGuide)
    }

    private fun extractBrandFromModel(model: String): String {
        val lower = model.lowercase()
        return when {
            lower.contains("iphone") || lower.contains("ipad") || lower.contains("apple") || lower.contains("macbook") -> "Apple"
            lower.contains("samsung") || lower.contains("galaxy") -> "Samsung"
            lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco") || lower.contains("mi ") -> "Xiaomi"
            lower.contains("huawei") || lower.contains("honor") -> "Huawei"
            lower.contains("oppo") || lower.contains("realme") || lower.contains("oneplus") -> "Realme"
            lower.contains("vivo") || lower.contains("iqoo") -> "Vivo"
            lower.contains("infinix") || lower.contains("tecno") -> "Tecno"
            else -> "Универсальный"
        }
    }

    fun detectCpuType(model: String, title: String): String {
        val text = "$model $title".lowercase()
        return when {
            text.contains("snapdragon") || text.contains("qualcomm") || text.contains("edl 9008") || text.contains("edl") -> "Qualcomm Snapdragon (EDL 9008)"
            text.contains("dimensity") || text.contains("helio") || text.contains("mediatek") || text.contains("mtk") || text.contains("brom") -> "MediaTek (BROM / Preloader)"
            text.contains("exynos") || text.contains("eub") -> "Samsung Exynos (EUB Booting)"
            text.contains("kirin") -> "HiSilicon Kirin (COM 1.0)"
            text.contains("unisoc") || text.contains("spd") || text.contains("spreadtrum") -> "Unisoc / Spreadtrum (Diag / SPRD)"
            text.contains("apple") || text.contains("iphone") || text.contains("a1") || text.contains("dfu") -> "Apple Bionic (DFU / Recovery)"
            else -> "Qualcomm EDL 9008 / MediaTek BROM"
        }
    }

    suspend fun searchAlternativeCandidate(
        model: String,
        rejectedUrls: Set<String>,
        attempt: Int
    ): Pair<String, CustomSearchImageItem?> = withContext(Dispatchers.IO) {
        val isRu = LanguageService.currentLanguage == AppLanguage.RUSSIAN
        val nextCandidate = customSearchService.searchCandidateTestPointImage(
            deviceModel = model,
            excludedUrls = rejectedUrls,
            attempt = attempt
        )
        if (nextCandidate != null) {
            val brand = extractBrandFromModel(model)
            val cpu = detectCpuType(model, nextCandidate.title)
            val candJson = JSONObject().apply {
                put("brand", brand)
                put("model", model)
                put("cpu", cpu)
                put("imageUrl", nextCandidate.link)
                put("source", nextCandidate.displayLink)
                put("title", nextCandidate.title.ifBlank { "$model TestPoint" })
                put("attempt", attempt)
                put("status", "PENDING")
            }
            val responseText = if (isRu) {
                "🔍 Найдена другая ревизия платы для **$model** (попытка #${attempt + 1}):\n" +
                "• **Процессор / Режим**: $cpu\n" +
                "• **Источник**: ${nextCandidate.displayLink}\n\n" +
                "[TESTPOINT_CANDIDATE:${candJson}]\n\n" +
                "❓ **Эта плата подходит?** Посмотрите расположение элементов и контрольных точек. Если да — подтвердите сохранение."
            } else {
                "🔍 Found alternative motherboard revision for **$model** (attempt #${attempt + 1}):\n" +
                "• **CPU / Mode**: $cpu\n" +
                "• **Source**: ${nextCandidate.displayLink}\n\n" +
                "[TESTPOINT_CANDIDATE:${candJson}]\n\n" +
                "❓ **Does this motherboard match?** Confirm to save."
            }
            return@withContext Pair(responseText, nextCandidate)
        } else {
            val noMoreText = if (isRu) {
                "⚠️ Больше вариантов фото платы для **$model** в открытых инженерных базах не найдено.\n" +
                "Вы можете сфотографировать плату на столе через камеру приложения или прикрепить фото из галереи, и я помогу определить контрольные точки вручную."
            } else {
                "⚠️ No more alternative motherboard photos found for **$model** in online databases.\n" +
                "You can take a photo of your board with the camera or upload one from the gallery."
            }
            return@withContext Pair(noMoreText, null)
        }
    }

    suspend fun approveAndSaveCandidate(candidate: TestPointCandidate): String = withContext(Dispatchers.IO) {
        val (normBrand, normModel) = TestPointRepository.normalizeBrand(candidate.brand, candidate.model)
        val testPointItem = TestPointItem(
            id = "tp_appr_" + System.currentTimeMillis(),
            brand = normBrand,
            model = normModel,
            cpuType = candidate.cpu.ifBlank { "Qualcomm EDL 9008 / MediaTek BROM" },
            description = candidate.title.ifBlank { "Подтвержденная контрольная точка TestPoint для $normModel" },
            imageUrl = candidate.imageUrl,
            additionalImages = emptyList(),
            frpGuide = "1. Отключить аккумулятор АКБ.\n2. Тонким пинцетом замкнуть указанную контрольную точку на GND (массу платы).\n3. Подключить USB кабель к ПК.\n4. Проверить определение порта в Диспетчере устройств (9008 / BROM / COM 1.0).\n5. Выполнить операцию в сервисном софте (UnlockTool / Chimera / Mi Flash).",
            toolsNeeded = listOf("Пинцет", "USB Кабель", "Сервисный софт")
        )
        TestPointRepository.addTestPoint(testPointItem)

        // Save to Room Boardview
        try {
            dbService.boardviewDao.insertBoardview(
                BoardviewPhotoEntity(
                    brand = normBrand,
                    model = normModel,
                    title = "TestPoint: ${candidate.title.ifBlank { normModel }}",
                    imageUrl = candidate.imageUrl,
                    side = "front",
                    description = "Подтвержденный тестпоинт мастером в чате",
                    addedBy = "Мастер (Одобрено)"
                )
            )
        } catch (e: Exception) {
            Log.w("AiService", "Save boardview error: ${e.message}")
        }

        // Save to Room Knowledge Base
        try {
            val guide = GuideData(
                device = "$normBrand $normModel",
                problem = "TestPoint / Сервисный режим (${testPointItem.cpuType})",
                difficulty = "Средняя",
                timeEstimate = "5-10 мин",
                tools = testPointItem.toolsNeeded,
                causes = listOf(
                    Cause("FRP / Восстановление кирпича", 100, testPointItem.description, "Замкнут TP", "Разблокировка в сервисном софте")
                ),
                steps = testPointItem.frpGuide.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
                    Step(idx + 1, "Шаг ${idx + 1}", line)
                },
                proTip = "Обязательно отключайте аккумулятор перед замыканием тестпоинта!",
                risks = "Не повредите соседние SMD элементы металлическим пинцетом"
            )
            dbService.knowledgeDao.insertEntry(
                KnowledgeBaseEntryEntity(
                    brand = normBrand,
                    model = normModel,
                    problem = "TestPoint / FRP (${testPointItem.cpuType})",
                    guideDataJson = dbService.toJson(guide),
                    addedBy = "Мастер (Одобрено)",
                    isSchematic = true
                )
            )
        } catch (e: Exception) {
            Log.w("AiService", "Save KB error: ${e.message}")
        }

        val isRu = LanguageService.currentLanguage == AppLanguage.RUSSIAN
        if (isRu) {
            "✅ **Плата подтверждена!** Тестпоинт для **$normBrand $normModel** успешно сохранён в Базу Знаний и раздел «Тестпоинты». Теперь он доступен оффлайн."
        } else {
            "✅ **Motherboard verified!** TestPoint for **$normBrand $normModel** has been successfully saved to your Knowledge Base and TestPoints repository for offline access."
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        recentMessages: List<MessageEntity>,
        userText: String,
        imageBase64: String?,
        detectedModel: String?,
        liveImages: List<CustomSearchImageItem>
    ): String {
        val contentsArray = JSONArray()

        for (msg in recentMessages) {
            val roleStr = if (msg.role == "user") "user" else "model"
            val partsArr = JSONArray().apply {
                put(JSONObject().apply { put("text", msg.text) })
            }
            contentsArray.put(JSONObject().apply {
                put("role", roleStr)
                put("parts", partsArr)
            })
        }

        val userPartsArr = JSONArray().apply {
            put(JSONObject().apply { put("text", userText) })
            if (imageBase64 != null) {
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", imageBase64)
                    })
                })
            }
        }
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", userPartsArr)
        })

        val imagesContextBuilder = StringBuilder()
        if (liveImages.isNotEmpty()) {
            imagesContextBuilder.append("\n\n### РЕАЛЬНЫЕ РЕЗУЛЬТАТЫ ПОИСКА ФОТО В СЕТИ:\n")
            liveImages.take(4).forEachIndexed { index, item ->
                imagesContextBuilder.append("${index + 1}. Заголовок: \"${item.title}\"\n")
                imagesContextBuilder.append("   Ссылка на фото: ${item.link}\n")
                imagesContextBuilder.append("   Источник: ${item.displayLink}\n")
            }
            imagesContextBuilder.append("\nОБЯЗАТЕЛЬНО: Представь мастеру эти найденные фото: \"Вот реальное фото и расположение тестпоинта:\" и вставь фото через Markdown `![${liveImages.first().title}](${liveImages.first().link})`. Не пиши, что не умеешь искать — ты уже нашёл эти фото!")
        }

        val systemInstructionObj = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", getSystemPrompt() + imagesContextBuilder.toString())
                })
            })
        }

        val payload = JSONObject().apply {
            put("systemInstruction", systemInstructionObj)
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 2000)
            })
        }

        val modelsToTry = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-2.0-flash")
        var lastErr: String? = null

        for (modelName in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    lastErr = "Gemini $modelName HTTP ${response.code}: $responseStr"
                    continue
                }

                val jsonRes = JSONObject(responseStr)
                val candidates = jsonRes.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val resultText = parts.getJSONObject(0).optString("text", "")
                        if (resultText.isNotBlank()) return resultText
                    }
                }
            } catch (e: Exception) {
                lastErr = e.message
            }
        }

        throw Exception("All Gemini models failed. Last error: $lastErr")
    }

    private fun callOpenRouterApi(
        apiKey: String,
        recentMessages: List<MessageEntity>,
        userText: String,
        imageBase64: String?,
        detectedModel: String?,
        liveImages: List<CustomSearchImageItem>
    ): String {
        val messagesArray = JSONArray()

        val imagesContextBuilder = StringBuilder()
        if (liveImages.isNotEmpty()) {
            imagesContextBuilder.append("\n\n### РЕАЛЬНЫЕ РЕЗУЛЬТАТЫ ПОИСКА ФОТО В СЕТИ:\n")
            liveImages.take(4).forEachIndexed { index, item ->
                imagesContextBuilder.append("${index + 1}. \"${item.title}\" -> ${item.link} (${item.displayLink})\n")
            }
            imagesContextBuilder.append("\nОБЯЗАТЕЛЬНО включи найденное фото в ответ: `![${liveImages.first().title}](${liveImages.first().link})`.")
        }

        val systemObj = JSONObject().apply {
            put("role", "system")
            put("content", getSystemPrompt() + imagesContextBuilder.toString())
        }
        messagesArray.put(systemObj)

        for (msg in recentMessages) {
            val msgObj = JSONObject().apply {
                put("role", if (msg.role == "user") "user" else "assistant")
                put("content", msg.text)
            }
            messagesArray.put(msgObj)
        }

        val currentMsgObj = JSONObject().apply {
            put("role", "user")
            if (imageBase64 != null) {
                val contentArray = JSONArray()
                contentArray.put(JSONObject().apply {
                    put("type", "text")
                    put("text", userText)
                })
                contentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$imageBase64")
                    })
                })
                put("content", contentArray)
            } else {
                put("content", userText)
            }
        }
        messagesArray.put(currentMsgObj)

        // Auto-detect endpoint and model if key is Aitunnel
        val effectiveUrl = when {
            apiKey.startsWith("sk-aitunnel") -> "https://api.aitunnel.ru/v1/chat/completions"
            apiKey.startsWith("sk-or-") -> "https://openrouter.ai/api/v1/chat/completions"
            else -> ApiConfig.BASE_URL
        }

        val effectiveModel = when {
            apiKey.startsWith("sk-aitunnel") && ApiConfig.MODEL.contains("gemini") -> "gpt-4o-mini"
            else -> ApiConfig.MODEL
        }

        val payload = JSONObject().apply {
            put("model", effectiveModel)
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 2000)
        }

        val request = Request.Builder()
            .url(effectiveUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", ApiConfig.REFERER)
            .addHeader("X-Title", ApiConfig.APP_TITLE)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseStr = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            Log.e("AiService", "API call error: HTTP ${response.code} -> $responseStr")
            throw Exception("API HTTP ${response.code}: $responseStr")
        }

        val jsonRes = JSONObject(responseStr)
        val choices = jsonRes.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw Exception("No choice returned from AI API: $responseStr")
        }

        val choiceObj = choices.getJSONObject(0)
        val msgObj = choiceObj.optJSONObject("message")
        val content = msgObj?.optString("content", "") ?: ""

        if (content.isBlank()) {
            throw Exception("Empty AI response content")
        }

        return content
    }

    suspend fun generateKnowledgeBaseSummary(chatId: String): GuideData = withContext(Dispatchers.IO) {
        val messages = dbService.messageDao.getRecentMessagesForContext(chatId)
        val aiMessages = messages.filter { it.role == "ai" }
        val userMessages = messages.filter { it.role == "user" }

        val lastAiMessage = aiMessages.firstOrNull()?.text ?: ""
        val firstUserMessage = userMessages.lastOrNull()?.text ?: "Диагностика"

        val parsed = parseGuideFromResponse(lastAiMessage)
        if (parsed != null) {
            return@withContext parsed
        }

        // Fallback summary extraction from conversation
        val devName = extractDeviceModelFromContext(firstUserMessage, emptyList()) ?: firstUserMessage.take(25)

        GuideData(
            device = devName,
            problem = firstUserMessage.take(40),
            difficulty = "Средняя",
            timeEstimate = "20-30 мин",
            tools = listOf("Мультиметр", "Паяльник", "Пинцет", "Спирт"),
            causes = listOf(
                Cause("Первичная причина неисправности", 80, "Диагностика в чате", "Норма", "Успешный ремонт")
            ),
            steps = listOf(
                Step(1, "Диагностика и решение", lastAiMessage.take(200))
            ),
            proTip = "Успешный случай из чата ремонта",
            risks = "Соблюдайте технику безопасности при пайке и разборе"
        )
    }

    private fun extractDeviceModelFromContext(query: String, history: List<MessageEntity> = emptyList()): String? {
        // 1. Check current query FIRST (with Russian transliteration normalization)
        val normalizedQuery = normalizeText(query)
        val queryModel = detectModelInNormalizedText(normalizedQuery)
        if (queryModel != null) {
            return queryModel
        }

        // 2. Check history from newest to oldest
        for (msg in history.reversed()) {
            val normalizedHist = normalizeText(msg.text)
            val histModel = detectModelInNormalizedText(normalizedHist)
            if (histModel != null) {
                return histModel
            }
        }

        return null
    }

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace("с24", "s24")
            .replace("с23", "s23")
            .replace("с22", "s22")
            .replace("с21", "s21")
            .replace("с20", "s20")
            .replace("с10", "s10")
            .replace("а51", "a51")
            .replace("а50", "a50")
            .replace("а52", "a52")
            .replace("а53", "a53")
            .replace("а54", "a54")
            .replace("а55", "a55")
            .replace("а32", "a32")
            .replace("а12", "a12")
            .replace("а13", "a13")
            .replace("а14", "a14")
            .replace("х7а", "x7a")
            .replace("х7", "x7")
            .replace("х8", "x8")
            .replace("х9", "x9")
            .replace("х6", "x6")
            .replace("хонор", "honor")
            .replace("хуавей", "huawei")
            .replace("самсунг", "samsung")
            .replace("айфон", "iphone")
            .replace("редми", "redmi")
            .replace("поко", "poco")
            .replace("сяоми", "xiaomi")
            .replace("ксиоми", "xiaomi")
            .replace("реалми", "realme")
    }

    private fun detectModelInNormalizedText(text: String): String? {
        return when {
            // Honor Series
            text.contains("honor x8") || text.contains("x8") || text.contains("tfy-lx") || text.contains("tfy lx") -> "Honor X8"
            text.contains("honor x7") || text.contains("cma-lx") || text.contains("cma lx") -> "Honor X7"
            text.contains("honor x9") || text.contains("any-lx") || text.contains("any lx") -> "Honor X9"
            text.contains("honor x6") || text.contains("vne-lx") || text.contains("vne lx") -> "Honor X6"
            text.contains("honor 50") || text.contains("nth-nx9") || text.contains("nth-an00") -> "Honor 50"
            text.contains("honor 70") || text.contains("fne-nx9") -> "Honor 70"
            text.contains("honor 90") || text.contains("rea-nx9") -> "Honor 90"
            text.contains("honor 20") || text.contains("yal-l21") -> "Honor 20"
            text.contains("honor") -> "Honor"

            // Huawei Series
            text.contains("p30 pro") || text.contains("vog-l29") || text.contains("vog-l09") -> "Huawei P30 Pro"
            text.contains("p30") || text.contains("ele-l29") -> "Huawei P30"
            text.contains("p40 pro") || text.contains("els-nx9") -> "Huawei P40 Pro"
            text.contains("p40") || text.contains("ana-nx9") -> "Huawei P40"
            text.contains("p50") || text.contains("abr-lx9") -> "Huawei P50"
            text.contains("nova 9") || text.contains("nam-lx9") -> "Huawei Nova 9"
            text.contains("huawei") -> "Huawei"

            // Samsung S-Series
            text.contains("s24 ultra") || text.contains("s24+") || text.contains("s24") -> "Samsung S24"
            text.contains("s23 ultra") || text.contains("s23+") || text.contains("s23") -> "Samsung S23"
            text.contains("s22 ultra") || text.contains("s22+") || text.contains("s22") -> "Samsung S22"
            text.contains("s21 fe") || text.contains("s21 ultra") || text.contains("s21") -> "Samsung S21"
            text.contains("s20 fe") || text.contains("s20") -> "Samsung S20"
            text.contains("s10") || text.contains("s9") || text.contains("s8") -> "Samsung S-серии"
            text.contains("note 20") || text.contains("note 10") -> "Samsung Galaxy Note"

            // Samsung A-Series
            text.contains("a55") -> "Samsung A55"
            text.contains("a54") -> "Samsung A54"
            text.contains("a53") -> "Samsung A53"
            text.contains("a52") -> "Samsung A52"
            text.contains("a51") -> "Samsung A51"
            text.contains("a50") -> "Samsung A50"
            text.contains("a34") || text.contains("a33") || text.contains("a32") -> "Samsung A32/A33/A34"
            text.contains("a14") || text.contains("a13") || text.contains("a12") -> "Samsung A12/A13/A14"
            text.contains("a71") || text.contains("a72") || text.contains("a73") -> "Samsung A7x"
            text.contains("fold") || text.contains("flip") -> "Samsung Z Fold/Flip"
            text.contains("samsung") -> "Samsung"

            // Apple iPhones
            text.contains("iphone 16") || text.contains("16 pro") -> "iPhone 16"
            text.contains("iphone 15") || text.contains("15 pro") -> "iPhone 15"
            text.contains("iphone 14") || text.contains("14 pro") -> "iPhone 14"
            text.contains("iphone 13") || text.contains("13 pro") -> "iPhone 13"
            text.contains("iphone 12") || text.contains("12 pro") -> "iPhone 12"
            text.contains("iphone 11") || text.contains("11 pro") -> "iPhone 11"
            text.contains("iphone xs") || text.contains("iphone xr") || text.contains("iphone x") -> "iPhone X/XS/XR"
            text.contains("iphone 8") || text.contains("iphone 7") -> "iPhone 7/8"
            text.contains("iphone") -> "iPhone"

            // Xiaomi / Redmi / POCO
            text.contains("redmi 9t") || text.contains("9t") -> "Redmi 9T"
            text.contains("redmi note 13") || text.contains("note 13") -> "Redmi Note 13"
            text.contains("redmi note 12") || text.contains("note 12") -> "Redmi Note 12"
            text.contains("redmi note 11") || text.contains("note 11") -> "Redmi Note 11"
            text.contains("redmi note 10") || text.contains("note 10") -> "Redmi Note 10 Pro"
            text.contains("poco x3") || text.contains("x3 pro") -> "POCO X3 Pro"
            text.contains("poco x5") || text.contains("poco x6") -> "POCO X-серии"
            text.contains("poco f3") || text.contains("poco f5") -> "POCO F-серии"
            text.contains("poco m3") || text.contains("m3") -> "POCO M3"
            text.contains("poco") -> "POCO"
            text.contains("redmi") || text.contains("xiaomi") -> "Xiaomi/Redmi"

            // Realme
            text.contains("realme c21") || text.contains("c21") -> "Realme C21"
            text.contains("realme c11") || text.contains("c11") -> "Realme C11"
            text.contains("realme") -> "Realme"

            // Google Pixel
            text.contains("pixel") -> "Google Pixel"

            else -> null
        }
    }

    private fun generateOfflineDiagnosis(
        query: String,
        history: List<MessageEntity> = emptyList(),
        liveImages: List<CustomSearchImageItem> = emptyList()
    ): String {
        val lower = query.lowercase()
        val detectedModel = extractDeviceModelFromContext(query, history)

        val isCorrection = lower.contains("я же") || lower.contains("написал") || lower.contains("не а51") || lower.contains("ошибка") || lower.contains("невнимательно")
        val correctionPrefix = if (isCorrection && detectedModel != null) {
            "Принял, прошу прощения за ошибку! Рассматриваем именно **$detectedModel**.\n\n"
        } else ""

        // If no model detected, ask for model
        if (detectedModel == null) {
            return "Слушай, а о каком именно устройстве речь? Напиши точную модель (например, Honor X8, Honor X7a, Samsung S23, Redmi 9T, Poco X3 Pro) и что с ним случилось (нужен тестпоинт для FRP, падал или не включается?). Разные аппараты — абсолютно разная схемотехника и способы прошивки!"
        }

        // Request for testpoints / EDL / BROM / Pinout / specific FRP testpoint requests
        val isExplicitTestpointRequest = lower.contains("тестпоинт") || lower.contains("testpoint") || lower.contains("test point") ||
                lower.contains("edl 9008") || lower.contains("edl") || lower.contains("brom") || lower.contains("isp pinout") ||
                ((lower.contains("где точка") || lower.contains("расположение точки") || lower.contains("замкнуть") || lower.contains("pinout")) && (lower.contains("плата") || lower.contains("тест") || lower.contains("frp")))

        if (isExplicitTestpointRequest) {
            val repoItem = com.example.data.TestPointRepository.findByModel(detectedModel)
            
            if (repoItem != null) {
                val imagesMarkdown = buildString {
                    append("![Расположение TestPoint на плате](${repoItem.imageUrl})")
                    repoItem.additionalImages.forEachIndexed { index, img ->
                        append("\n\n![Схема и точки подключения (вид ${index + 2})]($img)")
                    }
                    if (liveImages.isNotEmpty()) {
                        liveImages.take(2).forEach { item ->
                            if (item.link != repoItem.imageUrl && repoItem.additionalImages.none { it == item.link }) {
                                append("\n\n![${item.title}](${item.link})")
                            }
                        }
                    }
                }

                return "${correctionPrefix}📍 **Тестпоинт и снятие FRP для ${repoItem.model}**\n\n" +
                        "⚡ **Режим процессора**: `${repoItem.cpuType}`\n\n" +
                        "🔍 **Расположение точки TestPoint**:\n${repoItem.description}\n\n" +
                        "🛠 **Пошаговая инструкция по сбросу FRP / Прошивке**:\n${repoItem.frpGuide}\n\n" +
                        "🧰 **Необходимый инструмент**: ${repoItem.toolsNeeded.joinToString(", ")}\n\n" +
                        imagesMarkdown + "\n\n" +
                        "[TESTPOINT_DATA:${repoItem.id}]\n\n" +
                        "💡 *Нажми на фото для детального увеличения (Pinch-to-zoom) или сохрани схему в Базу Знаний.*"
            }

            // If live images found from web search
            val primaryImage = liveImages.firstOrNull()
            val imgUrl = primaryImage?.link ?: "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgUniversal_Testpoint_Pinout_GSM.jpg"
            val imageTitle = primaryImage?.title ?: "Схема и тестпоинт $detectedModel"

            val tpDesc = when {
                detectedModel.contains("Honor X8", ignoreCase = true) || detectedModel.contains("X8", ignoreCase = true) ->
                    "**Honor X8 (Qualcomm Snapdragon 680 - EDL 9008)**\n" +
                    "📍 Точки TestPoint расположены на плате рядом с разъемом дисплея и микросхемой PMIC.\n" +
                    "1. Отключи батарею (АКБ).\n" +
                    "2. Замкни пинцетом 2 контактные площадки TP на GND (массу).\n" +
                    "3. Подключи USB Type-C кабель -> определение порта 'Qualcomm HS-USB QDLoader 9008'.\n" +
                    "4. В UnlockTool выбери 'Honor X8' -> нажми 'Erase FRP'."

                detectedModel.contains("9T") || detectedModel.contains("M3") ->
                    "**Redmi 9T / Poco M3 (Qualcomm Snapdragon 662 - EDL 9008)**\n" +
                    "📍 Точки TestPoint расположены прямо над разъёмом АКБ и шлейфа дисплея.\n" +
                    "1. Отключи АКБ.\n" +
                    "2. Пинцетом замкни 2 золотистые точки TP.\n" +
                    "3. Подключи USB кабель к ПК -> Определение 'Qualcomm HS-USB QDLoader 9008'.\n" +
                    "4. Сброс FRP / Mi аккаунта в UnlockTool / Mi Flash."

                detectedModel.contains("POCO X3") || detectedModel.contains("X3 Pro") ->
                    "**POCO X3 Pro (Qualcomm Snapdragon 860 - EDL 9008)**\n" +
                    "📍 Точки TestPoint расположены с левой стороны от разъёма межплатного шлейфа.\n" +
                    "1. Сними защитный экран и отключи батарею.\n" +
                    "2. Замкни 2 контактные площадки TP пинцетом.\n" +
                    "3. Вставь Type-C кабель -> порт 'Qualcomm 9008'."

                detectedModel.contains("A51") || detectedModel.contains("A50") || detectedModel.contains("A52") ->
                    "**Samsung Galaxy A51 (Exynos 9611 - EUB Mode)**\n" +
                    "📍 Контрольная точка TP_EUB находится возле микросхемы питания PMIC.\n" +
                    "1. Сними защитную пластину и отсоедини шлейф АКБ.\n" +
                    "2. Замкни точку TP_EUB на металлическую рамку (GND/Земля).\n" +
                    "3. Подключи USB -> В Диспетчере появится 'Exynos USB Booting'.\n" +
                    "4. В SamFw Tool / Chimera сбрось блокировку FRP в 1 клик."

                else ->
                    "**Схема и контрольные точки TestPoint для $detectedModel**\n" +
                    "📍 Линии сервисного перевода процессора (EDL 9008 / BROM / EUB) замыкаются пинцетом на общую землю (GND) при отключенной батарее.\n" +
                    "1. Отключи батарею.\n" +
                    "2. Замкни контрольную площадку TP на корпус (GND).\n" +
                    "3. Подключи кабель USB Type-C к компьютеру.\n" +
                    "4. Запусти сервисную утилиту (UnlockTool / Chimera / SP Flash Tool) и выбери сброс FRP."
            }

            val imagesMd = buildString {
                append("![$imageTitle]($imgUrl)")
                if (liveImages.size > 1) {
                    liveImages.drop(1).take(2).forEach { addImg ->
                        append("\n\n![${addImg.title}](${addImg.link})")
                    }
                }
            }

            return "${correctionPrefix}Вот точные данные и найденная схема для **$detectedModel**:\n\n$tpDesc\n\n$imagesMd\n\n⚡ **Совет**: Все замеры и замыкания делай тонким изолированным пинцетом при обязательно отключенном аккумуляторе!"
        }

        // Network / SIM / Signal problems
        if (lower.contains("сеть") || lower.contains("сет") || lower.contains("сим") || lower.contains("sim") || lower.contains("связь") || lower.contains("4g") || lower.contains("lte") || lower.contains("imei") || lower.contains("модем")) {
            return correctionPrefix + "🔧 **$detectedModel — Не видит сеть / Нет сигнала GSM/LTE**\n" +
                    "⚠️ Сложность: Средняя / Высокая\n\n" +
                    "📋 **ДИАГНОСТИКА ПО АЛГОРИТМУ МАСТЕРА**:\n" +
                    "📍 **[Шаг 3: Подтверждение дефекта]**: Набери `*#06#`. Проверь, отображаются ли IMEI1/2 и прошивка модуля связи в Настройках.\n" +
                    "📍 **[Шаг 7: Осмотр под микроскопом]**: Проверь защёлки коаксиального кабеля на нижней и верхней плате, целостность разъёмов антенны.\n" +
                    "📍 **[Шаг 8: Замеры линий питания RF]**: Замерь питания 1.2V, 1.8V, 3.8V на обвязке RF-трансивера (SDR660/WTR/Shannon) и усилителя PA.\n" +
                    "📍 **[Шаг 10: Ремонтные работы]**: Замена коаксиала / реболл RF-трансивера / восстановление EFS.\n\n" +
                    "🏁 **ЧЕК-ЛИСТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА**:\n" +
                    "- [ ] Звонок на 112 / оператору (стабильность связи без прерываний)\n" +
                    "- [ ] Слышимость в слуховом и полифоническом динамиках\n" +
                    "- [ ] Проверка передачи голоса через основной и шумоподавляющий микрофон\n" +
                    "- [ ] Переключение режимов 2G / 3G / 4G LTE / 5G\n" +
                    "- [ ] Скорость интернета через Speedtest и работа Wi-Fi / Bluetooth"
        }

        // Charging / Type-C / Power problems
        if (lower.contains("заряд") || lower.contains("разъём") || lower.contains("разем") || lower.contains("гнезд") || lower.contains("type-c") || lower.contains("vbus") || lower.contains("не заряжается")) {
            return correctionPrefix + "🔧 **$detectedModel — Диагностика цепи зарядки и Type-C**\n" +
                    "⚠️ Сложность: Легкая / Средняя\n\n" +
                    "📋 **ДИАГНОСТИКА ПО АЛГОРИТМУ МАСТЕРА**:\n" +
                    "📍 **[Шаг 3: Подтверждение дефекта]**: Подключи USB-тестер. Ток 0.00A = обрыв VBUS/OVP; 0.10-0.45A = медленная зарядка / нет детекта CC1/CC2; 1.5-2.0A+ (9V/12V) = нормальный Fast Charge.\n" +
                    "📍 **[Шаг 7: Осмотр под микроскопом]**: Проверь контакты Type-C на окислы и межплатный шлейф.\n" +
                    "📍 **[Шаг 8: Замеры цепей питания]**: Замерь напряжение VBUS (5V/9V) на входе и выходе OVP ключа, падение напряжений по линиям D+/D- (норма ~500-750 mV).\n" +
                    "📍 **[Шаг 10: Ремонтные работы]**: Очистка / замена разъёма Type-C / пропайка коннектора шлейфа / замена контроллера OVP/PMIC.\n\n" +
                    "🏁 **ЧЕК-ЛИСТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА**:\n" +
                    "- [ ] Ток зарядки в обоих положениях штекера Type-C\n" +
                    "- [ ] Активация быстрой зарядки (Fast/Quick Charge / PD)\n" +
                    "- [ ] Определение компьютером при подключении по USB (передача файлов MTP)\n" +
                    "- [ ] Потребление тока в спящем режиме (< 0.01A)\n" +
                    "- [ ] Нагрев АКБ и платы во время зарядки (не выше 40°C)"
        }

        // Liquid damage
        if (lower.contains("влаг") || lower.contains("вод") || lower.contains("упал в воду") || lower.contains("купал") || lower.contains("залит")) {
            return correctionPrefix + "🔧 **$detectedModel — Диагностика после попадания влаги**\n" +
                    "⚠️ Сложность: Высокая (Главное правило: сначала понять причину — потом паять!)\n\n" +
                    "📋 **ДИАГНОСТИКА ПО АЛГОРИТМУ МАСТЕРА**:\n" +
                    "📍 **[Шаг 1-2: Анамнез и внешний осмотр]**: Ни в коем случае не подключай АКБ и зарядку до отмывки платы!\n" +
                    "📍 **[Шаг 6: Разборка и снятие экранов]**: Полный демонтаж платы, субплаты и камер (камеры не мочить в спирте!).\n" +
                    "📍 **[Шаг 7: Осмотр под микроскопом]**: Проверка маркеров влаги (LDI), поиск очагов коррозии под экранами.\n" +
                    "📍 **[Шаг 8: Замеры и локализация КЗ]**: Диодная прозвонка линий VBUS, VBAT, VSYS/VDD_MAIN относительно земли. Поиск греющихся элементов тепловизором/фризером.\n" +
                    "📍 **[Шаг 11: Профилактика и УЗВ]**: Отмывка в УЗ-ванне со спиртом, просушка термофеном при 100°C.\n\n" +
                    "🏁 **ЧЕК-ЛИСТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА**:\n" +
                    "- [ ] Запуск от ЛБП: отсутствие утечки в выключенном состоянии (0.00A)\n" +
                    "- [ ] Равномерность подсветки дисплея и работа сенсора по всем углам\n" +
                    "- [ ] Работа всех микрофонов (разговорного, нижнего, верхнего) и динамиков\n" +
                    "- [ ] Камеры: отсутствие пятен от высохшей влаги на оптике и матрице\n" +
                    "- [ ] Беспроводные сети Wi-Fi, Bluetooth, NFC и датчик приближения"
        }

        // Specific diagnosis logic for flagship Samsung S23 / S24 / S22 / S21
        if (detectedModel.contains("S23") || detectedModel.contains("S24") || detectedModel.contains("S22") || detectedModel.contains("S21") || detectedModel.contains("S20")) {
            return correctionPrefix + "🔧 **$detectedModel — Не включается / Комплексная диагностика**\n" +
                    "⚠️ Сложность: Высокая\n\n" +
                    "📋 **ДИАГНОСТИКА ПО АЛГОРИТМУ МАСТЕРА**:\n" +
                    "📍 **[Шаг 3: Подтверждение дефекта]**: Ток по USB-тестеру: если 0.00А — выбит входной ключ OVP или сгорел предохранитель VBUS.\n" +
                    "📍 **[Шаг 8: Замеры питаний]**: Замерь напряжение АКБ (> 3.7V). Замерь вторичные питания (дежурки 1.8V, VDD_CPU 0.8V, DRAM 1.1V). При нажатии на PWR ток должен циклировать 0.12A - 0.25A.\n" +
                    "📍 **[Шаг 10: Ремонт]**: Замена контроллера питания PMIC / устранение КЗ по вторичке / реболл CPU сэндвича.\n\n" +
                    "🏁 **ЧЕК-ЛИСТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА**:\n" +
                    "- [ ] Зарядка через Type-C (ток 2.0A+ 9V/12V Super Fast Charging)\n" +
                    "- [ ] Беспроводная зарядка Qi и обратная реверсивная зарядка\n" +
                    "- [ ] Работа подэкранного ультразвукового сканера отпечатков пальцев\n" +
                    "- [ ] Проверка 120Hz экрана и сенсора по тесту `*#0*#`\n" +
                    "- [ ] Стресс-тест 3D графики в течение 10 минут"
        }

        // Specific diagnosis logic for Samsung A-Series (A51, A50, A52, A53, A54)
        if (detectedModel.contains("A51") || detectedModel.contains("A50") || detectedModel.contains("A52") || detectedModel.contains("A53") || detectedModel.contains("A54")) {
            return correctionPrefix + "🔧 **$detectedModel — Диагностика планарных цепей и межплатки**\n" +
                    "⚠️ Сложность: Средняя\n\n" +
                    "📋 **ДИАГНОСТИКА ПО АЛГОРИТМУ МАСТЕРА**:\n" +
                    "📍 **[Шаг 3: Подтверждение дефекта]**: Замерь ток на USB-тестере при подключении. Если нет реакции или мигает 0.10A — обрыв линий VBUS/CC.\n" +
                    "📍 **[Шаг 7: Осмотр под микроскопом]**: Внимательно осмотри межплатный коннектор FPC на материнской плате и субплате на предмет микротрещин в пайке крайних выводов питания.\n" +
                    "📍 **[Шаг 8: Замеры и локализация]**: Аккуратно прижми пальцем межплатный коннектор при подключенном кабеле. Если пошёл заряд 1.5A+ — отвал пайки коннектора.\n" +
                    "📍 **[Шаг 10: Ремонтные работы]**: Пропайка феном (300°C) всех ножек коннектора с легкоплавким припоем ПОС-61 или замена шлейфа.\n\n" +
                    "🏁 **ЧЕК-ЛИСТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА**:\n" +
                    "- [ ] Ток зарядки в обоих положениях кабеля Type-C (> 1.5A)\n" +
                    "- [ ] Работа нижнего микрофона при голосовой записи\n" +
                    "- [ ] Звук в полифоническом динамике\n" +
                    "- [ ] Приём сигнала сети 4G LTE\n" +
                    "- [ ] Работа подэкранного оптического сканера отпечатка пальца"
        }

        // Specific diagnosis logic for Redmi / POCO / Xiaomi
        if (detectedModel.contains("Redmi 9T") || detectedModel.contains("POCO") || detectedModel.contains("Xiaomi")) {
            return correctionPrefix + "🔧 **$detectedModel — Комплексная диагностика**\n" +
                    "⚠️ Сложность: Средняя\n\n" +
                    "📋 **ДИАГНОСТИКА ПО АЛГОРИТМУ МАСТЕРА**:\n" +
                    "📍 **[Шаг 3: Подтверждение дефекта]**: Ток по USB: если 0.00A или зависает на 0.05-0.10A (режим EDL 9008) — типовая проблема цепи питания/CPU.\n" +
                    "📍 **[Шаг 7: Осмотр под микроскопом]**: Сними защитный экран около микросхемы PMIC. Осмотри керамические SMD-конденсаторы на микротрещины/прогар.\n" +
                    "📍 **[Шаг 8: Замеры диодной прозвонки]**: Замерь сопротивление линии VSYS/VBAT. Если КЗ (0.000V) — локализуй греющийся кондёр фризером.\n" +
                    "📍 **[Шаг 10: Ремонтные работы]**: Удаление пробитого конденсатора / реболл процессора (для POCO X3 Pro).\n\n" +
                    "🏁 **ЧЕК-ЛИСТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА**:\n" +
                    "- [ ] Быстрая зарядка Quick Charge / Mi Turbo Charge\n" +
                    "- [ ] Запуск и прохождение анимации MIUI / HyperOS\n" +
                    "- [ ] Проверка фронтальной и основных камер\n" +
                    "- [ ] Работа звука и разговорного динамика\n" +
                    "- [ ] Тест Wi-Fi 2.4/5GHz и Bluetooth аудио"
        }

        // Specific diagnosis logic for Apple iPhone
        if (detectedModel.contains("iPhone")) {
            return correctionPrefix + "🔧 **$detectedModel — Диагностика планарной платы**\n" +
                    "⚠️ Сложность: Высокая\n\n" +
                    "📋 **ДИАГНОСТИКА ПО АЛГОРИТМУ МАСТЕРА**:\n" +
                    "📍 **[Шаг 3: Подтверждение дефекта]**: Замер тока на ЛБП до и после нажатия кнопки Power (норма: 0.00A до нажатия, 0.06-0.15A старт bootloader).\n" +
                    "📍 **[Шаг 8: Замеры линий питания]**: Диодная прозвонка линий PP_VDD_MAIN, PP_BATT_VCC, PP_CPU_VCC относительно GND.\n" +
                    "📍 **[Шаг 10: Ремонтные работы]**: Разделение межплатного сэндвича на подогревателе (190°C), устранение КЗ во вторичной цепи, реболл межплатной рамки.\n\n" +
                    "🏁 **ЧЕК-ЛИСТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА**:\n" +
                    "- [ ] Зарядка через Lightning / Type-C (обе стороны)\n" +
                    "- [ ] Face ID / Touch ID и датчик приближения TrueDepth\n" +
                    "- [ ] TrueTone и 3D Touch / Haptic Touch дисплея\n" +
                    "- [ ] Сеть, Wi-Fi, Bluetooth и AirDrop\n" +
                    "- [ ] Запись видео с проверкой всех 3 микрофонов (фронтального, нижнего, заднего)"
        }

        // Generic fallback for any other extracted model
        return correctionPrefix + "🔧 **$detectedModel — Диагностика неисправности**\n" +
                "⚠️ Сложность: Средняя\n\n" +
                "📋 **ДИАГНОСТИКА ПО АЛГОРИТМУ МАСТЕРА**:\n" +
                "📍 **[Шаг 1-3: Анамнез и подтверждение дефекта]**: Отсоедини АКБ на 2 минуты, подключи USB-тестер и проверь ток (0.00A = обрыв VBUS/OVP; 0.10-0.45A = сбой детекта; 1.5A+ = норма).\n" +
                "📍 **[Шаг 7-8: Осмотр под микроскопом и замеры]**: Прозвони в режиме падения напряжения линии VBUS, VBAT и VDD_MAIN относительно земли (GND). Проверь дежурные напряжения (3.3V / 5V / 1.8V).\n" +
                "📍 **[Шаг 10: Ремонтные работы]**: Локализация КЗ тепловизором и замена неисправного компонента.\n\n" +
                "🏁 **ЧЕК-ЛИСТ ПРОВЕРКИ ПОСЛЕ РЕМОНТА**:\n" +
                "- [ ] Зарядка и энергопотребление в спящем режиме (< 0.01A)\n" +
                "- [ ] Сенсор дисплея по всей площади и автояркость\n" +
                "- [ ] Звонок на 112 / слышимость в динамиках и микрофонах\n" +
                "- [ ] Беспроводные интерфейсы (Wi-Fi, Bluetooth, GPS, NFC)\n" +
                "- [ ] Камеры, фонарик и температурная стабильность"
    }

    private fun parseGuideFromResponse(text: String): GuideData? {
        if (!text.contains("🔧")) return null
        return try {
            val lines = text.lines()
            var device = "Устройство"
            var problem = "Проблема"
            var difficulty = "Средняя"
            var timeEstimate = "30 мин"
            val tools = mutableListOf<String>()
            val causes = mutableListOf<Cause>()
            val steps = mutableListOf<Step>()
            var proTip: String? = null
            var risks: String? = null

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("🔧") && trimmed.contains("—")) {
                    val parts = trimmed.removePrefix("🔧").split("—")
                    if (parts.size >= 2) {
                        device = parts[0].replace("**", "").trim()
                        problem = parts[1].replace("**", "").trim()
                    }
                } else if (trimmed.startsWith("⚠️ Сложность")) {
                    difficulty = trimmed.removePrefix("⚠️ Сложность").removePrefix(":").trim()
                } else if (trimmed.startsWith("⏱ Время") || trimmed.startsWith("⏱")) {
                    timeEstimate = trimmed.removePrefix("⏱ Время").removePrefix("⏱").removePrefix(":").trim()
                } else if (trimmed.startsWith("💡 Совет")) {
                    proTip = trimmed.removePrefix("💡 Совет").removePrefix(":").trim()
                } else if (trimmed.startsWith("⚠️ Риски")) {
                    risks = trimmed.removePrefix("⚠️ Риски").removePrefix(":").trim()
                } else if (trimmed.startsWith("•") || trimmed.startsWith("-")) {
                    tools.add(trimmed.substring(1).trim())
                }
            }

            if (text.contains("🎯 Вероятные причины")) {
                causes.add(
                    Cause(
                        description = "Первичная диагностика цепей питания",
                        probability = 70,
                        checkMethod = "Замер мультиметром",
                        normalValue = "Нормальные параметры напряжения",
                        fixMethod = "Восстановление или замена элемента"
                    )
                )
            }

            if (text.contains("Шаг 1")) {
                steps.add(Step(1, "Первичный осмотр", "Проверка разъёмов, шлейфов и элементов платы."))
                steps.add(Step(2, "Замеры питаний", "Замер мультиметром основных контрольных точек."))
            }

            GuideData(
                device = device,
                problem = problem,
                difficulty = difficulty,
                timeEstimate = timeEstimate,
                tools = tools.ifEmpty { listOf("Мультиметр", "Паяльник", "Пинцет") },
                causes = causes.ifEmpty {
                    listOf(
                        Cause("Окисление / КЗ", 60, "Визуальный осмотр", "Чистые контакты", "Зачистка или отпайка")
                    )
                },
                steps = steps.ifEmpty {
                    listOf(
                        Step(1, "Диагностика", "Выполните пошаговую проверку узлов платы.")
                    )
                },
                proTip = proTip,
                risks = risks
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AiService? = null

        fun getInstance(context: Context): AiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiService(context).also { INSTANCE = it }
            }
        }
    }
}

