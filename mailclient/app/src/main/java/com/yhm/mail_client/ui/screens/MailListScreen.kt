package com.yhm.mail_client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yhm.mail_client.R
import com.yhm.mail_client.data.model.Email
import com.yhm.mail_client.data.model.MailboxType
import com.yhm.mail_client.ui.viewmodel.EmailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailListScreen(
    viewModel: EmailViewModel,
    onEmailClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onComposeClick: (String?) -> Unit
) {
    val emails by viewModel.emails.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()
    val currentMailbox by viewModel.currentMailboxType.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentAccount?.name ?: stringResource(R.string.mail_title),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                Divider()
                
                // Inbox
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                    label = { Text(stringResource(R.string.mailbox_inbox)) },
                    selected = currentMailbox == MailboxType.INBOX,
                    onClick = {
                        viewModel.selectMailbox(MailboxType.INBOX)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                // Sent
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Send, contentDescription = null) },
                    label = { Text(stringResource(R.string.mailbox_sent)) },
                    selected = currentMailbox == MailboxType.SENT,
                    onClick = {
                        viewModel.selectMailbox(MailboxType.SENT)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                // Drafts
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Drafts, contentDescription = null) },
                    label = { Text(stringResource(R.string.mailbox_drafts)) },
                    selected = currentMailbox == MailboxType.DRAFT,
                    onClick = {
                        viewModel.selectMailbox(MailboxType.DRAFT)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                // Starred
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text(stringResource(R.string.mailbox_starred)) },
                    selected = currentMailbox == MailboxType.STARRED,
                    onClick = {
                        viewModel.selectMailbox(MailboxType.STARRED)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Settings
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        onSettingsClick()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(currentMailbox.displayName) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.open_drawer)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        if (currentMailbox == MailboxType.INBOX) {
                            IconButton(onClick = { viewModel.syncEmails() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.sync_emails)
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (currentAccount != null && currentMailbox != MailboxType.SENT) {
                FloatingActionButton(
                    onClick = { onComposeClick(null) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.compose_email)
                    )
                }
            }    }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    currentAccount == null -> {
                        // No account setup
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_email_account),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.add_account_to_start),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onSettingsClick) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.add_account))
                            }
                        }
                    }
                    emails.isEmpty() && !uiState.isSyncing -> {
                        // No emails
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val icon = when (currentMailbox) {
                                MailboxType.INBOX -> Icons.Default.Inbox
                                MailboxType.SENT -> Icons.Default.Send
                                MailboxType.DRAFT -> Icons.Default.Drafts
                                MailboxType.STARRED -> Icons.Default.Star
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_emails),
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (currentMailbox == MailboxType.INBOX) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.pull_to_refresh),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        // Show email list
                        PullToRefreshBox(
                            isRefreshing = uiState.isSyncing,
                            onRefresh = { 
                                if (currentMailbox == MailboxType.INBOX) {
                                    viewModel.syncEmails() 
                                }
                            }
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(emails, key = { it.uid }) { email ->
                                    EmailListItem(
                                        email = email,
                                        onClick = { 
                                            // If it's a draft, open in compose screen for editing
                                            if (email.isDraft) {
                                                onComposeClick(email.uid)
                                            } else {
                                                onEmailClick(email.uid)
                                            }
                                        },
                                        onStarClick = {
                                            viewModel.toggleStarred(email.uid, email.isStarred)
                                        }
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
                
                // Show sync message
                uiState.syncMessage?.let { message ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearSyncMessage() }) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                    ) {
                        Text(message)
                    }
                }
                
                // Show error
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                    ) {
                        Text(stringResource(R.string.error_prefix, error))
                    }
                }
            }
        }
    }
}

@Composable
fun EmailListItem(
    email: Email,
    onClick: () -> Unit,
    onStarClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (email.isRead) 
            MaterialTheme.colorScheme.surface 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.from,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormat.format(Date(email.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = email.subject,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = email.content.take(100).replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Star icon
            IconButton(
                onClick = onStarClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (email.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (email.isStarred) 
                        stringResource(R.string.unstar) 
                    else 
                        stringResource(R.string.star),
                    tint = if (email.isStarred) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
