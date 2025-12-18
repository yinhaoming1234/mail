package com.yhm.mymail.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhm.mymail.data.Email
import com.yhm.mymail.data.EmailAccount
import com.yhm.mymail.network.EmailRepository
import kotlinx.coroutines.launch

/**
 * UI 状态
 */
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/**
 * 邮件应用 ViewModel
 */
class EmailViewModel : ViewModel() {
    
    private val repository = EmailRepository()
    
    // 登录状态
    var loginState by mutableStateOf<UiState<Unit>>(UiState.Idle)
        private set
    
    // 邮件列表状态
    var emailListState by mutableStateOf<UiState<List<Email>>>(UiState.Idle)
        private set
    
    // 邮件详情状态
    var emailDetailState by mutableStateOf<UiState<Email>>(UiState.Idle)
        private set
    
    // 当前账户配置
    var currentAccount by mutableStateOf<EmailAccount?>(null)
        private set
    
    // 是否已登录
    val isLoggedIn: Boolean
        get() = loginState is UiState.Success
    
    // 表单输入状态
    var hostInput by mutableStateOf("10.0.2.2") // Android 模拟器访问主机的地址
        private set
    var portInput by mutableStateOf("1100")
        private set
    var usernameInput by mutableStateOf("")
        private set
    var passwordInput by mutableStateOf("")
        private set
    
    fun updateHost(value: String) { hostInput = value }
    fun updatePort(value: String) { portInput = value }
    fun updateUsername(value: String) { usernameInput = value }
    fun updatePassword(value: String) { passwordInput = value }
    
    /**
     * 连接到邮件服务器
     */
    fun connect() {
        val port = portInput.toIntOrNull() ?: 110
        val account = EmailAccount(
            host = hostInput,
            port = port,
            username = usernameInput,
            password = passwordInput
        )
        
        viewModelScope.launch {
            loginState = UiState.Loading
            
            val result = repository.connect(account)
            
            loginState = if (result.isSuccess) {
                currentAccount = account
                UiState.Success(Unit)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "连接失败")
            }
        }
    }
    
    /**
     * 获取邮件列表
     */
    fun fetchEmails() {
        viewModelScope.launch {
            emailListState = UiState.Loading
            
            val result = repository.fetchEmails()
            
            emailListState = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "获取邮件失败")
            }
        }
    }
    
    /**
     * 刷新邮件列表
     */
    fun refreshEmails() {
        viewModelScope.launch {
            emailListState = UiState.Loading
            
            val result = repository.refresh()
            
            emailListState = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "刷新失败")
            }
        }
    }
    
    /**
     * 获取邮件详情
     */
    fun fetchEmailDetail(messageNumber: Int) {
        viewModelScope.launch {
            emailDetailState = UiState.Loading
            
            val result = repository.fetchEmail(messageNumber)
            
            emailDetailState = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "获取邮件详情失败")
            }
        }
    }
    
    /**
     * 删除邮件
     */
    fun deleteEmail(messageNumber: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteEmail(messageNumber)
            
            if (result.isSuccess) {
                // 刷新列表
                refreshEmails()
                onSuccess()
            }
        }
    }
    
    /**
     * 清除邮件详情状态
     */
    fun clearEmailDetail() {
        emailDetailState = UiState.Idle
    }
    
    /**
     * 登出
     */
    fun logout() {
        viewModelScope.launch {
            repository.disconnect()
            loginState = UiState.Idle
            emailListState = UiState.Idle
            emailDetailState = UiState.Idle
            currentAccount = null
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.disconnect()
        }
    }
}
