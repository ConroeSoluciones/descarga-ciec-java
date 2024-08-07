package com.csfacturacion.descarga.error;

public class InvalidQueryException extends Exception {

    public InvalidQueryException(String message) {
        super(message);
    }

    public InvalidQueryException(Throwable cause) {
        super(cause);
    }
}
