package mx.montacargasobregon.backend.cartas.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mx.montacargasobregon.backend.cartas.dto.CambioCarta;

/**
 * Guarda los datos con que se generó la carta como texto ("campo<TAB>valor" por renglón)
 * y los compara con los actuales para saber qué cambió en la renta.
 */
final class ResumenCarta {

    private ResumenCarta() {
    }

    static String texto(Map<String, String> datos) {
        StringBuilder texto = new StringBuilder();
        datos.forEach((campo, valor) -> texto.append(limpiar(campo)).append('\t').append(limpiar(valor)).append('\n'));
        return texto.toString();
    }

    static Map<String, String> leer(String texto) {
        Map<String, String> datos = new LinkedHashMap<>();
        if (texto == null) return datos;
        for (String renglon : texto.split("\n")) {
            int tab = renglon.indexOf('\t');
            if (tab > 0) datos.put(renglon.substring(0, tab), renglon.substring(tab + 1));
        }
        return datos;
    }

    /** Los datos que cambiaron, en el orden de la carta. */
    static List<CambioCarta> cambios(Map<String, String> antes, Map<String, String> ahora) {
        Set<String> campos = new LinkedHashSet<>(ahora.keySet());
        campos.addAll(antes.keySet());
        List<CambioCarta> cambios = new ArrayList<>();
        for (String campo : campos) {
            String anterior = antes.getOrDefault(campo, "");
            String actual = limpiar(ahora.getOrDefault(campo, ""));
            if (!Objects.equals(anterior, actual)) cambios.add(new CambioCarta(campo, anterior, actual));
        }
        return cambios;
    }

    // Un tabulador o un salto de línea dentro de un dato rompería el formato
    private static String limpiar(String valor) {
        return valor == null ? "" : valor.replaceAll("[\t\r\n]+", " ").strip();
    }
}
