package com.csfacturacion.descarga.contract;

import com.csfacturacion.descarga.error.InvalidQueryException;
import com.csfacturacion.descarga.model.Parametros;
import java.util.UUID;

public interface DescargaCiec {

    /**
     * Realiza una consulta para obtener los CFDIs que correspondan de acuerdo
     * a los parámetros especificados.
     *
     * @param params       los parámetros de búsqueda.
     * @return la consulta que contiene la funcionalidad para obtener el status
     * y resultados de la misma.
     * @throws InvalidQueryException si ocurre un problema con los
     *                               parámetros de la consulta.
     */
    QueryRetriever query(Parametros params) throws InvalidQueryException;

    /**
     * Realiza una consulta para obtener los CFDIs que correspondan de acuerdo
     * a los parámetros especificados. Si se especifica el callback, éste será
     * llamado una vez la consulta se encuentre terminada.
     *
     * @param params       los parámetros de búsqueda.
     * @param listener     implementación de un {@link QueryProgressListener} que
     *                     será utilizado para manejar los cambios de
     *                     {@link QueryRetriever.Status} de la consulta resultante.
     * @return la consulta que contiene la funcionalidad para obtener el status
     * y resultados de la misma.
     * @throws InvalidQueryException si ocurre un problema con los
     *                               parámetros de la consulta.
     */
    QueryRetriever query(Parametros params, QueryProgressListener listener) throws InvalidQueryException;

    /**
     * Es posible buscar consultas por folio específico, en caso que se hayan
     * realizado previamente y se quiera consultar sus resultados.
     *
     * @param folio de la consulta previamente realizada.
     * @return la consulta con el folio especificado.
     * @throws InvalidQueryException si no se encuentra ninguna consulta
     *                               con el folio especificado.
     */
    QueryRetriever search(UUID folio) throws InvalidQueryException;

    /**
     * Es posible buscar consultas por folio específico, en caso que se hayan
     * realizado previamente y se quiera consultar sus resultados. Este método
     * permite utilizar un ProgresoConsultaListener, el cual es útil cuando
     * la consulta que se busca tiene status REPETIR, al ser utilizado este
     * método se comprobará automáticamente si tiene ese status, de ser así
     * se repite la consulta y se notifican los cambios de status al listener.
     *
     * @param folio    de la consulta previamente realizada.
     * @param listener que será utilizado para manejar los cambios de status.
     * @return la consulta con el folio especificado.
     * @throws InvalidQueryException si no se encuentra ninguna consulta
     *                               con el folio especificado.
     */
    QueryRetriever search(UUID folio, QueryProgressListener listener) throws InvalidQueryException;

    /**
     * Si la consulta con el folio dado está en status REPETIR, este método
     * repetirá la consulta para obtener los resultados necesarios.
     *
     * @param folio de la consulta previamente realizada.
     * @return la consulta que se repetirá.
     * @throws InvalidQueryException si no es posible repetir la consulta
     *                               (e.g. status != "REPETIR" o no existe el folio).
     */
    QueryRetriever repeat(UUID folio) throws InvalidQueryException;

    /**
     * Si la consulta con el folio dado está en status REPETIR, este método
     * repetirá la consulta para obtener los resultados necesarios.
     *
     * @param folio    de la consulta previamente realizada.
     * @param listener implementación de un {@link QueryProgressListener} que
     *                 será utilizado para manejar los cambios de {@link QueryRetriever.Status} de
     *                 la consulta a repetir.
     * @return la consulta que se repetirá.
     * @throws InvalidQueryException si no es posible repetir la consulta
     *                               (e.g. status != "REPETIR" o no existe el folio).
     */
    QueryRetriever repeat(UUID folio, QueryProgressListener listener) throws InvalidQueryException;
}
