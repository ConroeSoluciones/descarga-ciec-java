package com.csfacturacion.descarga.util;

import com.csfacturacion.descarga.model.Credenciales;
import com.csfacturacion.descarga.model.Parametros;
import com.csfacturacion.descarga.model.Request;
import com.csfacturacion.descarga.util.json.GsonFactory;
import com.google.gson.Gson;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RequestFactory {

    private static final String BASE_URI = "https://www.csfacturacion.com/webservices/csdescargasat/v3";

    private static DateTimeFormatter dateFormatter;

    private final String baseUri;

    private final Gson gson;

    public RequestFactory() {
        this(BASE_URI);
    }

    public RequestFactory(String baseUri) {
        this.baseUri = baseUri;
        gson = GsonFactory.getGsonInstance();
    }

    protected URI newBaseUri() throws URISyntaxException {
        return newBaseUri("");
    }

    protected URI newBaseUri(String path) throws URISyntaxException {
        return new URI(this.baseUri + path);
    }

    public HttpRequest newConsultaRequest(Credenciales csCredenciales, Parametros params) {

        try {
            URI uri = newBaseUri("/consultar");

            return HttpRequest.newBuilder()
                    .setHeader("rfc", csCredenciales.user())
                    .setHeader("password", csCredenciales.password())
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Accept", "application/json")
                    .uri(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(params)))
                    .build();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpRequest newRepetirConsultaRequest(UUID folio, Credenciales credenciales) {
        try {
            URI uri = newBaseUri("/repetir?uuid=" + folio);

            return HttpRequest.newBuilder()
                    .uri(uri)
                    .setHeader("rfc", credenciales.user())
                    .setHeader("password", credenciales.password())
                    .GET()
                    .build();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpRequest newStatusRequest(UUID folio) throws URISyntaxException {
        URI uri = newBaseUri("/consultas/" + folio.toString() + "/progreso");

        return HttpRequest.newBuilder(uri)
                .GET()
                .header("Accept", Request.MediaType.JSON.value())
                .build();
    }

    public HttpRequest newResumenRequest(UUID folio) throws URISyntaxException {

        URI uri = newBaseUri("/consultas/" + folio.toString() + "/resumen");

        return HttpRequest.newBuilder(uri)
                .GET()
                .header("Accept", Request.MediaType.JSON.value())
                .build();
    }

    public HttpRequest newResultadosRequest(UUID folio, int page) throws URISyntaxException {

        URI uri = newBaseUri("/consultas/" + folio.toString() + "/" + page);

        return HttpRequest.newBuilder(uri)
                .GET()
                .header("Accept", Request.MediaType.JSON.value())
                .build();
    }

    public HttpRequest newDescargaZipRequest(UUID folio) throws URISyntaxException {

        URI uri = newBaseUri("/consultas/" + folio.toString());

        return HttpRequest.newBuilder(uri)
                .GET()
                .header("Accept", Request.MediaType.ZIP.value())
                .build();
    }

    public HttpRequest newDescargaCfdiRequest(UUID folioCFDI, Request.MediaType as) throws URISyntaxException {

        URI uri = newBaseUri("/cfdi/" + folioCFDI);

        return HttpRequest.newBuilder(uri).GET().header("Accept", as.value()).build();
    }
}
