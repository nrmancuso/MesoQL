# Grammar

MesoQL's grammar is defined in `src/main/resources/MesoQL.g4` using ANTLR4. The grammar is the
primary artifact; all parsing, validation, and extension work starts here.

## Maven Setup

Add the ANTLR4 Maven plugin to `pom.xml`. It generates lexer/parser sources from the `.g4` file at
compile time into `target/generated-sources/antlr4`.

```xml
<plugin>
    <groupId>org.antlr</groupId>
    <artifactId>antlr4-maven-plugin</artifactId>
    <version>4.13.1</version>
    <executions>
        <execution>
            <goals>
                <goal>antlr4</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <sourceDirectory>${basedir}/src/main/resources</sourceDirectory>
        <listener>false</listener>
        <visitor>true</visitor>
    </configuration>
</plugin>

<dependency>
    <groupId>org.antlr</groupId>
    <artifactId>antlr4-runtime</artifactId>
    <version>4.13.1</version>
</dependency>
```

`listener` is disabled; `visitor` is enabled. MesoQL uses the visitor pattern for AST construction.

## Grammar Structure

```text
query
  searchClause       → SEARCH <source>
  whereClause        → WHERE semanticClause filterClause*
    semanticClause   → SEMANTIC("...")         ← required
    filterClause     → AND filter
      filter         → inFilter | betweenFilter | comparisonFilter
  outputClause*      → SYNTHESIZE | CLUSTER BY THEME | EXPLAIN | LIMIT
```

Key grammar decisions:

- `caseInsensitive = true` (ANTLR 4.10+) handles case-insensitivity globally; no per-token
  character class workarounds needed
- Multi-character operators (`>=`, `<=`, `!=`) are declared before single-character variants
  (`>`, `<`, `=`) in the lexer; ANTLR lexer rules are order-sensitive
- `BETWEEN x AND y` reuses the `AND` keyword; ANTLR4's LL(*) parser resolves the ambiguity
  correctly in context
- The `source` rule is the designated extension point; adding a new data source is a one-line
  grammar change plus a new `Ingester` implementation

## Visitor Implementation

The ANTLR plugin generates `MesoQLVisitor<T>` and `MesoQLBaseVisitor<T>`. Extend
`MesoQLBaseVisitor<QueryAST.Node>` in `MesoQLVisitor.java`:

```java
public class MesoQLVisitor extends MesoQLBaseVisitor<QueryAST.Node> {

    @Override
    public QueryAST.Node visitQuery(MesoQLParser.QueryContext ctx) {
        QueryAST.SearchClause search = (QueryAST.SearchClause) visit(ctx.searchClause());
        QueryAST.WhereClause where = (QueryAST.WhereClause) visit(ctx.whereClause());
        List<QueryAST.OutputClause> outputs = ctx.outputClause().stream()
            .map(c -> (QueryAST.OutputClause) visit(c))
            .toList();
        return new QueryAST.Query(search, where, outputs);
    }

    @Override
    public QueryAST.Node visitSearchClause(MesoQLParser.SearchClauseContext ctx) {
        String source = ctx.source().getText().toLowerCase();
        return new QueryAST.SearchClause(source);
    }

    @Override
    public QueryAST.Node visitSemanticClause(MesoQLParser.SemanticClauseContext ctx) {
        String text = stripQuotes(ctx.STRING().getText());
        return new QueryAST.SemanticClause(text);
    }

    @Override
    public QueryAST.Node visitInFilter(MesoQLParser.InFilterContext ctx) {
        String field = ctx.field().getText();
        List<String> values = ctx.stringList().STRING().stream()
            .map(t -> stripQuotes(t.getText()))
            .toList();
        return new QueryAST.InFilter(field, values);
    }

    @Override
    public QueryAST.Node visitBetweenFilter(MesoQLParser.BetweenFilterContext ctx) {
        String field = ctx.field().getText();
        double low = Double.parseDouble(ctx.numericLiteral(0).getText());
        double high = Double.parseDouble(ctx.numericLiteral(1).getText());
        return new QueryAST.BetweenFilter(field, low, high);
    }

    @Override
    public QueryAST.Node visitComparisonFilter(MesoQLParser.ComparisonFilterContext ctx) {
        String field = ctx.field().getText();
        String op = ctx.comparisonOp().getText();
        String value = ctx.literal().getText();
        return new QueryAST.ComparisonFilter(field, op, stripQuotes(value));
    }

    // visitSynthesizeClause, visitClusterClause, visitExplainClause, visitLimitClause follow same pattern

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
```

## QueryAST Node Types

Define sealed interfaces for type safety:

```java
public class QueryAST {

    public sealed interface Node permits Query, SearchClause, WhereClause,
        SemanticClause, InFilter, BetweenFilter, ComparisonFilter,
        SynthesizeClause, ClusterClause, ExplainClause, LimitClause {}

    public record Query(SearchClause search, WhereClause where, List<OutputClause> outputs) implements Node {}
    public record SearchClause(String source) implements Node {}
    public record WhereClause(SemanticClause semantic, List<Filter> filters) implements Node {}
    public record SemanticClause(String text) implements Node {}

    public sealed interface Filter extends Node permits InFilter, BetweenFilter, ComparisonFilter {}
    public record InFilter(String field, List<String> values) implements Filter {}
    public record BetweenFilter(String field, double low, double high) implements Filter {}
    public record ComparisonFilter(String field, String op, String value) implements Filter {}

    public sealed interface OutputClause extends Node permits SynthesizeClause, ClusterClause, ExplainClause, LimitClause {}
    public record SynthesizeClause(String question) implements OutputClause {}
    public record ClusterClause() implements OutputClause {}
    public record ExplainClause() implements OutputClause {}
    public record LimitClause(int n) implements OutputClause {}
}
```

## Parsing Entry Point

```java
public static QueryAST.Query parse(String input) {
    CharStream chars = CharStreams.fromString(input);
    MesoQLLexer lexer = new MesoQLLexer(chars);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    MesoQLParser parser = new MesoQLParser(tokens);

    // Fail fast on syntax errors
    parser.removeErrorListeners();
    parser.addErrorListener(new ThrowingErrorListener());

    MesoQLParser.QueryContext tree = parser.query();
    return (QueryAST.Query) new MesoQLVisitor().visit(tree);
}
```

`ThrowingErrorListener` should extend `BaseErrorListener` and throw a `MesoQLSyntaxException` from
`syntaxError(...)` with the line, column, and message.

## Extending Sources

To add a new source (e.g., `climate_normals`):

1. Add to the `source` rule in `MesoQL.g4`:

```antlr
source
    : STORM_EVENTS
    | FORECAST_DISCUSSIONS
    | CLIMATE_NORMALS        // new
    ;

CLIMATE_NORMALS : 'climate_normals' ;
```

2. Add field definitions to `QueryPlanner` (see [opensearch.md](opensearch.md) for the validation
   model)
3. Implement `ClimateNormalsIngester` (see [ingestion.md](ingestion.md))
