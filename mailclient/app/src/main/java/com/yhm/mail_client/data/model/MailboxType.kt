package com.yhm.mail_client.data.model

/**
 * 邮箱类型枚举
 */
enum class MailboxType(val displayName: String, val value: String) {
    INBOX("收件箱", "INBOX"),
    SENT("已发送", "SENT"),
    DRAFT("草稿箱", "DRAFT"),
    STARRED("星标邮件", "STARRED");
    
    companion object {
        fun fromValue(value: String): MailboxType {
            return values().find { it.value == value } ?: INBOX
        }
    }
}
