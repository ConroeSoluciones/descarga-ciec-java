package com.csfacturacion.descarga.error;

import java.util.UUID;

public class QueryNotReadyYet extends Exception {

    public QueryNotReadyYet(UUID folio) {
        super("La consulta con folio " + folio + " aun no ha terminado");
    }
}
