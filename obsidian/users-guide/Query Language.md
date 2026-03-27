# Query Language

Every MesoQL query follows this structure:

```sql
SEARCH <source>
WHERE SEMANTIC("...")
[AND <filter>]*
[output clauses]
```

`SEMANTIC(...)` is required on every query — it drives the vector similarity ranking. Everything
else is optional.

## Sources

| Source | Description |
|---|---|
| `storm_events` | NOAA Storm Events (tornadoes, hail, flooding, etc.) |
| `forecast_discussions` | NWS Area Forecast Discussions |

## Filters

Filters narrow the search space before ranking. They combine with `AND`:

```sql
-- Exact match on keyword fields
AND state IN ("KS", "NE", "OK")
AND event_type IN ("Tornado", "Hail")

-- Numeric comparisons
AND fatalities > 0
AND damage_property >= 100000

-- Range
AND fatalities BETWEEN 1 AND 10
```

### storm_events fields

| Field | Type | Notes |
|---|---|---|
| `state` | keyword | Two-letter abbreviation |
| `event_type` | keyword | e.g., "Tornado", "Hail", "Flash Flood" |
| `fatalities` | integer | Direct + indirect deaths |
| `damage_property` | integer | Dollar amount |
| `begin_date` | date | ISO-8601 |

### forecast_discussions fields

| Field | Type | Notes |
|---|---|---|
| `region` | keyword | e.g., "Pacific Northwest" |
| `office` | keyword | e.g., "NWS Seattle WA" |
| `season` | keyword | `spring`, `summer`, `fall`, `winter` |
| `issuance_time` | date | ISO-8601 |

## Output clauses

Output clauses control what happens after retrieval. They are composable except where noted.

### LIMIT

```sql
LIMIT 10
```

Caps the number of results returned. Defaults to 10 if omitted.

### SYNTHESIZE

```sql
SYNTHESIZE "What were the common contributing factors?"
```

Passes all results to the LLM with your prompt. Returns a synthesized answer grounded in the
retrieved records. Cannot be combined with `CLUSTER BY THEME`.

### EXPLAIN

```sql
EXPLAIN
```

Adds a one-sentence explanation to each result describing why it matched the semantic query.

### CLUSTER BY THEME

```sql
CLUSTER BY THEME
```

Groups results into 2-5 labeled thematic clusters. Cannot be combined with `SYNTHESIZE`.

## Examples

```sql
-- Basic semantic search
SEARCH storm_events WHERE SEMANTIC("tornado outbreak") LIMIT 10

-- Filtered by state and event type
SEARCH storm_events
WHERE SEMANTIC("hail damage to crops")
AND state IN ("KS", "NE", "OK")
AND event_type IN ("Hail")
LIMIT 20

-- With LLM synthesis
SEARCH storm_events
WHERE SEMANTIC("flash flooding in urban areas")
AND fatalities > 0
SYNTHESIZE "What were the common contributing factors?"
LIMIT 15

-- Forecast discussions with explanation
SEARCH forecast_discussions
WHERE SEMANTIC("atmospheric river precipitation")
AND region IN ("Pacific Northwest")
EXPLAIN
LIMIT 5

-- Thematic clustering
SEARCH storm_events
WHERE SEMANTIC("hurricane damage")
AND state IN ("FL", "TX", "LA")
CLUSTER BY THEME
LIMIT 30
```

## Related

- [[users-guide/Shell]] — interactive shell for running queries
- [[components/Grammar]] — ANTLR4 grammar implementation details
- [[components/OpenSearch]] — how queries map to k-NN + bool filters
