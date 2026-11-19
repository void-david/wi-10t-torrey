# Torrey WI-10T: primera comunicación en Java

Lector de diagnóstico por consola para PC Windows, destino confirmado por el usuario el 19/11/2026, Java 17 y jSerialComm 2.11.4. Lista puertos y muestra bytes recibidos en hexadecimal y ASCII con hora UTC. No es todavía una interfaz gráfica.

## ¿Sirve un cable USB a Ethernet?

**En el caso descrito, no: el puerto dice CELL y el cable se utiliza con un Zebra DS2278. CELL corresponde a la celda de carga (sensor de peso), no a Ethernet ni a una salida de datos para PC. No conectar el cable Zebra a CELL.** El usuario confirma además que el indicador **sí tiene un conector RS232 de 9 pines**: ésa es la salida elegida para comunicar con la PC. El manual de la serie WI distingue el conector de celda del DB9 RS-232. Falta cotejar físicamente la revisión y el cableado.

La guía oficial Zebra DS2278 describe un cable de interfaz USB entre su base y la computadora. Ese cable no es por ello un adaptador USB a Ethernet ni un convertidor USB a RS-232 reutilizable con la báscula. No se ha identificado su número de parte ni distribución de pines.

Un adaptador USB a Ethernet común tampoco convierte RS-232. El manual TORREY de la **serie WI**, revisión 1.0, describe salida DB9 RS-232 hacia PC/impresora, 9600 baudios y datos ASCII. No se atribuye Ethernet nativo al equipo.

Conexión propuesta para PC: **báscula, cableado RS-232 compatible, convertidor activo USB a RS-232, USB de la PC**. Deben confirmarse distribución de pines, necesidad de cruce y sexo de los conectores antes de elegir el cable. Un adaptador USB a TTL no sustituye al convertidor RS-232.

También existen cables USB a RJ45 **de consola serial RS-232**: el conector RJ45 por sí solo no los convierte en Ethernet. Uno de estos podría aprovecharse con una adaptación RJ45/DB9 correctamente cableada, tras identificar modelo y señales. No conectar por semejanza del conector.

Para usar red se necesita un **servidor/conversor activo RS-232 a Ethernet**, además del cableado serial y su configuración. El adaptador USB a Ethernet podría dar acceso de red a la PC, pero no reemplaza ese conversor.

## Compilar y ejecutar en Windows

Desde esta carpeta, con JDK 17 en PATH. Ya se descargó localmente la dependencia `lib/jSerialComm-2.11.4.jar` desde [Maven Central](https://repo.maven.apache.org/maven2/com/fazecast/jSerialComm/2.11.4/jSerialComm-2.11.4.jar). Está excluida de Git; si se copia sólo el código, descargarla en esa ruta. SHA256 contrastado con el publicado en el repositorio: `b5f7ec40d622864b92dc679159cdbaa3030d37884f0af2deef45b56c16bda7a6`.

```powershell
New-Item -ItemType Directory -Force lib,target/classes | Out-Null
javac -encoding UTF-8 --release 17 -cp "lib/jSerialComm-2.11.4.jar" -d target/classes src/main/java/mx/coposa/bascula/LectorTorrey.java
java -cp "target/classes;lib/jSerialComm-2.11.4.jar" mx.coposa.bascula.LectorTorrey listar
java -cp "target/classes;lib/jSerialComm-2.11.4.jar" mx.coposa.bascula.LectorTorrey leer COM3 9600 8 N 1 60
```

Sustituir COM3 por el puerto enumerado. También se incluye `pom.xml` para compilar con `mvn compile` si se dispone de Maven. En Java 24 o posterior, la biblioteca indica que puede requerirse `--enable-native-access=ALL-UNNAMED` como opción de `java`.

Parámetros: `leer PUERTO [BAUDIOS BITS PARIDAD STOP SEGUNDOS]`. Predeterminados: `9600 8 N 1 60`, sin control de flujo. **9600 figura en el manual de la serie; los parámetros completos y su correspondencia con el equipo concreto quedan por validar.** Se pueden indicar paridad N/E/O, uno o dos bits de parada y duración de 1 a 3600 segundos. El programa sólo abre el puerto seleccionado expresamente, no envía comandos ni interpreta el peso.

El manual describe una tecla para enviar el peso a la PC/impresora. Con el lector abierto, usar PRINT si el equipo corresponde a esa descripción. No se presupone transmisión continua ni se inventa un comando de solicitud. Para guardar evidencia, agregar `| Tee-Object -FilePath captura.log` al comando de lectura.

Cada renglón representa un bloque recibido por el sistema: una trama puede llegar en varios bloques o varias tramas juntas. Conservar HEX para reconstruir delimitadores y formato. No interpretar ausencia de bytes como peso cero, ni bytes recibidos como pesada estable. Códigos de salida: 0 ejecución normal, 1 error, 3 escucha terminada sin datos.

## Pendientes y alcance

- Salida RS232 de 9 pines confirmada por el usuario. Confirmar sexo de conectores, pinout y cruce para elegir el cableado y convertidor USB a RS-232; no utilizar CELL como sustituto.
- Si hace falta identificar exactamente el cable Zebra, obtener su número de parte; su uso declarado no demuestra pinout.
- Confirmar manual/revisión del equipo y cableado serial.
- Capturar bytes reales con valores conocidos, incluyendo cero, unidad, tara y signo cuando aplique.
- Validar delimitadores, peso, unidad y estado antes de construir la pantalla o guardar pesadas.

**Validación local del 19/11/2026:** compilación correcta con JDK 17; ejecución correcta de ayuda y listado. La PC local enumeró COM1, sin abrirlo ni atribuirlo a la báscula. Se comprobó rechazo de velocidad inválida, paridad inválida y puerto inexistente. No se ha conectado la báscula ni comprobado una pesada real, recepción serial, desconexión, estabilidad o formato de peso. No hay cambios a Microsip.

## Fuentes consultadas el 19/11/2026

- [Manual TORREY serie WI, revisión 1.0, copia alojada en Manualzz](https://manualzz.com/doc/5363996/torrey-wi-serie-electronic-indicator-manual-de-usuario): páginas impresas 4, 5 y 11, conector RS-232, envío por tecla, velocidad y ASCII. Documentación de familia, sin cotejo físico del WI-10T.
- [ATEN UC232B](https://www.aten.com/global/en/products/usb-solutions/converters/uc232b/?p=1): ejemplo de USB a RJ45 de consola RS-232; no identifica el cable del usuario.
- [Zebra DS2278, guía oficial](https://www.zebra.com/content/dam/support-dam/en/documentation/unrestricted/guide/product/ds2278-prg-en.pdf): sección Connecting a USB Interface, página 8-2; cable USB de interfaz entre base y PC. No es prueba de compatibilidad con Torrey.
- [jSerialComm](https://fazecast.github.io/jSerialComm/) y [API de SerialPort](https://fazecast.github.io/jSerialComm/javadoc/com/fazecast/jSerialComm/SerialPort.html): enumeración, parámetros y recepción serial. [Versión fijada 2.11.4](https://central.sonatype.com/artifact/com.fazecast/jSerialComm/2.11.4).
