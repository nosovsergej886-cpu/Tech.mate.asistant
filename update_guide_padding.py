import re

with open("app/src/main/java/com/example/ui/widgets/GuideCard.kt", "r") as f:
    content = f.read()

content = re.sub(r'\.padding\(16\.dp\)', '.padding(10.dp)', content)
content = re.sub(r'\.padding\(vertical = 4\.dp\)', '.padding(vertical = 2.dp)', content)

with open("app/src/main/java/com/example/ui/widgets/GuideCard.kt", "w") as f:
    f.write(content)
