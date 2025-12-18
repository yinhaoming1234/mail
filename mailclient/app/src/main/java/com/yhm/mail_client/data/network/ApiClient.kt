package com.yhm.mail_client.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * API 客户端
 * 用于与服务器管理后台通信
 */
class ApiClient(
    private val baseUrl: String = ServerConfig.API_BASE_URL
) {
    companion object {
        private const val TAG = "ApiClient"
        private const val TIMEOUT = 15000
    }

    /**
     * 用户注册
     */
    suspend fun register(
        username: String,
        domain: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/users/register")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val jsonBody = JSONObject().apply {
                put("username", username)
                put("domain", domain)
                put("password", password)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Register response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { 
                    it.readText() 
                }
                Result.success(response)
            } else {
                val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { 
                    it.readText() 
                }
                val errorJson = JSONObject(errorResponse)
                val errorMessage = errorJson.optString("message", "注册失败")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Register failed", e)
            Result.failure(e)
        }
    }

    /**
     * 用户登录验证
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/users/login")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Login response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { 
                    it.readText() 
                }
                Result.success(JSONObject(response))
            } else {
                val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { 
                    it.readText() 
                }
                val errorJson = JSONObject(errorResponse)
                val errorMessage = errorJson.optString("message", "登录失败")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(e)
        }
    }

    /**
     * 修改密码
     */
    suspend fun changePassword(
        email: String,
        currentPassword: String,
        newPassword: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/users/change-password")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val jsonBody = JSONObject().apply {
                put("email", email)
                put("currentPassword", currentPassword)
                put("newPassword", newPassword)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Change password response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Result.success("密码修改成功")
            } else {
                val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { 
                    it.readText() 
                }
                val errorJson = JSONObject(errorResponse)
                val errorMessage = errorJson.optString("message", "密码修改失败")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Change password failed", e)
            Result.failure(e)
        }
    }

    /**
     * 获取可用域名列表
     */
    suspend fun getAvailableDomains(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/domains")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Get domains response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { 
                    it.readText() 
                }
                val jsonArray = org.json.JSONArray(response)
                val domains = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val domain = jsonArray.getJSONObject(i).getString("domain")
                    domains.add(domain)
                }
                Result.success(domains)
            } else {
                Result.failure(Exception("获取域名列表失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get domains failed", e)
            // 返回默认域名列表
            Result.success(listOf("localhost", "example.com"))
        }
    }
}
