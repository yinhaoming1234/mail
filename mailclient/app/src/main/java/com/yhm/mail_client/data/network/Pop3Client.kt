package com.yhm.mail_client.data.network

import android.util.Log
import com.yhm.mail_client.data.model.Email
import com.yhm.mail_client.data.model.EmailAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.*
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class Pop3Client(private val account: EmailAccount) {
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    
    companion object {
        private const val TAG = "Pop3Client"
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
    
    suspend fun connect(): Result<Unit> = withRetry {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting to ${account.pop3Host}:${account.pop3Port} (SSL: ${account.useSsl})")
                
                socket = if (account.useSsl) {
                    val factory = SSLSocketFactory.getDefault()
                    factory.createSocket(account.pop3Host, account.pop3Port)
                } else {
                    Socket(account.pop3Host, account.pop3Port)
                }
                
                socket?.soTimeout = TIMEOUT
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                writer = PrintWriter(socket?.getOutputStream(), true)
                
                // Read greeting
                val greeting = reader?.readLine()
                Log.d(TAG, "Server greeting: $greeting")
                
                if (greeting?.startsWith("+OK") != true) {
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
    
    suspend fun login(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Send USER command
            sendCommand("USER ${account.username}")
            val userResponse = readResponse()
            if (!userResponse.startsWith("+OK")) {
                throw Exception("USER command failed: $userResponse")
            }
            
            // Send PASS command
            sendCommand("PASS ${account.password}")
            val passResponse = readResponse()
            if (!passResponse.startsWith("+OK")) {
                throw Exception("Authentication failed: $passResponse")
            }
            
            Log.d(TAG, "Login successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun getEmailCount(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            sendCommand("STAT")
            val response = readResponse()
            
            if (!response.startsWith("+OK")) {
                throw Exception("STAT command failed: $response")
            }
            
            // Parse "+OK 10 12345" -> 10 messages, 12345 bytes
            val parts = response.split(" ")
            val count = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val size = parts.getOrNull(2)?.toIntOrNull() ?: 0
            
            Log.d(TAG, "Email count: $count, Total size: $size")
            Result.success(Pair(count, size))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get email count", e)
            Result.failure(e)
        }
    }
    
    suspend fun listMessages(): Result<List<Pair<Int, Int>>> = withContext(Dispatchers.IO) {
        try {
            sendCommand("LIST")
            val response = readResponse()
            
            if (!response.startsWith("+OK")) {
                throw Exception("LIST command failed: $response")
            }
            
            val messages = mutableListOf<Pair<Int, Int>>()
            var line = reader?.readLine()
            
            while (line != null && line != ".") {
                val parts = line.trim().split(" ")
                if (parts.size >= 2) {
                    val msgNum = parts[0].toIntOrNull()
                    val msgSize = parts[1].toIntOrNull()
                    if (msgNum != null && msgSize != null) {
                        messages.add(Pair(msgNum, msgSize))
                    }
                }
                line = reader?.readLine()
            }
            
            Log.d(TAG, "Listed ${messages.size} messages")
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list messages", e)
            Result.failure(e)
        }
    }
    
    suspend fun retrieveMessage(messageNumber: Int): Result<Email> = withContext(Dispatchers.IO) {
        try {
            sendCommand("RETR $messageNumber")
            val response = readResponse()
            
            if (!response.startsWith("+OK")) {
                throw Exception("RETR command failed: $response")
            }
            
            val messageLines = StringBuilder()
            var line = reader?.readLine()
            
            while (line != null && line != ".") {
                messageLines.append(line).append("\r\n")
                line = reader?.readLine()
            }
            
            val email = parseMessage(messageLines.toString(), messageNumber)
            Log.d(TAG, "Retrieved message $messageNumber: ${email.subject}")
            
            Result.success(email)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve message $messageNumber", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteMessage(messageNumber: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sendCommand("DELE $messageNumber")
            val response = readResponse()
            
            if (!response.startsWith("+OK")) {
                throw Exception("DELE command failed: $response")
            }
            
            Log.d(TAG, "Deleted message $messageNumber")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message $messageNumber", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get unique IDs for all messages using UIDL command
     * Returns map of message number to unique ID
     */
    suspend fun getUniqueIds(): Result<Map<Int, String>> = withContext(Dispatchers.IO) {
        try {
            sendCommand("UIDL")
            val response = readResponse()
            
            if (!response.startsWith("+OK")) {
                throw Exception("UIDL command failed: $response")
            }
            
            val uidMap = mutableMapOf<Int, String>()
            var line = reader?.readLine()
            
            while (line != null && line != ".") {
                val parts = line.trim().split(" ", limit = 2)
                if (parts.size >= 2) {
                    val msgNum = parts[0].toIntOrNull()
                    val uid = parts[1]
                    if (msgNum != null) {
                        uidMap[msgNum] = uid
                    }
                }
                line = reader?.readLine()
            }
            
            Log.d(TAG, "Retrieved ${uidMap.size} unique IDs")
            Result.success(uidMap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unique IDs", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get unique ID for a specific message
     */
    suspend fun getUniqueId(messageNumber: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            sendCommand("UIDL $messageNumber")
            val response = readResponse()
            
            if (!response.startsWith("+OK")) {
                throw Exception("UIDL command failed: $response")
            }
            
            // Parse "+OK msgnum uid"
            val parts = response.split(" ")
            if (parts.size >= 3) {
                val uid = parts[2]
                Log.d(TAG, "Retrieved UID for message $messageNumber: $uid")
                Result.success(uid)
            } else {
                throw Exception("Invalid UIDL response: $response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unique ID for message $messageNumber", e)
            Result.failure(e)
        }
    }
    
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
    
    private fun sendCommand(command: String) {
        Log.d(TAG, "Sending: $command")
        writer?.println(command)
    }
    
    private fun readResponse(): String {
        val response = reader?.readLine() ?: ""
        Log.d(TAG, "Received: $response")
        return response
    }
    
    /**
     * Parse email message and get server UIDL
     */
    private suspend fun parseMessage(rawMessage: String, messageNumber: Int): Email {
        val session = Session.getDefaultInstance(Properties())
        val mimeMessage = MimeMessage(session, rawMessage.byteInputStream())
        
        val from = mimeMessage.from?.firstOrNull()?.toString() ?: "Unknown"
        val to = mimeMessage.allRecipients?.joinToString(", ") ?: ""
        val subject = mimeMessage.subject ?: "(No Subject)"
        val date = mimeMessage.sentDate?.time ?: System.currentTimeMillis()
        
        var content = ""
        var contentType = "text/plain"
        
        try {
            when {
                mimeMessage.isMimeType("text/plain") -> {
                    content = mimeMessage.content.toString()
                    contentType = "text/plain"
                }
                mimeMessage.isMimeType("text/html") -> {
                    content = mimeMessage.content.toString()
                    contentType = "text/html"
                }
                mimeMessage.isMimeType("multipart/*") -> {
                    val multipart = mimeMessage.content as? javax.mail.Multipart
                    for (i in 0 until (multipart?.count ?: 0)) {
                        val bodyPart = multipart?.getBodyPart(i)
                        if (bodyPart?.isMimeType("text/plain") == true && content.isEmpty()) {
                            content = bodyPart.content.toString()
                            contentType = "text/plain"
                        } else if (bodyPart?.isMimeType("text/html") == true) {
                            content = bodyPart.content.toString()
                            contentType = "text/html"
                        }
                    }
                }
                else -> {
                    content = "Unsupported content type: ${mimeMessage.contentType}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message content", e)
            content = "Error parsing message: ${e.message}"
        }
        
        // Try to get server UIDL, fallback to local generation
        val uid = try {
            val uidlResult = getUniqueId(messageNumber)
            if (uidlResult.isSuccess) {
                "${account.id}_${uidlResult.getOrNull()}"
            } else {
                "${account.id}_${messageNumber}_${date}"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get UIDL for message $messageNumber, using fallback", e)
            "${account.id}_${messageNumber}_${date}"
        }
        
        return Email(
            uid = uid,
            accountId = account.id,
            messageNumber = messageNumber,
            from = from,
            to = to,
            subject = subject,
            date = date,
            content = content,
            contentType = contentType,
            size = rawMessage.length,
            isRead = false
        )
    }
}
