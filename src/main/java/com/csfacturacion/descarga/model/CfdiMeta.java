package com.csfacturacion.descarga.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class CfdiMeta implements Serializable, Comparable<CfdiMeta> {

    private CfdiMeta(Builder builder) {
        folio = builder.folio;
        emisor = builder.emisor;
        receptor = builder.receptor;
        fechaEmision = builder.fechaEmision;
        fechaCertificacion = builder.fechaCertificacion;
        PACCertificador = builder.pacCertificador;
        total = builder.total;
        tipo = builder.tipo;
        status = builder.status;
    }

    /**
     * Tipo marcado en el CFDIMeta.
     */
    public enum Tipo {
        INGRESO,
        EGRESO,
        TRASLADO,
        PAGO,
        NOMINA
    }

    /**
     * Status del CFDIMeta, de acuerdo a los valores reportados por el SAT.
     */
    public enum Status {
        CANCELADO,
        VIGENTE
    }

    private UUID folio;

    private EmpresaFiscal emisor;

    private EmpresaFiscal receptor;

    private LocalDateTime fechaEmision;

    private LocalDateTime fechaCertificacion;

    private EmpresaFiscal PACCertificador;

    private BigDecimal total;

    private Tipo tipo;

    private Status status;

    protected CfdiMeta() {}

    public UUID getFolio() {
        return folio;
    }

    public EmpresaFiscal getEmisor() {
        return emisor;
    }

    public EmpresaFiscal getReceptor() {
        return receptor;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public LocalDateTime getFechaCertificacion() {
        return fechaCertificacion;
    }

    public EmpresaFiscal getPACCertificador() {
        return PACCertificador;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public static final class Builder {
        private UUID folio;
        private EmpresaFiscal emisor;
        private EmpresaFiscal receptor;
        private LocalDateTime fechaEmision;
        private LocalDateTime fechaCertificacion;
        private EmpresaFiscal pacCertificador;
        private BigDecimal total;
        private Tipo tipo;
        private Status status;

        public Builder() {}

        public Builder folio(String val) {
            folio = UUID.fromString(val);
            return this;
        }

        public Builder emisor(EmpresaFiscal val) {
            emisor = val;
            return this;
        }

        public Builder receptor(EmpresaFiscal val) {
            receptor = val;
            return this;
        }

        public Builder fechaEmision(LocalDateTime val) {
            fechaEmision = val;
            return this;
        }

        public Builder fechaCertificacion(LocalDateTime val) {
            fechaCertificacion = val;
            return this;
        }

        public Builder pacCertificador(EmpresaFiscal val) {
            pacCertificador = val;
            return this;
        }

        public Builder total(BigDecimal val) {
            total = val;
            return this;
        }

        public Builder tipo(Tipo val) {
            tipo = val;
            return this;
        }

        public Builder status(Status val) {
            status = val;
            return this;
        }

        public CfdiMeta build() {
            return new CfdiMeta(this);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.folio != null ? this.folio.hashCode() : 0);
        hash = 59 * hash + (this.status != null ? this.status.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CfdiMeta other = (CfdiMeta) obj;
        if (!Objects.equals(this.folio, other.folio)) {
            return false;
        }
        return this.status == other.status;
    }

    @Override
    public int compareTo(CfdiMeta o) {
        int comp = getFechaEmision().compareTo(o.getFechaEmision());
        if (comp == 0) {
            comp = getFolio().compareTo(o.getFolio());
        }
        return comp;
    }
}
