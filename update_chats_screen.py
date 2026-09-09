import re

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "r") as f:
    content = f.read()

# I will just write a small sed or python script, or use edit_file tool to replace.
# Actually, since I have to rewrite large files, using Python is safer.
