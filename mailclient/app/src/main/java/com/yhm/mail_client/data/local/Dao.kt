package com.yhm.mail_client.data.local

import androidx.room.*
import com.yhm.mail_client.data.model.Email
import com.yhm.mail_client.data.model.EmailAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails WHERE accountId = :accountId AND isDeleted = 0 ORDER BY date DESC")
    fun getEmailsByAccount(accountId: Long): Flow<List<Email>>
    
    @Query("SELECT * FROM emails WHERE uid = :uid")
    suspend fun getEmailByUid(uid: String): Email?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: Email)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmails(emails: List<Email>)
    
    @Update
    suspend fun updateEmail(email: Email)
    
    @Query("UPDATE emails SET isRead = :isRead WHERE uid = :uid")
    suspend fun markAsRead(uid: String, isRead: Boolean)
    
    @Query("UPDATE emails SET isDeleted = 1 WHERE uid = :uid")
    suspend fun markAsDeleted(uid: String)
    
    @Query("DELETE FROM emails WHERE accountId = :accountId")
    suspend fun deleteEmailsByAccount(accountId: Long)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, createdAt DESC")
    fun getAllAccounts(): Flow<List<EmailAccount>>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): EmailAccount?
    
    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): EmailAccount?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: EmailAccount): Long
    
    @Update
    suspend fun updateAccount(account: EmailAccount)
    
    @Delete
    suspend fun deleteAccount(account: EmailAccount)
    
    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefaultAccount()
    
    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultAccount(id: Long)
}
