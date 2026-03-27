package com.mesoql.parser;

/**
 * Runtime exception thrown when the MesoQL parser encounters a syntax error.
 */
public class MesoQLSyntaxException extends RuntimeException {

    private final int line;
    private final int column;

    /**
     * Constructs a syntax exception with the offending line, column, and message.
     */
    public MesoQLSyntaxException(int line, int column, String message) {
        super("line " + line + ":" + column + " " + message);
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the line number where the syntax error occurred.
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column position where the syntax error occurred.
     */
    public int getColumn() {
        return column;
    }
}
