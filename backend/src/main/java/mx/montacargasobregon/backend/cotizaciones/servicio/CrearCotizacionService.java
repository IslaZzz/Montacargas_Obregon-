package mx.montacargasobregon.backend.cotizaciones.servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.clientes.servicio.ClienteService;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.comun.dto.DatosDireccionRequest;
import mx.montacargasobregon.backend.cotizaciones.dto.CrearCotizacionRequest;
import mx.montacargasobregon.backend.cotizaciones.modelo.CalculoPrecio;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.repositorio.CotizacionRepository;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.servicio.EquipoService;

@Service
public class CrearCotizacionService {

    private final CotizacionRepository repositorio;
    private final ClienteService clienteService;
    private final EquipoService equipoService;

    public CrearCotizacionService(CotizacionRepository repositorio, ClienteService clienteService,
                                  EquipoService equipoService) {
        this.repositorio = repositorio;
        this.clienteService = clienteService;
        this.equipoService = equipoService;
    }

    @Transactional
    public Cotizacion crear(CrearCotizacionRequest datos) {
        Cliente cliente = clienteService.obtener(datos.clienteId());
        RazonSocial razonSocial = razonSocialDelCliente(cliente, datos.razonSocialId());
        int dias = diasDeRenta(datos.fechaEntrega(), datos.fechaRecoleccion());
        validarSinRepetidos(datos.equipoIds());

        List<EquipoCotizacion> equipos = datos.equipoIds().stream()
                .map(this::equipoRentable)
                .map(equipo -> equipoCotizado(equipo, dias))
                .toList();
        BigDecimal subtotal = CalculoPrecio.calcularSubtotal(equipos.stream().map(EquipoCotizacion::getImporte).toList());
        BigDecimal iva = CalculoPrecio.calcularIva(subtotal);

        Cotizacion cotizacion = new Cotizacion(
                siguienteFolio(), cliente, razonSocial,
                datos.fechaEntrega(), datos.fechaRecoleccion(),
                direccionObra(datos.direccionObra()),
                new DatosContactoSitio(datos.contactoObra().trim(), datos.telefonoContacto()),
                subtotal, iva, CalculoPrecio.calcularTotal(subtotal, iva));
        equipos.forEach(cotizacion::agregarEquipo);
        return repositorio.save(cotizacion);
    }

    // Sin espacios de más
    private static DatosDireccion direccionObra(DatosDireccionRequest d) {
        return new DatosDireccion(d.calle().trim(), d.numero().trim(), d.colonia().trim(), d.ciudad().trim(),
                d.estado().trim(), d.codigoPostal());
    }

    private static RazonSocial razonSocialDelCliente(Cliente cliente, Long razonSocialId) {
        return cliente.getRazonesSociales().stream()
                .filter(r -> r.getId().equals(razonSocialId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "La razón social no pertenece al cliente"));
    }

    private static int diasDeRenta(LocalDateTime entrega, LocalDateTime recoleccion) {
        if (recoleccion.isBefore(entrega)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La recolección no puede ser antes de la entrega");
        }
        return CalculoPrecio.calcularDias(entrega.toLocalDate(), recoleccion.toLocalDate());
    }

    private static void validarSinRepetidos(List<Long> equipoIds) {
        if (new HashSet<>(equipoIds).size() != equipoIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un equipo no puede agregarse dos veces");
        }
    }

    private Equipo equipoRentable(Long equipoId) {
        Equipo equipo = equipoService.obtener(equipoId);
        if (equipo.getEstado() != EstadoEquipo.OPERATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El equipo " + equipo.getNumeroUnidad() + " no está disponible para rentar");
        }
        return equipo;
    }

    private static EquipoCotizacion equipoCotizado(Equipo equipo, int dias) {
        BigDecimal tarifa = equipo.getModelo().getTarifaDia();
        return new EquipoCotizacion(equipo, tarifa, CalculoPrecio.calcularImporte(dias, tarifa));
    }

    private String siguienteFolio() {
        return String.format("COT-%04d", repositorio.ultimoId() + 1);
    }
}
