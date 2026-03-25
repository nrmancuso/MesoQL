package com.mesoql.parser;

public class MesoQLSyntaxException extends RuntimeException {

    private final int line;
    private final int column;

    public MesoQLSyntaxException(int line, int column, String message) {
        super("line " + line + ":" + column + " " + message);
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
