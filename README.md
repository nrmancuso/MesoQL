# MesoQL

MesoQL is an open-source query engine for semantic search over weather data. It combines a SQL-style
DSL with vector search (OpenSearch k-NN) and local LLM inference (Ollama) to run expressive hybrid
queries over NOAA storm event narratives and NWS Area Forecast Discussions. Fully self-hostable; no
API keys required.

Every query is a hybrid query: structured filters narrow the search space, semantic similarity
drives ranking, and an LLM synthesizes or explains results.

## Requirements

- Java 17+
- Maven 3.8+
- OpenSearch 2.x with the k-NN plugin enabled
- Ollama with `nomic-embed-text` and `llama3` pulled

## Quick Start

```bash
# Pull required Ollama models
ollama pull nomic-embed-text
ollama pull llama3

# Index NOAA storm events
mesoql index --source storm_events --data ./StormEvents_2023.csv

# Run a query
mesoql query --inline "SEARCH storm_events WHERE SEMANTIC(\"tornado with no warning\") AND state IN (\"Kansas\", \"Oklahoma\") LIMIT 5"

# Or from a file
mesoql query my_query.mql
```

## Query Language

```sql
SEARCH storm_events
WHERE SEMANTIC("tornado that formed rapidly without warning")
  AND state IN ("Kansas", "Oklahoma", "Texas")
  AND year BETWEEN 2000 AND 2020
  AND fatalities > 0
SYNTHESIZE "what atmospheric conditions allowed rapid tornado formation?"
LIMIT 10
```

```sql
SEARCH forecast_discussions
WHERE SEMANTIC("unexpected ridge breakdown over the Pacific")
  AND region = "Pacific Northwest"
  AND season = "winter"
CLUSTER BY THEME
```

```sql
SEARCH storm_events
WHERE SEMANTIC("flooding caused by a stalled frontal boundary")
  AND damage_property > 1000000
EXPLAIN
LIMIT 5
```

See [docs/BUILDING.md](docs/BUILDING.md) for the full build guide and [docs/](docs/) for
component-level documentation.

## Data Sources

- [NOAA Storm Events Database](https://www.ncdc.noaa.gov/stormevents/ftp.jsp): 1.8M+ storm records
  from 1950 onward; public domain
- [NWS Area Forecast Discussions](https://api.weather.gov/products/types/AFD): daily
  meteorologist-authored forecast discussions; public domain

## License

Apache 2.0
