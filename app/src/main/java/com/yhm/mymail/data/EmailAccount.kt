package com.yhm.mymail.data

/**
 * POP3 账户配置数据模型
 */
data class EmailAccount(
    val host: String,
    val port: Int = 110,
    val username: String,
    val password: String,
    val useSSL: Boolean = false
)
