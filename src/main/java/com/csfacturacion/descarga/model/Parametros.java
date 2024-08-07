package com.csfacturacion.descarga.model;

import java.time.LocalDateTime;

public class Parametros {

    private Parametros(Builder builder) {
        rfcBusqueda = builder.rfcBusqueda;
        fechaInicio = builder.fechaInicio;
        fechaFin = builder.fechaFin;
        status = builder.status;
        tipo = builder.tipo;
        modoBusqueda = builder.modoBusqueda;
        servicio = builder.servicio;
        credenciales = builder.credenciales;
        tipoDoc = builder.tipoDoc;
    }

    public enum Status {
        VIGENTE,
        CANCELADO,
        TODOS
    }

    public enum Tipo {
        EMITIDAS,
        RECIBIDAS,
        TODAS
    }

    public enum TipoDoc {
        CFDI,
        RETENCION
    }

    /**
     * Para las consultas de comprobantes recibidos, es necesario permitir
     * especificar el tipo de búsqueda que se quiere realizar, ya que en
     * ocasiones, el portal del SAT devuelve resultados erróneos si se busca en
     * rangos de tiempo que contienen muchos CFDIs (más de 500), en esos casos
     * es necesario acotar los rangos de búsqueda. Sin embargo, si se realiza
     * esta acción para todas las consultas, estas tardaran mucho más tiempo del
     * actual, siendo innecesario en la mayoría de los casos.
     *
     * @author emerino
     */
    public enum ModoBusqueda {
        NORMAL, // busca por día
        EXHAUSTIVA // busca por hora
    }

    public enum Servicio {
        CSREPORTER("CSRN"),
        API_CIEC("CRAPI");

        private final String clave;

        Servicio(String clave) {
            this.clave = clave;
        }

        public String clave() {
            return clave;
        }
    }

    private final String rfcBusqueda;

    private final LocalDateTime fechaInicio;

    private final LocalDateTime fechaFin;

    private final Status status;

    private final Tipo tipo;

    private final ModoBusqueda modoBusqueda;

    private final Servicio servicio;

    private final TipoDoc tipoDoc;

    private final Credenciales credenciales;

    public ModoBusqueda getModoBusqueda() {
        return modoBusqueda;
    }

    public String getRfcBusqueda() {
        return rfcBusqueda;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public Status getStatus() {
        return status;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public TipoDoc getTipoDoc() {
        return tipoDoc;
    }

    public Credenciales getCredenciales() {
        return credenciales;
    }

    public static final class Builder {
        private String rfcBusqueda;

        private LocalDateTime fechaInicio;

        private LocalDateTime fechaFin = LocalDateTime.now();

        private Status status = Status.TODOS;

        private Tipo tipo;

        private Servicio servicio = Servicio.CSREPORTER;

        private ModoBusqueda modoBusqueda = ModoBusqueda.NORMAL;

        private TipoDoc tipoDoc = TipoDoc.CFDI;

        private Credenciales credenciales;

        public ModoBusqueda getModoBusqueda() {
            return modoBusqueda;
        }

        public Builder modoBusqueda(ModoBusqueda modoBusqueda) {
            this.modoBusqueda = modoBusqueda;
            return this;
        }

        /**
         * El Servicio a utilizar en la consulta, por defecto CSREPORTER.
         *
         * @param servicio a utilizar.
         * @return este builder, para encadenamiento.
         */
        public Builder servicio(Servicio servicio) {
            this.servicio = servicio;
            return this;
        }

        /**
         * El RFC del emisor/receptor, según el status definido.
         *
         * @param rfc a buscar.
         * @return este builder, para encadenamiento.
         */
        public Builder rfcBusqueda(String rfc) {
            this.rfcBusqueda = rfc;
            return this;
        }

        /**
         * Fecha de emisión inicial de los CFDIs a buscar.
         *
         * @param fechaInicio para buscar.
         * @return este builder, para encadenamiento.
         */
        public Builder fechaInicio(LocalDateTime fechaInicio) {
            this.fechaInicio = fechaInicio;
            return this;
        }

        /**
         * Fecha de emisión final de los CFDIs a buscar.
         *
         * @param fechaFin para buscar.
         * @return este builder, para encadenamiento.
         */
        public Builder fechaFin(LocalDateTime fechaFin) {
            this.fechaFin = fechaFin;
            return this;
        }

        /**
         * El status de los CFDIs a buscar, por defecto es TODOS.
         *
         * @param status a buscar.
         * @return este builder, para encadenamiento.
         */
        public Builder status(Status status) {
            if (status != null) {
                this.status = status;
            }
            return this;
        }

        /**
         * El tipo de comprobantes a buscar (emitidos/recibidos).
         *
         * @param tipo de comprobante.
         * @return este builder, para encadenamiento.
         */
        public Builder tipo(Tipo tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder tipoDoc(TipoDoc tipoDoc) {
            this.tipoDoc = tipoDoc;
            return this;
        }

        public Builder credenciales(Credenciales credenciales) {
            this.credenciales = credenciales;
            return this;
        }

        public Parametros build() {
            if (fechaInicio.isAfter(fechaFin)) {
                throw new IllegalStateException("La fecha de inicio debe ser <= fechaFin");
            }

            if (credenciales == null || credenciales.user() == null || credenciales.password() == null) {
                throw new IllegalStateException("Las credenciales SAT deben establecerse");
            }

            return new Parametros(this);
        }

        public String getRfcBusqueda() {
            return rfcBusqueda;
        }

        public LocalDateTime getFechaInicio() {
            return fechaInicio;
        }

        public LocalDateTime getFechaFin() {
            return fechaFin;
        }

        public Status getStatus() {
            return status;
        }

        public Tipo getTipo() {
            return tipo;
        }

        public Servicio getServicio() {
            return servicio;
        }

        public TipoDoc getTipoDoc() {
            return tipoDoc;
        }

        public Credenciales getCredenciales() {
            return credenciales;
        }
    }
}
