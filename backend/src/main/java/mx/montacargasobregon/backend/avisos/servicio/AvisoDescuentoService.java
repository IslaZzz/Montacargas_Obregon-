package mx.montacargasobregon.backend.avisos.servicio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.dto.AvisoDescuentoResponse;
import mx.montacargasobregon.backend.avisos.modelo.AvisoDescuento;
import mx.montacargasobregon.backend.avisos.repositorio.AvisoDescuentoRepository;
import mx.montacargasobregon.backend.cotizaciones.modelo.AutorizacionDescuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Avisos que gerencia deja al auxiliar cuando responde a una solicitud de descuento. */
@Service
public class AvisoDescuentoService {

    private final AvisoDescuentoRepository avisos;
    private final UsuarioRepository usuarios;

    public AvisoDescuentoService(AvisoDescuentoRepository avisos, UsuarioRepository usuarios) {
        this.avisos = avisos;
        this.usuarios = usuarios;
    }

    /** Avisa a quien pidió el descuento. Se llama justo después de que gerencia responde. */
    @Transactional
    public void crear(Cotizacion cotizacion) {
        AutorizacionDescuento a = cotizacion.getAutorizacion();
        Usuario destinatario = a.getSolicitadaPor();

        // Si gerencia respondió su propia solicitud, no hay a quién avisar
        if (destinatario.getId().equals(a.getRespondidaPor().getId())) {
            return;
        }
        avisos.save(new AvisoDescuento(destinatario, cotizacion, a));
    }

    @Transactional(readOnly = true)
    public List<AvisoDescuentoResponse> listar(String correo) {
        return avisos.findTop20ByDestinatarioOrderByFechaDesc(usuario(correo)).stream()
                .map(AvisoDescuentoResponse::de)
                .toList();
    }

    /** Marca todos los avisos del usuario como leídos y devuelve la lista ya actualizada. */
    @Transactional
    public List<AvisoDescuentoResponse> marcarLeidos(String correo) {
        Usuario usuario = usuario(correo);
        avisos.marcarLeidos(usuario);
        return listar(correo);
    }

    /** Marca un aviso como leído y devuelve la lista ya actualizada. Un aviso ajeno no se modifica. */
    @Transactional
    public List<AvisoDescuentoResponse> marcarLeido(Long id, String correo) {
        avisos.marcarLeido(id, usuario(correo));
        return listar(correo);
    }

    private Usuario usuario(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar"));
    }
}