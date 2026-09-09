import re

with open("app/src/main/java/com/example/ui/screens/PostsScreen.kt", "r") as f:
    content = f.read()

# Make sure we have the imports
if "import com.example.ui.components.StoryOverlay" not in content:
    content = content.replace("import androidx.compose.ui.unit.sp", "import androidx.compose.ui.unit.sp\nimport com.example.ui.components.StoryOverlay")

# Insert StoryOverlay at the top of LazyColumn
story_overlay_code = """
            item {
                StoryOverlay(
                    onOpenAlgorithm = { /* Handle algorithm */ }
                )
            }
            // "What's new" input card"""

if "StoryOverlay" not in content:
    content = content.replace('            // "What\'s new" input card', story_overlay_code)

with open("app/src/main/java/com/example/ui/screens/PostsScreen.kt", "w") as f:
    f.write(content)
