# Issue #25: Migrate to GraphQL HTTP API

## Summary

Replace the ANTLR4-parsed MesoQL DSL and picocli CLI with a Spring GraphQL HTTP service.
The semantic search, filter, and output-clause semantics are preserved; only the interface changes.

**Decisions locked in:**
- CLI dropped entirely (no picocli, JLine, shell, validate, query commands)
- Unified `search(source, input)` query root + union return type (`StormEventHit | ForecastDiscussionHit`)
- GraphiQL playground enabled at `/graphiql`
- `app/` module repurposed in-place (no directory rename)
- Ingestion triggered via new admin REST endpoints (consequence of dropping CLI)

---

## Architecture After Migration

```text
GraphQL HTTP API  (/graphql)
    └── SearchResolver
            └── InputValidator  ← field schema validation (replaces QueryPlanner)
            └── QueryExecutor   ← unchanged search + LLM logic
                    ├── OpenSearchService
                    └── OllamaClient

Admin REST API  (/admin/*)
    └── IndexController  ← triggers ingesters
    └── StatsController  ← index doc counts + sizes

Ingestion Pipeline  (unchanged)
    ├── StormEventsIngester
    └── AFDIngester
```

**Module layout (after):**

| Module | Purpose |
|---|---|
| `core` | QueryExecutor, SearchRequest types, InputValidator, OpenSearch/Ollama clients |
| `ingestion` | StormEventsIngester, AFDIngester (unchanged) |
| `app` | Spring Boot app, GraphQL schema + resolver, admin controllers |
| `integration-tests` | Full-stack GraphQL endpoint tests via Testcontainers (currently empty, brought into build) |

---

## Proposed GraphQL Schema

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

## Decisions

All open questions are resolved:

| # | Question | Decision |
|---|---|---|
| 1 | `Long` scalar for `damageProperty` | Add `graphql-java-extended-scalars`; use `Long` scalar in SDL |
| 2 | `ComparisonFilter.op` — enum or String | `ComparisonOp` enum (`GT`, `GTE`, `LT`, `LTE`, `EQ`, `NEQ`); update `OpenSearchService` accordingly |
| 3 | Ingestion endpoint — sync or async | `202 Accepted` + UUID job ID; `GET /admin/index/{jobId}` for status polling |
| 4 | Integration test strategy | Testcontainers wrapping the Docker Compose stack; keep `integration-tests` as a separate module |
| 5 | Server port | Keep Spring Boot default `8080` |

### Ingestion job tracking design

```
POST /admin/index/storm-events          (multipart: file=<csv>)
POST /admin/index/forecast-discussions  (query param: since=yyyy-MM-dd, optional)

Response 202:
  { "jobId": "550e8400-...", "status": "RUNNING" }

GET /admin/index/{jobId}

Response 200:
  { "jobId": "550e8400-...", "status": "RUNNING|DONE|FAILED",
    "docsIndexed": 1234, "error": null }
```

Job state is held in an in-memory `ConcurrentHashMap` — no persistence required.
Jobs are keyed by UUID and updated by the background ingestion thread.

### Integration test approach

The `integration-tests` module uses Testcontainers to start OpenSearch and Ollama from the
existing `docker-compose.yml`. Tests run against a real stack, giving higher fidelity than
mocks. The module remains separate from `app` so it can be excluded from the standard
`./gradlew test` run and triggered explicitly (e.g., `./gradlew :integration-tests:test`)
or in a dedicated CI job.

---

## What Gets Removed

### Entire module deleted
- `parser/` — ANTLR4 grammar, generated lexer/parser, `QueryAST`, `MesoQLASTVisitor`,
  `MesoQLParserHelper`, `MesoQLSyntaxException`, `ThrowingErrorListener`

### Files deleted from `core/`
| File | Reason |
|---|---|
| `planner/QueryPlanner.java` | Replaced by `InputValidator` in resolver layer |
| `planner/MesoQLValidationException.java` | Replaced by `ValidationException` |
| `planner/FieldSchema.java` | Moved into `InputValidator` |
| `planner/FieldType.java` | Moved into `InputValidator` |
| `executor/ResultPrinter.java` | CLI concern; JSON serialization handled by Spring GraphQL |
| `test/…/planner/QueryPlannerTest.java` | Replaced by `InputValidatorTest` |

