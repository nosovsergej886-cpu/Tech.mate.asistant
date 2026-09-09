import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

# Add BackHandler import if not present
if "import androidx.activity.compose.BackHandler" not in content:
    content = content.replace("import androidx.compose.ui.unit.sp", "import androidx.compose.ui.unit.sp\nimport androidx.activity.compose.BackHandler\nimport androidx.compose.runtime.saveable.rememberSaveable")

# Change mutableIntStateOf to use rememberSaveable
content = content.replace(
    "var selectedTab by remember { mutableIntStateOf(initialTab) }",
    "var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }"
)

# Insert BackHandler before Scaffold
back_handler_code = """
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold("""

content = content.replace("    Scaffold(", back_handler_code)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
