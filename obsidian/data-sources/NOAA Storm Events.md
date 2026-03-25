# NOAA Storm Events

**Coverage:** 1.8M+ storm records from 1950 onward
**License:** Public domain
**Format:** CSV (gzipped), one file per year

## Acquisition

Download from NCEI FTP: `ftp://ftp.ncdc.noaa.gov/pub/data/swdi/stormevents/csvfiles/`

Use the `details` file variant:

```text
StormEvents_details-ftp_v1.0_d2023_c20240117.csv.gz
```

Decompress and ingest:

```bash
gunzip StormEvents_details-ftp_v1.0_d2023_c20240117.csv.gz
mesoql index --source storm_events --data ./StormEvents_details-ftp_v1.0_d2023_c20240117.csv
```

## Key Fields

| CSV Column | Index Field | Notes |
|---|---|---|
| `EVENT_ID` | `event_id` | Doc ID |
| `STATE` | `state` | Two-letter abbreviation |
| `EVENT_TYPE` | `event_type` | e.g., "Tornado" |
| `BEGIN_DATE_TIME` | `begin_date` | ISO-8601 |
| `DEATHS_DIRECT` + `DEATHS_INDIRECT` | `fatalities` | Summed |
| `DAMAGE_PROPERTY` | `damage_property` | Parsed from "10.00K" etc. |
| `EPISODE_NARRATIVE` + `EVENT_NARRATIVE` | embedded text | Concatenated |

## Related

- [[components/Ingestion]]
- [[components/OpenSearch]]