### Files deleted from `app/`
| File | Reason |
|---|---|
| `cli/MesoQLCLI.java` | CLI dropped |
| `cli/ShellCommand.java` | CLI dropped |
| `cli/QueryCommand.java` | CLI dropped |
| `cli/ValidateCommand.java` | CLI dropped (grammar validation no longer applicable) |
| `cli/IndexCommand.java` | Replaced by admin HTTP endpoint |
| `cli/StatsCommand.java` | Replaced by admin HTTP endpoint |
| `MesoQLApplication.java` | Replaced (no longer a `CommandLineRunner`) |

### Docs deleted
| File | Reason |
|---|---|
| `docs/grammar.md` | Grammar removed |
| `docs/cli.md` | CLI removed |

### Obsidian notes deleted
| File | Reason |
|---|---|
| `obsidian/components/Grammar.md` | Grammar removed |
| `obsidian/components/CLI.md` | CLI removed |
| `obsidian/users-guide/Shell.md` | Shell removed |
| `obsidian/users-guide/Query Language.md` | DSL removed; replaced by GraphQL reference |

---

## What Gets Added

### New types in `core/src/main/java/com/mesoql/search/`
| File | Purpose |
|---|---|
| `SearchRequest.java` | Replaces `QueryAST.Query`; plain Java record consumed by `QueryExecutor` |
| `FilterInput.java` | Sealed interface + `InFilterInput`, `BetweenFilterInput`, `ComparisonFilterInput` records |
| `InputValidator.java` | Holds field schemas; validates field names, filter types, mutual exclusion |
| `ValidationException.java` | Thrown by `InputValidator` on invalid input |
| `test/…/search/InputValidatorTest.java` | Unit tests for all validation rules |

**`SearchRequest` shape:**
```java
public record SearchRequest(
    String source,
    String semantic,
    List<FilterInput> filters,
    Optional<String> synthesizePrompt,
    boolean explain,
    boolean clusterByTheme,
    int limit
) {}
```

### New files in `app/`
| File | Purpose |
|---|---|
| `api/SearchResolver.java` | `@QueryMapping` resolver; maps GraphQL input → `SearchRequest` → `QueryExecutor` |
| `admin/IndexController.java` | `POST /admin/index/*` endpoints; returns job ID; `GET /admin/index/{jobId}` for status |
| `admin/IngestionJob.java` | Record holding job state: `jobId`, `status` (RUNNING/DONE/FAILED), `docsIndexed`, `error` |
| `admin/IngestionJobStore.java` | In-memory `ConcurrentHashMap`-backed `@Component` for job tracking |
| `admin/StatsController.java` | `GET /admin/stats` — returns index doc counts and sizes as JSON |
| `MesoQLApplication.java` | Standard `@SpringBootApplication` (no picocli `CommandLineRunner`) |
| `resources/graphql/schema.graphqls` | GraphQL SDL (see above) |

### New docs
| File | Purpose |
|---|---|
| `docs/graphql.md` | Schema reference, example queries, GraphiQL usage |
| `docs/api.md` | HTTP API reference: `/graphql`, `/graphiql`, `/admin/*`; running the server |

### New obsidian notes
| File | Purpose |
|---|---|
| `obsidian/components/GraphQL.md` | Schema decisions, resolver design, spring-graphql config |

### Integration tests
Add `integration-tests` to `settings.gradle.kts` and create:
| File | Purpose |
|---|---|
| `integration-tests/build.gradle.kts` | `@SpringBootTest` + Testcontainers dependencies |
| `integration-tests/src/test/java/…/GraphQLSearchIT.java` | Full-stack tests via Testcontainers (OpenSearch + Ollama); GraphQL wiring, validation, and ingestion job lifecycle |

---

## What Gets Changed

### `settings.gradle.kts`
- Remove `"parser"` from `include()`
- Add `"integration-tests"` to `include()`

### `gradle/libs.versions.toml`
- **Remove:** `antlr`, `antlr-runtime`, `picocli-spring-boot-starter`, `jline`
  (and their `[libraries]` and `[plugins]` entries)
- **Add:** `spring-graphql-starter` entry pointing to
  `org.springframework.boot:spring-boot-starter-graphql` (version from Spring Boot BOM)
- **Add:** `graphql-java-extended-scalars` (`com.graphql-java:graphql-java-extended-scalars`)

