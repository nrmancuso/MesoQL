# CLI

MesoQL is a CLI tool built with picocli and Spring Boot. This document covers dependency setup,
command structure, and output modes.

## Dependency

```kotlin
implementation("info.picocli:picocli-spring-boot-starter:4.7.5")
```

The `picocli-spring-boot-starter` integrates picocli with Spring Boot's DI container — all
`@Command` classes are Spring beans and can use `@Autowired`/constructor injection.

## Entry Point

```java
@SpringBootApplication
public class MesoQLApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
            SpringApplication.run(MesoQLApplication.class, args)
        ));
    }
}
```

```java
@Command(
    name = "mesoql",
    mixinStandardHelpOptions = true,
    version = "MesoQL 0.1.0",
    description = "Semantic search over weather data.",
    subcommands = {
        QueryCommand.class,
        IndexCommand.class,
        ValidateCommand.class,
        StatsCommand.class,
        ShellCommand.class
    }
)
@Component
public class MesoQLCLI implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
```

## Commands

### `mesoql query`

Parses and executes a MesoQL query.

```java
@Command(name = "query", description = "Execute a MesoQL query.")
public class QueryCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to .mql query file.", arity = "0..1")
    private Path queryFile;

    @Option(names = "--inline", description = "Run an inline query string instead of a file.")
    private String inlineQuery;

    @Option(names = "--json", description = "Output results as JSON.")
    private boolean json;

    @Option(names = "--config", description = "Path to config file.", defaultValue = "~/.mesoql/config.yaml")
    private Path configFile;

    @Override
    public Integer call() {
        String queryText = resolveQuery();
        MesoQLConfig config = MesoQLConfig.load(configFile);
        QueryAST.Query ast = MesoQLParser.parse(queryText);
        QueryPlan plan = new QueryPlanner().plan(ast);
        QueryResult result = new QueryExecutor(config).execute(plan);
        new ResultPrinter(json).print(result);
        return 0;
    }

    private String resolveQuery() {
        if (inlineQuery != null) return inlineQuery;
        if (queryFile != null) return Files.readString(queryFile);
        throw new ParameterException("Provide a query file or --inline.");
    }
}
```

Usage:

```bash
mesoql query my_query.mql
mesoql query --inline "SEARCH storm_events WHERE SEMANTIC(\"tornado\") LIMIT 5"
mesoql query my_query.mql --json
mesoql query my_query.mql --config ./custom-config.yaml
```

### `mesoql index`

Runs the ingestion pipeline for a data source.

```java
@Command(name = "index", description = "Index a data source into OpenSearch.")
public class IndexCommand implements Callable<Integer> {

    @Option(names = "--source", required = true, description = "storm_events | forecast_discussions")
    private String source;

    @Option(names = "--data", description = "Path to CSV file (storm_events only).")
    private Path dataFile;

    @Option(names = "--since", description = "Backfill AFDs since this date (forecast_discussions only). Format: yyyy-MM-dd")
    private String since;

    @Option(names = "--config", defaultValue = "~/.mesoql/config.yaml")
    private Path configFile;

    @Override
    public Integer call() {
        MesoQLConfig config = MesoQLConfig.load(configFile);
        switch (source) {
            case "storm_events" -> new StormEventsIngester(config).ingest(dataFile);
            case "forecast_discussions" -> new AFDIngester(config).ingest(since);
            default -> throw new ParameterException("Unknown source: " + source);
        }
        return 0;
    }
}
```

Usage:

```bash
mesoql index --source storm_events --data ./StormEvents_2023.csv
mesoql index --source forecast_discussions --since 2020-01-01
```

### `mesoql validate`

Parses and validates a query without executing it. No network calls.

```java
@Command(name = "validate", description = "Validate a MesoQL query without executing it.")
public class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to .mql query file.")
    private Path queryFile;

    @Override
    public Integer call() {
        String queryText = Files.readString(queryFile);
        try {
            QueryAST.Query ast = MesoQLParser.parse(queryText);
            new QueryPlanner().plan(ast);
            System.out.println("OK");
            return 0;
        } catch (MesoQLSyntaxException | MesoQLValidationException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
```

