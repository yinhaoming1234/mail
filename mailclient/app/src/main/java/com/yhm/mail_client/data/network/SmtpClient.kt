package com.yhm.mail_client.data.network

import android.util.Log
import com.yhm.mail_client.data.model.EmailAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLSocketFactory

/**
 * SMTP 客户端
 * 实现 SMTP 协议用于发送邮件
 */
class SmtpClient(private val account: EmailAccount) {
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    
    companion object {
        private const val TAG = "SmtpClient"
        private const val TIMEOUT = 10000 // 10 seconds
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY = 1000L // 1 second
    }
    
    /**
     * Retry wrapper for network operations
     */
    private suspend fun <T> withRetry(
        maxRetries: Int = MAX_RETRIES,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null
        var delay = INITIAL_RETRY_DELAY
        
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                if (result.isSuccess) {
                    return result
                }
                lastException = result.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1}/$maxRetries failed: ${e.message}")
            }
            
            if (attempt < maxRetries - 1) {
                kotlinx.coroutines.delay(delay)
                delay *= 2 // Exponential backoff
            }
        }
        
        return Result.failure(lastException ?: Exception("Operation failed after $maxRetries retries"))
    }
    
    /**
     * 连接到 SMTP 服务器
     */
    suspend fun connect(): Result<Unit> = withRetry {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting to ${account.smtpHost}:${account.smtpPort} (SSL: ${account.smtpUseSsl})")
                
                socket = if (account.smtpUseSsl) {
                    val factory = SSLSocketFactory.getDefault()
                    factory.createSocket(account.smtpHost, account.smtpPort)
                } else {
                    Socket(account.smtpHost, account.smtpPort)
                }
                
                socket?.soTimeout = TIMEOUT
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                writer = PrintWriter(socket?.getOutputStream(), true)
                
                // Read greeting (220 response)
                val greeting = readResponse()
                Log.d(TAG, "Server greeting: $greeting")
                
                if (!greeting.startsWith("220")) {
                    throw Exception("Invalid server greeting: $greeting")
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                disconnect()
                Result.failure(e)
            }
        }
    }
    
    /**
     * 发送 EHLO/HELO 命令
     */
    suspend fun hello(clientName: String = "localhost"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Try EHLO first (Extended SMTP)
            sendCommand("EHLO $clientName")
            val response = readResponse()
            
            if (response.startsWith("250")) {
                // Read multi-line EHLO response
                readMultiLineResponse(response)
                Log.d(TAG, "EHLO successful")
                Result.success(Unit)
            } else if (response.startsWith("5")) {
                // EHLO not supported, try HELO
                sendCommand("HELO $clientName")
                val heloResponse = readResponse()
                if (heloResponse.startsWith("250")) {
                    Log.d(TAG, "HELO successful")
                    Result.success(Unit)
                } else {
                    throw Exception("HELO failed: $heloResponse")
                }
            } else {
                throw Exception("EHLO/HELO failed: $response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hello command failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * 发送 MAIL FROM 命令
     */
    suspend fun mailFrom(sender: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sendCommand("MAIL FROM: <$sender>")
            val response = readResponse()
            
            if (!response.startsWith("250")) {
                throw Exception("MAIL FROM failed: $response")
            }
            
            Log.d(TAG, "MAIL FROM successful: $sender")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "MAIL FROM failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * 发送 RCPT TO 命令
     */
    suspend fun rcptTo(recipient: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sendCommand("RCPT TO: <$recipient>")
            val response = readResponse()
            
            if (!response.startsWith("250")) {
                throw Exception("RCPT TO failed: $response")
            }
            
            Log.d(TAG, "RCPT TO successful: $recipient")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "RCPT TO failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * 发送邮件数据
     */
    suspend fun data(
        from: String,
        to: List<String>,
        subject: String,
        body: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Send DATA command
            sendCommand("DATA")
            val response = readResponse()
            
            if (!response.startsWith("354")) {
                throw Exception("DATA command failed: $response")
            }
            
            // Build email content
            val emailContent = buildEmailContent(from, to, subject, body)
            
            // Send email content line by line
            val lines = emailContent.split("\n")
            for (line in lines) {
                val trimmedLine = line.trimEnd('\r')
                // Dot stuffing: if line starts with '.', add another '.'
                if (trimmedLine.startsWith(".")) {
                    writer?.println(".$trimmedLine")
                } else {
                    writer?.println(trimmedLine)
                }
            }
            
            // Send end of data marker
            writer?.println(".")
            writer?.flush()
            
            val dataResponse = readResponse()
            if (!dataResponse.startsWith("250")) {
                throw Exception("Email data not accepted: $dataResponse")
            }
            
            Log.d(TAG, "Email sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "DATA command failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * 构建邮件内容 (RFC 5322 格式)
     */
    private fun buildEmailContent(
        from: String,
        to: List<String>,
        subject: String,
        body: String
    ): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        val date = dateFormat.format(Date())
        
        return buildString {
            // Headers
            appendLine("From: <$from>")
            appendLine("To: ${to.joinToString(", ") { "<$it>" }}")
            appendLine("Subject: $subject")
            appendLine("Date: $date")
            appendLine("MIME-Version: 1.0")
            appendLine("Content-Type: text/plain; charset=utf-8")
            appendLine("Content-Transfer-Encoding: 8bit")
            appendLine() // Empty line to separate headers from body
            
            // Body
            append(body)
        }
    }
    
    /**
     * 发送完整邮件 (便捷方法)
     */
    suspend fun sendEmail(
        from: String,
        to: List<String>,
        subject: String,
        body: String
    ): Result<Unit> = withRetry {
        try {
            // Connect
            connect().getOrThrow()
            
            // Say hello
            hello().getOrThrow()
            
            // Set sender
            mailFrom(from).getOrThrow()
            
            // Set recipients
            for (recipient in to) {
                rcptTo(recipient).getOrThrow()
            }
            
            // Send email content
            data(from, to, subject, body).getOrThrow()
            
            // Disconnect
            disconnect()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email", e)
            disconnect()
            Result.failure(e)
        }
    }
    
    /**
     * 断开连接
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            sendCommand("QUIT")
            readResponse()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            reader?.close()
            writer?.close()
            socket?.close()
            Log.d(TAG, "Disconnected")
        }
    }
    
    /**
     * 发送命令
     */
    private fun sendCommand(command: String) {
        Log.d(TAG, "Sending: $command")
        writer?.println(command)
    }
    
    /**
     * 读取响应
     */
    private fun readResponse(): String {
        val response = reader?.readLine() ?: ""
        Log.d(TAG, "Received: $response")
        return response
    }
    
    /**
     * 读取多行响应 (用于 EHLO)
     */
    private fun readMultiLineResponse(firstLine: String) {
        var line = firstLine
        // Multi-line response has format "250-..." for continuation, "250 ..." for last line
        while (line.length >= 4 && line[3] == '-') {
            line = readResponse()
        }
    }
}
