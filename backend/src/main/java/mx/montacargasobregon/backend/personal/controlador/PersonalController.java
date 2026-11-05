package mx.montacargasobregon.backend.personal.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.personal.dto.PersonalRequest;
import mx.montacargasobregon.backend.personal.dto.PersonalResponse;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.servicio.PersonalService;

/** Rutas HTTP del personal. */
@RestController
@RequestMapping("/api/personal")
public class PersonalController {

    private final PersonalService servicio;

    public PersonalController(PersonalService servicio) {
        this.servicio = servicio;
    }

    /** POST /api/personal: registra a alguien del personal. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalResponse registrar(@Valid @RequestBody PersonalRequest datos) {
        return PersonalResponse.toPersonalResponse(servicio.registrar(datos));
    }

    /**
     * GET /api/personal?texto=abc: busca por nombre, apellido o código.
     * GET /api/personal?tipo=MECANICO: lista los activos de esa función.
     */
    @GetMapping
    public List<PersonalResponse> buscar(@RequestParam(required = false) String texto,
                                         @RequestParam(required = false) TipoPersonal tipo) {
        return servicio.buscar(texto, tipo).stream()
                .map(PersonalResponse::toPersonalResponse)
                .toList();
    }

    /** GET /api/personal/5: obtiene al personal con id 5; si no existe, responde 404. */
    @GetMapping("/{id}")
    public PersonalResponse obtener(@PathVariable Long id) {
        return PersonalResponse.toPersonalResponse(servicio.obtener(id));
    }

    /** PUT /api/personal/5: cambia los datos. */
    @PutMapping("/{id}")
    public PersonalResponse actualizar(@PathVariable Long id, @Valid @RequestBody PersonalRequest datos) {
        return PersonalResponse.toPersonalResponse(servicio.actualizar(id, datos));
    }

    /** DELETE /api/personal/5: desactiva, no borra. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        servicio.desactivar(id);
    }
}