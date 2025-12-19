package com.yhm.mail_client.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "emails")
data class Email(
    @PrimaryKey val uid: String,
    val accountId: Long,
    val messageNumber: Int,
    val from: String,
    val to: String,
    val subject: String,
    val date: Long,
    val content: String,
    val contentType: String, // "text/plain" or "text/html"
    val size: Int,
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val receivedDate: Long = System.currentTimeMillis(),
    val mailboxType: String = "INBOX", // INBOX, SENT, DRAFT
    val isStarred: Boolean = false,
    val isDraft: Boolean = false
)

@Entity(tableName = "accounts")
data class EmailAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val pop3Host: String,
    val pop3Port: Int,
    val username: String,
    val password: String, // In production, use Android Keystore
    val useSsl: Boolean = true,
    val smtpHost: String = "localhost",
    val smtpPort: Int = 25,
    val smtpUseSsl: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

