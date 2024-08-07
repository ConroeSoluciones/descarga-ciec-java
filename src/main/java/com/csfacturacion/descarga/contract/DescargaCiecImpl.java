package com.csfacturacion.descarga.contract;

import com.csfacturacion.descarga.error.InvalidQueryException;
import com.csfacturacion.descarga.error.QueryNotFoundException;
import com.csfacturacion.descarga.model.Credenciales;
import com.csfacturacion.descarga.model.Parametros;
import com.csfacturacion.descarga.util.RequestFactory;
import com.csfacturacion.descarga.util.http.ApiClient;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DescargaCiecImpl implements CloseableDescargaCiec {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescargaCiecImpl.class);

    private static final int DEFAULT_TIMEOUT = 15; // seconds

    private Credenciales csCredenciales;

    private final int statusCheckTimeout = 15000; // ms

    private final StatusChecker statusChecker;

    private final ApiClient apiClient;

    private final ScheduledFuture<?> statusCheckerHandle;

    private final RequestFactory requestFactory;

    private DescargaCiecImpl() {
        this(new RequestFactory(), DEFAULT_TIMEOUT * 1000);
    }

    public DescargaCiecImpl(Credenciales csCredenciales, ApiClient apiClient) {
        this(null, apiClient, DEFAULT_TIMEOUT * 1000);
        this.csCredenciales = csCredenciales;
    }

    private DescargaCiecImpl(RequestFactory requestFactory) {
        this(requestFactory, DEFAULT_TIMEOUT * 1000);
    }

    private DescargaCiecImpl(RequestFactory requestFactory, int timeout) {
        this(
                requestFactory,
                new ApiClient(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                        .build()),
                timeout);
    }

    private DescargaCiecImpl(RequestFactory requestFactory, ApiClient apiClient) {
        this(requestFactory, apiClient, DEFAULT_TIMEOUT * 1000);
    }

    private DescargaCiecImpl(RequestFactory requestFactory, ApiClient apiClient, int timeout) {

        this.requestFactory = requestFactory;
        this.statusChecker = new StatusChecker();
        this.apiClient = apiClient;

        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            statusCheckerHandle = scheduler.scheduleAtFixedRate(statusChecker, timeout, timeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Crea una nueva instancia de un DescargaSATHttpClient, usando el usuario y
     * pass del contrato con CS Facturación.
     *
     * @param csCredenciales del contrato con CSFacturación
     */
    public DescargaCiecImpl(Credenciales csCredenciales) {
        this();
        this.csCredenciales = csCredenciales;
    }

    @Override
    public QueryRetriever query(Parametros params) throws InvalidQueryException {
        return query(params, null);
    }

    @Override
    public QueryRetriever query(Parametros params, QueryProgressListener listener) throws InvalidQueryException {

        validateCredentials();

        HttpRequest request = requestFactory.newConsultaRequest(csCredenciales, params);

        try {
            HttpResponse<String> response = apiClient.send(request);

            JsonObject payload = JsonParser.parseString(response.body()).getAsJsonObject();

            if (payload.has("error")) {
                LOGGER.error("Error al crear consulta " + payload.get("error").getAsString());
                throw new InvalidQueryException("Ocurrió un error al "
                        + "comunicarse con el servidor de descarga masiva."
                        + "mensaje de la solicitud: "
                        + payload.get("error").getAsString());
            }

            String folio = payload.get("data").getAsJsonObject().get("uuid").getAsString();

            return newQueryRetriever(params, UUID.fromString(folio), listener);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public QueryRetriever search(java.util.UUID folio) throws InvalidQueryException {

        try {
            alreadyExistsValidate(folio);
        } catch (QueryNotFoundException e) {
            throw new InvalidQueryException(e);
        }

        // TODO: Actualmente, el WS no devuelve los parámetros de búsqueda
        // utilizados para generar la consulta que se busca, por lo que los
        // parámetros serán nulos
        return newQueryRetriever(null, folio, null);
    }

    @Override
    public QueryRetriever search(java.util.UUID folio, QueryProgressListener listener) throws InvalidQueryException {

        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }

        QueryRetriever consulta = search(folio);
        QueryRetriever.Status status = consulta.getProgress().status();
        if (status == QueryRetriever.Status.REPETIR) {
            // repetir de ser necesario
            consulta = repeat(folio, listener);
        } else if (!status.isFinished()) {
            // si aún no ha terminado, agrega la consulta para ser verificada
            // con el status checker
            statusChecker.addConsulta(consulta, listener);
        } else {
            // si ya había terminado, simplemente llama al método onStatusChanged
            // directamente
            listener.onStatusChanged(status, consulta);
        }

        return consulta;
    }

    @Override
    public QueryRetriever repeat(java.util.UUID folio) throws InvalidQueryException {
        return repeat(folio, null);
    }

    @Override
    public QueryRetriever repeat(java.util.UUID folio, QueryProgressListener listener) throws InvalidQueryException {

        try {
            alreadyExistsValidate(folio);
            HttpResponse<String> response =
                    apiClient.send(requestFactory.newRepetirConsultaRequest(folio, csCredenciales));

            JsonObject payload = JsonParser.parseString(response.body()).getAsJsonObject();

            if (payload.has("error")) {
                LOGGER.error(
                        "Error al repetir la consulta " + payload.get("error").getAsString());
                throw new InvalidQueryException("Error en la solicitud de repeticion de consulta: "
                        + payload.get("error").getAsString());
            }

            // TODO: la solicitud de repetir query no regresa el mismo payload de una query normal
            return newQueryRetriever(null, folio, listener);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (QueryNotFoundException e) {
            throw new InvalidQueryException(e);
        }
    }

    @Override
    public void close() {
        statusCheckerHandle.cancel(false);
        apiClient.close();
    }

    public int getStatusCheckTimeout() {
        return statusCheckTimeout;
    }

    protected void validateCredentials() {
        if (csCredenciales == null) {
            throw new IllegalStateException("No se han establecido las credenciales del contrato.");
        }
    }

    protected QueryRetriever newQueryRetriever(Parametros parametros, UUID response) throws InvalidQueryException {

        return new QueryRetrieverImpl(parametros, response, requestFactory, apiClient);
    }

    private QueryRetriever newQueryRetriever(Parametros parametros, UUID response, QueryProgressListener listener)
            throws InvalidQueryException {

        QueryRetriever queryRetriever = newQueryRetriever(parametros, response);

        if (listener != null) {
            statusChecker.addConsulta(queryRetriever, listener);
        }

        return queryRetriever;
    }

    /**
     * Las instancias de esta clase se encargan de checar el status actual de
     * una o varias consultas, cada N segundos.
     */
    protected static class StatusChecker implements Runnable {

        private final List<QueryHolder> queriesList = Lists.newArrayList();

        private final ReentrantLock consultasLock;

        public StatusChecker() {
            this.consultasLock = new ReentrantLock();
        }

        public void addConsulta(QueryRetriever consulta, QueryProgressListener listener) {

            consultasLock.lock();

            try {
                queriesList.add(new QueryHolder(consulta, listener));
            } finally {
                consultasLock.unlock();
            }
        }

        @Override
        public void run() {
            List<QueryHolder> finished = Lists.newArrayList();
            consultasLock.lock();

            try {
                for (QueryHolder holder : queriesList) {
                    QueryRetriever.Status status;

                    try {
                        status = holder.queryRetriever.getProgress().status();

                        // notifica de ser necesario
                        if (holder.listener != null && status != holder.previousStatus) {
                            holder.previousStatus = status;

                            if (status.isFinished()) {
                                finished.add(holder);
                            }

                            holder.listener.onStatusChanged(status, holder.queryRetriever);
                        }

                    } catch (Exception e) {
                        // actualmente si no se puede comunicar con el servidor,
                        // la instancia de UserAgent utilizada falla, lo que
                        // causa que el Thread actual termine
                        // TODO: Se necesita una excepción más específica,
                        // y probablemente verificada
                        LOGGER.error("Hubo un problema al intentar conectarse " + "al servidor de cfdis descarga", e);

                        // también da por terminada la consulta
                        // TODO: Notificar
                        finished.add(holder);
                    }
                }

                for (QueryHolder finishedOne : finished) {
                    queriesList.remove(finishedOne);
                }
            } finally {
                consultasLock.unlock();
            }
        }
    }

    private static class QueryHolder {

        private final QueryRetriever queryRetriever;
        private final QueryProgressListener listener;
        private QueryRetriever.Status previousStatus;

        public QueryHolder(QueryRetriever queryRetriever, QueryProgressListener listener) {

            this.queryRetriever = queryRetriever;
            this.listener = listener;
            this.previousStatus = queryRetriever.getProgress().status();
        }
    }

    private void alreadyExistsValidate(java.util.UUID folio) throws QueryNotFoundException {
        validateCredentials();

        try {
            HttpResponse<String> response = apiClient.send(requestFactory.newStatusRequest(folio));

            // verifica que la respuesta sea la esperada, de lo contrario
            // no existe una consulta asociada con el folio dado
            if (response.statusCode() != 200) {
                throw new QueryNotFoundException("No existe ninguna consulta " + "con el UUID dado.");
            }
        } catch (URISyntaxException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
