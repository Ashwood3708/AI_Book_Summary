package com.booksummarizer

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AiClientTest {

  // ── Helpers ───────────────────────────────────────────────────────────────

  private fun mockHttpClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient {
    val mockEngine = MockEngine { _ ->
      respond(
        content = responseBody,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
      )
    }
    return HttpClient(mockEngine) {
      install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
      }
    }
  }

  private val successResponse = """
        {
          "id": "chatcmpl-test123",
          "choices": [
            {
              "message": { "role": "assistant", "content": "Dune is a sci-fi epic about politics and ecology." },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "prompt_tokens": 120,
            "completion_tokens": 45
          }
        }
    """.trimIndent()

  // ── Tests ─────────────────────────────────────────────────────────────────

  @Test
  fun `success response is parsed into SummaryResult Success`() = runTest {
    val client = AiClient(httpClient = mockHttpClient(successResponse))

    val result = client.summarizeBook("Dune")

    assertIs<SummaryResult.Success>(result)
    assertEquals("Dune", result.title)
    assertEquals("Dune is a sci-fi epic about politics and ecology.", result.summary)
    assertEquals(120, result.inputTokens)
    assertEquals(45, result.outputTokens)
  }

  @Test
  fun `network error returns SummaryResult Error`() = runTest {
    val errorEngine = MockEngine { _ -> throw RuntimeException("Connection refused") }
    val httpClient = HttpClient(errorEngine) {
      install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    val result = AiClient(httpClient = httpClient).summarizeBook("Any Book")

    assertIs<SummaryResult.Error>(result)
    assertTrue(result.message.contains("API call failed"))
  }

  @Test
  fun `empty choices list returns SummaryResult Error`() = runTest {
    val emptyResponse = """
            {
              "id": "chatcmpl-empty",
              "choices": [],
              "usage": { "prompt_tokens": 10, "completion_tokens": 0 }
            }
        """.trimIndent()

    val result = AiClient(httpClient = mockHttpClient(emptyResponse)).summarizeBook("Ghost Book")

    assertIs<SummaryResult.Error>(result)
    assertEquals("No content in response", result.message)
  }

  @Test
  fun `non-2xx response surfaces human readable error`() = runTest {
    val errorBody = """
            { "error": { "message": "Model not loaded", "code": 503 } }
        """.trimIndent()

    val result = AiClient(
      httpClient = mockHttpClient(errorBody, HttpStatusCode.ServiceUnavailable)
    ).summarizeBook("Any Book")

    assertIs<SummaryResult.Error>(result)
    assertTrue(result.message.contains("Model not loaded"))
  }
}