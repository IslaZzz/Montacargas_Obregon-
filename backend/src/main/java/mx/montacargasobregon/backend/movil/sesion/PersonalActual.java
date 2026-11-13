package mx.montacargasobregon.backend.movil.sesion;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;

/**
 * Quién usa la app móvil. Se recibe como parámetro en cualquier controlador:
 *   public List<...> misTraslados(PersonalActual personal)
 * y lo llena PersonalActualResolver con el encabezado X-Personal-Id.
 */
public record PersonalActual(Long id, String codigo, String nombre, TipoPersonal tipo) {

    public static PersonalActual de(Personal p) {
        return new PersonalActual(p.getId(), p.getCodigo(), p.getNombres() + " " + p.getApellidoPaterno(), p.getTipo());
    }

    /** Para acciones de un solo tipo de personal: personal.exigir(TipoPersonal.MECANICO) */
    public void exigir(TipoPersonal requerido) {
        if (tipo != requerido) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta acción no corresponde a tu puesto.");
        }
    }
}