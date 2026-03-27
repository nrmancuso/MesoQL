package com.mesoql.cli;

import com.mesoql.ast.QueryAST;
import com.mesoql.executor.QueryExecutor;
import com.mesoql.executor.QueryResult;
import com.mesoql.executor.ResultPrinter;
import com.mesoql.parser.MesoQLParserHelper;
import com.mesoql.parser.MesoQLSyntaxException;
import com.mesoql.planner.MesoQLValidationException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

@Command(name = "shell", description = "Start an interactive MesoQL shell.")
@Component
public class ShellCommand implements Callable<Integer> {

    private final QueryExecutor executor;

    public ShellCommand(QueryExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Integer call() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            final PrintWriter out = terminal.writer();

            final LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                    .variable(LineReader.HISTORY_FILE,
                            System.getProperty("user.home") + "/.mesoql_history")
                    .build();

            out.println("MesoQL (type \\q to quit)");
            out.println();
            out.flush();

            while (true) {
                final String line;
                try {
                    line = reader.readLine("mesoql> ").trim();
                } catch (UserInterruptException e) {
                    continue;
                } catch (EndOfFileException e) {
                    break;
                }

                if (line.isEmpty()) continue;
                if (line.equals("\\q") || line.equalsIgnoreCase("exit")
                        || line.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    final QueryAST.Query ast = MesoQLParserHelper.parse(line);
                    final QueryResult result = executor.execute(ast);
                    out.println();
                    new ResultPrinter(false).print(result, out);
                    out.println();
                    out.flush();
                } catch (Throwable e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    out.println("ERROR: " + msg);
                    out.flush();
                }
            }
        }
        return 0;
    }
}
