package com.csfacturacion.descarga.error;

public class ZipException extends Exception {
    public ZipException(String message) {
        super(message);
    }

    public ZipException(Throwable cause) {
        super(cause);
    }
}
