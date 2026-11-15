package mx.montacargasobregon.backend.equipos.servicio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.repositorio.TarifaRepository;

@Service
public class TarifaService {

    private final TarifaRepository repositorio;

    public TarifaService(TarifaRepository repositorio) {
        this.repositorio = repositorio;
    }

    // Todos los modelos con su tarifa, en el orden en que se registraron
    @Transactional(readOnly = true)
    public List<Modelo> listar() {
        return repositorio.findAll(Sort.by("id"));
    }

    @Transactional
    public Modelo actualizar(Long modeloId, BigDecimal tarifaDia) {
        // Si no cambió ninguna fila, el modelo no existe
        if (repositorio.actualizarTarifa(modeloId, tarifaDia) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado");
        }

        // Se vuelve a leer para responder con los datos ya guardados
        return repositorio.findById(modeloId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));
    }
}