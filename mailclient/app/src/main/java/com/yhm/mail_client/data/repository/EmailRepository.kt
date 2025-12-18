package com.yhm.mail_client.data.repository

import android.util.Log
import com.yhm.mail_client.data.local.AccountDao
import com.yhm.mail_client.data.local.EmailDao
import com.yhm.mail_client.data.model.Email
import com.yhm.mail_client.data.model.EmailAccount
import com.yhm.mail_client.data.network.Pop3Client
import com.yhm.mail_client.data.network.SmtpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class EmailRepository(
    private val emailDao: EmailDao,
    private val accountDao: AccountDao
) {
    companion object {
        private const val TAG = "EmailRepository"
    }
    
    // Account operations
    fun getAllAccounts(): Flow<List<EmailAccount>> = accountDao.getAllAccounts()
    
    suspend fun getAccountById(id: Long): EmailAccount? = accountDao.getAccountById(id)
    
    suspend fun getDefaultAccount(): EmailAccount? = accountDao.getDefaultAccount()
    
    suspend fun insertAccount(account: EmailAccount): Long {
        val id = accountDao.insertAccount(account)
        if (account.isDefault) {
            accountDao.clearDefaultAccount()
            accountDao.setDefaultAccount(id)
        }
        return id
    }
    
    suspend fun updateAccount(account: EmailAccount) {
        accountDao.updateAccount(account)
        if (account.isDefault) {
            accountDao.clearDefaultAccount()
            accountDao.setDefaultAccount(account.id)
        }
    }
    
    suspend fun deleteAccount(account: EmailAccount) {
        emailDao.deleteEmailsByAccount(account.id)
        accountDao.deleteAccount(account)
    }
    
    suspend fun setDefaultAccount(id: Long) {
        accountDao.clearDefaultAccount()
        accountDao.setDefaultAccount(id)
    }
    
    // Email operations
    fun getEmailsByAccount(accountId: Long): Flow<List<Email>> = 
        emailDao.getEmailsByAccount(accountId)
    
    suspend fun getEmailByUid(uid: String): Email? = emailDao.getEmailByUid(uid)
    
    suspend fun markAsRead(uid: String, isRead: Boolean = true) {
        emailDao.markAsRead(uid, isRead)
    }
    
    suspend fun markAsDeleted(uid: String) {
        emailDao.markAsDeleted(uid)
    }
    
    // POP3 operations
    suspend fun testConnection(account: EmailAccount): Result<String> {
        val client = Pop3Client(account)
        return try {
            client.connect().getOrThrow()
            client.login().getOrThrow()
            val (count, size) = client.getEmailCount().getOrThrow()
            client.disconnect()
            Result.success("Success! Found $count emails ($size bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun syncEmails(account: EmailAccount): Result<Int> {
        val client = Pop3Client(account)
        return try {
            client.connect().getOrThrow()
            client.login().getOrThrow()
            
            val messages = client.listMessages().getOrThrow()
            Log.d(TAG, "Syncing ${messages.size} messages for account ${account.email}")
            
            var newEmailCount = 0
            for ((msgNum, _) in messages) {
                try {
                    val email = client.retrieveMessage(msgNum).getOrThrow()
                    
                    // Check if email already exists
                    val existing = emailDao.getEmailByUid(email.uid)
                    if (existing == null) {
                        emailDao.insertEmail(email)
                        newEmailCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to retrieve message $msgNum", e)
                    // Continue with next message
                }
            }
            
            client.disconnect()
            Log.d(TAG, "Sync complete: $newEmailCount new emails")
            Result.success(newEmailCount)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            client.disconnect()
            Result.failure(e)
        }
    }
    
    suspend fun deleteEmailFromServer(email: Email): Result<Unit> {
        val account = accountDao.getAccountById(email.accountId) ?: 
            return Result.failure(Exception("Account not found"))
        
        val client = Pop3Client(account)
        return try {
            client.connect().getOrThrow()
            client.login().getOrThrow()
            client.deleteMessage(email.messageNumber).getOrThrow()
            client.disconnect()
            
            // Mark as deleted locally
            emailDao.markAsDeleted(email.uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete email from server", e)
            client.disconnect()
            Result.failure(e)
        }
    }
    
    // SMTP operations
    suspend fun testSmtpConnection(account: EmailAccount): Result<String> {
        val client = SmtpClient(account)
        return try {
            client.connect().getOrThrow()
            client.hello().getOrThrow()
            client.disconnect()
            Result.success("SMTP连接成功!")
        } catch (e: Exception) {
            Log.e(TAG, "SMTP connection test failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun sendEmail(
        account: EmailAccount,
        from: String,
        to: List<String>,
        subject: String,
        body: String
    ): Result<Unit> {
        val client = SmtpClient(account)
        return try {
            Log.d(TAG, "Sending email from $from to $to")
            client.sendEmail(from, to, subject, body).getOrThrow()
            Log.d(TAG, "Email sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email", e)
            Result.failure(e)
        }
    }
}
