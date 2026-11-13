package mx.montacargasobregon.backend.movil.fotos;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

/**
 * Fotos de la app móvil. Cada foto se sube en cuanto se toma; la historia valida al terminar
 * que estén todas con verificarCompletas(...).
 */
@Service
public class FotoService {

    // Tamaño máximo de una foto. La app las reduce antes de subirlas (≈300 KB)
    static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int LARGO_ETIQUETA = 50;

    private final FotoRepository fotoRepositorio;
    private final PersonalRepository personalRepositorio;

    public FotoService(FotoRepository fotoRepositorio, PersonalRepository personalRepositorio) {
        this.fotoRepositorio = fotoRepositorio;
        this.personalRepositorio = personalRepositorio;
    }

    /** Guarda la foto. Si ya había una con la misma etiqueta, la reemplaza. */
    @Transactional
    public FotoResponse subir(OrigenFoto origen, Long referenciaId, String etiqueta, MultipartFile archivo,
                              PersonalActual personal) {
        String nombre = validarEtiqueta(etiqueta);
        validarArchivo(archivo);

        fotoRepositorio.findByOrigenAndReferenciaIdAndEtiqueta(origen, referenciaId, nombre)
                .ifPresent(anterior -> {
                    fotoRepositorio.delete(anterior);
                    fotoRepositorio.flush(); // Primero borra; así no quedan dos con la misma etiqueta
                });

        Foto foto = new Foto(origen, referenciaId, nombre, archivo.getContentType(), leer(archivo),
                personalRepositorio.getReferenceById(personal.id()), LocalDateTime.now());
        return FotoResponse.de(fotoRepositorio.save(foto));
    }

    @Transactional(readOnly = true)
    public List<FotoResponse> listar(OrigenFoto origen, Long referenciaId) {
        return fotoRepositorio.listar(origen, referenciaId);
    }

    @Transactional(readOnly = true)
    public Foto obtener(Long id) {
        return fotoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La foto no existe."));
    }

    /** Solo quien la subió puede borrarla. */
    @Transactional
    public void borrar(Long id, PersonalActual personal) {
        Foto foto = obtener(id);
        if (!foto.getSubidaPor().getId().equals(personal.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo quien tomó la foto puede borrarla.");
        }
        fotoRepositorio.delete(foto);
    }

    /**
     * Para las historias: lanza 409 si falta alguna de las fotos requeridas.
     * Ejemplo: fotoService.verificarCompletas(OrigenFoto.REVISION, id, List.of("Frente", "Lateral"));
     */
    @Transactional(readOnly = true)
    public void verificarCompletas(OrigenFoto origen, Long referenciaId, List<String> requeridas) {
        List<String> tomadas = fotoRepositorio.etiquetas(origen, referenciaId);
        List<String> faltan = requeridas.stream().filter(r -> !tomadas.contains(r)).toList();
        if (!faltan.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Faltan fotos: " + String.join(", ", faltan) + ".");
        }
    }

    private static String validarEtiqueta(String etiqueta) {
        if (etiqueta == null || etiqueta.isBlank()) {
            throw invalido("Indica qué muestra la foto.");
        }
        String limpia = etiqueta.trim();
        if (limpia.length() > LARGO_ETIQUETA) {
            throw invalido("El nombre de la foto admite máximo " + LARGO_ETIQUETA + " caracteres.");
        }
        return limpia;
    }

    private static void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw invalido("No se recibió la foto.");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw invalido("La foto debe ser JPG, PNG o WEBP.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw invalido("La foto pesa más de 5 MB.");
        }
    }

    private static byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw invalido("No se pudo leer la foto. Intenta de nuevo.");
        }
    }

    private static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}