# Ingestion

Two ingesters: `StormEventsIngester` (NOAA CSV) and `AFDIngester` (NWS API).

## Common Patterns

- **Incremental**: check OpenSearch by doc ID before embedding; skip already-indexed docs
- **Batch embed**: groups of 32, rate-limited
- **Bulk index**: OpenSearch bulk API after each batch
- **Doc ID**: `event_id` for storm events, `discussion_id` for AFDs

## StormEventsIngester

**Source:** NOAA CSV downloaded from NCEI FTP
**Chunking:** None — narratives are short (<512 tokens), indexed as whole documents

Key parsing:

- `DAMAGE_PROPERTY`: `"10.00K"` → `10000`, `"1.50M"` → `1500000` (multiplier map)
- `fatalities`: `DEATHS_DIRECT + DEATHS_INDIRECT`
- Parse by column name, not index (NOAA shifts columns between versions)
- Embed: concatenation of `EPISODE_NARRATIVE` + `EVENT_NARRATIVE`

## AFDIngester

**Source:** NWS API (`api.weather.gov/products?type=AFD`)
**Chunking:** Sliding window, `maxTokens=512`, `overlapTokens=64` (AFDs can be 1000–3000 tokens)

Derive `region` from `senderName` via static office→region map.
Derive `season` from `issuanceTime` month.

For deep historical data, use IEM archive instead of NWS API.

## Performance

Large CSV files (10+ years): expect 30–90 min on local machine depending on GPU availability.

## Related

- [[components/OpenSearch]] — index must exist before ingestion
- [[components/Ollama]] — embedding during ingestion
- [[data-sources/NOAA Storm Events]]
- [[data-sources/NWS Area Forecast Discussions]]
