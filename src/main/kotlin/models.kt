package com.booksummarizer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Request ──────────────────────────────────────────────────────────────────

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1024
)

@Serializable
data class ChatMessage(
    val role: String,   // "system" | "user" | "assistant"
    val content: String
)

// ── Response ─────────────────────────────────────────────────────────────────

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val message: ChatMessageWithReasoning,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatMessageWithReasoning(
    val role: String,
    val content: String = "",
    @SerialName("reasoning_content") val reasoningContent: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0
)