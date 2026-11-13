package mx.montacargasobregon.backend.movil.fotos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

@ExtendWith(MockitoExtension.class)
class FotoServiceTest {

    @Mock
    private FotoRepository fotoRepositorio;

    @Mock
    private PersonalRepository personalRepositorio;

    @InjectMocks
    private FotoService servicio;

    private final Personal mecanico = personal(3L);
    private final PersonalActual actual = PersonalActual.de(mecanico);

    @Test
    void subirGuardaLaFotoConSuEtiqueta() {
        when(fotoRepositorio.findByOrigenAndReferenciaIdAndEtiqueta(OrigenFoto.REVISION, 15L, "Frente"))
                .thenReturn(Optional.empty());
        when(personalRepositorio.getReferenceById(3L)).thenReturn(mecanico);
        when(fotoRepositorio.save(any(Foto.class))).thenAnswer(i -> conId(i.getArgument(0), 40L));

        FotoResponse foto = servicio.subir(OrigenFoto.REVISION, 15L, " Frente ", jpg(1000), actual);

        assertEquals("Frente", foto.etiqueta());
        assertEquals("/api/movil/fotos/40", foto.url());
    }

    @Test
    void subirReemplazaLaFotoConLaMismaEtiqueta() {
        Foto anterior = foto(39L, mecanico);
        when(fotoRepositorio.findByOrigenAndReferenciaIdAndEtiqueta(OrigenFoto.REVISION, 15L, "Frente"))
                .thenReturn(Optional.of(anterior));
        when(personalRepositorio.getReferenceById(3L)).thenReturn(mecanico);
        when(fotoRepositorio.save(any(Foto.class))).thenAnswer(i -> conId(i.getArgument(0), 40L));

        servicio.subir(OrigenFoto.REVISION, 15L, "Frente", jpg(1000), actual);

        InOrder orden = inOrder(fotoRepositorio);
        orden.verify(fotoRepositorio).delete(anterior);
        orden.verify(fotoRepositorio).flush();
        orden.verify(fotoRepositorio).save(any(Foto.class));
    }

    @Test
    void subirRechazaArchivosQueNoSonImagen() {
        MockMultipartFile pdf = new MockMultipartFile("archivo", "carta.pdf", "application/pdf", new byte[10]);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.subir(OrigenFoto.REVISION, 15L, "Frente", pdf, actual));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(fotoRepositorio, never()).save(any());
    }

    @Test
    void subirRechazaFotosDeMasDeCincoMegas() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.subir(OrigenFoto.REVISION, 15L, "Frente",
                        jpg((int) FotoService.TAMANO_MAXIMO + 1), actual));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void subirPideEtiqueta() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.subir(OrigenFoto.REVISION, 15L, "  ", jpg(1000), actual));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void soloQuienSubioLaFotoPuedeBorrarla() {
        when(fotoRepositorio.findById(39L)).thenReturn(Optional.of(foto(39L, mecanico)));
        PersonalActual otro = PersonalActual.de(personal(4L));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.borrar(39L, otro));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(fotoRepositorio, never()).delete(any());
    }

    @Test
    void verificarCompletasIndicaLasFotosQueFaltan() {
        when(fotoRepositorio.etiquetas(OrigenFoto.REVISION, 15L)).thenReturn(List.of("Frente"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.verificarCompletas(OrigenFoto.REVISION, 15L, List.of("Frente", "Lateral", "Llantas")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Faltan fotos: Lateral, Llantas.", error.getReason());
    }

    private static Personal personal(Long id) {
        Personal p = new Personal("MEC-" + id, "Jorge", "Valenzuela", null, TipoPersonal.MECANICO);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private static Foto foto(Long id, Personal subidaPor) {
        return conId(new Foto(OrigenFoto.REVISION, 15L, "Frente", "image/jpeg", new byte[10],
                subidaPor, LocalDateTime.of(2026, 10, 8, 9, 0)), id);
    }

    private static Foto conId(Foto foto, Long id) {
        ReflectionTestUtils.setField(foto, "id", id);
        return foto;
    }

    private static MockMultipartFile jpg(int bytes) {
        return new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[bytes]);
    }
}