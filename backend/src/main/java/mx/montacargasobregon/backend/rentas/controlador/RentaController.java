package mx.montacargasobregon.backend.rentas.controlador;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.montacargasobregon.backend.rentas.dto.RentaResponseDTO;
import mx.montacargasobregon.backend.rentas.dto.RecoleccionRequestDTO;
import mx.montacargasobregon.backend.rentas.servicio.RentaService;

@RestController
@RequestMapping("/api/rentas")
@RequiredArgsConstructor
@Validated
public class RentaController {

    private final RentaService rentaService;

    @GetMapping
    public List<RentaResponseDTO> listarRentas() {
        return rentaService.listarRentas();
    }

    @PutMapping("/{id}/recoleccion")
    public RentaResponseDTO actualizarRecoleccion(
        @PathVariable Long id,
        @Valid @RequestBody RecoleccionRequestDTO request
    ) {
        return rentaService.actualizarRecoleccion(id, request);
    }
}
