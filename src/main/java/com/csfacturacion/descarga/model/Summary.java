package com.csfacturacion.descarga.model;

import com.google.gson.annotations.SerializedName;

public record Summary(
        Long total,
        @SerializedName("paginas") int pages,
        @SerializedName("xmlFaltantes") boolean hasMissingXml,
        Long cancelados) {}
