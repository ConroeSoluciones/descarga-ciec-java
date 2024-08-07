package com.csfacturacion.descarga.model;

import java.io.Serial;
import java.io.Serializable;

public record EmpresaFiscal(String rfc, String razonSocial) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
