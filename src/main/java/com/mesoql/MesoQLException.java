package com.mesoql;

public class MesoQLException extends RuntimeException {

    public MesoQLException(String message) {
        super(message);
    }

    public MesoQLException(String message, Throwable cause) {
        super(message, cause);
    }
}
