# NWS Area Forecast Discussions

**Coverage:** Daily meteorologist-authored forecast discussions by NWS office
**License:** Public domain
**API:** `api.weather.gov` (no authentication required)

## Acquisition

```
GET https://api.weather.gov/products?type=AFD&limit=500
GET https://api.weather.gov/products/{productId}
```

NWS API returns the most recent 500 products per request. For historical backfill, use the
Iowa Environmental Mesonet (IEM) archive:

```
https://mesonet.agron.iastate.edu/api/1/nwstext_search.json
```

## Key Fields

| API Field | Index Field | Notes |
|---|---|---|
| `id` | `discussion_id` | Doc ID |
| `issuanceTime` | `issuance_time` | ISO-8601 |
| `senderName` | `office` | e.g., "NWS Seattle WA" |
| `productText` | `text` | Full discussion; chunked before indexing |

`region` derived from `senderName` via static office→region map.
`season` derived from `issuanceTime` month.

## Related

- [[components/Ingestion]]
- [[components/OpenSearch]]
