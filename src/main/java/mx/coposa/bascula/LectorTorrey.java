package mx.coposa.bascula;

import com.fazecast.jSerialComm.SerialPort;
import java.time.Instant;
import java.util.Locale;

/** Diagnostico de recepcion serial. No interpreta ni solicita pesadas. */
public final class LectorTorrey {
    public static void main(String[] args) {
        int result;
        try {
            result = ejecutar(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            result = 1;
        }
        if (result != 0) System.exit(result);
    }

    private static int ejecutar(String[] args) {
        if (args.length == 0 || "--help".equals(args[0])) {
            System.out.println("Uso: LectorTorrey listar");
            System.out.println("     LectorTorrey leer PUERTO [BAUDIOS BITS PARIDAD STOP SEGUNDOS]");
            System.out.println("Ejemplo inicial, por validar en el equipo: leer COM3 9600 8 N 1 60");
            return 0;
        }
        if ("listar".equals(args[0]) && args.length == 1) {
            SerialPort[] ports = SerialPort.getCommPorts();
            if (ports.length == 0) System.out.println("No se detectaron puertos seriales.");
            for (SerialPort port : ports) {
                System.out.println(port.getSystemPortName() + " | " + port.getDescriptivePortName());
            }
            return 0;
        }
        if (!"leer".equals(args[0]) || args.length < 2 || args.length > 7) {
            throw new IllegalArgumentException("Usa --help para consultar los argumentos.");
        }
        int baud = numero(args, 2, 9600, 1, 4000000);
        int bits = numero(args, 3, 8, 5, 8);
        String parityName = args.length > 4 ? args[4].toUpperCase(Locale.ROOT) : "N";
        int parity = switch (parityName) {
            case "N" -> SerialPort.NO_PARITY;
            case "E" -> SerialPort.EVEN_PARITY;
            case "O" -> SerialPort.ODD_PARITY;
            default -> throw new IllegalArgumentException("Paridad permitida: N, E, O.");
        };
        int stop = numero(args, 5, 1, 1, 2);
        int seconds = numero(args, 6, 60, 1, 3600);
        // Exige un puerto enumerado: no abre rutas arbitrarias ni elige uno automaticamente.
        SerialPort selected = null;
        for (SerialPort port : SerialPort.getCommPorts()) {
            if (args[1].equalsIgnoreCase(port.getSystemPortName())
                    || args[1].equals(port.getSystemPortPath())) {
                selected = port;
                break;
            }
        }
        if (selected == null) throw new IllegalArgumentException("Puerto no detectado. Ejecuta listar.");
        final SerialPort port = selected;
        if (!port.setComPortParameters(baud, bits,
                stop == 1 ? SerialPort.ONE_STOP_BIT : SerialPort.TWO_STOP_BITS, parity)
                || !port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED)
                || !port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0)) {
            throw new IllegalStateException("No se pudo configurar el puerto.");
        }
        if (!port.openPort()) throw new IllegalStateException("No se pudo abrir el puerto; revisa controlador y uso por otra aplicacion.");
        Thread shutdown = new Thread(() -> port.closePort(), "cerrar-puerto-torrey");
        Runtime.getRuntime().addShutdownHook(shutdown);
        long total = 0;
        try {
            System.err.printf("Escuchando %s: %d %d%s%d, sin control de flujo, %d s.%n",
                    port.getSystemPortName(), baud, bits, parityName, stop, seconds);
            System.err.println("Configuracion inicial pendiente de validar. Pulsa PRINT en la bascula si corresponde.");
            System.err.println("Cada registro es un bloque de bytes; no necesariamente una pesada completa.");
            byte[] buffer = new byte[256];
            long start = System.nanoTime();
            while (System.nanoTime() - start < seconds * 1_000_000_000L) {
                int count = port.readBytes(buffer, buffer.length);
                if (count < 0) throw new IllegalStateException("Fallo de lectura o dispositivo desconectado.");
                if (count == 0) continue;
                total += count;
                StringBuilder hex = new StringBuilder();
                StringBuilder ascii = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    int value = buffer[i] & 0xff;
                    if (i > 0) hex.append(' ');
                    hex.append(String.format(Locale.ROOT, "%02X", value));
                    if (value == '\r') ascii.append("\\r");
                    else if (value == '\n') ascii.append("\\n");
                    else if (value == '\\') ascii.append("\\\\");
                    else if (value >= 32 && value <= 126) ascii.append((char) value);
                    else ascii.append(String.format(Locale.ROOT, "\\x%02X", value));
                }
                System.out.println(Instant.now() + " | HEX=" + hex + " | ASCII=" + ascii);
            }
        } finally {
            port.closePort();
            try {
                Runtime.getRuntime().removeShutdownHook(shutdown);
            } catch (IllegalStateException ignored) {
                // La JVM ya esta apagandose; el hook tambien cierra el puerto.
            }
        }
        System.err.println("Bytes recibidos: " + total);
        if (total == 0) {
            System.err.println("Sin datos: comprobar cableado, parametros y modo de envio. No demuestra peso cero.");
            return 3;
        }
        return 0;
    }

    private static int numero(String[] args, int index, int defaultValue, int min, int max) {
        if (args.length <= index) return defaultValue;
        int value = Integer.parseInt(args[index]);
        if (value < min || value > max) throw new IllegalArgumentException("Argumento " + index + " fuera de rango.");
        return value;
    }
}
