package com.mesoql;

/**
 * General-purpose runtime exception thrown by the MesoQL query engine.
 */
public class MesoQLException extends RuntimeException {

    /**
     * Constructs a new exception with the given message.
     */
    public MesoQLException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given message and cause.
     */
    public MesoQLException(String message, Throwable cause) {
        super(message, cause);
    }
}
