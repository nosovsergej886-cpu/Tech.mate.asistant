import re

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "r") as f:
    content = f.read()

# Instead of doing regex on all the variants, I can just replace the TopAppBar title logic
content = re.sub(
    r'when \(tokens\.variant\) \{.*?AppDesignVariant\.TELEGRAM -> \{.*?\).*?AppDesignVariant\.VK -> \{.*?Text\(\s*text = "Чаты".*?\).*?AppDesignVariant\.WHATSAPP -> null\s*\}',
    r'Text(text = "Tech.Mate", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = tokens.topBarContentColor, fontSize = 20.sp)',
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "w") as f:
    f.write(content)
