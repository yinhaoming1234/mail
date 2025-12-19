package com.yhm.mail_client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yhm.mail_client.R
import com.yhm.mail_client.ui.viewmodel.EmailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeEmailScreen(
    viewModel: EmailViewModel,
    onNavigateBack: () -> Unit,
    draftEmail: com.yhm.mail_client.data.model.Email? = null
) {
    var recipients by remember { mutableStateOf(draftEmail?.to ?: "") }
    var subject by remember { mutableStateOf(draftEmail?.subject ?: "") }
    var body by remember { mutableStateOf(draftEmail?.content ?: "") }
    val draftUid = remember { draftEmail?.uid }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Navigate back on successful send
    LaunchedEffect(uiState.sendSuccess) {
        if (uiState.sendSuccess) {
            viewModel.clearSendSuccess()
            onNavigateBack()
        }
    }
    
    // Show draft saved message
    LaunchedEffect(uiState.draftSaved) {
        if (uiState.draftSaved) {
            kotlinx.coroutines.delay(1500)
            viewModel.clearDraftSaved()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compose_email)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Save Draft button
                    IconButton(
                        onClick = {
                            viewModel.saveDraft(
                                to = recipients,
                                subject = subject,
                                body = body,
                                existingDraftUid = draftUid
                            )
                        },
                        enabled = !uiState.isSending && 
                                 (recipients.isNotBlank() || subject.isNotBlank() || body.isNotBlank())
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.save_draft)
                        )
                    }
                    
                    // Send button
                    IconButton(
                        onClick = {
                            viewModel.sendEmail(
                                to = recipients,
                                subject = subject,
                                body = body,
                                draftUid = draftUid
                            )
                        },
                        enabled = !uiState.isSending && 
                                 recipients.isNotBlank() && 
                                 subject.isNotBlank() && 
                                 body.isNotBlank()
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = stringResource(R.string.send)
                            )
                        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Draft indicator
            if (draftEmail != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.load_draft),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Recipients
            OutlinedTextField(
                value = recipients,
                onValueChange = { recipients = it },
                label = { Text(stringResource(R.string.recipients)) },
                placeholder = { Text(stringResource(R.string.recipients_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isSending
            )
            
            // Subject
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(stringResource(R.string.subject)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isSending
            )
            
            // Body
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.message_body)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                maxLines = 15,
                enabled = !uiState.isSending
            )
            
            // Send Button
            Button(
                onClick = {
                    viewModel.sendEmail(
                        to = recipients,
                        subject = subject,
                        body = body,
                        draftUid = draftUid
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSending && 
                         recipients.isNotBlank() && 
                         subject.isNotBlank() && 
                         body.isNotBlank()
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sending))
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.send))
                }
            }
            
            // Draft saved message
            if (uiState.draftSaved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.draft_saved),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Show error
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "错误: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
