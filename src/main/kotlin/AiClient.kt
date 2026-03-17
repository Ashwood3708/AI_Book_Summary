package com.booksummarizer

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://127.0.0.1:1234",
    // LM Studio ignores this string but the field is required by the API shape
    private val model: String = "local-model"
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun summarizeBook(title: String): SummaryResult {
        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "You are a helpful literary assistant. Be concise and spoiler-conscious."
                ),
                ChatMessage(
                    role = "user",
                    content = buildPrompt(title)
                )
            )
        )

        return try {
            val httpResponse: HttpResponse = httpClient.post("$baseUrl/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val rawBody = httpResponse.bodyAsText()

            if (!httpResponse.status.isSuccess()) {
                return SummaryResult.Error(extractApiError(rawBody, httpResponse.status))
            }

            val response = json.decodeFromString<ChatResponse>(rawBody)

            val message = response.choices.firstOrNull()?.message
                ?: return SummaryResult.Error("No content in response")

            // Reasoning models (like qwen-distilled) return their answer in
            // reasoning_content when content is empty
            val summaryText = message.content.ifBlank { message.reasoningContent }
                ?: return SummaryResult.Error("No content in response")

            SummaryResult.Success(
                title = title,
                summary = summaryText,
                inputTokens = response.usage?.promptTokens ?: 0,
                outputTokens = response.usage?.completionTokens ?: 0
            )
        } catch (e: Exception) {
            SummaryResult.Error("API call failed: ${e.message}")
        }
    }

    private fun buildPrompt(title: String): String = """
        For the book titled "$title", please provide:
        1. A 2-3 sentence overview of what the book is about
        2. The main themes (bullet points)
        3. Who should read it and why (1-2 sentences)

        If you don't recognise the title, say so clearly.
    """.trimIndent()

    private fun extractApiError(body: String, status: HttpStatusCode): String {
        return try {
            val message = Json.parseToJsonElement(body)
                .jsonObject["error"]
                ?.jsonObject?.get("message")
                ?.jsonPrimitive?.content
            "API error ${status.value}: ${message ?: body}"
        } catch (_: Exception) {
            "API error ${status.value}: $body"
        }
    }
}

// ── Result type ───────────────────────────────────────────────────────────────

sealed class SummaryResult {
    data class Success(
        val title: String,
        val summary: String,
        val inputTokens: Int,
        val outputTokens: Int
    ) : SummaryResult()

    data class Error(val message: String) : SummaryResult()
}