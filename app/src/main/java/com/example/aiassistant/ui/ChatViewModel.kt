package com.example.aiassistant.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.aiassistant.data.ChatMessage
import com.example.aiassistant.data.MoonshotApi
import com.example.aiassistant.data.Msg

class ChatViewModel : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>()
    val apiKey = mutableStateOf("")
    val model = mutableStateOf(DEFAULT_MODEL)
    val busy = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    private val history = mutableListOf<Msg>()
    private val main = Handler(Looper.getMainLooper())

    companion object {
        const val DEFAULT_MODEL = "kimi-k2-0711-preview"
        val MODELS = listOf(
            "kimi-k2-0711-preview",
            "kimi-k2-turbo-preview",
            "kimi-latest",
            "moonshot-v1-8k",
            "moonshot-v1-32k",
            "moonshot-v1-128k"
        )
    }

    fun send(text: String, attachmentName: String?) {
        if (busy.value || text.isBlank()) return

        val full = buildString {
            append(text)
            if (attachmentName != null) {
                append("\n\n[Прикреплён файл: $attachmentName]")
            }
        }

        messages.add(ChatMessage(role = "user", content = text, attachmentName = attachmentName))
        history.add(Msg("user", full))
        busy.value = true
        error.value = null

        val replyId = java.util.UUID.randomUUID().toString()
        messages.add(ChatMessage(id = replyId, role = "assistant", content = "", streaming = true))

        MoonshotApi.streamChat(
            apiKey = apiKey.value,
            model = model.value,
            history = history.toList(),
            onToken = { token -> main.post { appendToken(replyId, token) } },
            onError = { e -> main.post { error.value = e } },
            onDone = { main.post { finish(replyId) } }
        )
    }

    private fun appendToken(id: String, token: String) {
        val i = messages.indexOfFirst { it.id == id }
        if (i >= 0) {
            messages[i] = messages[i].copy(content = messages[i].content + token)
        }
    }

    private fun finish(id: String) {
        busy.value = false
        val i = messages.indexOfFirst { it.id == id }
        if (i >= 0) {
            messages[i] = messages[i].copy(streaming = false)
            if (messages[i].content.isNotBlank()) {
                history.add(Msg("assistant", messages[i].content))
            }
        }
    }
}
