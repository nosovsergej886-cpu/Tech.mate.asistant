import re

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "r") as f:
    content = f.read()

content = content.replace('text = "Чаты"', 'text = "Tech.Mate"')

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "w") as f:
    f.write(content)
