package mx.montacargasobregon.backend.equipos.controlador;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;
import mx.montacargasobregon.backend.equipos.dto.DisponibilidadResponse;
import mx.montacargasobregon.backend.equipos.servicio.DisponibilidadService;

@RestController
@RequestMapping("/api")
public class DisponibilidadController {

    private final DisponibilidadService servicio;

    public DisponibilidadController(DisponibilidadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse consultar(
            @RequestParam Long categoriaId,
            @RequestParam(required = false) Etiqueta etiqueta,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.consultar(categoriaId, etiqueta, desde, hasta);
    }
}