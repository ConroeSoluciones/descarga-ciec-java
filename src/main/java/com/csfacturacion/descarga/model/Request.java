package com.csfacturacion.descarga.model;

public final class Request {

    public enum MediaType {
        JSON("application/json"),
        XML("application/xml"),
        ZIP("application/zip");

        private final String value;

        MediaType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
