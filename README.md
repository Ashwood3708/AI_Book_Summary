# AI_Book_Summary
# 📖 BookBrief

> A lightweight CLI tool that takes a book title and returns an AI-generated summary using a locally running language model.

---

## Purpose

This project was built to practice **LLM API integration** — specifically:

- Calling a local AI model via HTTP
- Handling API errors gracefully
- Parsing and displaying structured responses

---

## How It Works

1. You provide a book title (via CLI argument or prompt)
2. The app sends a request to your local LLM (e.g. Ollama, LM Studio)
3. The model returns a summary
4. The summary is parsed and printed to the terminal

```
$ python main.py "The Great Gatsby"

📖 The Great Gatsby — Summary
─────────────────────────────
Set in 1920s America, The Great Gatsby follows Nick Carraway as he becomes
entangled in the obsessive pursuit of the enigmatic Jay Gatsby, whose lavish
parties mask a desperate longing for a lost love. F. Scott Fitzgerald's novel
is a searing critique of the American Dream and the hollow excess of the Jazz Age.
```

---

## Tech Stack

| Layer       | Tool                          |
|-------------|-------------------------------|
| Language    | Kotlin                        |
| LLM Backend | Ollama / LM Studio (local)    |
| HTTP Client | `OkHttp` / `Ktor`             |
| Serialization | `kotlinx.serialization`     |
---

## Setup

**1. Clone the repo**
```bash
git clone https://github.com/your-username/bookbrief.git
cd bookbrief
```

**2. Build the project**
```bash
./gradlew build
```

**3. Start your local LLM**

Make sure your local model server is running. For example, with Ollama:
```bash
ollama serve
ollama pull llama3
```

**4. Configure the endpoint**

Copy the example config and set your model URL:
```bash
cp .env.example .env
```

```env
# .env
LLM_BASE_URL=http://localhost:11434
LLM_MODEL=llama3
```

---

## Usage

**Basic usage:**
```bash
./gradlew run --args="\"To Kill a Mockingbird\""
```

**With flags:**
```bash
./gradlew run --args="--title \"1984\" --length short"
./gradlew run --args="--title \"Dune\" --length detailed"
```

| Flag       | Options                  | Default  |
|------------|--------------------------|----------|
| `--title`  | Any book title (string)  | prompted |
| `--length` | `short`, `medium`, `detailed` | `medium` |

---

## Project Structure

```
bookbrief/
├── src/
│   └── main/
│       └── kotlin/
│           ├── Main.kt          # Entry point, CLI argument parsing
│           ├── LlmClient.kt     # API call logic, error handling
│           ├── Parser.kt        # Response parsing and formatting
│           └── Config.kt        # Loads environment variables
├── .env.example                 # Config template
├── build.gradle.kts
└── README.md
```

---

## Error Handling

The app handles the following failure cases:

| Scenario                      | Behaviour                                      |
|-------------------------------|------------------------------------------------|
| LLM server not running        | Prints a clear connection error and exits      |
| Model not found               | Suggests running `ollama pull <model>`         |
| Empty or malformed response   | Falls back with a descriptive error message    |
| Request timeout               | Retries once, then exits with a timeout notice |

---

## Example API Call (under the hood)

```kotlin
val client = OkHttpClient.Builder()
    .callTimeout(30, TimeUnit.SECONDS)
    .build()

val body = Json.encodeToString(
    mapOf(
        "model" to model,
        "prompt" to "Give me a concise summary of the book: $title",
        "stream" to false
    )
).toRequestBody("application/json".toMediaType())

val request = Request.Builder()
    .url("$baseUrl/api/generate")
    .post(body)
    .build()

val response = client.newCall(request).execute()
val summary = Json.parseToJsonElement(response.body!!.string())
    .jsonObject["response"]!!.jsonPrimitive.content
```

---

## What I Learned

- How to structure HTTP requests to a local LLM endpoint
- How to handle timeouts, connection errors, and bad responses robustly
- How prompt phrasing affects the quality and format of model output
- Parsing free-form text responses and presenting them cleanly

---

## Future Ideas

- [ ] Cache summaries locally to avoid repeat calls
- [ ] Add support for multiple LLM backends (Ollama, LM Studio, llama.cpp)
- [ ] Web UI with a simple search interface
- [ ] Export summaries to a text or markdown file

---

## License

MIT