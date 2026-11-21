package mx.montacargasobregon.backend.codigospostales.dto;

import java.util.List;

public record CodigoPostalResponse(String codigoPostal, List<String> colonias, String ciudad, String estado) {
}
