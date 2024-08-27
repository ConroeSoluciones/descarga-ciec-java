package com.csfacturacion.descarga;

import static org.junit.jupiter.api.Assertions.*;

import com.csfacturacion.descarga.contract.DescargaCiecImpl;
import com.csfacturacion.descarga.contract.QueryProgressListener;
import com.csfacturacion.descarga.contract.QueryRetriever;
import com.csfacturacion.descarga.error.InvalidQueryException;
import com.csfacturacion.descarga.error.QueryNotReadyYet;
import com.csfacturacion.descarga.error.XmlNotFoundException;
import com.csfacturacion.descarga.error.ZipException;
import com.csfacturacion.descarga.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DescargaCiecIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescargaCiecIT.class);

    private static DescargaCiecImpl descargaCiec;

    private static UUID consultaFolio;

    private static UUID cfdiFolio;

    private static String cfdiXml;

    private static Parametros parametros;

    private volatile boolean consultaTerminada;

    public DescargaCiecIT() {}

    private static String getResourceAsString(String filename) throws IOException {
        return new String(
                Objects.requireNonNull(DescargaCiecIT.class.getClassLoader().getResourceAsStream(filename))
                        .readAllBytes());
    }

    @BeforeAll
    public static void globalSetup() throws Exception {
        Gson gson = new GsonBuilder().create();

        Credenciales csCredenciales = gson.fromJson(
            getResourceAsString("csCredenciales.json"),
            Credenciales.class);

        Credenciales satCredenciales = gson.fromJson(
            getResourceAsString("satCredenciales.json"),
            Credenciales.class);

        descargaCiec = new DescargaCiecImpl(csCredenciales);

        JsonObject config =
                JsonParser.parseString(getResourceAsString("config.json")).getAsJsonObject();

        consultaFolio = UUID.fromString(config.get("consultaFolio").getAsString());

        cfdiFolio = UUID.fromString(config.get("cfdiFolio").getAsString());
        cfdiXml = getResourceAsString("cfdi.xml").trim();

        parametros = new Parametros.Builder()
                .credenciales(satCredenciales)
                .tipo(Parametros.Tipo.EMITIDAS)
                .status(Parametros.Status.TODOS)
                .servicio(Parametros.Servicio.API_CIEC)
                .fechaInicio(LocalDateTime.of(2022, 1, 1, 0, 0, 0))
                .fechaFin(LocalDateTime.of(2022, 1, 31, 23, 59, 59))
                .build();
    }

    public void setUp() {
        consultaTerminada = false;
    }

    @Test
    public void consultarAsyncTest() throws Exception {
        // este método termina al obtener el UUID de la consulta realizada,
        // recibe el listener (callback) como parámetro, el cuál será ejecutado
        // cada vez que cambie el status de la consulta
        QueryRetriever retriever = descargaCiec.query(parametros, (status, consulta) -> {
            // todou lo que hay en este método se ejecuta en un
            // Thread distinto, cada vez que hay un cambio de status
            // en la consulta.
            DescargaCiecIT.this.onStatusChanged(consulta);
        });

        esperarConsulta(retriever);
    }

    @Test
    @DisabledIfSystemProperty(named = "run-sync", matches = "true")
    public void consultarSync() throws Exception {
        // este método devuelve la consutla de inmediato, como no toma
        // el listener (callback) como parámetro, esta funcionalidad queda
        // a cargo del código cliente
        QueryRetriever retriever = descargaCiec.query(parametros);

        while (!retriever.isFinished()) {
            Progress p = retriever.getProgress();
            System.out.println(p.status());
            System.out.println(p.found());
            System.out.println(retriever.getFolio());
            Thread.sleep(Duration.ofSeconds(2));
        }

        assertTrue(retriever.isFinished());
    }

    @Test
    public void buscarSync() throws Exception {
        QueryRetriever queryRetriever = descargaCiec.search(consultaFolio);

        assertTrue(queryRetriever.isToRepeat() || queryRetriever.isFinished());
    }

    @Test
    public void buscarAsync() throws Exception {
        QueryRetriever queryRetriever = descargaCiec.search(consultaFolio, (status, c) -> {
            System.out.println(status);
            DescargaCiecIT.this.onStatusChanged(c);
        });

        esperarConsulta(queryRetriever);

        assertTrue(queryRetriever.isFinished());
    }

    @Test
    public void buscarConsultaInexistente() {
        // a menos que exista el random UUID, debe lanzar excepción
        assertThrows(InvalidQueryException.class, () -> descargaCiec.search(UUID.randomUUID()));
    }

    @Test
    @DisabledIfSystemProperty(named = "run-sync", matches = "true")
    public void repetirConsultaAnterior() throws Exception {
        QueryRetriever consulta =
                descargaCiec.repeat(consultaFolio, (status, c) -> DescargaCiecIT.this.onStatusChanged(c));

        esperarConsulta(consulta);
    }

    @Test
    public void repetirInexistente() {
        assertThrows(InvalidQueryException.class, () -> descargaCiec.repeat(UUID.randomUUID(), null));
    }

    @Test
    public void iterarResultados() throws Exception {
        descargaCiec.search(consultaFolio, new ConsultaTerminadaListener() {

            @Override
            public void onTerminada(QueryRetriever c) throws QueryNotReadyYet {
                assertFalse(c.isFailed());
                assertTrue(c.getSummary().total() > 0);
                assertTrue(c.getSummary().pages() > 0);

                for (int i = 1; i <= c.getSummary().pages(); i++) {
                    // cada lista contiene hasta 20 cfdis, estos NO
                    // se almacenan en memoria, son descartados tan
                    // pronto como deje de usarse la lista devuelta
                    List<? extends CfdiMeta> resultados = c.getResults(i);

                    for (CfdiMeta cfdi : resultados) {
                        // trabajar con el CFDIMeta
                        assertNotNull(cfdi.getFolio());
                    }
                }
            }
        });
    }

    @Test
    public void getCFDIDirecto() throws InvalidQueryException {
        descargaCiec.search(consultaFolio, new ConsultaTerminadaListener() {

            @Override
            public void onTerminada(QueryRetriever consulta) {
                CfdiMeta meta = consulta.getCfdi(cfdiFolio);
                assertNotNull(meta);
            }
        });
    }

    @Test
    public void getCFDIXML() throws Exception {
        descargaCiec.search(consultaFolio, new ConsultaTerminadaListener() {

            @Override
            public void onTerminada(QueryRetriever consulta) {
                try {
                    String xml = consulta.getXml(cfdiFolio);
                    assertEquals(cfdiXml, xml.trim());
                    assertNotNull(xml);
                } catch (XmlNotFoundException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });
    }

    @Test
    public void zipTest() throws InvalidQueryException, ZipException {
        QueryRetriever retriever = descargaCiec.search(consultaFolio);
        Path p = Path.of("src", "test", "resources", "results.zip");
        System.out.println(p.getFileName());
        retriever.asZip(p);

        assertTrue(p.toFile().exists());
    }

    private void onStatusChanged(QueryRetriever consulta) {
        if (consulta.isFinished()) {
            consultaTerminada = true;
        }
    }

    private void esperarConsulta(QueryRetriever consulta) throws InterruptedException {
        while (!consultaTerminada) {
            System.out.println(consulta.getProgress().status());
            Thread.sleep(1000);
        }

        assertTrue(consulta.isFinished());
    }

    @AfterAll
    public static void globalClose() {
        descargaCiec.close();
    }

    /**
     * Contiene un método que se ejecuta cuando la consulta se encuentra
     * terminada.
     */
    private abstract static class ConsultaTerminadaListener implements QueryProgressListener {

        @Override
        public final void onStatusChanged(QueryRetriever.Status status, QueryRetriever consulta) {
            if (status.isFinished()) {
                try {
                    onTerminada(consulta);
                } catch (QueryNotReadyYet e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public abstract void onTerminada(QueryRetriever consulta) throws QueryNotReadyYet;
    }
}