Usage:

```bash
mesoql validate my_query.mql
# OK
# or: Error: Unknown field 'magnitude' for source 'storm_events'
```

### `mesoql stats`

Prints index stats for all MesoQL-managed indices.

```java
@Command(name = "stats", description = "Show OpenSearch index stats.")
public class StatsCommand implements Callable<Integer> {

    @Option(names = "--config", defaultValue = "~/.mesoql/config.yaml")
    private Path configFile;

    @Override
    public Integer call() {
        MesoQLConfig config = MesoQLConfig.load(configFile);
        OpenSearchClient client = OpenSearchClientFactory.create(config);
        // Print doc count and index size for storm_events and forecast_discussions
        for (String index : List.of("storm_events", "forecast_discussions")) {
            IndicesStatsResponse stats = client.indices().stats(s -> s.index(index));
            long docCount = stats.indices().get(index).total().docs().count();
            long sizeBytes = stats.indices().get(index).total().store().sizeInBytes();
            System.out.printf("%-30s docs: %,d  size: %s%n",
                index, docCount, humanReadableBytes(sizeBytes));
        }
        return 0;
    }
}
```

Usage:

```bash
mesoql stats
# storm_events                   docs: 1,842,301  size: 4.2 GB
# forecast_discussions           docs: 214,083    size: 1.1 GB
```

### `mesoql shell`

Starts an interactive REPL for running queries without re-invoking the CLI each time. Useful for
exploratory querying.

```java
@Command(name = "shell", description = "Start an interactive MesoQL REPL.")
public class ShellCommand implements Callable<Integer> {

    @Option(names = "--config", defaultValue = "~/.mesoql/config.yaml")
    private Path configFile;

    @Override
    public Integer call() {
        MesoQLConfig config = MesoQLConfig.load(configFile);
        QueryExecutor executor = new QueryExecutor(config);
        Scanner scanner = new Scanner(System.in);
        System.out.println("MesoQL shell. Type 'exit' to quit.");
        while (true) {
            System.out.print("mesoql> ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            if (line.isEmpty()) continue;
            try {
                QueryAST.Query ast = MesoQLParser.parse(line);
                QueryPlan plan = new QueryPlanner().plan(ast);
                QueryResult result = executor.execute(plan);
                new ResultPrinter(false).print(result);
            } catch (MesoQLSyntaxException | MesoQLValidationException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        return 0;
    }
}
```

Usage:

```bash
mesoql shell
# mesoql> SEARCH storm_events WHERE SEMANTIC("hail") LIMIT 3
# mesoql> exit
```

The REPL reuses the same `QueryExecutor` (and its underlying HTTP clients) across queries, avoiding
reconnection overhead.

## Output Modes

**Human-readable (default):** Results are printed as numbered entries with metadata and narrative
text, followed by the synthesis/explanation if an output clause was specified.

**JSON (`--json`):** Results are emitted as a JSON array. Each element includes all metadata fields,
the narrative text, and the LLM output if applicable. Useful for piping into `jq` or downstream
tooling.

## Config File

`~/.mesoql/config.yaml`:

```yaml
opensearch_url: http://localhost:9200
ollama_base_url: http://localhost:11434
embed_model: nomic-embed-text
generate_model: llama3
```

All fields have defaults (see [ollama.md](ollama.md)); the config file only needs to include
overrides.

## Config

Spring Boot binds config from `src/main/resources/application.yml`:

```yaml
mesoql:
  opensearch-url: http://localhost:9200
  ollama-base-url: http://localhost:11434
  embed-model: nomic-embed-text
  generate-model: llama3
```

Override at runtime with standard Spring Boot mechanisms (`--mesoql.opensearch-url=...`,
environment variables, etc.).

## Packaging

Build and run the executable fat JAR via the Spring Boot Gradle plugin:

```bash
./gradlew bootJar
# output: build/libs/mesoql-0.1.0.jar
```

Wrap in a shell script named `mesoql`:

```bash
#!/bin/bash
exec java -jar /usr/local/lib/mesoql/mesoql.jar "$@"
```

The only external requirement is Java 21.
