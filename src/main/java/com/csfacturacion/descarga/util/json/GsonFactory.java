package com.csfacturacion.descarga.util.json;

import com.csfacturacion.descarga.model.Parametros;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;

public final class GsonFactory {

    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(Parametros.class, new ParametrosJsonSerializer())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    public static Gson getGsonInstance() {
        return gson;
    }
}
