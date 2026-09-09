package com.example.data

import androidx.compose.runtime.mutableStateListOf

data class TestPointItem(
    val id: String,
    val brand: String,
    val model: String,
    val cpuType: String, // "Qualcomm EDL 9008", "MediaTek BROM", "Exynos TestPoint", "Kirin TestPoint"
    val description: String,
    val imageUrl: String,
    val additionalImages: List<String> = emptyList(),
    val frpGuide: String,
    val toolsNeeded: List<String>,
    val searchQuery: String = ""
)

object TestPointRepository {
    fun normalizeBrand(inputBrand: String, inputModel: String): Pair<String, String> {
        val b = inputBrand.trim()
        val m = inputModel.trim()
        val combined = "$b $m".lowercase()

        val resolvedBrand = when {
            combined.contains("iphone") || combined.contains("ipad") || combined.contains("apple") -> "Apple"
            combined.contains("samsung") || combined.contains("galaxy") -> "Samsung"
            combined.contains("redmi") || combined.contains("xiaomi") || combined.contains("mi ") -> "Xiaomi"
            combined.contains("poco") -> "Poco"
            combined.contains("honor") -> "Honor"
            combined.contains("huawei") -> "Huawei"
            combined.contains("realme") -> "Realme"
            combined.contains("oppo") -> "Oppo"
            combined.contains("vivo") -> "Vivo"
            combined.contains("infinix") -> "Infinix"
            combined.contains("tecno") -> "Tecno"
            b.isNotBlank() -> b.replaceFirstChar { it.uppercase() }
            else -> "Xiaomi"
        }

        var cleanModel = m
        if (cleanModel.startsWith(resolvedBrand, ignoreCase = true)) {
            cleanModel = cleanModel.substring(resolvedBrand.length).trim()
        }
        if (cleanModel.isBlank()) cleanModel = m

        return Pair(resolvedBrand, cleanModel)
    }

