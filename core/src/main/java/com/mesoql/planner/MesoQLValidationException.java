package com.mesoql.planner;

/**
 * Exception thrown when a MesoQL query fails semantic or schema validation.
 */
public class MesoQLValidationException extends RuntimeException {

    /**
     * Constructs a new validation exception with the given message.
     */
    public MesoQLValidationException(String message) {
        super(message);
    }
}
