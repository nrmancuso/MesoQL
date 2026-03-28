# GraphQL

Spring GraphQL HTTP API serving as the primary interface to MesoQL.

## Framework

- **`spring-boot-starter-graphql`** (Spring Boot 4.x) — brings in `spring-graphql`, Spring MVC,
  and the GraphQL Java engine
- **`graphql-java-extended-scalars`** — provides the `Long` scalar used for `damageProperty` on
  `StormEventHit`

## Schema Location

`app/src/main/resources/graphql/schema.graphqls`

Spring Boot auto-discovers SDL files from `classpath:graphql/**/*.graphqls`.

## Resolver

`SearchResolver` in `com.mesoql.api`:

- Annotated with `@Controller` (Spring GraphQL convention)
- `@QueryMapping` on `search(source, input)` maps to the `Query.search` root field
- Flow: map GraphQL input → `InputValidator.validate()` → `QueryExecutor.execute(SearchRequest)` →
  return shaped `SearchResponse`
- Mutually exclusive `synthesize`/`clusterByTheme` violation throws `GraphQlException` mapped to
  a GraphQL error

## `Long` Scalar

Registered via a `RuntimeWiringConfigurer` bean that adds `ExtendedScalars.GraphQLLong`. The SDL
declares `scalar Long` and `damageProperty: Long` on `StormEventHit`. Values serialize as JSON
numbers.

## GraphiQL

Enabled at `/graphiql` via `spring.graphql.graphiql.enabled: true` in `application.yml`. Ships
with the Spring Boot starter; no extra dependencies required.

## Union Type

`SearchHit = StormEventHit | ForecastDiscussionHit`

Spring GraphQL requires a `TypeResolver` bean (or `@SchemaMapping` based resolution) for union
types. The resolver inspects the runtime type of each hit and returns the appropriate GraphQL type
name.

## Input Validation

`InputValidator` is a Spring `@Component` in `core/src/main/java/com/mesoql/search/`. It holds
static field schemas for each source and validates:

- Field existence
- Filter type compatibility (keyword vs. numeric/date)
- Mutual exclusion of `synthesize` / `clusterByTheme`
- Enum-valued field constraints (`season`, `state`)

Throws `ValidationException` on failure. `SearchResolver` catches it and converts it to a GraphQL
error response.

## Related

- [[components/OpenSearch]] — field schemas match OpenSearch index mappings
- [[components/Ollama]] — synthesis/explain/cluster calls triggered by resolver
- [[architecture/Overview]] — component dependency order
