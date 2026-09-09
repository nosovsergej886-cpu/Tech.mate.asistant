import re

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "r") as f:
    content = f.read()

# Replace all bad interpolations
# "support_chat_${currentUser?.email ?: "guest"}"
# Поддержка (${currentUser?.email ?: "guest"})

content = content.replace(
    'val supportId = "support_chat_${currentUser?.email ?: "guest"}"',
    'val guestEmail = currentUser?.email ?: "guest"\n        val supportId = "support_chat_$guestEmail"'
)

content = content.replace(
    'title = "Поддержка (${currentUser?.email ?: "guest"})"',
    'title = "Поддержка ($guestEmail)"'
)

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "w") as f:
    f.write(content)
