package com.example.aiassistant.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class Msg(val role: String, val content: String)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Msg>,
    val temperature: Double = 0.6,
    val stream: Boolean = true
)

@Serializable
data class Delta(val content: String? = null)

@Serializable
data class Choice(val delta: Delta? = null)

@Serializable
data class Chunk(val choices: List<Choice>? = null)

object MoonshotApi {

    var baseUrl: String = "https://api.orcarouter.ai/v1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun streamChat(
        apiKey: String,
        model: String,
        history: List<Msg>,
        onToken: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ) {
        if (apiKey.isBlank()) {
            onError("Сначала введите API-ключ OrcaRouter в настройках")
            onDone()
            return
        }
        Thread {
            try {
                val payload = json.encodeToString(
                    ChatRequest.serializer(),
                    ChatRequest(model = model, messages = history, stream = true)
                )
                val request = Request.Builder()
                    .url("$baseUrl/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Accept", "text/event-stream")
                    .post(payload.toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val detail = resp.body?.string()?.take(300).orEmpty()
                        onError("Ошибка API ${resp.code}: $detail")
                        return@Thread
                    }
                    val source = resp.body!!.source()
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (!trimmed.startsWith("data:")) continue
                        val data = trimmed.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        try {
                            val chunk = json.decodeFromString(Chunk.serializer(), data)
                            chunk.choices?.firstOrNull()?.delta?.content?.let(onToken)
                        } catch (_: Exception) {
                        }
                    }
                }
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "Сетевая ошибка")
                onDone()
            }
        }.start()
    }
}
