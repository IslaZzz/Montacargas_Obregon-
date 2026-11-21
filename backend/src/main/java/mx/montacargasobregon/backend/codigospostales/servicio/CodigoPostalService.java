package mx.montacargasobregon.backend.codigospostales.servicio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.codigospostales.dto.CodigoPostalResponse;
import mx.montacargasobregon.backend.codigospostales.modelo.CodigoPostal;
import mx.montacargasobregon.backend.codigospostales.repositorio.CodigoPostalRepository;

@Service
public class CodigoPostalService {

    private final CodigoPostalRepository repositorio;

    public CodigoPostalService(CodigoPostalRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public CodigoPostalResponse buscar(String codigoPostal) {
        List<CodigoPostal> asentamientos = repositorio.findByCodigoPostalOrderByColoniaAsc(codigoPostal);
        if (asentamientos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese código postal");
        }

        CodigoPostal primero = asentamientos.get(0);
        String ciudad = asentamientos.stream()
                .map(CodigoPostal::getCiudad)
                .filter(c -> c != null && !c.isBlank())
                .findFirst()
                .orElse(primero.getMunicipio());
        List<String> colonias = asentamientos.stream().map(CodigoPostal::getColonia).distinct().toList();

        return new CodigoPostalResponse(codigoPostal, colonias, ciudad, primero.getEstado());
    }
}
