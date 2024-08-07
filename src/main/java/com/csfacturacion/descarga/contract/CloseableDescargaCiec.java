package com.csfacturacion.descarga.contract;

import java.io.IOException;

public interface CloseableDescargaCiec extends DescargaCiec {

    /**
     * Realiza el cierre de los recursos utilizados por la implementación
     * de la descarga CIEC. Cada implementación es responsable de informar si
     * el CSReporter puede ser usado después de cerrarse o no.
     * @throws IOException si el canal no se puede cerrar
     */
    void close() throws IOException;
}
