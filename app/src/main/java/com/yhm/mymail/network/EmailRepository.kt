package com.yhm.mymail.network

import com.yhm.mymail.data.Email
import com.yhm.mymail.data.EmailAccount

/**
 * 邮件仓库层
 * 封装 POP3 操作，提供统一接口
 */
class EmailRepository {
    
    private val pop3Client = Pop3Client()
    private var currentAccount: EmailAccount? = null
    
    /**
     * 连接到邮件服务器
     */
    suspend fun connect(account: EmailAccount): Result<Unit> {
        currentAccount = account
        return pop3Client.connect(account)
    }
    
    /**
     * 获取邮箱状态
     */
    suspend fun getMailboxStatus(): Result<Pair<Int, Int>> {
        return pop3Client.getMailboxStatus()
    }
    
    /**
     * 获取邮件列表
     */
    suspend fun fetchEmails(): Result<List<Email>> {
        return pop3Client.fetchEmailList()
    }
    
    /**
     * 获取单封邮件详情
     */
    suspend fun fetchEmail(messageNumber: Int): Result<Email> {
        return pop3Client.fetchEmail(messageNumber)
    }
    
    /**
     * 删除邮件
     */
    suspend fun deleteEmail(messageNumber: Int): Result<Unit> {
        return pop3Client.deleteEmail(messageNumber)
    }
    
    /**
     * 断开连接
     */
    suspend fun disconnect() {
        pop3Client.disconnect()
        currentAccount = null
    }
    
    /**
     * 刷新邮件列表（重新连接并获取）
     */
    suspend fun refresh(): Result<List<Email>> {
        val account = currentAccount ?: return Result.failure(Exception("未配置账户"))
        
        // 断开当前连接
        disconnect()
        
        // 重新连接
        val connectResult = connect(account)
        if (connectResult.isFailure) {
            return Result.failure(connectResult.exceptionOrNull()!!)
        }
        
        // 获取邮件列表
        return fetchEmails()
    }
}
