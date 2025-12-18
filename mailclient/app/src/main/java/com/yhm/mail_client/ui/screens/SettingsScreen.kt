package com.yhm.mail_client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yhm.mail_client.ui.viewmodel.EmailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: EmailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onLogout: () -> Unit
) {
    val currentAccount by viewModel.currentAccount.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Account Information Section
            SettingsSection(title = "账号信息") {
                currentAccount?.let { account ->
                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = "邮箱地址",
                        subtitle = account.email
                    )
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "账户名称",
                        subtitle = account.name
                    )
                    Divider()
                    SettingsClickableItem(
                        icon = Icons.Default.Lock,
                        title = "修改密码",
                        onClick = onNavigateToChangePassword
                    )
                }
            }

            Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // Server Settings Section
            SettingsSection(title = "服务器设置") {
                currentAccount?.let { account ->
                    SettingsItem(
                        icon = Icons.Default.Cloud,
                        title = "POP3 服务器",
                        subtitle = "${account.pop3Host}:${account.pop3Port}"
                    )
                    SettingsItem(
                        icon = Icons.Default.Send,
                        title = "SMTP 服务器",
                        subtitle = "${account.smtpHost}:${account.smtpPort}"
                    )
                    Divider()
                    SettingsClickableItem(
                        icon = Icons.Default.Settings,
                        title = "高级设置",
                        onClick = {
                            // TODO: Navigate to advanced settings
                        }
                    )
                }
            }

            Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // App Settings Section
            SettingsSection(title = "应用设置") {
                SettingsClickableItem(
                    icon = Icons.Default.Notifications,
                    title = "通知设置",
                    onClick = {
                        // TODO: Navigate to notification settings
                    }
                )
                SettingsClickableItem(
                    icon = Icons.Default.Sync,
                    title = "同步设置",
                    onClick = {
                        // TODO: Navigate to sync settings
                    }
                )
            }

            Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // Account Actions Section
            SettingsSection(title = "账号操作") {
                SettingsClickableItem(
                    icon = Icons.Default.ExitToApp,
                    title = "退出登录",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        showLogoutDialog = true
                    }
                )
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
