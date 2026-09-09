import re

with open("app/src/main/java/com/example/ui/widgets/ChatBubble.kt", "r") as f:
    content = f.read()

# Make the outer padding of the row tighter
content = re.sub(r'modifier = Modifier\n\s*\.fillMaxWidth\(\)\n\s*\.padding\(horizontal = 8\.dp, vertical = 4\.dp\)', 
                 r'modifier = Modifier\n                .fillMaxWidth()\n                .padding(horizontal = 8.dp, vertical = 2.dp)', content)

# Make the inner padding of the bubble tighter
content = re.sub(r'Column\(modifier = Modifier\.padding\(start = 10\.dp, end = 10\.dp, bottom = 10\.dp, top = 4\.dp\)\)',
                 r'Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp, top = 2.dp))', content)

# Remove specific variants from checkmarks just to clean up (since it's only VK now)
content = re.sub(r'val \(checkText, checkColor\) = when \(tokens\.variant\) \{.*?\}', 'val checkText = "✓"\nval checkColor = if (isDark) VkTextSecondaryDark else VkTextSecondaryLight', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/widgets/ChatBubble.kt", "w") as f:
    f.write(content)
