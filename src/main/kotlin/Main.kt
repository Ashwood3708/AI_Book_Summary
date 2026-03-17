package com.booksummarizer

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    // ── 1. Parse input ────────────────────────────────────────────────────────
    val title = when {
        args.isEmpty() -> {
            print("Enter a book title: ")
            readLine()?.trim()
        }
        else -> args.joinToString(" ")
    }

    if (title.isNullOrBlank()) {
        println("❌  Please provide a book title.")
        return
    }

    // ── 2. Run the summarizer ─────────────────────────────────────────────────
    println("\n📚 Fetching summary for: \"$title\"\n")

    val client = AiClient(
        httpClient = HttpClientFactory.create()
        // baseUrl defaults to http://127.0.0.1:1234
        // Change it here if your LM Studio runs on a different port
    )

    runBlocking {
        when (val result = client.summarizeBook(title)) {
            is SummaryResult.Success -> {
                println("─".repeat(60))
                println(result.summary)
                println("─".repeat(60))
                println("📊 Tokens used — input: ${result.inputTokens}, output: ${result.outputTokens}")
            }
            is SummaryResult.Error -> {
                println("❌  Error: ${result.message}")
            }
        }
    }
}