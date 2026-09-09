import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Force VK when providing LocalDesignVariant
content = re.sub(r'LocalDesignVariant provides designVariant,', 'LocalDesignVariant provides com.example.services.AppDesignVariant.VK,', content)
content = re.sub(r'LocalDesignTokens provides getDesignTokens\(isDark, designVariant\)', 'LocalDesignTokens provides getDesignTokens(isDark, com.example.services.AppDesignVariant.VK)', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
