package mx.montacargasobregon.backend.clientes.dto;

import java.util.List;

import mx.montacargasobregon.backend.clientes.Cliente;

/**
 * Cliente DTO de respuesta que el backend devuelve al front.
 */
public record ClienteResponse(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String empresa,
        String telefono,
        String correo,
        List<RazonSocialResponse> razonesSociales) {

    /**
     * Convierte una entidad Cliente en su versión para enviar.
     */
    public static ClienteResponse toClienteResponse(Cliente c) {
        return new ClienteResponse(
                c.getId(), 
                c.getNombre(), 
                c.getApellidoPaterno(), 
                c.getApellidoMaterno(),
                c.getEmpresa(), 
                c.getTelefono(), 
                c.getCorreo(),
                c.getRazonesSociales().stream().map(RazonSocialResponse::toRazonSocialResponse).toList());
    }
}
