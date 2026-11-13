package mx.montacargasobregon.backend.movil.sesion;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

/** Entrar a la app móvil con el código del gafete, mientras el personal no tiene contraseña. */
@RestController
@RequestMapping("/api/movil")
public class SesionMovilController {

    private final PersonalRepository personalRepositorio;

    public SesionMovilController(PersonalRepository personalRepositorio) {
        this.personalRepositorio = personalRepositorio;
    }

    /** POST /api/movil/sesion: busca a la persona por su código; 401 si no existe o está de baja. */
    @PostMapping("/sesion")
    public PersonalActual entrar(@Valid @RequestBody IngresarCodigoRequest datos) {
        return personalRepositorio.findByCodigoIgnoreCaseAndActivoTrue(datos.codigo().trim())
                .map(PersonalActual::de)
                // El mismo mensaje si el código no existe o si la persona está de baja
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No encontramos ese código."));
    }

    /** GET /api/movil/yo: confirma que la persona guardada en el celular sigue activa (401 si no). */
    @GetMapping("/yo")
    public PersonalActual yo(PersonalActual personal) {
        return personal;
    }
}