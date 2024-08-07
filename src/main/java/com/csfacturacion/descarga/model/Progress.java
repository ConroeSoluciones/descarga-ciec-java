package com.csfacturacion.descarga.model;

import com.csfacturacion.descarga.contract.QueryRetriever;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public record Progress(
        @SerializedName("estado") QueryRetriever.Status status, @SerializedName("encontrados") long found)
        implements Serializable {}