### `core/build.gradle.kts`
- Remove `implementation(project(":parser"))` dependency
- `QueryExecutor` no longer imports anything from the `parser` module

### `core/src/main/java/com/mesoql/executor/QueryExecutor.java`
- Change signature: `execute(QueryAST.Query)` → `execute(SearchRequest)`
- Remove `QueryPlanner` field and constructor injection
- Input validation is no longer called here; it lives in `SearchResolver` via `InputValidator`
- Internal logic unchanged (OpenSearch hybrid search + Ollama calls)

### `app/build.gradle.kts`
- **Remove:** `project(":parser")`, `picocli-spring-boot-starter`, `jline`
- **Add:** `spring-boot-starter-graphql` (brings in Spring Web + spring-graphql)
- Keep: `project(":core")`, `project(":ingestion")`

### `app/src/main/resources/application.yml`
```yaml
spring:
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
  main:
    banner-mode: off
    log-startup-info: false

logging:
  level:
    root: WARN

mesoql:
  opensearch-url: http://localhost:9200
  ollama-base-url: http://localhost:11434
  embed-model: nomic-embed-text
  generate-model: llama3
```

### `Justfile`
- **Remove:** `mesoql *args:`, `index-storm file:`, `index-afd:`
- **Add:**
  ```just
  # Start the MesoQL HTTP server (GraphQL at :8080/graphql)
  serve:
      java -jar {{jar}}

  # Index a NOAA Storm Events CSV via admin endpoint (returns job ID)
  index-storm file:
      curl -X POST "http://localhost:8080/admin/index/storm-events" \
           -F "file=@{{file}}"

  # Poll ingestion job status
  index-status job_id:
      curl -s "http://localhost:8080/admin/index/{{job_id}}" | jq .

  # Index NWS AFDs via admin endpoint (returns job ID)
  index-afd:
      curl -X POST "http://localhost:8080/admin/index/forecast-discussions"

  # Show index stats via admin endpoint
  stats:
      curl -s "http://localhost:8080/admin/stats" | jq .
  ```

### `CLAUDE.md`
- Remove grammar references from the architecture section
- Update build commands (replace `just mesoql` with `just serve`)
- Remove `parser` from key dependencies table
- Add `spring-graphql` to key dependencies table
- Remove `docs/grammar.md` and `docs/cli.md` from the documentation list
- Add `docs/graphql.md` and `docs/api.md`

### Obsidian notes updated
| Note | Change |
|---|---|
| `architecture/Overview.md` | Remove Grammar node; replace CLI node with "HTTP API (spring-graphql)" |
| `architecture/Infrastructure.md` | Add server port 8080; update `just` commands |
| `users-guide/Quick Start.md` | Replace shell interaction with GraphiQL / curl examples |
| `users-guide/Deployment.md` | Replace `just mesoql` with `just serve`; add GraphiQL URL |
| `users-guide/Data Ingestion.md` | Replace CLI commands with admin endpoint curl examples |
| `components/OpenSearch.md` | Update field validation note (now in `InputValidator`, not `QueryPlanner`) |

---

## Implementation Phases

### Phase 1 — Strip out the DSL and CLI
1. Delete `parser/` directory
2. Remove `"parser"` from `settings.gradle.kts`
3. Delete `core/src/main/java/com/mesoql/planner/` package
4. Delete `core/src/test/java/com/mesoql/planner/QueryPlannerTest.java`
5. Delete `core/src/main/java/com/mesoql/executor/ResultPrinter.java`
6. Delete `app/src/main/java/com/mesoql/cli/` package
7. Delete `app/src/main/java/com/mesoql/MesoQLApplication.java`
8. Remove antlr, picocli, jline from `libs.versions.toml`
9. Update `core/build.gradle.kts` and `app/build.gradle.kts`
10. Verify `./gradlew compileJava` fails gracefully (QueryExecutor still references old types)

### Phase 2 — Define new input types and refactor `QueryExecutor`
1. Add `SearchRequest.java` and `FilterInput.java` (sealed interface + subtypes) to `core`
2. Add `ValidationException.java` to `core/src/main/java/com/mesoql/search/`
3. Add `InputValidator.java` — field schemas + all validation rules from `QueryPlanner`
4. Refactor `QueryExecutor.execute()` to accept `SearchRequest`
5. Write `InputValidatorTest.java`
6. Verify `./gradlew :core:test` passes

