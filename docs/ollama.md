# Ollama

MesoQL uses Ollama for both embeddings and LLM generation. No external API keys; Ollama runs locally. This document covers client setup, the embedding call, and prompt design for each output clause.

## Prerequisites

```bash
# Install Ollama (macOS/Linux)
curl -fsSL https://ollama.com/install.sh | sh

# Pull required models
ollama pull nomic-embed-text   # 768-dim embeddings; used at index and query time
ollama pull llama3             # generation; used for SYNTHESIZE, EXPLAIN, CLUSTER BY THEME

# Verify
ollama list
```

Ollama's default base URL is `http://localhost:11434`. Make this configurable via `MesoQLConfig`.

## Java Client

Use `java.net.http.HttpClient` directly; no third-party Ollama client needed.

```java
public class OllamaClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String embedModel;
    private final String generateModel;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaClient(String baseUrl, String embedModel, String generateModel) {
        this.baseUrl = baseUrl;
        this.embedModel = embedModel;
        this.generateModel = generateModel;
    }
}
```

## Embeddings

Used at both index time (to embed document narratives) and query time (to embed the `SEMANTIC(...)` string).

```java
public float[] embed(String text) throws IOException, InterruptedException {
    String body = mapper.writeValueAsString(Map.of(
        "model", embedModel,
        "prompt", text
    ));

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/embeddings"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode root = mapper.readTree(response.body());

    JsonNode embeddingNode = root.get("embedding");
    float[] vector = new float[embeddingNode.size()];
    for (int i = 0; i < vector.length; i++) {
        vector[i] = (float) embeddingNode.get(i).asDouble();
    }
    return vector;
}
```

For bulk ingestion, embed in batches and rate-limit to avoid overwhelming Ollama. A batch size of 32 with a short sleep between batches works well locally.

## Generation

```java
public String generate(String prompt) throws IOException, InterruptedException {
    String body = mapper.writeValueAsString(Map.of(
        "model", generateModel,
        "prompt", prompt,
        "stream", false
    ));

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/generate"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode root = mapper.readTree(response.body());
    return root.get("response").asText();
}
```

## Prompt Design

### SYNTHESIZE

Context window: concatenate the `narrative` or `text` fields of retrieved documents, truncated to fit model context. Then append the user's synthesis question.

```text
You are an expert meteorologist. The following are real weather event records retrieved from the NOAA Storm Events Database.

--- RECORDS ---
[1] {narrative of result 1}
[2] {narrative of result 2}
...
--- END RECORDS ---

Based only on the records above, answer the following question:
{user's SYNTHESIZE question}

Be specific. Cite record numbers where relevant.
```

Keep the system framing tight. Don't ask the model to speculate beyond the retrieved records.

### EXPLAIN

One call per result. Keep it short; the goal is a one-sentence explanation of semantic relevance.

```text
You are an expert meteorologist. A user searched for weather events matching the following description:

"{semantic query text}"

The following event was retrieved as a match:

{narrative of the result}

In one sentence, explain why this event is semantically relevant to the user's search.
```

### CLUSTER BY THEME

Single call over all retrieved results. Ask the model to group them and label each group.

```text
You are an expert meteorologist. The following weather records were retrieved for the query:

"{semantic query text}"

--- RECORDS ---
[1] {narrative of result 1}
[2] {narrative of result 2}
...
--- END RECORDS ---

Group these records into 2-5 thematic clusters based on their meteorological characteristics.
For each cluster, provide:
- A short label (3-5 words)
- The record numbers in that cluster
- One sentence describing what the records in the cluster have in common
```

## Configuration

All model names and the base URL are configurable in `MesoQLConfig`:

```java
public record MesoQLConfig(
    String openSearchUrl,
    String ollamaBaseUrl,
    String embedModel,
    String generateModel
) {
    public static MesoQLConfig defaults() {
        return new MesoQLConfig(
            "http://localhost:9200",
            "http://localhost:11434",
            "nomic-embed-text",
            "llama3"
        );
    }
}
```

Override via environment variables or a `~/.mesoql/config.yaml` file at startup.
