# OpenSearch

MesoQL uses OpenSearch 2.x with the k-NN plugin for vector storage and hybrid search. This document covers index mappings, client setup, and hybrid query construction.

## Prerequisites

The k-NN plugin must be loaded. Verify:

```bash
curl http://localhost:9200/_cat/plugins | grep knn
```

Set `knn.memory.circuit_breaker.enabled: false` in `opensearch.yml` for local development to avoid memory limits with small heap sizes.

## Index Mappings

### `storm_events`

```json
PUT /storm_events
{
  "settings": {
    "index": {
      "knn": true,
      "knn.algo_param.ef_search": 100
    }
  },
  "mappings": {
    "properties": {
      "event_id":          { "type": "keyword" },
      "state":             { "type": "keyword" },
      "event_type":        { "type": "keyword" },
      "year":              { "type": "integer" },
      "begin_date":        { "type": "date" },
      "fatalities":        { "type": "integer" },
      "damage_property":   { "type": "long" },
      "narrative":         { "type": "text" },
      "narrative_vector": {
        "type": "knn_vector",
        "dimension": 768,
        "method": {
          "name": "hnsw",
          "engine": "lucene",
          "parameters": { "m": 16, "ef_construction": 128 }
        }
      }
    }
  }
}
```

### `forecast_discussions`

```json
PUT /forecast_discussions
{
  "settings": {
    "index": {
      "knn": true,
      "knn.algo_param.ef_search": 100
    }
  },
  "mappings": {
    "properties": {
      "discussion_id":   { "type": "keyword" },
      "office":          { "type": "keyword" },
      "region":          { "type": "keyword" },
      "issuance_time":   { "type": "date" },
      "season":          { "type": "keyword" },
      "text":            { "type": "text" },
      "text_vector": {
        "type": "knn_vector",
        "dimension": 768,
        "method": {
          "name": "hnsw",
          "engine": "lucene",
          "parameters": { "m": 16, "ef_construction": 128 }
        }
      }
    }
  }
}
```

`nomic-embed-text` outputs 768-dimensional vectors. `m: 16` and `ef_construction: 128` are solid HNSW defaults for this scale; tune `ef_search` upward for better recall at the cost of latency.

## Java Client Setup

Use the `opensearch-java` client (not the legacy `RestHighLevelClient`):

```xml
<dependency>
    <groupId>org.opensearch.client</groupId>
    <artifactId>opensearch-java</artifactId>
    <version>2.6.0</version>
</dependency>
```

```java
RestClient restClient = RestClient.builder(
    new HttpHost("localhost", 9200, "http")
).build();

OpenSearchTransport transport = new RestClientTransport(
    restClient, new JacksonJsonpMapper()
);

OpenSearchClient client = new OpenSearchClient(transport);
```

## Hybrid Query Construction

A hybrid query combines a k-NN vector search with a boolean filter. OpenSearch's `hybrid` query type (2.10+) handles score normalization natively.

```java
public SearchResponse<Map> hybridSearch(
        String index,
        float[] queryVector,
        List<QueryAST.Filter> filters,
        int topK) throws IOException {

    // Build the kNN query
    Query knnQuery = Query.of(q -> q
        .knn(knn -> knn
            .field(vectorField(index))
            .vector(queryVector)
            .k(topK)
        )
    );

    // Build bool filter from structured clauses
    Query boolFilter = buildBoolFilter(filters);

    // Combine via hybrid query
    Query hybridQuery = Query.of(q -> q
        .hybrid(h -> h
            .queries(knnQuery, boolFilter)
        )
    );

    return client.search(s -> s
        .index(index)
        .query(hybridQuery)
        .size(topK),
        (Class<Map>) (Class<?>) Map.class
    );
}
```

### Building the Bool Filter

```java
private Query buildBoolFilter(List<QueryAST.Filter> filters) {
    List<Query> musts = filters.stream()
        .map(this::filterToQuery)
        .toList();

    return Query.of(q -> q
        .bool(b -> b.must(musts))
    );
}

private Query filterToQuery(QueryAST.Filter filter) {
    return switch (filter) {
        case QueryAST.InFilter f -> Query.of(q -> q
            .terms(t -> t.field(f.field()).terms(tv -> tv
                .value(f.values().stream().map(FieldValue::of).toList())
            ))
        );
        case QueryAST.BetweenFilter f -> Query.of(q -> q
            .range(r -> r.field(f.field()).gte(JsonData.of(f.low())).lte(JsonData.of(f.high())))
        );
        case QueryAST.ComparisonFilter f -> comparisonToQuery(f);
    };
}

private Query comparisonToQuery(QueryAST.ComparisonFilter f) {
    return switch (f.op()) {
        case "=" -> Query.of(q -> q.term(t -> t.field(f.field()).value(FieldValue.of(f.value()))));
        case "!=" -> Query.of(q -> q.bool(b -> b.mustNot(
            Query.of(q2 -> q2.term(t -> t.field(f.field()).value(FieldValue.of(f.value()))))
        )));
        case ">", ">=", "<", "<=" -> {
            double val = Double.parseDouble(f.value());
            yield Query.of(q -> q.range(r -> {
                r.field(f.field());
                switch (f.op()) {
                    case ">"  -> r.gt(JsonData.of(val));
                    case ">=" -> r.gte(JsonData.of(val));
                    case "<"  -> r.lt(JsonData.of(val));
                    case "<=" -> r.lte(JsonData.of(val));
                }
                return r;
            }));
        }
        default -> throw new MesoQLException("Unknown operator: " + f.op());
    };
}
```

## Query Planner: Field Validation

The `QueryPlanner` validates field names and types against a per-source schema before any network calls. Define the schema as a static map:

```java
public static final Map<String, FieldSchema> STORM_EVENTS_FIELDS = Map.of(
    "state",           new FieldSchema(FieldType.KEYWORD),
    "event_type",      new FieldSchema(FieldType.KEYWORD),
    "year",            new FieldSchema(FieldType.INTEGER),
    "begin_date",      new FieldSchema(FieldType.DATE),
    "fatalities",      new FieldSchema(FieldType.INTEGER),
    "damage_property", new FieldSchema(FieldType.LONG)
);

public static final Map<String, FieldSchema> FORECAST_DISCUSSIONS_FIELDS = Map.of(
    "office",          new FieldSchema(FieldType.KEYWORD),
    "region",          new FieldSchema(FieldType.KEYWORD),
    "season",          new FieldSchema(FieldType.KEYWORD),
    "issuance_time",   new FieldSchema(FieldType.DATE)
);
```

Validation checks:

- Field exists in the source schema; throw `MesoQLValidationException` if not
- `BETWEEN` is only applied to numeric or date fields
- `IN` is only applied to keyword fields
- `SYNTHESIZE` and `CLUSTER BY THEME` are not both present
- `season` values are one of `spring`, `summer`, `fall`, `winter`
- `state` values are valid two-letter US state abbreviations

## Vector Field Names by Source

```java
private String vectorField(String source) {
    return switch (source) {
        case "storm_events"          -> "narrative_vector";
        case "forecast_discussions"  -> "text_vector";
        default -> throw new MesoQLException("Unknown source: " + source);
    };
}
```
