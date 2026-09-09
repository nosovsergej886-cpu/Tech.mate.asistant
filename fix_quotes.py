import re

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    'val supportId = "support_chat_${currentUser?.email ?: \\"guest\\"}"',
    'val supportId = "support_chat_${currentUser?.email ?: \\'guest\\'}"'.replace("\\'", '"')
)

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "w") as f:
    f.write(content)
