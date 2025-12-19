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
import com.yhm.mail_client.ui.screens.AccountSetupScreen
import com.yhm.mail_client.ui.screens.ChangePasswordScreen
import com.yhm.mail_client.ui.screens.ComposeEmailScreen
import com.yhm.mail_client.ui.screens.LoginScreen
import com.yhm.mail_client.ui.screens.MailDetailScreen
import com.yhm.mail_client.ui.screens.MailListScreen
import com.yhm.mail_client.ui.screens.RegisterScreen
import com.yhm.mail_client.ui.screens.SettingsScreen
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
                onComposeClick = { draftId ->
                    navController.navigate(Screen.ComposeEmail.createRoute(draftId))
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
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Change Password Screen
        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPasswordChanged = {
                    navController.popBackStack()
                }
            )
        }
        
        // Compose Email Screen
        composable(
            route = Screen.ComposeEmail.route,
            arguments = listOf(
                navArgument("draftId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val draftId = backStackEntry.arguments?.getString("draftId")
            val emails by viewModel.emails.collectAsState()
            val draftEmail = draftId?.let { id -> emails.find { it.uid == id } }
            
            ComposeEmailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                draftEmail = draftEmail
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
