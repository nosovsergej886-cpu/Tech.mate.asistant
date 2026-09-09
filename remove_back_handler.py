import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'\s*BackHandler\(enabled = selectedTab != 0\) \{\s*selectedTab = 0\s*\}',
    '',
    content
)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
