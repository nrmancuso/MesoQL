# MesoQL Knowledge Base

Semantic weather data query engine. Self-hostable; no external API keys.

## Quick Links

- [[architecture/Overview]]
- [[components/Grammar]]
- [[components/OpenSearch]]
- [[components/Ollama]]
- [[components/Ingestion]]
- [[components/CLI]]
- [[architecture/Infrastructure]]
- [[data-sources/NOAA Storm Events]]
- [[data-sources/NWS Area Forecast Discussions]]

## What is MesoQL?

MesoQL is a query engine combining a SQL-style DSL with vector search (OpenSearch k-NN) and local
LLM inference (Ollama) to run hybrid queries over NOAA storm event narratives and NWS Area Forecast
Discussions.

Every query is hybrid: structured filters narrow the search space, semantic similarity drives
ranking, and an optional LLM clause synthesizes or explains results.
