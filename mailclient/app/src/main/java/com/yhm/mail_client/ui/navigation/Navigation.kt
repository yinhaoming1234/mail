package com.yhm.mail_client.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object AccountSetup : Screen("account_setup")
    object MailList : Screen("mail_list")
    object MailDetail : Screen("mail_detail/{emailUid}") {
        fun createRoute(emailUid: String) = "mail_detail/$emailUid"
    }
    object ComposeEmail : Screen("compose_email")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Admin : Screen("admin")
}
