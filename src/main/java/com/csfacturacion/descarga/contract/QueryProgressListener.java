package com.csfacturacion.descarga.contract;

public interface QueryProgressListener {

    /**
     * Este método es llamado cada vez que cambia el {@link QueryRetriever.Status} de
     * una consulta en curso. Es importante leer el status de la consulta desde
     * la variable provista, ya que los métodos que checan el status en el
     * objeto QueryRetriever realizan una llamada al webservice.
     *
     * @param status    actual de la consulta.
     * @param retriever la consulta cuyo status ha cambiado.
     */
    void onStatusChanged(QueryRetriever.Status status, QueryRetriever retriever);
}
