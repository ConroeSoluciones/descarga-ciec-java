package com.csfacturacion.descarga.contract;

import com.csfacturacion.descarga.error.NotEnoughResultsException;
import com.csfacturacion.descarga.error.QueryNotReadyYet;
import com.csfacturacion.descarga.error.XmlNotFoundException;
import com.csfacturacion.descarga.error.ZipException;
import com.csfacturacion.descarga.model.*;
import com.csfacturacion.descarga.util.RequestFactory;
import com.csfacturacion.descarga.util.http.ApiClient;
import com.csfacturacion.descarga.util.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class QueryRetrieverImpl implements QueryRetriever {

    private final ApiClient apiClient;
    private final UUID folio;

    private final RequestFactory requestFactory;

    private Progress progress;

    private final Parametros parameters;

    private Summary summary;

    private final Gson gson = GsonFactory.getGsonInstance();

    protected QueryRetrieverImpl(
            Parametros parameters, UUID folio, RequestFactory requestFactory, ApiClient apiClient) {

        this.folio = folio;
        this.apiClient = apiClient;
        this.requestFactory = requestFactory;
        this.parameters = parameters;
    }

    @Override
    public Parametros getParameters() {
        return parameters;
    }

    /**
     *
     */
    @Override
    public Progress getProgress() {
        Progress progressNuevo = progress; // EN_PROGRESO
        try {
            if (progressNuevo == null) {
                HttpResponse<String> response = apiClient.send(requestFactory.newStatusRequest(folio));

                // no deberia ocurrir
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Consulta con UUID: " + folio + " dio error " + response.body());
                }
                progressNuevo = gson.fromJson(response.body(), Progress.class);
                if (progressNuevo.status().isFinished()) {
                    progress = progressNuevo; // EN_PROGRESO
                }
            }

            return progressNuevo;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isFinished() {
        return getProgress().status().isFinished();
    }

    @Override
    public boolean isFailed() {
        return getProgress().status().isFailed();
    }

    @Override
    public boolean isToRepeat() {
        return Status.REPETIR.equals(getProgress().status());
    }

    @Override
    public UUID getFolio() {
        return folio;
    }

    @Override
    public Summary getSummary() throws QueryNotReadyYet {
        if (summary == null) {
            try {
                HttpResponse<String> response = apiClient.send(requestFactory.newResumenRequest(folio));

                if (response.statusCode() != 200) {
                    throw new QueryNotReadyYet(folio);
                }

                summary = gson.fromJson(response.body(), Summary.class);

            } catch (IOException | URISyntaxException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return summary;
    }

    @Override
    public List<CfdiMeta> getResults(int page) throws QueryNotReadyYet {

        validarTerminada();
        validarResultadosSuficientes(page);

        try {
            return newResultadosList(apiClient.send(requestFactory.newResultadosRequest(folio, page)));
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<CfdiMeta> newResultadosList(HttpResponse<String> response) {
        Type collectionType = new TypeToken<List<CfdiMeta>>() {}.getType();

        return gson.fromJson(response.body(), collectionType);
    }

    @Override
    public boolean hasResults() throws QueryNotReadyYet {
        return getSummary().total() > 0;
    }

    protected void validarResultadosSuficientes(int page) throws QueryNotReadyYet {
        if (!hasResults() || page > getSummary().total()) {
            throw new NotEnoughResultsException("No existen suficientes " + "resultados para mostrar, total páginas: "
                    + getSummary().pages());
        }
    }

    protected ApiClient getApiClient() {
        return apiClient;
    }

    protected RequestFactory getRequestFactory() {
        return requestFactory;
    }

    @Override
    public CfdiMeta getCfdi(java.util.UUID folio) {
        throw new UnsupportedOperationException("Not supported yet.");
        // To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getXml(CfdiMeta cfdi) throws XmlNotFoundException {
        return getXml(cfdi.getFolio());
    }

    @Override
    public String getXml(UUID folioCFDI) throws XmlNotFoundException {
        validarTerminada();

        try {
            HttpResponse<String> response =
                    apiClient.send(requestFactory.newDescargaCfdiRequest(folioCFDI, Request.MediaType.XML));

            if (response.statusCode() != 200) {
                throw new XmlNotFoundException("No se encontró el XML para el " + "folio: " + folioCFDI);
            }

            if (response.body() == null || response.body().isEmpty()) {
                return null;
            }

            // clean white-spaces
            return response.body().replaceAll("\\p{Cf}", "").trim();

        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void asZip(Path dest) throws ZipException {

        try {
            HttpResponse<InputStream> resp = apiClient.download(requestFactory.newDescargaZipRequest(folio));
            if (resp.statusCode() != 200) {
                throw new ZipException("Error al obtener el ZIP. Posiblemente la consulta no ha terminado");
            }

            try (InputStream in = resp.body();
                    FileOutputStream out = new FileOutputStream(dest.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (URISyntaxException | InterruptedException | IOException e) {
            throw new ZipException(e);
        }
    }

    protected void validarTerminada() {
        if (!isFinished()) {
            throw new IllegalStateException("La consulta no ha terminado.");
        }
    }
}
