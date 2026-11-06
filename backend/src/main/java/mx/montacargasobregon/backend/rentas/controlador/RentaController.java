package mx.montacargasobregon.backend.rentas.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import mx.montacargasobregon.backend.rentas.dto.RentaResponseDTO;
import mx.montacargasobregon.backend.rentas.servicio.RentaService;

@RestController
@RequestMapping("/api/rentas")
@RequiredArgsConstructor
public class RentaController {

    private final RentaService rentaService;

    @GetMapping
    public List<RentaResponseDTO> listarRentas() {
        return rentaService.listarRentas();
    }
}
