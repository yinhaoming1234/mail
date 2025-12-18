package com.yhm.mail_client.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.yhm.mail_client.data.local.AppDatabase
import com.yhm.mail_client.data.model.Email
import com.yhm.mail_client.data.model.EmailAccount
import com.yhm.mail_client.data.repository.EmailRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmailViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "mail_database"
    ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    
    private val repository = EmailRepository(
        emailDao = database.emailDao(),
        accountDao = database.accountDao()
    )
    
    // UI State
    private val _uiState = MutableStateFlow(EmailUiState())
    val uiState: StateFlow<EmailUiState> = _uiState.asStateFlow()
    
    // Current account
    private val _currentAccount = MutableStateFlow<EmailAccount?>(null)
    val currentAccount: StateFlow<EmailAccount?> = _currentAccount.asStateFlow()
    
    // Emails for current account
    val emails: StateFlow<List<Email>> = currentAccount
        .flatMapLatest { account ->
            if (account != null) {
                repository.getEmailsByAccount(account.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // All accounts
    val accounts: StateFlow<List<EmailAccount>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    init {
        loadDefaultAccount()
    }
    
    private fun loadDefaultAccount() {
        viewModelScope.launch {
            val account = repository.getDefaultAccount()
            _currentAccount.value = account
        }
    }
    
    fun saveAccount(account: EmailAccount) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val id = if (account.id == 0L) {
                    repository.insertAccount(account)
                } else {
                    repository.updateAccount(account)
                    account.id
                }
                
                if (account.isDefault) {
                    val savedAccount = repository.getAccountById(id)
                    _currentAccount.value = savedAccount
                }
                
                _uiState.update { it.copy(isLoading = false, accountSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun testConnection(account: EmailAccount) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, testResult = null) }
            try {
                val result = repository.testConnection(account)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        testResult = result.getOrNull(),
                        error = result.exceptionOrNull()?.message
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun syncEmails() {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            try {
                val result = repository.syncEmails(account)
                result.fold(
                    onSuccess = { count ->
                        _uiState.update { 
                            it.copy(
                                isSyncing = false,
                                syncMessage = "Synced $count new emails"
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(isSyncing = false, error = error.message) 
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, error = e.message) }
            }
        }
    }
    
    fun markAsRead(uid: String, isRead: Boolean = true) {
        viewModelScope.launch {
            repository.markAsRead(uid, isRead)
        }
    }
    
    fun deleteEmail(email: Email) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteEmailFromServer(email)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun selectAccount(account: EmailAccount) {
        _currentAccount.value = account
    }
    
    fun logout() {
        viewModelScope.launch {
            // Clear current account session
            _currentAccount.value = null
            // Reset UI state
            _uiState.value = EmailUiState()
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearTestResult() {
        _uiState.update { it.copy(testResult = null) }
    }
    
    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }
    
    fun resetAccountSaved() {
        _uiState.update { it.copy(accountSaved = false) }
    }
    
    fun sendEmail(to: String, subject: String, body: String) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            _uiState.update { it.copy(isSending = true, error = null, sendSuccess = false) }
            
            try {
                // Parse recipients (comma-separated)
                val recipients = to.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (recipients.isEmpty()) {
                    throw Exception("请输入至少一个收件人")
                }
                
                val result = repository.sendEmail(
                    account = account,
                    from = account.email,
                    to = recipients,
                    subject = subject,
                    body = body
                )
                
                result.fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(
                                isSending = false,
                                sendSuccess = true
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(isSending = false, error = error.message) 
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, error = e.message) }
            }
        }
    }
    
    fun clearSendSuccess() {
        _uiState.update { it.copy(sendSuccess = false) }
    }
}

data class EmailUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val testResult: String? = null,
    val syncMessage: String? = null,
    val accountSaved: Boolean = false,
    val sendSuccess: Boolean = false
)
