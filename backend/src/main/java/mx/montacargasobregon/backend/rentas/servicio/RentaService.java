package mx.montacargasobregon.backend.rentas.servicio;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import mx.montacargasobregon.backend.rentas.dto.RentaResponseDTO;
import mx.montacargasobregon.backend.rentas.dto.RecoleccionRequestDTO;
import mx.montacargasobregon.backend.rentas.mapper.RentaMapper;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;

@Service
@RequiredArgsConstructor
public class RentaService {

    private final RentaRepository rentaRepository;
    private final RentaMapper rentaMapper;

    @Transactional(readOnly = true)
    public List<RentaResponseDTO> listarRentas() {
        return rentaRepository.findAllConDetalles()
            .stream()
            .map(rentaMapper::toResponse)
            .toList();
    }

    @Transactional
    public RentaResponseDTO actualizarRecoleccion(
        Long id,
        RecoleccionRequestDTO request
    ) {
        Renta renta = rentaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se encontró la renta solicitada."
            ));

        if (request.desde().isAfter(request.hasta())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La hora inicial no puede ser posterior a la hora final."
            );
        }

        if (renta.getEstado() == null ||
            renta.getEstado().name().equals("CERRADA") ||
            renta.getEstado().name().equals("CANCELADA")) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se puede programar la recolección de una renta cerrada o cancelada."
            );
        }

        renta.confirmarRecoleccion(
            request.desde(),
            request.hasta()
        );

        Renta actualizada = rentaRepository.save(renta);

        return rentaMapper.toResponse(actualizada);
    }
}
