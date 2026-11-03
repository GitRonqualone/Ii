package com.example.aiassistant.data

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,                 // "user" или "assistant"
    val content: String,
    val attachmentName: String? = null,
    val streaming: Boolean = false
)
