package com.mesoql.cli;

import com.mesoql.ast.QueryAST;
import com.mesoql.executor.QueryExecutor;
import com.mesoql.executor.QueryResult;
import com.mesoql.executor.ResultPrinter;
import com.mesoql.parser.MesoQLParserHelper;
import com.mesoql.parser.MesoQLSyntaxException;
import com.mesoql.planner.MesoQLValidationException;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.util.Scanner;
import java.util.concurrent.Callable;

@Command(name = "shell", description = "Start an interactive MesoQL REPL.")
@Component
public class ShellCommand implements Callable<Integer> {

    private final QueryExecutor executor;

    public ShellCommand(QueryExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Integer call() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("MesoQL shell. Type 'exit' to quit.");
        while (true) {
            System.out.print("mesoql> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            if (line.isEmpty()) continue;
            try {
                QueryAST.Query ast = MesoQLParserHelper.parse(line);
                QueryResult result = executor.execute(ast);
                new ResultPrinter(false).print(result);
            } catch (MesoQLSyntaxException | MesoQLValidationException e) {
                System.err.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        return 0;
    }
}
