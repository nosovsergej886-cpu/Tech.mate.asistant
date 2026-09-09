import re

with open("app/src/main/java/com/example/services/ThemeManager.kt", "r") as f:
    content = f.read()

content = re.sub(r'AppDesignVariant\.valueOf\(prefs\.getString\(KEY_DESIGN_VARIANT, AppDesignVariant\.TELEGRAM\.name\) \?: AppDesignVariant\.TELEGRAM\.name\)', 
                 r'AppDesignVariant.VK', content)
content = re.sub(r'AppDesignVariant\.TELEGRAM', 'AppDesignVariant.VK', content)

with open("app/src/main/java/com/example/services/ThemeManager.kt", "w") as f:
    f.write(content)
