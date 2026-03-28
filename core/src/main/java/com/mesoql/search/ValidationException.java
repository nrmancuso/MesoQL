package com.mesoql.search;

/**
 * Unchecked exception thrown by {@link InputValidator} when a {@link SearchRequest} is invalid.
 */
public class ValidationException extends RuntimeException {

    /**
     * Constructs a new ValidationException with the given message.
     */
    public ValidationException(String message) {
        super(message);
    }
}
