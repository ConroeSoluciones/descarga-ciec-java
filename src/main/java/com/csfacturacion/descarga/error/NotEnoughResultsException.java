package com.csfacturacion.descarga.error;

public class NotEnoughResultsException extends RuntimeException {
    public NotEnoughResultsException(String message) {
        super(message);
    }
}
