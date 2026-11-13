package mx.montacargasobregon.backend.rentas.servicio;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import mx.montacargasobregon.backend.rentas.dto.RentaResponseDTO;
import mx.montacargasobregon.backend.rentas.dto.RentaMapper;
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
}
