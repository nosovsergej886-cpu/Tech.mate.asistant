import re
import os

files = [
    "app/src/main/java/com/example/ui/screens/PostsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ChatsScreen.kt",
    "app/src/main/java/com/example/ui/screens/KnowledgeScreen.kt",
    "app/src/main/java/com/example/ui/screens/ProfileAndAdminScreens.kt"
]

for file in files:
    with open(file, "r") as f:
        content = f.read()
    
    # Check if contentWindowInsets is already there
    if "contentWindowInsets = WindowInsets" not in content:
        content = re.sub(
            r'Scaffold\(',
            r'Scaffold(\n        contentWindowInsets = WindowInsets(0, 0, 0, 0),',
            content
        )
        with open(file, "w") as f:
            f.write(content)
