import re

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "r") as f:
    content = f.read()

# 1. Remove Stories and Filters
# The stories and filters are usually in item {} blocks inside LazyColumn.
# I will use a simple regex to remove them.
# The user wants pure messenger, so no stories in ChatsScreen.
content = re.sub(r'// VK Stories Carousel & Filter Chips.*?// WhatsApp Stories Bar', '// WhatsApp Stories Bar', content, flags=re.DOTALL)
content = re.sub(r'// WhatsApp Stories Bar.*?// --- ALL CHATS ---', '// --- ALL CHATS ---', content, flags=re.DOTALL)
content = re.sub(r'val vkFilters = listOf\("Все", "iPhone", "Xiaomi", "Samsung", "BGA"\).*?var vkSelectedFilter by remember \{ mutableStateOf\("Все"\) \}', '', content, flags=re.DOTALL)


# 2. Fix Support Chat Duplication
# Change supportId = "support_official_channel" to supportId = "support_chat_${currentUser?.email ?: \"guest\"}"
content = content.replace('val supportId = "support_official_channel"', 'val supportId = "support_chat_${currentUser?.email ?: \\"guest\\"}"')

# Also, when they click, we don't need to intercept it to create a new chat! The chat is already created.
# Let's remove the intercept logic entirely, EXCEPT for God mode which needs to show Support Desk.

old_click_logic = '''                                onClick = {
                                    val isSupportChat = chat.id == "support_official_channel" || chat.title == "Поддержка"
                                    if (isSupportChat) {
                                        val isGod = authService.isGodMode() || AuthService.isGodEmail(currentUser?.email)
                                        if (isGod) {
                                            showSupportDeskDialog = true
                                        } else {
                                            val supportId = "support_chat_${currentUser?.email ?: "guest"}"
                                            scope.launch {
                                                if (dbService.chatDao.getChatById(supportId) == null) {
                                                    dbService.chatDao.insertChat(
                                                        ChatEntity(
                                                            id = supportId,
                                                            title = "Поддержка (${currentUser?.email ?: "guest"})",
                                                            lastMessage = "Напишите ваш вопрос в поддержку",
                                                            lastMessageTime = System.currentTimeMillis()
                                                        )
                                                    )
                                                }
                                                onOpenChat(supportId)
                                            }
                                        }
                                    } else {
                                        onOpenChat(chat.id)
                                    }
                                }'''

new_click_logic = '''                                onClick = {
                                    val isSupportChat = chat.id.startsWith("support_chat_") && chat.title == "Поддержка"
                                    val isGod = authService.isGodMode() || AuthService.isGodEmail(currentUser?.email)
                                    if (isSupportChat && isGod) {
                                        showSupportDeskDialog = true
                                    } else {
                                        onOpenChat(chat.id)
                                    }
                                }'''

content = content.replace(old_click_logic, new_click_logic)

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "w") as f:
    f.write(content)
