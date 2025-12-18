package com.yhm.mymail.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yhm.mymail.ui.screens.EmailDetailScreen
import com.yhm.mymail.ui.screens.EmailListScreen
import com.yhm.mymail.ui.screens.LoginScreen
import com.yhm.mymail.viewmodel.EmailViewModel

/**
 * 导航路由定义
 */
object Routes {
    const val LOGIN = "login"
    const val EMAIL_LIST = "email_list"
    const val EMAIL_DETAIL = "email_detail/{messageNumber}"
    
    fun emailDetail(messageNumber: Int) = "email_detail/$messageNumber"
}

/**
 * 应用导航图
 */
@Composable
fun AppNavigation(
    viewModel: EmailViewModel = viewModel()
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        // 登录页
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.EMAIL_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        // 邮件列表页
        composable(Routes.EMAIL_LIST) {
            EmailListScreen(
                viewModel = viewModel,
                onEmailClick = { email ->
                    navController.navigate(Routes.emailDetail(email.messageNumber))
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.EMAIL_LIST) { inclusive = true }
                    }
                }
            )
        }
        
        // 邮件详情页
        composable(
            route = Routes.EMAIL_DETAIL,
            arguments = listOf(
                navArgument("messageNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val messageNumber = backStackEntry.arguments?.getInt("messageNumber") ?: 0
            EmailDetailScreen(
                viewModel = viewModel,
                messageNumber = messageNumber,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
