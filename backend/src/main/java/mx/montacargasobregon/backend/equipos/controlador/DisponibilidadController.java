package mx.montacargasobregon.backend.equipos.controlador;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.montacargasobregon.backend.equipos.repositorio.CategoriaRepository;
import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;
import mx.montacargasobregon.backend.equipos.dto.CategoriaResponse;
import mx.montacargasobregon.backend.equipos.dto.DisponibilidadResponse;
import mx.montacargasobregon.backend.equipos.servicio.DisponibilidadService;

@RestController
@RequestMapping("/api")
public class DisponibilidadController {

    private final DisponibilidadService servicio;
    private final CategoriaRepository categoriaRepositorio;

    public DisponibilidadController(DisponibilidadService servicio, CategoriaRepository categoriaRepositorio) {
        this.servicio = servicio;
        this.categoriaRepositorio = categoriaRepositorio;
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse consultar(
            @RequestParam Long categoriaId,
            @RequestParam(required = false) Etiqueta etiqueta,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.consultar(categoriaId, etiqueta, desde, hasta);
    }

    // Para la lista de categorías del filtro
    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return categoriaRepositorio.findAll(Sort.by("nombre")).stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre()))
                .toList();
    }
}