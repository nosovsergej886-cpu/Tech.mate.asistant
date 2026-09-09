import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

# Remove navigationBarsPadding from Row
content = re.sub(
    r'\.navigationBarsPadding\(\)\s*\.height\(52\.dp\)',
    r'.height(52.dp)',
    content
)

# Add navigationBarsPadding to Column after background
content = re.sub(
    r'\.background\(if \(isDark\) VkDarkSurface else Color\.White\)',
    r'.background(if (isDark) VkDarkSurface else Color.White)\n                    .navigationBarsPadding()',
    content
)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
