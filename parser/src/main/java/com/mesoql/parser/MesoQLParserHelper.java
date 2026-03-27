package com.mesoql.parser;

import com.mesoql.ast.MesoQLASTVisitor;
import com.mesoql.ast.QueryAST;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * Utility class for parsing MesoQL query strings into an AST.
 */
public class MesoQLParserHelper {

    /**
     * Parses a MesoQL query string and returns the root {@link QueryAST.Query} node.
     */
    public static QueryAST.Query parse(String input) {
        final CharStream chars = CharStreams.fromString(input);
        final MesoQLLexer lexer = new MesoQLLexer(chars);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener());

        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final MesoQLParser parser = new MesoQLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener());

        final MesoQLParser.QueryContext tree = parser.query();
        return (QueryAST.Query) new MesoQLASTVisitor().visit(tree);
    }
}
