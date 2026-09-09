import re

with open("app/src/main/java/com/example/ui/components/StoryOverlay.kt", "r") as f:
    content = f.read()

# Replace filter options
content = re.sub(
    r'val filterOptions = listOf\("Все", "📱 iPhone".*?\)',
    'val filterOptions = listOf("Для всех", "Мой СЦ")',
    content
)

# Update filtering logic
old_filter_logic = '''    val filteredStories = remember(allStories, selectedTagFilter) {
        if (selectedTagFilter == "Все") {
            allStories
        } else {
            val key = when (selectedTagFilter) {
                "📱 iPhone" -> "iphone"
                "📱 Xiaomi" -> "redmi|xiaomi|poco"
                "📱 Samsung" -> "samsung"
                "🔧 BGA" -> "bga|пайка|ребол"
                "⚡ ПО / Bootloop" -> "bootloop|прошивк|ios|сбой"
                "🔋 Питание" -> "vbus|питани|кз|акб"
                else -> selectedTagFilter.lowercase()
            }
            val regex = Regex(key, RegexOption.IGNORE_CASE)
            allStories.filter {
                regex.containsMatchIn(it.title) ||
                regex.containsMatchIn(it.subtitle) ||
                regex.containsMatchIn(it.content) ||
                regex.containsMatchIn(it.taggedDeviceModel) ||
                regex.containsMatchIn(it.taggedCategory)
            }
        }
    }'''

new_filter_logic = '''    val filteredStories = remember(allStories, selectedTagFilter, currentScId) {
        if (selectedTagFilter == "Для всех") {
            allStories
        } else {
            allStories.filter { it.authorServiceCenter == currentUser?.serviceCenterName || it.authorServiceCenter == currentScId || it.authorServiceCenter == "СЦ «ТехноМастер»" }
        }
    }'''

content = content.replace(old_filter_logic, new_filter_logic)
content = content.replace('var selectedTagFilter by remember { mutableStateOf("Все") }', 'var selectedTagFilter by remember { mutableStateOf("Для всех") }')

with open("app/src/main/java/com/example/ui/components/StoryOverlay.kt", "w") as f:
    f.write(content)
