import re

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "r") as f:
    content = f.read()

# Remove WhatsApp Stories Bar
content = re.sub(
    r'// WhatsApp Stories Bar.*?if \(tokens\.variant == AppDesignVariant\.WHATSAPP.*?\).*?item \{.*?StoryOverlay\(\s*onOpenAlgorithm = \{ showRepairAlgorithmDialog = true \}\s*\)\s*\}.*?\}',
    '',
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "w") as f:
    f.write(content)
