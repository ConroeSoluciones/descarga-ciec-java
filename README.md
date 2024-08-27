# Descarga CIEC SDK (Antes CSReporter API)

Provee una API sencilla para realizar consultas al portal del SAT a través
de nuestro web service de descarga masiva mediante CIEC

Consta de 2 interfaces principales:

- `com.csfacturacion.descarga.DescargaCiec`: Interface para crear consultas (`Query`) o repetirlas.
- `com.csfacturacion.descarga.QueryRetriever`: Interface para monitorear el estatus y obtener resultados
  de las consultas

## Dependencies

* Java 21+
* [Apache Maven](http://maven.apache.org/)

## Installation

La API se encuentra en el repositorio central de Maven, por lo que sólo se
debe incluir la dependencia en el POM del proyecto donde se quiera utilizar.

    <dependency>
        <groupId>com.csfacturacion.descarga</groupId>
        <artifactId>descarga-ciec-java</artifactId>
        <version>v1.0.0</version>
    </dependency>

## Build

Ejecutar el siguiente comando para construir e instalar la API en el repositorio
local de maven:

    mvn install


## Docs

Para generar la documentación del proyecto, ejecutar el siguiente comando:

    mvn site

Esto generará la documentación en la carpeta "target/site", comenzar a navegar
por el archivo index.html.

## Usage

Inicio rápido:

```java
import com.csfacturacion.descarga.contract.DescargaCiec;
import com.csfacturacion.descarga.contract.DescargaCiecImpl;
import com.csfacturacion.descarga.contract.QueryProgressListener;
import com.csfacturacion.descarga.contract.QueryRetriever;
import com.csfacturacion.descarga.model.Credenciales;
import com.csfacturacion.descarga.model.Parametros;
import java.time.LocalDateTime;

public class DemoUsage {
    
    public static void main(String[] args) {
        
    // credenciales para CSFacturación
    Credenciales csCredenciales = new Credenciales("XXXXXXXXXXXXX", "pass");

    // inicializar DescargaCiec (todas las configuraciones por defecto)
    DescargaCiec descargaCiec = new DescargaCiecImpl(csCredenciales);

    // credenciales para el portal del SAT
    Credenciales satCredenciales = new Credenciales("XXXXXXXXXXXXX", "pass");

    // obtener todos los CFDIs emitidos en el periodo de 2015-01-01 a las 
    // 00:00:00 horas hasta 2015-01-01 a las 23:59:59
    QueryRetriever queryRetriever = descargaCiec.query(
        new Parametros.Builder()
            .tipo(Parametros.Tipo.EMITIDAS)
            .status(Parametros.Status.TODOS)
            .tipoDoc(Parametros.TipoDoc.CFDI) // default
            .servicio(Parametros.Servicio.API_CIEC)
            .fechaInicio(LocalDateTime.of(2015, 1, 1, 0, 0, 0))
            .fechaFin(LocalDateTime.of(2015, 1, 1, 23, 59, 59))
            .credenciales(satCredenciales)
            .build(),
            (status,retriever) -> {
                // verificar status y hacer algo con los resultados
                if (status.isFinished()) {
                    for (int i = 1; i <= retriever.get().pages(); i++) {
                        // obtener todos los CFDIs de la página i
                        List<CFDI> cfdis = retriever.getResults(i);
                    }
                }
            }
        );
    }
}

```

### Progreso

El progreso de una consulta (`Query`) se obtiene mediante el método `getProgress()` de `QueryRetriever`

> **Advertencia** 
> Llamar al método `getProgress()` hace una llamada HTTP al webservice.

Ejemplo:

```java

import com.csfacturacion.descarga.contract.QueryRetriever;
import com.csfacturacion.descarga.model.Progress;

public class Demo {

    public static void main(String[] args) {
        QueryRetriever retriever = descargaCiec.query(params);
        while (!retriever.isFinished()) {
            Progress p = retriever.getProgress();
            System.out.println(p.found()); // CFDI encontrados hasta el momento; 
            System.out.println(p.status()); // Status de la consulta
        }
    }

}

```

### Resumen

Obtener el resumen de una consulta solo es posible si esta ha finalizado (con error o sin error)
Para obtener el resumen de una consulta debe acceder al objeto `Summary`:

```java

import com.csfacturacion.descarga.contract.QueryRetriever;
import com.csfacturacion.descarga.model.Progress;
import com.csfacturacion.descarga.model.Summary;

public class Demo {

    public static void main(String[] args) {
        
        QueryRetriever retriever = descargaCiec.query(params);
        while (!retriever.isFinished()) {
            Progress p = retriever.getProgress();
            System.out.println(p.found()); // CFDI encontrados hasta el momento; 
            System.out.println(p.status()); // Status de la consulta
        }

        Summary s = retriever.getSummary();
        long p = s.pages(); // total de paginas para consulta por paginación
        long t = s.total(); // total de CFDI encontrados
        long c = s.cancelados(); // total de CFDI no vigentes
    }

}

```

> **Advertencia** 
> Si la consulta no ha terminado, el error `QueryNotReadyYetException` es arrojado.


### Resultados

La obtención de resultados se logra a traves de un `QueryRetriever`:

```java

import com.csfacturacion.descarga.contract.QueryRetriever;
import com.csfacturacion.descarga.model.CfdiMeta;
import com.csfacturacion.descarga.model.Progress;
import com.csfacturacion.descarga.model.Summary;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class Demo {

    public static void main(String[] args) {

        UUID folioConsulta = UUID.randomUUID();
        UUID folioCfdi = UUID.randomUUID();

        QueryRetriever retriever = descargaCiec.search(folioConsulta);

        String xml = retriever.getXml(folioCfdi); // como XML 
        CfdiMeta cfdi = retriever.getCfdi(folioCfdi); // como metadata

        // todos los resultados
        retriever.asZip(Path.of("my", "dest", "results.zip")); // todos los XML en un ZIP

        // paginado
        for (long i = 0; i < retriever.getSummary().pages(); i++) {
            List<CfdiMeta> metaList = retriever.getResults(i);
            // hacer algo con los CFDI
        }
    }

}

```


Para más ejemplos, ver el archivo:

    src/tests/java/com/csfacturacion/descarga/DescargaCiecIT