    private val initialItems = listOf(
        TestPointItem(
            id = "tp_redmi_note_12_4g",
            brand = "Xiaomi",
            model = "Redmi Note 12 4G / 5G (tapas / topaz / sunstone)",
            cpuType = "Qualcomm Snapdragon 685 / 4 Gen 1 (EDL 9008)",
            description = "Две контрольные точки EDL TestPoint расположены на обратной стороне материнской платы возле коннектора шлейфа дисплея и датчика отпечатка пальца.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgRedmi_Note_12_EDL_TestPoint_9008.jpg",
            additionalImages = listOf(
                "https://www.martview-forum.com/attachments/redmi-note-12-edl-testpoint-pinout-jpg.76542/"
            ),
            frpGuide = """
                1. Снимите заднюю крышку и открутите верхнюю защитную рамку материнской платы.
                2. Обязательно отключите шлейф аккумулятора (АКБ).
                3. Тонким металлическим пинцетом замкните 2 золотистые точки TestPoint EDL (рядом с коннектором дисплея).
                4. Не размыкая пинцет, подключите USB Type-C кабель от ПК к телефону.
                5. В Диспетчере устройств Windows появится порт 'Qualcomm HS-USB QDLoader 9008'. Уберите пинцет.
                6. В UnlockTool / Mi Flash Pro / Chimera Tool выберите модель 'Redmi Note 12 4G (tapas)' и нажмите 'Erase FRP / Reset Mi Account'.
                7. Подключите аккумулятор и включите смартфон.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "Кабель Type-C", "UnlockTool / Mi Flash Pro / Chimera"),
            searchQuery = "redmi note 12 test point edl 9008 tapas topaz frp"
        ),
        TestPointItem(
            id = "tp_redmi_note_12_pro",
            brand = "Xiaomi",
            model = "Redmi Note 12 Pro / Pro+ 5G (ruby)",
            cpuType = "MediaTek Dimensity 1080 (BROM Mode)",
            description = "Тестпоинт BROM расположен рядом с микросхемой флеш-памяти UFS и процессором. Замыкается точка TP на массу (GND).",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhRedmi_Note_12_Pro_BROM_Pinout.jpg",
            frpGuide = """
                1. Отключите АКБ.
                2. Замкните контрольную точку TP на металлическую защитную рамку (GND).
                3. Вставьте Type-C кабель в ПК.
                4. Устройство определится как 'MediaTek USB Port'.
                5. В UnlockTool нажмите 'Bypass SLA/DA' и затем 'Erase FRP'.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "Type-C кабель", "UnlockTool / SP Flash Tool"),
            searchQuery = "redmi note 12 pro plus brom test point ruby frp"
        ),
        TestPointItem(
            id = "tp_redmi_note_11",
            brand = "Xiaomi",
            model = "Redmi Note 11 (spes / spesn)",
            cpuType = "Qualcomm Snapdragon 680 (EDL 9008)",
            description = "Две контрольные точки EDL TestPoint расположены возле разъёма камеры и разъема батареи под защитным экраном.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhRedmi_Note_11_EDL_Pinout_GSM.jpg",
            frpGuide = """
                1. Отключите аккумулятор.
                2. Замкните 2 золотистые точки EDL пинцетом.
                3. Подключите USB кабель к ПК (Qualcomm QDLoader 9008).
                4. В UnlockTool выберите 'Redmi Note 11' -> 'Erase FRP'.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "USB Type-C", "UnlockTool / Chimera"),
            searchQuery = "redmi note 11 spes test point edl 9008 frp"
        ),
        TestPointItem(
            id = "tp_honor_x8",
            brand = "Honor",
            model = "Honor X8 (TFY-LX1 / TFY-LX2 / TFY-LX3)",
            cpuType = "Qualcomm Snapdragon 680 4G (EDL 9008)",
            description = "Две контрольные точки EDL TestPoint расположены на обратной стороне материнской платы рядом с коннектором шлейфа экрана.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjNrq8226jE8VqP9_C3e7l5t8o_x_Y7o6l-c2X2k6p3r5m8n7/s1600/Honor_X8_Testpoint_EDL.jpg",
            additionalImages = listOf(
                "https://www.martview-forum.com/attachments/honor-x8-edl-testpoint-pinout-jpg.68541/"
            ),
            frpGuide = """
                1. Отключите шлейф аккумулятора.
                2. Замкните 2 золотистые точки EDL пинцетом.
                3. Вставьте USB-кабель -> в системе определится 'Qualcomm HS-USB QDLoader 9008'.
                4. В UnlockTool выберите 'Honor X8' -> нажмите 'Erase FRP'.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "Кабель Type-C", "UnlockTool"),
            searchQuery = "honor x8 test point edl 9008 frp"
        ),
        TestPointItem(
            id = "tp_honor_x7",
            brand = "Honor",
            model = "Honor X7 (CMA-LX1 / CMA-LX2)",
            cpuType = "Qualcomm Snapdragon 680 4G (EDL 9008)",
            description = "Точки TestPoint расположены возле разъёма АКБ и коннектора дисплея.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhHonor_X7_EDL_9008_Testpoint.jpg",
            frpGuide = """
                1. Отключите АКБ.
                2. Замкните точки EDL на плате.
                3. Подключите USB кабель к ПК (Qualcomm 9008).
                4. Выполните сброс FRP в сервисном софте.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "UnlockTool"),
            searchQuery = "honor x7 cma lx1 test point frp"
        ),
        TestPointItem(
            id = "tp_redmi_9t",
            brand = "Xiaomi",
            model = "Redmi 9T / Poco M3 (lime/citrus)",
            cpuType = "Qualcomm Snapdragon 662 (EDL 9008)",
            description = "Замкнуть 2 золотистые точки возле разъёма АКБ и коннектора шлейфа дисплея перед подключением USB кабеля.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgRedmi_9T_Poco_M3_EDL_9008_Pinout.jpg",
            additionalImages = listOf(
                "https://www.martview-forum.com/attachments/redmi-9t-edl-testpoint-jpg.64211/"
            ),
            frpGuide = """
                1. Снимите заднюю крышку и защитную пластину.
                2. Отключите шлейф аккумулятора.
                3. Пинцетом замкните 2 точки TestPoint EDL (возле коннектора батареи).
                4. Подключите USB-кабель к ПК (в Диспетчере устройств должен появиться 'Qualcomm HS-USB QDLoader 9008').
                5. Откройте UnlockTool / QPST / Mi Flash и нажмите 'Erase FRP' или 'Reset Mi Account'.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "USB Type-C", "QPST / UnlockTool / Mi Flash"),
            searchQuery = "redmi 9t test point edl 9008 frp"
        ),
        TestPointItem(
            id = "tp_samsung_a51",
            brand = "Samsung",
            model = "Samsung Galaxy A51 (SM-A515F)",
            cpuType = "Exynos 9611 (EUB / TestPoint)",
            description = "Замкнуть точку TP_EUB (возле микросхемы питания PMIC) на корпус GND для входа в порт Emergency Download Mode.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjSamsung_A51_EUB_Testpoint_SamFw.jpg",
            additionalImages = listOf(
                "https://www.martview-forum.com/attachments/samsung-a51-eub-pinout-jpg.61899/"
            ),
            frpGuide = """
                1. Разберите телефон и снимите плату.
                2. Замкните контрольную точку TP_EUB на металлическую рамку (GND).
                3. Вставьте Type-C кабель в компьютер.
                4. В Диспетчере устройств появится порт 'Exynos USB Booting' / 'Samsung Serial'.
                5. Используйте Chimera Tool / Odin / SamFw Tool для сброса Knox/FRP в 1 клик.
            """.trimIndent(),
            toolsNeeded = listOf("Изогнутый пинцет", "SamFw Tool / Chimera", "Кабель Type-C"),
            searchQuery = "samsung a51 test point eub frp"
        ),
        TestPointItem(
            id = "tp_samsung_a52",
            brand = "Samsung",
            model = "Samsung Galaxy A52 4G / 5G (SM-A525F / A526B)",
            cpuType = "Qualcomm Snapdragon 720G / 750G (EDL 9008)",
            description = "Две контрольные точки EDL находятся под металлическим экраном рядом с контроллером питания PMIC.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjSamsung_A52_EDL_Pinout.jpg",
            frpGuide = """
                1. Отключите АКБ.
                2. Замкните 2 контактные площадки EDL пинцетом.
                3. Подключите USB кабель к ПК (Qualcomm 9008).
                4. В UnlockTool / Chimera выберите SM-A525F -> Reset FRP.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "Кабель Type-C", "UnlockTool / Chimera"),
            searchQuery = "samsung a52 test point edl 9008 frp"
        ),
        TestPointItem(
            id = "tp_poco_x3_pro",
            brand = "Poco",
            model = "Poco X3 Pro (vayu / bhima)",
            cpuType = "Qualcomm Snapdragon 860 (EDL 9008)",
            description = "Точки TestPoint расположены прямо над разъёмом межплатного шлейфа с левой стороны.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhPoco_X3_Pro_EDL_TestPoint_Motherboard.jpg",
            additionalImages = listOf(
                "https://www.martview-forum.com/attachments/poco-x3-pro-edl-jpg.63412/"
            ),
            frpGuide = """
                1. Отключите аккумулятор.
                2. Замкните 2 контактные площадки EDL пинцетом.
                3. Вставьте USB-кабель. Проверьте порт Qualcomm 9008.
                4. Запустите UnlockTool -> Выберите Poco X3 Pro -> Reset FRP / Unlock Bootloader.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "UnlockTool / Mi Flash Pro"),
            searchQuery = "poco x3 pro test point edl 9008"
        ),
        TestPointItem(
            id = "tp_huawei_p30_pro",
            brand = "Huawei",
            model = "Huawei P30 Pro / Honor 20 Pro",
            cpuType = "Kirin 980 (COM 1.0 TestPoint)",
            description = "Точка TestPoint находится под металлическим экраном около разъёма фронтальной камеры. Замыкается на массу (GND).",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjHuawei_P30_Pro_Kirin_980_Testpoint.jpg",
            additionalImages = listOf(
                "https://www.martview-forum.com/attachments/huawei-p30-pro-com-jpg.58911/"
            ),
            frpGuide = """
                1. Отключите АКБ и экран.
                2. Замкните контрольную точку Kirin TP на землю (корпус сим-лотка или рамки).
                3. Подключите специализированный Testpoint Cable или обычный USB.
                4. ПК определит 'HUAWEI USB COM 1.0'.
                5. В Huawei Tool / Octoplus / SigmaSelect загрузите фабричный bootloader и нажмите 'Remove FRP'.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "Huawei Tool / Sigma / Octoplus", "Kirin Testpoint Cable"),
            searchQuery = "huawei p30 pro kirin 980 test point"
        ),
        TestPointItem(
            id = "tp_redmi_note_10",
            brand = "Xiaomi",
            model = "Redmi Note 10 Pro (sweet)",
            cpuType = "Qualcomm Snapdragon 732G (EDL 9008)",
            description = "Две контрольные точки с правой стороны от коннектора аккумулятора.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgRedmi_Note_10_Pro_EDL_Pinout.jpg",
            frpGuide = """
                1. Отсоедините батарею.
                2. Замкните 2 контрольные точки EDL.
                3. Подключите к ПК -> 'Qualcomm 9008'.
                4. Выберите в UnlockTool прошивку / сброс блокировки аккаунта.
            """.trimIndent(),
            toolsNeeded = listOf("Пинцет", "UnlockTool"),
            searchQuery = "redmi note 10 pro test point edl 9008"
        ),
        TestPointItem(
            id = "tp_realme_c21",
            brand = "Realme",
            model = "Realme C21 / C11 (MTK)",
            cpuType = "MediaTek Helio G35 (BROM Mode)",
            description = "Для BROM режима достаточно зажать кнопки Громкость+ и Громкость- без разбора или замкнуть точку CLK на GND.",
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEiRealme_C21_BROM_Pinout_GSM.jpg",
            frpGuide = """
                1. Выключите телефон.
                2. Откройте SP Flash Tool / MTK Client / MCT Bypass Tool.
                3. Зажмите обе кнопки громкости (Vol+ и Vol-) и вставьте USB кабель.
                4. Программа обойдёт SLA/DA защищенную прошивку и удалит раздел FRP за 5 секунд.
            """.trimIndent(),
            toolsNeeded = listOf("USB Кабель", "MTK Client / SP Flash Tool"),
            searchQuery = "realme c21 brom test point mtk frp"
        ),
        TestPointItem(
            id = "tp_apple_dfu",
            brand = "Apple",
            model = "iPhone 11 / 12 / 13 / 14 / 15 (DFU & Recovery)",
            cpuType = "Apple A-Series (DFU Mode / Hardware Key Combos)",
            description = "Аппаратный ввод в режим DFU (Direct Firmware Upgrade) без разборки устройства через комбинацию физических кнопок.",
            imageUrl = "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800",
            frpGuide = """
                1. Подключите iPhone к компьютеру кабелем Lightning / Type-C.
                2. Быстро нажмите и отпустите 'Громкость +'.
                3. Быстро нажмите и отпустите 'Громкость -'.
                4. Зажмите и удерживайте боковую кнопку включения Power (экран погаснет через 10 сек).
                5. Не отпуская кнопку Power, зажмите кнопку 'Громкость -' на 5 секунд.
                6. Отпустите кнопку Power, но продолжайте удерживать 'Громкость -' еще 10 секунд.
                7. Экран останется черным, а 3uTools / iTunes / Apple Configurator обнаружат устройство в DFU Mode!
            """.trimIndent(),
            toolsNeeded = listOf("Кабель Lightning / Type-C", "3uTools / iTunes / Apple Configurator"),
            searchQuery = "apple iphone dfu mode recovery hardware keys"
        )
    )

    val items = mutableStateListOf<TestPointItem>().apply {
        addAll(initialItems)
    }

    fun addTestPoint(item: TestPointItem) {
        val (normBrand, normModel) = normalizeBrand(item.brand, item.model)
        val normalizedItem = item.copy(brand = normBrand, model = normModel)
        
        // Remove existing item with same model if updating, or prevent duplicate
        val existingIndex = items.indexOfFirst { it.model.equals(normalizedItem.model, ignoreCase = true) }
        if (existingIndex >= 0) {
            items[existingIndex] = normalizedItem
        } else {
            items.add(0, normalizedItem)
        }
    }

    fun deleteTestPoint(id: String) {
        items.removeAll { it.id == id }
    }

    fun findByModel(modelQuery: String): TestPointItem? {
        val q = modelQuery.lowercase()
        return items.firstOrNull {
            q.contains(it.brand.lowercase()) && q.contains(it.model.lowercase().take(6)) ||
            it.model.lowercase().contains(q) ||
            q.contains(it.id.removePrefix("tp_").replace("_", " "))
        }
    }
}



