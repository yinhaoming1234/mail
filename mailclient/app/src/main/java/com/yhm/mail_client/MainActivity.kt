package com.yhm.mail_client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yhm.mail_client.ui.navigation.Screen
import com.yhm.mail_client.ui.screens.*
import com.yhm.mail_client.ui.theme.MailClientTheme
import com.yhm.mail_client.ui.viewmodel.EmailViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MailClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MailClientApp()
                }
            }
        }
    }
}

@Composable
fun MailClientApp() {
    val navController = rememberNavController()
    val viewModel: EmailViewModel = viewModel()
    val currentAccount by viewModel.currentAccount.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = if (currentAccount == null) 
            Screen.Login.route 
        else 
            Screen.MailList.route
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.MailList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // Register Screen
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Account Setup (for adding new accounts)
        composable(Screen.AccountSetup.route) {
            AccountSetupScreen(
                viewModel = viewModel,
                onAccountSaved = {
                    navController.popBackStack()
                }
            )
        }
        
        // Mail List Screen
        composable(Screen.MailList.route) {
            MailListScreen(
                viewModel = viewModel,
                onEmailClick = { emailUid ->
                    navController.navigate(Screen.MailDetail.createRoute(emailUid))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onComposeClick = {
                    navController.navigate(Screen.ComposeEmail.route)
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Compose Email Screen
        composable(Screen.ComposeEmail.route) {
            ComposeEmailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Mail Detail Screen
        composable(
            route = Screen.MailDetail.route,
            arguments = listOf(
                navArgument("emailUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val emailUid = backStackEntry.arguments?.getString("emailUid") ?: return@composable
            MailDetailScreen(
                emailUid = emailUid,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
