package com.kangkang.shiyangai.network

import com.kangkang.shiyangai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * DeepSeek 大模型客户端。任何异常 / 超时都返回 null，由调用方走本地回退。
 */
object DeepSeekClient {

    private const val URL = "https://api.deepseek.com/chat/completions"
    private const val MODEL = "deepseek-chat"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun chat(systemPrompt: String, userContent: String): String? =
        withContext(Dispatchers.IO) {
            if (BuildConfig.DEEPSEEK_API_KEY.isBlank()) return@withContext null
            try {
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userContent))

                val body = JSONObject()
                    .put("model", MODEL)
                    .put("messages", messages)
                    .put("temperature", 0.5)
                    .put("stream", false)
                    .toString()

                val request = Request.Builder()
                    .url(URL)
                    .addHeader("Authorization", "Bearer ${BuildConfig.DEEPSEEK_API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val text = response.body?.string() ?: return@withContext null
                    val json = JSONObject(text)
                    json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                }
            } catch (e: Exception) {
                null
            }
        }
}
