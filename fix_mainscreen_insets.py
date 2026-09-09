import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

# Add contentWindowInsets to Scaffold
content = re.sub(
    r'Scaffold\(\s*bottomBar = \{',
    'Scaffold(\n        contentWindowInsets = WindowInsets(0, 0, 0, 0),\n        bottomBar = {',
    content
)

# Add navigationBarsPadding to the Row inside bottomBar
# Specifically, we want it on the Row or Column to pad the content but keep background.
# In MainScreen, the Row has:
# modifier = Modifier
#     .fillMaxWidth()
#     .height(52.dp),

content = re.sub(
    r'modifier = Modifier\n\s*\.fillMaxWidth\(\)\n\s*\.height\(52\.dp\)',
    'modifier = Modifier\n                        .fillMaxWidth()\n                        .navigationBarsPadding()\n                        .height(52.dp)',
    content
)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
