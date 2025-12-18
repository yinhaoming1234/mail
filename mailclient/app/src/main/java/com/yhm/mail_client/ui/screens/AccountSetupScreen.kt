package com.yhm.mail_client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yhm.mail_client.R
import com.yhm.mail_client.data.model.EmailAccount
import com.yhm.mail_client.ui.viewmodel.EmailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetupScreen(
    viewModel: EmailViewModel,
    onAccountSaved: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pop3Host by remember { mutableStateOf("") }
    var pop3Port by remember { mutableStateOf("995") }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("25") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var useSsl by remember { mutableStateOf(true) }
    var smtpUseSsl by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.accountSaved) {
        if (uiState.accountSaved) {
            viewModel.resetAccountSaved()
            onAccountSaved()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_email_account)) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.account_information),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.account_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_address)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pop3_server_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                )
                
                // Quick setup button for local server
                OutlinedButton(
                    onClick = {
                        pop3Host = "localhost"
                        pop3Port = "1100"
                        smtpHost = "localhost"
                        smtpPort = "2525"
                        useSsl = false
                        smtpUseSsl = false
                    },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(stringResource(R.string.use_local_server))
                }
            }
            
            OutlinedTextField(
                value = pop3Host,
                onValueChange = { pop3Host = it },
                label = { Text(stringResource(R.string.pop3_host)) },
                placeholder = { Text(stringResource(R.string.pop3_host_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = pop3Port,
                onValueChange = { pop3Port = it },
                label = { Text(stringResource(R.string.port)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useSsl,
                    onCheckedChange = { 
                        useSsl = it
                        // Only auto-change port if it's a standard port
                        if (pop3Port == "995" || pop3Port == "110" || pop3Port == "1100") {
                            pop3Port = if (it) "995" else "110"
                        }
                    }
                )
                Text(stringResource(R.string.use_ssl_tls))
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // SMTP Server Settings
            Text(
                text = stringResource(R.string.smtp_server_settings),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = smtpHost,
                onValueChange = { smtpHost = it },
                label = { Text(stringResource(R.string.smtp_host)) },
                placeholder = { Text(stringResource(R.string.smtp_host_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = smtpPort,
                onValueChange = { smtpPort = it },
                label = { Text(stringResource(R.string.port)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = smtpUseSsl,
                    onCheckedChange = { 
                        smtpUseSsl = it
                        // Only auto-change port if it's a standard port
                        if (smtpPort == "465" || smtpPort == "587" || smtpPort == "25" || smtpPort == "2525") {
                            smtpPort = if (it) "465" else "25"
                        }
                    }
                )
                Text(stringResource(R.string.use_ssl_tls))
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = stringResource(R.string.credentials),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) 
                                Icons.Default.Visibility 
                            else 
                                Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) 
                                stringResource(R.string.hide_password) 
                            else 
                                stringResource(R.string.show_password)
                        )
                    }
                },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Test Connection Button
            Button(
                onClick = {
                    val account = EmailAccount(
                        name = name,
                        email = email,
                        pop3Host = pop3Host,
                        pop3Port = pop3Port.toIntOrNull() ?: 995,
                        smtpHost = smtpHost,
                        smtpPort = smtpPort.toIntOrNull() ?: 25,
                        smtpUseSsl = smtpUseSsl,
                        username = username,
                        password = password,
                        useSsl = useSsl,
                        isDefault = true
                    )
                    viewModel.testConnection(account)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && 
                         name.isNotBlank() && 
                         email.isNotBlank() && 
                         pop3Host.isNotBlank() && 
                         username.isNotBlank() && 
                         password.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.test_connection))
            }
            
            // Save Button
            Button(
                onClick = {
                    val account = EmailAccount(
                        name = name,
                        email = email,
                        pop3Host = pop3Host,
                        pop3Port = pop3Port.toIntOrNull() ?: 995,
                        smtpHost = smtpHost,
                        smtpPort = smtpPort.toIntOrNull() ?: 25,
                        smtpUseSsl = smtpUseSsl,
                        username = username,
                        password = password,
                        useSsl = useSsl,
                        isDefault = true
                    )
                    viewModel.saveAccount(account)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && 
                         name.isNotBlank() && 
                         email.isNotBlank() && 
                         pop3Host.isNotBlank() && 
                         username.isNotBlank() && 
                         password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.save_account))
            }
            
            // Show test result
            uiState.testResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp),
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
                        text = "Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
