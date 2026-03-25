# Grammar

**File:** `src/main/antlr/MesoQL.g4`
**Tool:** ANTLR4 4.13.x, Gradle `antlr` plugin
**Generated sources:** `build/generated-sources/antlr/main/java` (at compile time, no manual step)

## Query Structure

```text
SEARCH <source>
WHERE SEMANTIC("...") [AND <filter>]*
[SYNTHESIZE "..." | CLUSTER BY THEME | EXPLAIN | LIMIT n]*
```

`SEMANTIC(...)` is required. Output clauses are optional and composable (except `SYNTHESIZE` and
`CLUSTER BY THEME` cannot both be present).

## AST Types

```text
Query(SearchClause, WhereClause, List<OutputClause>)
  WhereClause → SemanticClause (required) + List<Filter>
  Filter subtypes: InFilter, BetweenFilter, ComparisonFilter
  OutputClause subtypes: SynthesizeClause, ClusterClause, ExplainClause, LimitClause
```

Implemented as Java sealed interfaces + records. `MesoQLVisitor` extends
`MesoQLBaseVisitor<QueryAST.Node>` (visitor pattern, not listener).

## Key Grammar Decisions

- `caseInsensitive = true` (ANTLR 4.10+) — handles case globally, no per-token workarounds
- Multi-char operators (`>=`, `<=`, `!=`) declared before single-char variants in lexer
- `BETWEEN x AND y` reuses `AND` keyword — ANTLR4 LL(*) resolves context correctly
- `source` rule is the extension point for new data sources

## Extending Sources

1. Add to `source` rule in `MesoQL.g4` (one line)
2. Add field schema to `QueryPlanner`
3. Implement new `Ingester`

## Related

- [[components/OpenSearch]] — field schema validation in QueryPlanner
- [[architecture/Overview]] — build phase 1
