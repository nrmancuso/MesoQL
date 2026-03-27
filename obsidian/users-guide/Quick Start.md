# Quick Start

## One command

```bash
just quickstart
```

This handles everything:

1. Starts OpenSearch and Ollama via Docker Compose
2. Waits for both services to be healthy
3. Checks for required models and pulls any that are missing (~4 GB on first run)
4. Builds the MesoQL JAR
5. Indexes NWS Area Forecast Discussions from the live API
6. Drops you into the interactive shell

```
MesoQL (type \q to quit)

mesoql>
```

## First query

```sql
mesoql> SEARCH forecast_discussions WHERE SEMANTIC("winter storm warning") LIMIT 5
```

## Stopping

```bash
just down           # stops services (data is preserved)
just clean          # stops services and deletes all data
```

## Next steps

- [[users-guide/Query Language]] — full grammar reference with examples
- [[users-guide/Data Ingestion]] — loading NOAA Storm Events CSVs
- [[users-guide/Shell]] — shell features, history, and commands
- [[users-guide/Deployment]] — service management details
