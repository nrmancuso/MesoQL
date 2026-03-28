# GraphQL Schema Reference

MesoQL exposes a single GraphQL endpoint at `POST /graphql`. Queries are submitted as standard
GraphQL documents. A browser-based playground is available at `GET /graphiql`.

---

## Full SDL

```graphql
enum Source {
  STORM_EVENTS
  FORECAST_DISCUSSIONS
}

enum ComparisonOp {
  EQ
  NEQ
  GT
  GTE
  LT
  LTE
}

input InFilterInput {
  field: String!
  values: [String!]!
}

input BetweenFilterInput {
  field: String!
  min: Float!
  max: Float!
}

input ComparisonFilterInput {
  field: String!
  op:   ComparisonOp!
  value: String!
}

input FiltersInput {
  in:          [InFilterInput!]
  between:     [BetweenFilterInput!]
  comparisons: [ComparisonFilterInput!]
}

input SearchInput {
  semantic:       String!
  filters:        FiltersInput
  synthesize:     String        # mutually exclusive with clusterByTheme
  clusterByTheme: Boolean
  explain:        Boolean
  limit:          Int
}

type StormEventHit {
  eventId:        String
  state:          String
  eventType:      String
  beginDate:      String
  fatalities:     Int
  damageProperty: Long          # custom scalar via graphql-java-extended-scalars
  narrative:      String
  explanation:    String        # populated only when explain: true
}

type ForecastDiscussionHit {
  discussionId: String
  office:       String
  region:       String
  season:       String
  issuanceTime: String
  text:         String
  explanation:  String          # populated only when explain: true
}

union SearchHit = StormEventHit | ForecastDiscussionHit

type SearchResponse {
  hits:      [SearchHit!]!
  synthesis: String             # populated only when synthesize is set
  clusters:  String             # populated only when clusterByTheme: true
}

type Query {
  search(source: Source!, input: SearchInput!): SearchResponse!
}
```

---

## The `Long` Scalar

`damageProperty` on `StormEventHit` uses a custom `Long` scalar provided by
`graphql-java-extended-scalars`. This represents 64-bit integer values that may exceed the range of
GraphQL's built-in `Int` scalar. Values are serialized as JSON numbers.

---

## Field Reference

### `storm_events`

| Field | GraphQL Type | Filter Types | Notes |
|---|---|---|---|
| `eventId` | `String` | `in` | Storm Events unique identifier |
| `state` | `String` | `in` | Two-letter US state abbreviation |
| `eventType` | `String` | `in` | e.g. `Tornado`, `Flash Flood` |
| `beginDate` | `String` | `between`, `comparisons` | ISO date string (YYYY-MM-DD) |
| `fatalities` | `Int` | `between`, `comparisons` | Direct + indirect deaths |
| `damageProperty` | `Long` | `between`, `comparisons` | Property damage in dollars |
| `narrative` | `String` | — | Full event narrative text (searchable via `semantic`) |
| `explanation` | `String` | — | LLM explanation; populated when `explain: true` |

### `forecast_discussions`

| Field | GraphQL Type | Filter Types | Notes |
|---|---|---|---|
| `discussionId` | `String` | `in` | NWS product ID |
| `office` | `String` | `in` | Three-letter NWS office code, e.g. `BOU` |
| `region` | `String` | `in` | Geographic region |
| `season` | `String` | `in` | `spring`, `summer`, `fall`, or `winter` |
| `issuanceTime` | `String` | `between`, `comparisons` | ISO timestamp |
| `text` | `String` | — | Full discussion text (searchable via `semantic`) |
| `explanation` | `String` | — | LLM explanation; populated when `explain: true` |

---

## Output Fields

### `synthesis`

Populated when `synthesize` is set to a non-empty prompt string. The LLM (llama3 via Ollama)
synthesizes a summary of the returned hits in response to the prompt. Mutually exclusive with
`clusterByTheme`.

### `clusters`

Populated when `clusterByTheme: true`. The LLM groups the returned hits into thematic clusters.
Mutually exclusive with `synthesize`.

### `explanation`

Populated on each hit when `explain: true`. The LLM generates a per-hit explanation of why the
result matches the semantic query.

---

## Validation Rules

- `semantic` is required and must be non-empty.
- All filter fields must exist in the schema for the requested `source`.
- `in` filters are only valid on keyword fields (`String`).
- `between` and `comparisons` filters are only valid on numeric or date fields.
- `synthesize` and `clusterByTheme` are mutually exclusive — providing both is an error.
- `season` values must be one of: `spring`, `summer`, `fall`, `winter`.
- `state` values must be valid two-letter US abbreviations.
- Validation failures are returned as GraphQL errors (not HTTP 4xx).

---

## Example Queries

### Search storm events for tornado narratives

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query": "{ search(source: STORM_EVENTS, input: {semantic: \"tornado outbreak\", limit: 5}) { hits { ... on StormEventHit { eventId state narrative } } } }"}'
```

### Search with filters and synthesis

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "query($input: SearchInput!) { search(source: STORM_EVENTS, input: $input) { hits { ... on StormEventHit { eventId state eventType fatalities narrative } } synthesis } }",
    "variables": {
      "input": {
        "semantic": "tornado outbreak causing fatalities",
        "filters": {
          "in": [{"field": "state", "values": ["TX", "OK", "KS"]}],
          "between": [{"field": "fatalities", "min": 1, "max": 100}]
        },
        "synthesize": "Summarize the deadliest tornado events in these states.",
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
    "query": "query($input: SearchInput!) { search(source: FORECAST_DISCUSSIONS, input: $input) { hits { ... on ForecastDiscussionHit { discussionId office text explanation } } } }",
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

### Cluster storm events by theme

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "query($input: SearchInput!) { search(source: STORM_EVENTS, input: $input) { hits { ... on StormEventHit { eventId eventType narrative } } clusters } }",
    "variables": {
      "input": {
        "semantic": "flooding damage infrastructure",
        "clusterByTheme": true,
        "limit": 20
      }
    }
  }'
```

---

## GraphiQL Playground

Navigate to [http://localhost:8080/graphiql](http://localhost:8080/graphiql) in a browser to use
the interactive GraphiQL editor. It provides schema introspection, query autocompletion, and a
built-in documentation explorer. Enabled by default via `spring.graphql.graphiql.enabled: true`.
