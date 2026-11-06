package mx.montacargasobregon.backend.cartas.servicio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** No se generó la carta porque faltan datos obligatorios: dato → a quién le toca (Cliente, Renta o Equipo). */
public class DatosFaltantesException extends RuntimeException {

    // En el mismo orden que en la carta
    private final transient Map<String, String> faltantes;

    public DatosFaltantesException(Map<String, String> faltantes) {
        super("Faltan datos obligatorios: " + String.join(", ", faltantes.keySet()));
        this.faltantes = Collections.unmodifiableMap(new LinkedHashMap<>(faltantes));
    }

    public Map<String, String> getFaltantes() {
        return faltantes;
    }
}
