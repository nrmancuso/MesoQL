package com.mesoql.cli;

import com.mesoql.ast.QueryAST;
import com.mesoql.parser.MesoQLParserHelper;
import com.mesoql.parser.MesoQLSyntaxException;
import com.mesoql.planner.MesoQLValidationException;
import com.mesoql.planner.QueryPlanner;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Validates a MesoQL query without executing it.
 */
@Command(name = "validate", description = "Validate a MesoQL query without executing it.")
@Component
public class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to .mql query file.")
    private Path queryFile;

    private final QueryPlanner planner;

    /**
     * Constructs the validate command with its planner dependency.
     *
     * @param planner the query planner
     */
    public ValidateCommand(QueryPlanner planner) {
        this.planner = planner;
    }

    /**
     * Parses and validates the supplied query file.
     *
     * @return exit code `0` when validation succeeds, `1` otherwise
     * @throws Exception if the query file cannot be read
     */
    @Override
    public Integer call() throws Exception {
        final String queryText = Files.readString(queryFile);
        try {
            final QueryAST.Query ast = MesoQLParserHelper.parse(queryText);
            planner.validate(ast);
            System.out.println("OK");
            return 0;
        } catch (MesoQLSyntaxException | MesoQLValidationException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
