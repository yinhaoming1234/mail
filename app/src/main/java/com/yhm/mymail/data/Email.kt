package com.yhm.mymail.data

/**
 * 邮件数据模型
 */
data class Email(
    val id: String,
    val messageNumber: Int,
    val from: String,
    val to: String,
    val subject: String,
    val content: String,
    val date: Long,
    val size: Int,
    val isRead: Boolean = false
)
