package com.yhm.mymail.network

import com.yhm.mymail.data.Email
import com.yhm.mymail.data.EmailAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * POP3 客户端封装类
 * 使用原生 Socket 实现 POP3 协议
 */
class Pop3Client {
    
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private var isConnected = false
    
    /**
     * 连接到 POP3 服务器并进行认证
     */
    suspend fun connect(account: EmailAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 创建 Socket 连接
            socket = Socket(account.host, account.port)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            writer = PrintWriter(socket!!.getOutputStream(), true)
            
            // 读取服务器欢迎信息
            val greeting = reader!!.readLine()
            if (!greeting.startsWith("+OK")) {
                return@withContext Result.failure(Exception("连接失败: $greeting"))
            }
            
            // 发送 USER 命令
            writer!!.println("USER ${account.username}")
            val userResponse = reader!!.readLine()
            if (!userResponse.startsWith("+OK")) {
                return@withContext Result.failure(Exception("用户名错误: $userResponse"))
            }
            
            // 发送 PASS 命令
            writer!!.println("PASS ${account.password}")
            val passResponse = reader!!.readLine()
            if (!passResponse.startsWith("+OK")) {
                return@withContext Result.failure(Exception("密码错误: $passResponse"))
            }
            
            isConnected = true
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }
    
    /**
     * 获取邮箱状态（邮件数量和总大小）
     */
    suspend fun getMailboxStatus(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                return@withContext Result.failure(Exception("未连接到服务器"))
            }
            
            writer!!.println("STAT")
            val response = reader!!.readLine()
            if (!response.startsWith("+OK")) {
                return@withContext Result.failure(Exception("获取状态失败: $response"))
            }
            
            // 解析响应: +OK count size
            val parts = response.split(" ")
            val count = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val size = parts.getOrNull(2)?.toIntOrNull() ?: 0
            
