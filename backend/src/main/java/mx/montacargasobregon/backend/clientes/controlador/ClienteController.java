package mx.montacargasobregon.backend.clientes.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.clientes.dto.ClienteRequest;
import mx.montacargasobregon.backend.clientes.dto.ClienteResponse;
import mx.montacargasobregon.backend.clientes.servicio.ClienteService;

/**
 * Rutas HTTP de los clientes.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService servicio;

    public ClienteController(ClienteService servicio) {
        this.servicio = servicio;
    }

    /** POST /api/clientes: registra un cliente nuevo. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Responde 201 en lugar de 200
    public ClienteResponse registrar(@Valid @RequestBody ClienteRequest datos) {
        // @RequestBody: toma el JSON recibido y lo convierte en un ClienteRequest
        // @Valid: aplica las validaciones del DTO; si fallan, responde 400 sin entrar aquí
        return ClienteResponse.toClienteResponse(servicio.registrar(datos));
    }

    /** GET /api/clientes?texto=abc: busca clientes; sin "texto" devuelve todos. */
    @GetMapping
    public List<ClienteResponse> buscar(@RequestParam(required = false) String texto) {
        // @RequestParam: lee el parámetro "texto" de la URL; es opcional
        return servicio.buscar(texto).stream()
                .map(ClienteResponse::toClienteResponse)
                .toList();
    }

    /** GET /api/clientes/5: obtiene el cliente con id 5; si no existe, responde 404. */
    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        // @PathVariable: toma el id de la propia ruta
        return ClienteResponse.toClienteResponse(servicio.obtener(id));
    }
}