### Phase 3 — Add GraphQL API to `app`
1. Add `spring-boot-starter-graphql` (and optionally `graphql-java-extended-scalars`) to
   `libs.versions.toml` and `app/build.gradle.kts`
2. Write `app/src/main/resources/graphql/schema.graphqls`
3. Write `app/src/main/java/com/mesoql/MesoQLApplication.java` (standard `@SpringBootApplication`)
4. Write `app/src/main/java/com/mesoql/api/SearchResolver.java`:
   - `@QueryMapping` method mapping `search(source, input)` → `InputValidator.validate()` →
     `QueryExecutor.execute(SearchRequest)` → return shaped result
   - Handle `synthesize`/`clusterByTheme` mutual exclusion with a `GraphQlException`
5. Update `application.yml` (add graphiql config)
6. Verify `./gradlew :app:bootJar` and `just serve` starts at `:8080/graphql`

### Phase 4 — Admin HTTP endpoints for ingestion
1. Write `app/src/main/java/com/mesoql/admin/IngestionJob.java` — job state record
2. Write `app/src/main/java/com/mesoql/admin/IngestionJobStore.java` — `@Component` with
   `ConcurrentHashMap<UUID, IngestionJob>`; methods: `create()`, `markDone(id, docsIndexed)`,
   `markFailed(id, error)`, `get(id)`
3. Write `app/src/main/java/com/mesoql/admin/IndexController.java`:
   - `POST /admin/index/storm-events` — multipart CSV; creates job, fires background thread,
     returns `202 { "jobId": "...", "status": "RUNNING" }`
   - `POST /admin/index/forecast-discussions` — optional `?since=yyyy-MM-dd`; same pattern
   - `GET /admin/index/{jobId}` — returns current job state
4. Write `app/src/main/java/com/mesoql/admin/StatsController.java`
   - `GET /admin/stats` — returns JSON with doc count and size per index
5. Update `Justfile` with new `just serve`, `just index-storm`, `just index-afd`, `just stats` targets

### Phase 5 — Integration tests
1. Add `"integration-tests"` to `settings.gradle.kts`
2. Create `integration-tests/build.gradle.kts` with Testcontainers + Spring Boot test dependencies
3. Configure Testcontainers to start services from `docker-compose.yml` (OpenSearch + Ollama)
   before the test suite; wire the container ports into Spring's `application.yml` via
   `@DynamicPropertySource`
4. Write `GraphQLSearchIT.java`:
   - Test happy-path `search` query for each source against real OpenSearch/Ollama containers
   - Test input validation errors (unknown field, `IN` on numeric field, etc.)
   - Test `synthesize`/`clusterByTheme` mutual exclusion rejection
   - Test admin index job lifecycle: POST → 202 → poll GET until DONE
   - Test GraphiQL endpoint responds at `/graphiql`
5. Verify `./gradlew :integration-tests:test` passes with the stack running

### Phase 6 — Docs and obsidian cleanup
1. Delete `docs/grammar.md`, `docs/cli.md`
2. Write `docs/graphql.md` — schema reference, example queries (curl + GraphiQL), field tables
3. Write `docs/api.md` — HTTP API reference, running the server, admin endpoint reference
4. Update `docs/BUILDING.md` — remove ANTLR step, add spring-graphql note
5. Update `CLAUDE.md`
6. Update all obsidian notes listed in the "What Gets Changed" section above
7. Delete `obsidian/components/Grammar.md`, `obsidian/components/CLI.md`,
   `obsidian/users-guide/Shell.md`, `obsidian/users-guide/Query Language.md`
8. Create `obsidian/components/GraphQL.md`
9. Create `obsidian/users-guide/GraphQL API.md` (replaces Query Language)

---

## Acceptance Criteria (from issue)

- [ ] GraphQL schema covers both sources, all filter types, and all output clauses
- [ ] `search` resolver executes correctly against the existing `QueryExecutor`
- [ ] Input validation rejects invalid field names, unsupported filter types per source,
      and mutually exclusive output clauses — with descriptive error messages
- [ ] ANTLR4 grammar, generated parser/lexer, AST visitor removed from the build
- [ ] Existing integration tests updated or replaced to exercise the GraphQL endpoint
- [ ] `just serve` starts the HTTP server; `just index-storm` / `just index-afd` trigger
      ingestion via admin endpoints
