package mx.montacargasobregon.backend.movil.fotos;

import java.time.LocalDateTime;

/** Foto sin su contenido; la imagen se pide aparte con url (se usa directo en <img src>). */
public record FotoResponse(Long id, String etiqueta, String url, LocalDateTime subidaEn) {

    // Lo usa la consulta de FotoRepository, que no carga el contenido
    public FotoResponse(Long id, String etiqueta, LocalDateTime subidaEn) {
        this(id, etiqueta, "/api/movil/fotos/" + id, subidaEn);
    }

    public static FotoResponse de(Foto foto) {
        return new FotoResponse(foto.getId(), foto.getEtiqueta(), foto.getSubidaEn());
    }
}