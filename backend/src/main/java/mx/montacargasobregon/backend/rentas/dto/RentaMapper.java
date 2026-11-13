package mx.montacargasobregon.backend.rentas.dto;

import java.util.List;
import org.springframework.stereotype.Component;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.rentas.dto.*;
import mx.montacargasobregon.backend.rentas.modelo.*;

@Component
public class RentaMapper {

    public RentaResponseDTO toResponse(Renta renta) {

        Cliente cliente = renta.getCliente();

        ClienteRentaDTO clienteDTO = new ClienteRentaDTO(
            cliente.getId(),
            cliente.getNombre(),
            cliente.getApellidoPaterno(),
            cliente.getApellidoMaterno(),
            cliente.getEmpresa()
        );

        List<EquipoRentaDTO> equiposDTO = renta.getEquipos()
            .stream()
            .map(this::toEquipoDTO)
            .toList();

        return new RentaResponseDTO(
            renta.getId(),
            renta.getFolio(),
            clienteDTO,
            renta.getFechaInicio(),
            renta.getFechaFin(),
            renta.getEntregaDesde(),
            renta.getEntregaHasta(),
            renta.getRecoleccionDesde(),
            renta.getRecoleccionHasta(),
            renta.getDireccionObra(),
            renta.getContactoObra(),
            renta.getTelefonoContacto(),
            renta.getEstado(),
            equiposDTO
        );
    }

    private EquipoRentaDTO toEquipoDTO(EquipoRenta equipoRenta) {

        Equipo equipo = equipoRenta.getEquipo();

        return new EquipoRentaDTO(
            equipoRenta.getId(),
            equipo.getId(),
            equipo.getNumeroUnidad(),
            equipo.getNumeroSerie(),
            equipoRenta.getFase()
        );
    }
}
