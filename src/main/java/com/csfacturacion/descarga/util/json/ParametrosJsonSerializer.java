package com.csfacturacion.descarga.util.json;

import com.csfacturacion.descarga.model.Parametros;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;

public class ParametrosJsonSerializer implements JsonSerializer<Parametros> {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public JsonElement serialize(Parametros o, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject root = new JsonObject();
        JsonObject descarga = new JsonObject();

        root.addProperty("servicio", o.getServicio().clave());

        descarga.addProperty("rfcContribuyente", o.getCredenciales().user());
        descarga.addProperty("password", o.getCredenciales().password());
        descarga.addProperty("fechaInicio", o.getFechaInicio().format(dateTimeFormatter));
        descarga.addProperty("fechaFin", o.getFechaFin().format(dateTimeFormatter));

        if (o.getTipo() != null) {
            descarga.addProperty("tipo", o.getTipo().name().toLowerCase());
        }

        if (o.getTipoDoc() != null) {
            descarga.addProperty("tipoDoc", o.getTipoDoc().name().toLowerCase());
        }

        if (o.getTipoDoc() != null) {
            descarga.addProperty("status", o.getStatus().name().toLowerCase() + "s");
        }

        if (o.getRfcBusqueda() != null) {
            descarga.addProperty("rfcBusqueda", o.getRfcBusqueda());
        }

        root.add("descarga", descarga);

        return root;
    }
}
