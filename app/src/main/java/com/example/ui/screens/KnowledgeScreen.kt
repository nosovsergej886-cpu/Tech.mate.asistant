package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.TestPointItem
import com.example.data.TestPointRepository
import com.example.model.Cause
import com.example.model.GuideData
import com.example.ui.widgets.TechAsyncImage
import com.example.model.KnowledgeBaseEntryEntity
import com.example.model.Step
import com.example.services.DatabaseService
import com.example.services.LanguageService
import com.example.ui.components.BoardviewInspectorScreen
import com.example.ui.components.AddBoardviewPhotoDialog
import com.example.model.BoardviewPhotoEntity
import com.example.ui.theme.*
import com.example.ui.util.filterDuplicateInput
import com.example.ui.widgets.BrandLogoIcon
import com.example.ui.widgets.GuideCard
import com.example.ui.widgets.ModelDeviceGraphic
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val dbService = remember { DatabaseService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val isDark = LocalIsDarkTheme.current

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Guides, 1 = Schematics, 2 = TestPoints & FRP
    val isSchematicTab = selectedTabIndex == 1

    var searchQuery by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var selectedEntryDetail by remember { mutableStateOf<KnowledgeBaseEntryEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddTestPointDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<KnowledgeBaseEntryEntity?>(null) }

    // Fix Issue #5: BackHandler unwinds model -> brand -> root instead of exiting to Chat screen!
    BackHandler(enabled = selectedEntryDetail != null || selectedModel != null || selectedBrand != null) {
        when {
            selectedEntryDetail != null -> selectedEntryDetail = null
            selectedModel != null -> selectedModel = null
            selectedBrand != null -> selectedBrand = null
        }
    }

    val entriesFlow = remember(searchQuery, isSchematicTab) {
        if (searchQuery.isBlank()) {
            dbService.knowledgeDao.getEntriesByType(isSchematicTab)
        } else {
            dbService.knowledgeDao.searchEntries(searchQuery, isSchematicTab)
        }
    }

    val allEntries by entriesFlow.collectAsState(initial = emptyList())

    val brands = listOf("Apple", "Samsung", "Xiaomi", "Huawei", "Honor", "Poco", "Realme")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageService.getString("knowledge_title"), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TelegramHeader
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 2) {
                        showAddTestPointDialog = true
                    } else {
                        showAddDialog = true
                    }
                },
                containerColor = if (selectedTabIndex == 2) TechGoldTestPoint else TechPrimaryBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) TelegramDarkBg else TechBackgroundLight)
                .padding(paddingValues)
        ) {
            // TabBar
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = if (isDark) TelegramDarkSurface else Color.White,
                contentColor = TelegramBlue,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = TelegramBlue
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        selectedBrand = null
                        selectedModel = null
                    },
                    text = { Text("📖 " + LanguageService.getString("tab_guides")) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        selectedBrand = null
                        selectedModel = null
                    },
                    text = { Text("⚡ " + LanguageService.getString("tab_schematics")) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        selectedBrand = null
                        selectedModel = null
                    },
                    text = { Text("📍 TestPoint / FRP") }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = filterDuplicateInput(searchQuery, it) },
                placeholder = {
                    Text(if (selectedTabIndex == 2) "Поиск тестпоинтов (например, Redmi 9T, A51)..." else LanguageService.getString("search_kb_hint"))
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (selectedTabIndex == 2) {
                // TESTPOINTS & FRP SECTION - HIERARCHICAL BRAND -> MODEL -> TESTPOINT
                val testPoints = TestPointRepository.items

                // Navigation Breadcrumbs for TestPoints
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = {
                            selectedBrand = null
                            selectedModel = null
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (selectedBrand == null) "📍 Бренды" else "Бренды",
                            fontWeight = if (selectedBrand == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBrand == null) TechGoldTestPoint else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedBrand != null) {
                        Text("›", color = Color.Gray, fontSize = 14.sp)
                        TextButton(
                            onClick = { selectedModel = null },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (selectedModel == null) "📱 $selectedBrand" else selectedBrand!!,
                                fontWeight = if (selectedModel == null) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedModel == null) TechGoldTestPoint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (selectedModel != null) {
                        Text("›", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            text = "🔧 $selectedModel",
                            fontWeight = FontWeight.Bold,
                            color = TechGoldTestPoint,
                            fontSize = 14.sp
                        )
                    }
                }

                if (searchQuery.isNotBlank()) {
                    SearchTestPointList(
                        searchQuery = searchQuery,
                        testPoints = testPoints,
                        context = context,
                        onDeleteTp = { tp ->
                            TestPointRepository.deleteTestPoint(tp.id)
                            Toast.makeText(context, "Тестпоинт удален", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else if (selectedBrand == null) {
                    TestPointBrandGridLevel(
                        brands = brands,
                        testPoints = testPoints,
                        onSelectBrand = { selectedBrand = it }
                    )
                } else if (selectedModel == null) {
                    TestPointModelGridLevel(
                        brand = selectedBrand!!,
                        testPoints = testPoints,
                        onSelectModel = { selectedModel = it },
                        onBackToBrands = { selectedBrand = null }
                    )
                } else {
                    TestPointListLevel(
                        brand = selectedBrand!!,
                        model = selectedModel!!,
                        testPoints = testPoints,
                        context = context,
                        onDeleteTp = { tp ->
                            TestPointRepository.deleteTestPoint(tp.id)
                            Toast.makeText(context, "Тестпоинт удален", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                // Navigation Breadcrumbs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = {
                            selectedBrand = null
                            selectedModel = null
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (selectedBrand == null) "📍 Бренды" else "Бренды",
                            fontWeight = if (selectedBrand == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBrand == null) TechPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedBrand != null) {
                        Text("›", color = Color.Gray, fontSize = 14.sp)
                        TextButton(
                            onClick = { selectedModel = null },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (selectedModel == null) "📱 $selectedBrand" else selectedBrand!!,
                                fontWeight = if (selectedModel == null) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedModel == null) TechPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (selectedModel != null) {
                        Text("›", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            text = "🔧 $selectedModel",
                            fontWeight = FontWeight.Bold,
                            color = TechPrimaryBlue,
                            fontSize = 14.sp
                        )
                    }
                }

                // Drilldown Content logic:
                if (searchQuery.isNotBlank()) {
                    SearchGuideList(
                        entries = allEntries,
                        dbService = dbService,
                        onSelectEntry = { selectedEntryDetail = it },
                        onDeleteEntry = { entryToDelete = it }
                    )
                } else if (selectedBrand == null) {
                    BrandGridLevel(
                        brands = brands,
                        allEntries = allEntries,
                        onSelectBrand = { selectedBrand = it }
                    )
                } else if (selectedModel == null) {
                    ModelGridLevel(
                        brand = selectedBrand!!,
                        allEntries = allEntries,
                        onSelectModel = { selectedModel = it },
                        onBackToBrands = { selectedBrand = null },
                        onAddNewModel = { showAddDialog = true }
                    )
                } else if (isSchematicTab) {
                    BoardviewInspectorScreen(
                        brand = selectedBrand!!,
                        model = selectedModel!!,
                        onBack = { selectedModel = null }
                    )
                } else {
                    ProblemsListLevel(
                        brand = selectedBrand!!,
                        model = selectedModel!!,
                        allEntries = allEntries,
                        dbService = dbService,
                        onSelectEntry = { selectedEntryDetail = it },
                        onDeleteEntry = { entryToDelete = it }
                    )
                }
            }
        }

        // Add Manual Guide Dialog
        if (showAddDialog) {
            AddGuideDialog(
                isSchematic = isSchematicTab,
                onDismiss = { showAddDialog = false },
                onSave = { brand, model, problem, difficulty, time, tools, causes, steps, proTip, risks ->
                    scope.launch {
                        val guide = GuideData(
                            device = "$brand $model",
                            problem = problem,
                            difficulty = difficulty,
                            timeEstimate = time,
                            tools = tools.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            causes = listOf(Cause(causes, 80, "Замер", "Норма", "Исправление")),
                            steps = steps.split("\n").mapIndexed { i, s -> Step(i + 1, "Шаг ${i + 1}", s) },
                            proTip = proTip.ifBlank { null },
                            risks = risks.ifBlank { null }
                        )

                        val entry = KnowledgeBaseEntryEntity(
                            brand = brand.trim(),
                            model = model.trim().lowercase(),
                            problem = problem.trim(),
                            guideDataJson = dbService.toJson(guide),
                            addedBy = "Мастер",
                            isSchematic = isSchematicTab
                        )

                        dbService.knowledgeDao.insertEntry(entry)
                        showAddDialog = false
                        Toast.makeText(context, "Руководство добавлено!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Add TestPoint Dialog
        if (showAddTestPointDialog) {
            AddTestPointDialog(
                onDismiss = { showAddTestPointDialog = false },
                onSave = { brand, model, cpu, desc, frp, tools, imgUrl ->
                    val (normBrand, normModel) = TestPointRepository.normalizeBrand(brand, model)
                    val newItem = TestPointItem(
                        id = "tp_custom_" + System.currentTimeMillis(),
                        brand = normBrand,
                        model = normModel,
                        cpuType = cpu,
                        description = desc,
                        imageUrl = imgUrl.ifBlank { "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=800" },
                        frpGuide = frp,
                        toolsNeeded = tools.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    )
                    TestPointRepository.addTestPoint(newItem)

                    // Also save to persistent Room database
                    scope.launch {
                        val guideData = GuideData(
                            device = "$normBrand $normModel",
                            problem = "TestPoint / FRP ($cpu)",
                            difficulty = "Средняя",
                            timeEstimate = "5-10 мин",
                            tools = newItem.toolsNeeded,
                            causes = listOf(Cause("Блокировка FRP / EDL", 100, desc, "Замкнут TestPoint", "Сброс в сервисном ПО")),
                            steps = frp.lines().filter { it.isNotBlank() }.mapIndexed { idx, line -> Step(idx + 1, "Шаг ${idx + 1}", line) },
                            proTip = "Обязательно отключайте аккумулятор перед замыканием тестпоинта!",
                            risks = "Не повредите соседние SMD элементы"
                        )
                        val entry = KnowledgeBaseEntryEntity(
                            brand = normBrand,
                            model = normModel,
                            problem = "TestPoint / FRP ($cpu)",
                            guideDataJson = dbService.toJson(guideData),
                            addedBy = "Мастер (Тестпоинт)"
                        )
                        dbService.knowledgeDao.insertEntry(entry)
                    }

                    // Auto navigate to the newly added brand & model
                    selectedBrand = normBrand
                    selectedModel = normModel
                    showAddTestPointDialog = false
                    Toast.makeText(context, "✅ Тестпоинт $normModel добавлен в категорию $normBrand!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Delete Confirm Dialog
        if (entryToDelete != null) {
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text("Удалить из базы знаний?") },
                text = { Text("Вы уверены, что хотите удалить ${entryToDelete!!.brand} ${entryToDelete!!.model}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = entryToDelete!!.id
                            scope.launch {
                                dbService.knowledgeDao.deleteEntry(id)
                                entryToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(LanguageService.getString("confirm"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text(LanguageService.getString("cancel"))
                    }
                }
            )
        }
    }
}

@Composable
fun AddGuideDialog(
    isSchematic: Boolean,
    onDismiss: () -> Unit,
    onSave: (brand: String, model: String, problem: String, difficulty: String, time: String, tools: String, causes: String, steps: String, proTip: String, risks: String) -> Unit
) {
    var brand by remember { mutableStateOf("Samsung") }
    var model by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Средняя") }
    var time by remember { mutableStateOf("30 минут") }
    var tools by remember { mutableStateOf("Мультиметр, Паяльник") }
    var causes by remember { mutableStateOf("КЗ по питанию") }
    var steps by remember { mutableStateOf("1. Осмотр платы\n2. Прозвонка элементов") }
    var proTip by remember { mutableStateOf("") }
    var risks by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSchematic) "Добавить схему" else LanguageService.getString("add_guide")) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = filterDuplicateInput(brand, it) },
                        label = { Text(LanguageService.getString("brand")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = filterDuplicateInput(model, it) },
                        label = { Text(LanguageService.getString("model")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = problem,
                        onValueChange = { problem = filterDuplicateInput(problem, it) },
                        label = { Text(LanguageService.getString("problem")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = tools,
                        onValueChange = { tools = filterDuplicateInput(tools, it) },
                        label = { Text("Инструменты (через запятую)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = steps,
                        onValueChange = { steps = filterDuplicateInput(steps, it) },
                        label = { Text("Шаги (по строкам)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
                item {
                    OutlinedTextField(
                        value = proTip,
                        onValueChange = { proTip = filterDuplicateInput(proTip, it) },
                        label = { Text(LanguageService.getString("pro_tip")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (model.isBlank() || problem.isBlank()) return@Button
                    onSave(brand, model, problem, difficulty, time, tools, causes, steps, proTip, risks)
                }
            ) {
                Text(LanguageService.getString("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageService.getString("cancel"))
            }
        }
    )
}

@Composable
fun AddTestPointDialog(
    onDismiss: () -> Unit,
    onSave: (brand: String, model: String, cpu: String, desc: String, frp: String, tools: String, imgUrl: String) -> Unit
) {
    var brand by remember { mutableStateOf("Xiaomi") }
    var model by remember { mutableStateOf("") }
    var cpu by remember { mutableStateOf("Qualcomm EDL 9008") }
    var desc by remember { mutableStateOf("") }
    var frp by remember { mutableStateOf("") }
    var tools by remember { mutableStateOf("Пинцет, USB Type-C") }
    var imgUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📍 Добавить TestPoint / FRP") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = filterDuplicateInput(brand, it) },
                        label = { Text("Бренд (напр. Xiaomi, Samsung)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = filterDuplicateInput(model, it) },
                        label = { Text("Модель (напр. Redmi Note 11)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = cpu,
                        onValueChange = { cpu = filterDuplicateInput(cpu, it) },
                        label = { Text("Режим / Процессор (напр. EDL 9008 / MTK)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = filterDuplicateInput(desc, it) },
                        label = { Text("Где находятся точки TestPoint") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = frp,
                        onValueChange = { frp = filterDuplicateInput(frp, it) },
                        label = { Text("Инструкция по сбросу FRP / EDL") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
                item {
                    OutlinedTextField(
                        value = tools,
                        onValueChange = { tools = filterDuplicateInput(tools, it) },
                        label = { Text("Необходимые инструменты") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = imgUrl,
                        onValueChange = { imgUrl = filterDuplicateInput(imgUrl, it) },
                        label = { Text("Ссылка на фото / схему (URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (model.isBlank() || desc.isBlank()) return@Button
                    onSave(brand, model, cpu, desc, frp, tools, imgUrl)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun BrandGridLevel(
    brands: List<String>,
    allEntries: List<KnowledgeBaseEntryEntity>,
    onSelectBrand: (String) -> Unit
) {
    val existingBrands = remember(allEntries) {
        val set = brands.toMutableSet()
        allEntries.forEach { if (it.brand.isNotBlank()) set.add(it.brand.trim()) }
        set.toList().filter { it.isNotBlank() }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(existingBrands) { brandName ->
            val brandEntries = allEntries.filter { it.brand.equals(brandName, ignoreCase = true) }
            val uniqueModelsCount = brandEntries.map { it.model.lowercase().trim() }.distinct().size

            Card(
                onClick = { onSelectBrand(brandName) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(110.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandLogoIcon(brand = brandName, modifier = Modifier.size(42.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = brandName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uniqueModelsCount > 0) "$uniqueModelsCount модел. • ${brandEntries.size} решен." else "Каталог моделей",
                            style = MaterialTheme.typography.bodySmall,
                            color = TechPrimaryBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelGridLevel(
    brand: String,
    allEntries: List<KnowledgeBaseEntryEntity>,
    onSelectModel: (String) -> Unit,
    onBackToBrands: () -> Unit,
    onAddNewModel: () -> Unit = {}
) {
    val modelList = remember(allEntries, brand) {
        allEntries
            .filter { it.brand.equals(brand, ignoreCase = true) && it.model.isNotBlank() }
            .map { it.model.trim() }
            .distinctBy { it.lowercase().trim() }
    }

    if (modelList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BrandLogoIcon(brand = brand, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "В категории $brand пока нет изученных моделей",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Модели появляются здесь автоматически после решения проблем в ИИ-чате или при добавлении мастерами.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onAddNewModel,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Добавить решение / схему")
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(modelList) { modelName ->
                val count = allEntries.count {
                    it.brand.equals(brand, ignoreCase = true) &&
                    it.model.equals(modelName, ignoreCase = true)
                }

                Card(
                    onClick = { onSelectModel(modelName) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ModelDeviceGraphic(
                            brand = brand,
                            model = modelName,
                            modifier = Modifier.size(60.dp, 85.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (count > 0) "$count реш." else "Изучено",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (count > 0) TechPrimaryBlue else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemsListLevel(
    brand: String,
    model: String,
    allEntries: List<KnowledgeBaseEntryEntity>,
    dbService: DatabaseService,
    onSelectEntry: (KnowledgeBaseEntryEntity) -> Unit,
    onDeleteEntry: (KnowledgeBaseEntryEntity) -> Unit
) {
    val modelEntries = remember(allEntries, brand, model) {
        allEntries.filter {
            it.brand.equals(brand, ignoreCase = true) &&
            it.model.contains(model, ignoreCase = true)
        }
    }

    if (modelEntries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Нет решений для $brand $model",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Нажмите + внизу, чтобы добавить первое руководство!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(modelEntries, key = { it.id }) { entry ->
                val guideData = dbService.parseGuide(entry.guideDataJson)

                if (guideData != null) {
                    GuideCard(
                        guide = guideData,
                        modifier = Modifier.clickable { onSelectEntry(entry) },
                        onSaveToKb = null
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectEntry(entry) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entry.brand} ${entry.model}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = entry.problem,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TechPrimaryBlue
                                )
                            }
                            IconButton(onClick = { onDeleteEntry(entry) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGuideList(
    entries: List<KnowledgeBaseEntryEntity>,
    dbService: DatabaseService,
    onSelectEntry: (KnowledgeBaseEntryEntity) -> Unit,
    onDeleteEntry: (KnowledgeBaseEntryEntity) -> Unit
) {
    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Ничего не найдено", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                val guideData = dbService.parseGuide(entry.guideDataJson)
                if (guideData != null) {
                    GuideCard(
                        guide = guideData,
                        modifier = Modifier.clickable { onSelectEntry(entry) },
                        onSaveToKb = null
                    )
                }
            }
        }
    }
}

@Composable
private fun TestPointBrandGridLevel(
    brands: List<String>,
    testPoints: List<TestPointItem>,
    onSelectBrand: (String) -> Unit
) {
    val existingBrands = remember(testPoints) {
        val set = brands.toMutableSet()
        testPoints.forEach { if (it.brand.isNotBlank()) set.add(it.brand.trim()) }
        set.toList().filter { it.isNotBlank() }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(existingBrands) { brandName ->
            val brandItems = testPoints.filter { it.brand.equals(brandName, ignoreCase = true) }
            val uniqueModelsCount = brandItems.map { it.model.lowercase().trim() }.distinct().size

            Card(
                onClick = { onSelectBrand(brandName) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(110.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandLogoIcon(brand = brandName, modifier = Modifier.size(42.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = brandName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (brandItems.isNotEmpty()) "$uniqueModelsCount модел. • ${brandItems.size} тп" else "Тестпоинты",
                            style = MaterialTheme.typography.bodySmall,
                            color = TechGoldTestPoint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestPointModelGridLevel(
    brand: String,
    testPoints: List<TestPointItem>,
    onSelectModel: (String) -> Unit,
    onBackToBrands: () -> Unit
) {
    val modelList = remember(testPoints, brand) {
        testPoints
            .filter { it.brand.equals(brand, ignoreCase = true) }
            .map { it.model.trim() }
            .distinctBy { it.lowercase().trim() }
    }

    if (modelList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "В категории $brand пока нет сохраненных тестпоинтов", color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Нажмите + внизу для добавления тестпоинта", fontSize = 12.sp, color = TechGoldTestPoint)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(modelList) { modelName ->
                val matching = testPoints.filter {
                    it.brand.equals(brand, ignoreCase = true) &&
                    it.model.equals(modelName, ignoreCase = true)
                }
                val firstItem = matching.firstOrNull()
                val count = matching.size

                Card(
                    onClick = { onSelectModel(modelName) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ModelDeviceGraphic(
                            brand = brand,
                            model = modelName,
                            modifier = Modifier.size(60.dp, 85.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        if (firstItem != null) {
                            Text(
                                text = firstItem.cpuType.take(25),
                                style = MaterialTheme.typography.bodySmall,
                                color = TechGoldTestPoint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = if (count > 1) "$count тестпоинта" else "1 тестпоинт",
                            style = MaterialTheme.typography.bodySmall,
                            color = TechPrimaryBlue,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestPointListLevel(
    brand: String,
    model: String,
    testPoints: List<TestPointItem>,
    context: Context,
    onDeleteTp: (TestPointItem) -> Unit
) {
    val modelPoints = remember(testPoints, brand, model) {
        testPoints.filter {
            it.brand.equals(brand, ignoreCase = true) &&
            (it.model.contains(model, ignoreCase = true) || model.contains(it.model, ignoreCase = true))
        }
    }

    if (modelPoints.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Нет тестпоинтов для $brand $model",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Нажмите + внизу, чтобы добавить тестпоинт для этой модели!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(modelPoints, key = { it.id }) { tp ->
                TestPointCard(
                    tp = tp,
                    context = context,
                    onDelete = { onDeleteTp(tp) }
                )
            }
        }
    }
}

@Composable
private fun SearchTestPointList(
    searchQuery: String,
    testPoints: List<TestPointItem>,
    context: Context,
    onDeleteTp: (TestPointItem) -> Unit
) {
    val filtered = remember(searchQuery, testPoints) {
        testPoints.filter {
            it.brand.contains(searchQuery, ignoreCase = true) ||
            it.model.contains(searchQuery, ignoreCase = true) ||
            it.cpuType.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Тестпоинты не найдены по запросу \"$searchQuery\"", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.id }) { tp ->
                TestPointCard(
                    tp = tp,
                    context = context,
                    onDelete = { onDeleteTp(tp) }
                )
            }
        }
    }
}

@Composable
private fun TestPointCard(
    tp: TestPointItem,
    context: Context,
    onDelete: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${tp.brand} ${tp.model}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tp.cpuType,
                        style = MaterialTheme.typography.bodySmall,
                        color = TechGoldTestPoint,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = tp.description, style = MaterialTheme.typography.bodyMedium)

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                var activePreviewUrl by remember { mutableStateOf<String?>(null) }
                val allPhotos = remember(tp) {
                    val list = mutableListOf<String>()
                    if (tp.imageUrl.isNotBlank()) list.add(tp.imageUrl)
                    list.addAll(tp.additionalImages.filter { it.isNotBlank() })
                    list.distinct()
                }

                if (allPhotos.size == 1) {
                    TechAsyncImage(
                        imageUrl = allPhotos.first(),
                        contentDescription = "TestPoint Diagram",
                        title = "${tp.brand} ${tp.model} TestPoint",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activePreviewUrl = allPhotos.first() },
                        contentScale = ContentScale.Crop
                    )
                } else if (allPhotos.size > 1) {
                    Column {
                        Text(
                            text = "📸 Доступно фото и схем: ${allPhotos.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = TechPrimaryBlue,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allPhotos.forEachIndexed { idx, url ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .width(170.dp)
                                        .clickable { activePreviewUrl = url }
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        TechAsyncImage(
                                            imageUrl = url,
                                            contentDescription = "Photo ${idx + 1}",
                                            title = "${tp.brand} ${tp.model} (Вид ${idx + 1})",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(115.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(
                                            text = if (idx == 0) "Вид 1: Расположение" else "Вид ${idx + 1}: Схема точек",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (activePreviewUrl != null) {
                    com.example.ui.widgets.InteractiveImageViewerDialog(
                        imageUrl = activePreviewUrl!!,
                        title = "${tp.brand} ${tp.model}",
                        subtitle = "${tp.cpuType} • Pinch to zoom",
                        onDismiss = { activePreviewUrl = null }
                    )
                }

                if (tp.frpGuide.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🔑 Инструкция по сбросу FRP / EDL 9008:",
                        fontWeight = FontWeight.Bold,
                        color = TechPrimaryBlue,
                        fontSize = 13.sp
                    )
                    Text(
                        text = tp.frpGuide,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }

                if (tp.toolsNeeded.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tp.toolsNeeded.forEach { tool ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tool, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp), tint = TechGoldTestPoint)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("TestPoint Guide", "${tp.brand} ${tp.model} TestPoint:\n${tp.description}\n\nFRP Guide:\n${tp.frpGuide}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Инструкция скопирована!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechPrimaryBlue)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Копировать", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val searchTarget = tp.searchQuery.ifBlank { "${tp.brand} ${tp.model} test point" }
                            val uri = Uri.parse("https://www.google.com/search?tbm=isch&q=${Uri.encode(searchTarget)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Поиск Google", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить тестпоинт?") },
            text = { Text("Вы уверены, что хотите удалить тестпоинт для ${tp.brand} ${tp.model}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

