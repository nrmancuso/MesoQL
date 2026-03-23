# Ingestion

MesoQL ships ingestion pipelines for two data sources: NOAA Storm Events and NWS Area Forecast Discussions. Both are public domain. This document covers data acquisition, parsing, chunking, embedding, and incremental indexing.

## Getting the Data

### NOAA Storm Events

Download CSV files by year from NCEI:

```text
https://www.ncdc.noaa.gov/stormevents/ftp.jsp
```

Direct FTP path: `ftp://ftp.ncdc.noaa.gov/pub/data/swdi/stormevents/csvfiles/`

Each year produces three files; use the `details` file:

```text
StormEvents_details-ftp_v1.0_d2023_c20240117.csv.gz
```

Decompress and pass to the ingester:

```bash
gunzip StormEvents_details-ftp_v1.0_d2023_c20240117.csv.gz
mesoql index --source storm_events --data ./StormEvents_details-ftp_v1.0_d2023_c20240117.csv
```

Key fields to extract:

| CSV Column | Index Field | Notes |
|---|---|---|
| `EVENT_ID` | `event_id` | Unique identifier |
| `STATE` | `state` | Two-letter abbreviation |
| `EVENT_TYPE` | `event_type` | e.g., "Tornado", "Flash Flood" |
| `BEGIN_DATE_TIME` | `begin_date` | Parse to ISO-8601 |
| `DEATHS_DIRECT` + `DEATHS_INDIRECT` | `fatalities` | Sum both |
| `DAMAGE_PROPERTY` | `damage_property` | Parse "10.00K", "1.50M" to long |
| `EPISODE_NARRATIVE` | narrative (prefix) | Episode-level context |
| `EVENT_NARRATIVE` | narrative (body) | Event-level detail |

Concatenate `EPISODE_NARRATIVE` and `EVENT_NARRATIVE` as the document text for embedding.

`DAMAGE_PROPERTY` is encoded as strings like `"10.00K"` or `"1.50M"`. Parse with a multiplier map: `K=1_000`, `M=1_000_000`, `B=1_000_000_000`.

### NWS Area Forecast Discussions

Fetch via the NWS API. No authentication required.

```
# List all AFD product IDs (paginated)
GET https://api.weather.gov/products?type=AFD&limit=500

# Fetch a specific product
GET https://api.weather.gov/products/{productId}
```

Key fields:

| API Field | Index Field | Notes |
|---|---|---|
| `id` | `discussion_id` | |
| `issuanceTime` | `issuance_time` | ISO-8601 |
| `senderName` | `office` | e.g., "NWS Seattle WA" |
| `productText` | `text` | Full discussion text |

Derive `region` from `senderName` using a static office-to-region map. Derive `season` from `issuanceTime` month.

The NWS API returns the most recent 500 products per request. For historical backfill, the API has limited history; consider the Iowa Environmental Mesonet (IEM) archive for deeper historical AFDs:

```text
https://mesonet.agron.iastate.edu/api/1/nwstext_search.json
```

## Chunking

Storm event narratives are typically short (under 512 tokens) and should be indexed as whole documents. Skip chunking for `storm_events`.

AFD texts can be long (1,000-3,000 tokens). Apply sliding window chunking:

```java
public List<String> chunk(String text, int maxTokens, int overlapTokens) {
    // Approximate token count as word count * 1.3
    String[] words = text.split("\\s+");
    int wordsPerChunk = (int) (maxTokens / 1.3);
    int overlapWords = (int) (overlapTokens / 1.3);

    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < words.length) {
        int end = Math.min(start + wordsPerChunk, words.length);
        chunks.add(String.join(" ", Arrays.copyOfRange(words, start, end)));
        if (end == words.length) break;
        start += wordsPerChunk - overlapWords;
    }
    return chunks;
}
```

Default: `maxTokens=512`, `overlapTokens=64`.

## Incremental Indexing

Skip documents already indexed by hashing the source ID and checking OpenSearch before embedding. Embedding is the expensive step; avoid re-embedding unchanged documents.

```java
public boolean isAlreadyIndexed(String index, String docId) throws IOException {
    return client.exists(e -> e.index(index).id(docId)).value();
}
```

Use the `event_id` for storm events and the `discussion_id` for AFDs as the OpenSearch document `_id`.

## Bulk Indexing

Embed in batches of 32; bulk index to OpenSearch after each batch.

```java
public void ingestStormEvents(Path csvFile) throws Exception {
    List<StormEvent> events = parseCsv(csvFile);
    List<StormEvent> toIndex = events.stream()
        .filter(e -> !isAlreadyIndexed("storm_events", e.eventId()))
        .toList();

    log.info("Indexing {} new events (skipping {} already indexed)",
        toIndex.size(), events.size() - toIndex.size());

    Lists.partition(toIndex, 32).forEach(batch -> {
        List<BulkOperation> ops = batch.stream().map(event -> {
            float[] vector = ollamaClient.embed(event.narrative());
            Map<String, Object> doc = toDocument(event, vector);
            return BulkOperation.of(b -> b
                .index(i -> i.index("storm_events").id(event.eventId()).document(doc))
            );
        }).toList();

        client.bulk(b -> b.operations(ops));
    });
}
```

Log progress per batch. For large CSV files (10+ years of data), expect ingestion to take 30-90 minutes on a local machine depending on GPU availability for Ollama.

## CSV Parsing

Use OpenCSV:

```xml
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>
```

```java
try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFile.toFile()))
        .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
        .build()) {

    String[] headers = reader.readNext();
    Map<String, Integer> colIndex = IntStream.range(0, headers.length)
        .boxed()
        .collect(Collectors.toMap(i -> headers[i], i -> i));

    String[] row;
    while ((row = reader.readNext()) != null) {
        // use colIndex.get("EVENT_ID") etc.
    }
}
```

Parse by column name rather than index; NOAA occasionally shifts column positions between file versions.
