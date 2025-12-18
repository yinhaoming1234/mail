package com.yhm.mail_client.data.network

/**
 * 服务器配置
 * 集中管理所有服务器地址配置
 */
object ServerConfig {
    // 服务器IP地址 - 真机调试时使用
    const val SERVER_IP = "192.168.1.106"
    
    // 管理后台API地址
    const val API_BASE_URL = "http://$SERVER_IP:8000"
    
    // SMTP服务器配置
    const val SMTP_HOST = SERVER_IP
    const val SMTP_PORT = 2525
    const val SMTP_USE_SSL = false
    
    // POP3服务器配置
    const val POP3_HOST = SERVER_IP
    const val POP3_PORT = 1100
    const val POP3_USE_SSL = false
    
    // 默认域名
    const val DEFAULT_DOMAIN = "localhost"
    
    /**
     * 获取完整的API URL
     */
    fun getApiUrl(path: String): String {
        return "$API_BASE_URL$path"
    }
}
