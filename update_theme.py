import re

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "r") as f:
    content = f.read()

# Force LocalDesignVariant to VK
content = re.sub(r'val LocalDesignVariant = staticCompositionLocalOf { AppDesignVariant\.[A-Z]+ }', 'val LocalDesignVariant = staticCompositionLocalOf { AppDesignVariant.VK }', content)
content = re.sub(r'val LocalDesignTokens = staticCompositionLocalOf \{ getDesignTokens\(false, AppDesignVariant\.[A-Z]+\) \}', 'val LocalDesignTokens = staticCompositionLocalOf { getDesignTokens(false, AppDesignVariant.VK) }', content)

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write(content)
