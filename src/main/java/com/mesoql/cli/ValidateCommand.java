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

@Command(name = "validate", description = "Validate a MesoQL query without executing it.")
@Component
public class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to .mql query file.")
    private Path queryFile;

    private final QueryPlanner planner;

    public ValidateCommand(QueryPlanner planner) {
        this.planner = planner;
    }

    @Override
    public Integer call() throws Exception {
        String queryText = Files.readString(queryFile);
        try {
            QueryAST.Query ast = MesoQLParserHelper.parse(queryText);
            planner.validate(ast);
            System.out.println("OK");
            return 0;
        } catch (MesoQLSyntaxException | MesoQLValidationException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
