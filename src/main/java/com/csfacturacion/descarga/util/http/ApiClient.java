package com.csfacturacion.descarga.util.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {

    private final HttpClient httpClient;

    public ApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<InputStream> download(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    public void close() {
        httpClient.close();
    }
}
