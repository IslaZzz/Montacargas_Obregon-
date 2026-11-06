package mx.montacargasobregon.backend.traslados.dto;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;

public record TrasladoResponse(
        Long id,
        String folio,
        Long rentaId,
        String rentaFolio,
        TipoTraslado tipo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String clienteNombre,
        String direccion,
        String contacto,
        String telefono,
        Long responsableId,
        String responsableCodigo,
        String responsableNombre,
        String responsableIniciales,
        EstadoTraslado estado,
        boolean yaInicio
) {
    public static TrasladoResponse from(Traslado t) {
        String clienteNombre = t.getRenta().getCliente().getEmpresa() != null && !t.getRenta().getCliente().getEmpresa().isBlank()
                ? t.getRenta().getCliente().getEmpresa()
                : (t.getRenta().getCliente().getNombre() + " " + t.getRenta().getCliente().getApellidoPaterno()).trim();

        Long respId = t.getResponsable() != null ? t.getResponsable().getId() : null;
        String respCodigo = t.getResponsable() != null ? t.getResponsable().getCodigo() : null;
        String respNombre = t.getResponsable() != null
                ? (t.getResponsable().getNombres() + " " + t.getResponsable().getApellidoPaterno()).trim()
                : null;

        String respIniciales = null;
        if (respNombre != null) {
            String[] partes = respNombre.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(2, partes.length); i++) {
                if (!partes[i].isEmpty()) {
                    sb.append(Character.toUpperCase(partes[i].charAt(0)));
                }
            }
            respIniciales = sb.toString();
        }

        return new TrasladoResponse(
                t.getId(),
                t.getFolio(),
                t.getRenta().getId(),
                t.getRenta().getFolio(),
                t.getTipo(),
                t.getFechaHoraInicio(),
                t.getFechaHoraFin(),
                clienteNombre,
                t.getRenta().getDireccionObra(),
                t.getRenta().getContactoObra(),
                t.getRenta().getTelefonoContacto(),
                respId,
                respCodigo,
                respNombre,
                respIniciales,
                t.getEstado(),
                t.isYaInicio()
        );
    }
}