            Result.success(Pair(count, size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取邮件列表
     */
    suspend fun fetchEmailList(): Result<List<Email>> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                return@withContext Result.failure(Exception("未连接到服务器"))
            }
            
            // 先获取邮件数量
            val statusResult = getMailboxStatus()
            if (statusResult.isFailure) {
                return@withContext Result.failure(statusResult.exceptionOrNull()!!)
            }
            val (count, _) = statusResult.getOrThrow()
            
            val emails = mutableListOf<Email>()
            
            // 获取每封邮件的头部信息
            for (i in 1..count) {
                val emailResult = fetchEmailHeaders(i)
                if (emailResult.isSuccess) {
                    emails.add(emailResult.getOrThrow())
                }
            }
            
            Result.success(emails.reversed()) // 最新的邮件在前面
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取邮件头部信息（使用 TOP 命令）
     */
    private suspend fun fetchEmailHeaders(messageNumber: Int): Result<Email> = withContext(Dispatchers.IO) {
        try {
            // 使用 TOP 命令获取邮件头部
            writer!!.println("TOP $messageNumber 0")
            val response = reader!!.readLine()
            if (!response.startsWith("+OK")) {
                return@withContext Result.failure(Exception("获取邮件头失败: $response"))
            }
            
            // 读取邮件头部
            val headers = mutableMapOf<String, String>()
            var line = reader!!.readLine()
            var currentHeader = ""
            var currentValue = StringBuilder()
            
            while (line != null && line != ".") {
                if (line.isEmpty()) {
                    // 保存最后一个头部
                    if (currentHeader.isNotEmpty()) {
                        headers[currentHeader.lowercase()] = currentValue.toString().trim()
                    }
                    break
                }
                
                if (line.startsWith(" ") || line.startsWith("\t")) {
                    // 续行
                    currentValue.append(" ").append(line.trim())
                } else {
                    // 保存前一个头部
                    if (currentHeader.isNotEmpty()) {
                        headers[currentHeader.lowercase()] = currentValue.toString().trim()
                    }
                    
                    // 解析新头部
                    val colonIndex = line.indexOf(':')
                    if (colonIndex > 0) {
                        currentHeader = line.substring(0, colonIndex)
                        currentValue = StringBuilder(line.substring(colonIndex + 1).trim())
                    }
                }
                line = reader!!.readLine()
            }
            
            // 读取剩余内容直到遇到单独的点
            while (line != null && line != ".") {
                line = reader!!.readLine()
            }
            
            // 获取邮件大小
            writer!!.println("LIST $messageNumber")
            val listResponse = reader!!.readLine()
            val size = if (listResponse.startsWith("+OK")) {
                listResponse.split(" ").getOrNull(2)?.toIntOrNull() ?: 0
            } else {
                0
            }
            
            // 获取 UIDL
            writer!!.println("UIDL $messageNumber")
            val uidlResponse = reader!!.readLine()
            val uidl = if (uidlResponse.startsWith("+OK")) {
                uidlResponse.split(" ").getOrNull(2) ?: messageNumber.toString()
            } else {
                messageNumber.toString()
            }
            
            // 解析日期
            val dateStr = headers["date"] ?: ""
            val date = parseDate(dateStr)
            
            val email = Email(
                id = uidl,
                messageNumber = messageNumber,
                from = decodeHeader(headers["from"] ?: "未知发件人"),
                to = decodeHeader(headers["to"] ?: ""),
                subject = decodeHeader(headers["subject"] ?: "(无主题)"),
                content = "", // 头部请求不获取正文
                date = date,
                size = size
            )
            
            Result.success(email)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取完整邮件内容
     */
    suspend fun fetchEmail(messageNumber: Int): Result<Email> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                return@withContext Result.failure(Exception("未连接到服务器"))
            }
            
            writer!!.println("RETR $messageNumber")
            val response = reader!!.readLine()
            if (!response.startsWith("+OK")) {
                return@withContext Result.failure(Exception("获取邮件失败: $response"))
            }
            
            // 读取完整邮件
            val headers = mutableMapOf<String, String>()
            val bodyBuilder = StringBuilder()
            var inBody = false
            var line = reader!!.readLine()
            var currentHeader = ""
            var currentValue = StringBuilder()
            
            while (line != null && line != ".") {
                if (!inBody) {
                    if (line.isEmpty()) {
                        // 保存最后一个头部
                        if (currentHeader.isNotEmpty()) {
                            headers[currentHeader.lowercase()] = currentValue.toString().trim()
                        }
                        inBody = true
                    } else if (line.startsWith(" ") || line.startsWith("\t")) {
                        currentValue.append(" ").append(line.trim())
                    } else {
                        if (currentHeader.isNotEmpty()) {
                            headers[currentHeader.lowercase()] = currentValue.toString().trim()
                        }
                        val colonIndex = line.indexOf(':')
                        if (colonIndex > 0) {
                            currentHeader = line.substring(0, colonIndex)
                            currentValue = StringBuilder(line.substring(colonIndex + 1).trim())
                        }
                    }
                } else {
                    // 处理字节填充 (byte-stuffing)
                    val actualLine = if (line.startsWith("..")) line.substring(1) else line
                    bodyBuilder.appendLine(actualLine)
                }
                line = reader!!.readLine()
            }
            
            // 获取 UIDL
            writer!!.println("UIDL $messageNumber")
            val uidlResponse = reader!!.readLine()
            val uidl = if (uidlResponse.startsWith("+OK")) {
                uidlResponse.split(" ").getOrNull(2) ?: messageNumber.toString()
            } else {
                messageNumber.toString()
            }
            
            val dateStr = headers["date"] ?: ""
            val date = parseDate(dateStr)
            
            val email = Email(
                id = uidl,
                messageNumber = messageNumber,
                from = decodeHeader(headers["from"] ?: "未知发件人"),
                to = decodeHeader(headers["to"] ?: ""),
                subject = decodeHeader(headers["subject"] ?: "(无主题)"),
                content = bodyBuilder.toString().trim(),
                date = date,
                size = 0
            )
            
            Result.success(email)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除邮件
     */
    suspend fun deleteEmail(messageNumber: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                return@withContext Result.failure(Exception("未连接到服务器"))
            }
            
            writer!!.println("DELE $messageNumber")
            val response = reader!!.readLine()
            if (!response.startsWith("+OK")) {
                return@withContext Result.failure(Exception("删除邮件失败: $response"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 断开连接
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            if (isConnected) {
                writer?.println("QUIT")
                reader?.readLine()
            }
        } catch (_: Exception) {
        } finally {
            try {
                reader?.close()
                writer?.close()
                socket?.close()
            } catch (_: Exception) {
            }
            reader = null
            writer = null
            socket = null
            isConnected = false
        }
    }
    
    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String): Long {
        if (dateStr.isEmpty()) return System.currentTimeMillis()
        
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss",
            "dd MMM yyyy HH:mm:ss"
        )
        
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                return sdf.parse(dateStr)?.time ?: continue
            } catch (_: Exception) {
            }
        }
        
        return System.currentTimeMillis()
    }
    
    /**
     * 解码邮件头部（处理 MIME 编码）
     */
    private fun decodeHeader(header: String): String {
        if (!header.contains("=?")) return header
        
        try {
            val regex = "=\\?([^?]+)\\?([BbQq])\\?([^?]*)\\?=".toRegex()
            return regex.replace(header) { match ->
                val charset = match.groupValues[1]
                val encoding = match.groupValues[2].uppercase()
                val encodedText = match.groupValues[3]
                
                try {
                    when (encoding) {
                        "B" -> {
                            val decoded = android.util.Base64.decode(encodedText, android.util.Base64.DEFAULT)
                            String(decoded, charset(charset))
                        }
                        "Q" -> {
                            val decoded = encodedText
                                .replace("_", " ")
                                .replace("=([0-9A-Fa-f]{2})".toRegex()) { m ->
                                    m.groupValues[1].toInt(16).toChar().toString()
                                }
                            decoded
                        }
                        else -> match.value
                    }
                } catch (_: Exception) {
                    match.value
                }
            }
        } catch (_: Exception) {
            return header
        }
    }
}
