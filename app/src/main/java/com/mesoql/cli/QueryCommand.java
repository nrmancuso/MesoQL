package com.mesoql.cli;

import com.mesoql.ast.QueryAST;
import com.mesoql.executor.QueryExecutor;
import com.mesoql.executor.QueryResult;
import com.mesoql.executor.ResultPrinter;
import com.mesoql.parser.MesoQLParserHelper;
import com.mesoql.planner.QueryPlanner;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "query", description = "Execute a MesoQL query.")
@Component
public class QueryCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to .mql query file.", arity = "0..1")
    private Path queryFile;

    @Option(names = "--inline", description = "Run an inline query string instead of a file.")
    private String inlineQuery;

    @Option(names = "--json", description = "Output results as JSON.")
    private boolean json;

    private final QueryExecutor executor;

    public QueryCommand(QueryExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Integer call() throws Exception {
        final String queryText = resolveQuery();
        final QueryAST.Query ast = MesoQLParserHelper.parse(queryText);
        final QueryResult result = executor.execute(ast);
        new ResultPrinter(json).print(result);
        return 0;
    }

    private String resolveQuery() throws Exception {
        if (inlineQuery != null) return inlineQuery;
        if (queryFile != null) return Files.readString(queryFile);
        throw new IllegalArgumentException("Provide a query file or --inline.");
    }
}
