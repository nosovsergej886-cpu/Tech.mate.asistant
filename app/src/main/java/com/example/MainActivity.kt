package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.services.AuthService
import com.example.services.DeviceSecurityService
import com.example.ui.screens.*
import com.example.ui.theme.TechMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.services.LanguageService.init(this)
        enableEdgeToEdge()

        setContent {
            val themeManager = remember { com.example.services.ThemeManager.getInstance(this@MainActivity) }
            val themeMode by themeManager.themeMode.collectAsState()
            val designVariant by themeManager.designVariant.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                com.example.services.AppThemeMode.DARK -> true
                com.example.services.AppThemeMode.LIGHT -> false
                com.example.services.AppThemeMode.SYSTEM -> systemInDark
            }

            TechMateTheme(darkTheme = isDarkTheme, designVariant = designVariant) {
                val navController = rememberNavController()
                val authService = remember { AuthService.getInstance(this@MainActivity) }
                val currentUser by authService.currentUser.collectAsState()

                var showPermissionsDialog by remember {
                    val prefs = this@MainActivity.getSharedPreferences("app_permissions", android.content.Context.MODE_PRIVATE)
                    val hasShown = prefs.getBoolean("has_shown_initial_permissions", false)
                    val hasAll = com.example.services.PermissionHelper.hasAllEssentialPermissions(this@MainActivity)
                    mutableStateOf(!hasShown && !hasAll)
                }

                if (showPermissionsDialog) {
                    com.example.ui.components.PermissionsDialog(
                        isFirstLaunch = true,
                        onDismiss = {
                            this@MainActivity.getSharedPreferences("app_permissions", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("has_shown_initial_permissions", true).apply()
                            showPermissionsDialog = false
                        }
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        SplashScreen(
                            onNavigateNext = {
                                val deviceSecurity = DeviceSecurityService.getInstance(this@MainActivity)
                                if (deviceSecurity.isDeviceRegistered()) {
                                    navController.navigate("auth") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else if (currentUser != null) {
                                    navController.navigate("main") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("auth") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("auth") {
                        AuthScreen(
                            onLoginSuccess = {
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("main") {
                        MainScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { themeManager.toggleDarkTheme(it) },
                            onOpenChat = { chatId ->
                                navController.navigate("chat/$chatId")
                            },
                            onNavigateAdmin = {
                                navController.navigate("admin")
                            },
                            onLogout = {
                                navController.navigate("auth") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("chats") {
                        MainScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { themeManager.toggleDarkTheme(it) },
                            onOpenChat = { chatId ->
                                navController.navigate("chat/$chatId")
                            },
                            onNavigateAdmin = {
                                navController.navigate("admin")
                            },
                            onLogout = {
                                navController.navigate("auth") {
                                    popUpTo("main") { inclusive = true }
                                }
                            },
                            initialTab = 0
                        )
                    }

                    composable(
                        route = "chat/{chatId}",
                        arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                        ChatScreen(
                            chatId = chatId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("knowledge") {
                        KnowledgeScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { themeManager.toggleDarkTheme(it) },
                            onNavigateAdmin = {
                                navController.navigate("admin")
                            },
                            onLogout = {
                                navController.navigate("auth") {
                                    popUpTo("chats") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("admin") {
                        AdminScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
