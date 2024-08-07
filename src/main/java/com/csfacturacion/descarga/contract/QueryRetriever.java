package com.csfacturacion.descarga.contract;

import com.csfacturacion.descarga.error.NotEnoughResultsException;
import com.csfacturacion.descarga.error.QueryNotReadyYet;
import com.csfacturacion.descarga.error.XmlNotFoundException;
import com.csfacturacion.descarga.error.ZipException;
import com.csfacturacion.descarga.model.*;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface QueryRetriever {

    /**
     * 'Enum' para listar los distintos STATUS posibles de una
     * consulta.
     */
    enum Status {
        EN_ESPERA,
        EN_PROCESO,
        DESCARGANDO,
        FALLO_AUTENTICACION,
        FALLO_500_MISMO_HORARIO,
        FALLO,
        COMPLETADO,
        COMPLETADO_CON_FALTANTES,
        COMPLETADO_XML_FALTANTES,
        REPETIR;

        public boolean isFinished() {
            return this.name().startsWith("COMPLETADO") || isFailed();
        }

        public boolean isFailed() {
            return this.name().startsWith("FALLO");
        }
    }

    /**
     * Los parámetros utilizados para generar esta Consulta.
     *
     * @return los parámetros utilizados.
     */
    Parametros getParameters();

    /**
     * El status actual de la consulta, reportado por el WS:
     * <ul>
     *  <li><b>EN_ESPERA:</b> No han comenzado a descargarse los CFDIs, se encuentra en cola la petición.</li>
     *  <li><b>EN_PROCESO</b>: La descarga de CFDIs está en curso.</li>
     *  <li><b>DESCARGANDO:</b> Ya se tiene el total de resultados, pero aún se están descargando los XMLs.</li>
     *  <li><b>FALLO_AUTENTICACION:</b> Cuando no se ha podido autenticar con el RFC contraseñas provistos
     *  con el portal del SAT. </li>
     *  <li><b>FALLO_500_MISMO_HORARIO:</b> Ocurre cuando se obtienen más de 500 resultados con la misma fecha y
     *      horario (minuto exacto). </li>
     *  <li><b>FALLO:</b> Distintos errores pueden causar este status. </li>
     *  <li><b>COMPLETADO_CON_FALTANTES:</b> TODO </li>
     *  <li><b>COMPLETADO_XML_FALTANTES:</b> Se descargaron todos los metadatos pero algunos XML no puedieron
     *      ser descargados (bloqueo de RFC, problemas SAT, etc) </li>
     *  <li><b>COMPLETADO:</b> Los CFDIs de la consulta se han descargado. </li>
     *  <li><b>REPETIR: </b>Cuando una consulta necesita repetirse
     *  (generalmente para descargar XMLs faltantes).</li>
     * </ul>
     *
     * @return El status actual de la consulta.
     */
    Progress getProgress();

    /**
     * Cuando una consulta ha terminado, su status puede ser:
     * <ul>
     *     <li>FALLO_AUTENTICACION</li>
     *     <li>FALLO_500_MISMO_HORARIO</li>
     *     <li>COMPLETADO</li>
     *     <li>COMPLETADO_XML_FALTANTES</li>
     *     <li>COMPLETADO_CON_FALTANTES</li>
     *     <li>FALLO</li>
     * </ul>
     * <p>
     * Para verificar que no se haya completado con error, verificar el método
     * {@link #isFailed()} ()} o directamente el status de la consulta.
     *
     * @return true si se devuelve cualquiera de los status anteriores o false
     * de otro modo.
     */
    boolean isFinished();

    /**
     * Cualquiera de los siguientes status deben marcar esta consulta como
     * fallo:
     * <ul>
     *     <li>FALLO_AUTENTICACION</li>
     *     <li>FALLO_500_MISMO_HORARIO</li>
     *     <li>FALLO</li>
     * </ul>
     *
     * @return true si se devuelve cualquiera de los status anteriores o false
     * de otro modo.
     */
    boolean isFailed();

    /**
     * Si la consulta ha sido marcada con status REPETIR, no habrá ningún
     * resultado disponible y será necesario repetir esta consulta.
     *
     * @return true si el status es REPETIR, false de otro modo.
     * @see DescargaCiec#repeat(java.util.UUID)
     */
    boolean isToRepeat();

    /**
     * Cuando se realiza una consulta a través de un IDescargaSAT, se genera un
     * folio único que identifica la consulta.
     *
     * @return el UUID que identifica a la consulta.
     */
    UUID getFolio();

    /**
     * Los resultados se envían paginados, devuelve el total de páginas
     * disponibles para obtener resultados.
     *
     * @return total de páginas disponibles o 0 si no hay resultados.
     */
    Summary getSummary() throws QueryNotReadyYet;

    /**
     * Este método sirve cuando se quiere obtener los resultados en objetos que
     * no sean CFDIMeta sino una extensión de éste. Esto es útil en casos donde
     * se requiere persistir el CFDIMeta, en lugar de tener que copiar
     * manualmente cada CFDIMeta a otro objeto, se obtienes los resultados
     * directamente con la clase esperada.
     *
     * @param page  que se desea obtener.
     * @return El total de registros found en la página dada o un arreglo
     * vacío si no hay suficientes resultados.
     */
    List<CfdiMeta> getResults(int page) throws NotEnoughResultsException, QueryNotReadyYet;

    /**
     * Determina si hay resultados disponibles para esta consulta.
     *
     * @return true si hay resultados disponibles, false de otro modo.
     */
    boolean hasResults() throws QueryNotReadyYet;

    /**
     * Un CFDIMeta se puede buscar directamente por folio si es un resultado de
     * esta consulta.
     *
     * @param folio del CFDIMeta
     * @return el CFDIMeta correspondiente o null si no se encontró en esta
     * consulta.
     */
    CfdiMeta getCfdi(java.util.UUID folio);

    /**
     * Devuelve el XML del CFDIMeta asociado con el folio dado. En ocasiones
     * puede no haber un XML asociado, en estos casos devuelve null.
     *
     * @param folio del CFDIMeta.
     * @return el XML asociado con el CFDIMeta o null si no hay ninguno.
     * @throws XmlNotFoundException si no se encuentra el XML solicitado.
     */
    String getXml(java.util.UUID folio) throws XmlNotFoundException;

    /**
     * Devuelve el XML del CFDIMeta dado. En ocasiones puede no haber un XML
     * asociado, en estos casos devuelve null.
     *
     * @param cfdi del CFDIMeta.
     * @return el XML asociado con el CFDIMeta o null si no hay ninguno.
     * @throws XmlNotFoundException si no se encuentra el XML solicitado.
     */
    String getXml(CfdiMeta cfdi) throws XmlNotFoundException;

    void asZip(Path dest) throws ZipException;
}
