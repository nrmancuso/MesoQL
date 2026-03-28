# GraphQL API

MesoQL's query interface is a GraphQL HTTP API. Queries are sent as JSON to `POST /graphql`. An
interactive browser-based playground is available at `http://localhost:8080/graphiql`.

## Sending Queries

### curl

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query": "...", "variables": {...}}'
```

### GraphiQL

Navigate to [http://localhost:8080/graphiql](http://localhost:8080/graphiql). The editor provides
schema introspection, autocomplete, and a documentation panel.

---

## The `search` Query

All queries use the single `search` root field:

```graphql
search(source: Source!, input: SearchInput!): SearchResponse!
```

`source` is either `STORM_EVENTS` or `FORECAST_DISCUSSIONS`.

---

## Sources and Filter Fields

### `STORM_EVENTS`

| Field | Type | Filterable with |
|---|---|---|
| `eventId` | String | `in` |
| `state` | String | `in` (two-letter US abbreviation) |
| `eventType` | String | `in` |
| `beginDate` | String | `between`, `comparisons` |
| `fatalities` | Int | `between`, `comparisons` |
| `damageProperty` | Long | `between`, `comparisons` |
| `narrative` | String | semantic only |
| `explanation` | String | output only (when `explain: true`) |

### `FORECAST_DISCUSSIONS`

| Field | Type | Filterable with |
|---|---|---|
| `discussionId` | String | `in` |
| `office` | String | `in` (three-letter NWS office code) |
| `region` | String | `in` |
| `season` | String | `in` (`spring`, `summer`, `fall`, `winter`) |
| `issuanceTime` | String | `between`, `comparisons` |
| `text` | String | semantic only |
| `explanation` | String | output only (when `explain: true`) |

---

## Filter Types

### `in` — Match any of a set of values

```graphql
filters: {
  in: [
    {field: "state", values: ["TX", "OK", "KS"]},
    {field: "eventType", values: ["Tornado", "Flash Flood"]}
  ]
}
```

Only valid on String (keyword) fields.

### `between` — Numeric or date range (inclusive)

```graphql
filters: {
  between: [
    {field: "fatalities", min: 1, max: 50},
    {field: "beginDate", min: "2020-01-01", max: "2023-12-31"}
  ]
}
```

Only valid on numeric or date fields.

### `comparisons` — Single-field comparison

```graphql
filters: {
  comparisons: [
    {field: "damageProperty", op: GTE, value: "1000000"}
  ]
}
```

Supported operators: `EQ`, `NEQ`, `GT`, `GTE`, `LT`, `LTE`. Only valid on numeric or date fields.

---

## Output Clauses

### `synthesize` — LLM synthesis

Provide a prompt string; the LLM synthesizes a summary over the returned hits.

```graphql
input: {semantic: "tornado outbreak", synthesize: "Summarize the key damage patterns."}
```

Result is in `SearchResponse.synthesis`. Mutually exclusive with `clusterByTheme`.

### `explain` — Per-hit LLM explanation

```graphql
input: {semantic: "flooding infrastructure damage", explain: true}
```

Each hit's `explanation` field is populated with a per-document explanation. Works with both
sources.

### `clusterByTheme` — LLM thematic clustering

```graphql
input: {semantic: "severe thunderstorm", clusterByTheme: true}
```

Result is in `SearchResponse.clusters`. Mutually exclusive with `synthesize`.

---

## Limit

Control the number of returned hits:

```graphql
input: {semantic: "blizzard warning", limit: 10}
```

Defaults to a server-side maximum if omitted.

---

## Example Queries

### Search storm events in specific states

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "query($input: SearchInput!) { search(source: STORM_EVENTS, input: $input) { hits { ... on StormEventHit { eventId state eventType fatalities narrative } } } }",
    "variables": {
      "input": {
        "semantic": "tornado causing injuries and fatalities",
        "filters": {
          "in": [{"field": "state", "values": ["TX", "OK"]}],
          "between": [{"field": "fatalities", "min": 1, "max": 100}]
        },
        "limit": 10
      }
    }
  }'
```

### Search forecast discussions with explanation

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "query($input: SearchInput!) { search(source: FORECAST_DISCUSSIONS, input: $input) { hits { ... on ForecastDiscussionHit { office season text explanation } } } }",
    "variables": {
      "input": {
        "semantic": "winter storm warning blizzard conditions",
        "filters": {
          "in": [{"field": "season", "values": ["winter"]}]
        },
        "explain": true,
        "limit": 5
      }
    }
  }'
```

### Synthesize storm event narratives

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "query($input: SearchInput!) { search(source: STORM_EVENTS, input: $input) { hits { ... on StormEventHit { eventId narrative } } synthesis } }",
    "variables": {
      "input": {
        "semantic": "flash flood urban infrastructure damage",
        "synthesize": "What infrastructure types were most commonly damaged?",
        "limit": 15
      }
    }
  }'
```

### Cluster forecast discussions by theme

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "query($input: SearchInput!) { search(source: FORECAST_DISCUSSIONS, input: $input) { hits { ... on ForecastDiscussionHit { discussionId office text } } clusters } }",
    "variables": {
      "input": {
        "semantic": "severe convective outlook tornado risk",
        "clusterByTheme": true,
        "limit": 20
      }
    }
  }'
```

---

## Common Validation Errors

| Error | Cause |
|---|---|
| `Unknown field 'xyz' for source STORM_EVENTS` | Field does not exist in schema |
| `Field 'narrative' does not support IN filters` | IN filter applied to non-keyword field |
| `Field 'state' does not support BETWEEN filters` | BETWEEN applied to keyword field |
| `synthesize and clusterByTheme are mutually exclusive` | Both output clauses set |
| `Invalid season value 'monsoon'` | Season not in `spring`, `summer`, `fall`, `winter` |

All validation errors are returned as GraphQL errors in the `errors` array of the response body.
HTTP status is always `200`.
