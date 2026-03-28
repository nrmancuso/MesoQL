# MesoQL Knowledge Base

Semantic weather data query engine. Self-hostable; no external API keys.

## User's Guide

- [[users-guide/Quick Start]] — get running in five minutes
- [[users-guide/GraphQL API]] — full query reference with examples
- [[users-guide/Data Ingestion]] — loading NOAA and NWS data
- [[users-guide/Deployment]] — Docker Compose setup and service management

## Internals

- [[architecture/Overview]]
- [[components/GraphQL]]
- [[components/OpenSearch]]
- [[components/Ollama]]
- [[components/Ingestion]]
- [[architecture/Infrastructure]]
- [[data-sources/NOAA Storm Events]]
- [[data-sources/NWS Area Forecast Discussions]]

## What is MesoQL?

MesoQL is a query engine combining a GraphQL HTTP API with vector search (OpenSearch k-NN) and
local LLM inference (Ollama) to run hybrid queries over NOAA storm event narratives and NWS Area
Forecast Discussions.

Every query is hybrid: structured filters narrow the search space, semantic similarity drives
ranking, and an optional LLM clause synthesizes or explains results.
