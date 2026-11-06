package mx.montacargasobregon.backend.traslados.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;
import mx.montacargasobregon.backend.traslados.dto.AsignarTrasladoRequest;
import mx.montacargasobregon.backend.traslados.dto.OperadorDisponibilidadResponse;
import mx.montacargasobregon.backend.traslados.dto.TrasladoResponse;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

@Service
public class TrasladoService {

    private final TrasladoRepository repositorio;
    private final PersonalRepository personalRepositorio;

    public TrasladoService(TrasladoRepository repositorio, PersonalRepository personalRepositorio) {
        this.repositorio = repositorio;
        this.personalRepositorio = personalRepositorio;
    }

    /**
     * Consulta traslados con filtros por tipo/asignación y por rango de fechas.
     */
    @Transactional(readOnly = true)
    public List<TrasladoResponse> listar(String filtro, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Traslado> todos = repositorio.listarTodosConRelaciones();

        return todos.stream()
                .filter(t -> {
                    if (filtro == null || filtro.isBlank() || filtro.equalsIgnoreCase("TODOS")) {
                        return true;
                    }
                    if (filtro.equalsIgnoreCase("SIN_ASIGNAR")) {
                        return t.getResponsable() == null;
                    }
                    if (filtro.equalsIgnoreCase("ENTREGAS")) {
                        return t.getTipo() == TipoTraslado.ENTREGA;
                    }
                    if (filtro.equalsIgnoreCase("RECOLECCIONES")) {
                        return t.getTipo() == TipoTraslado.RECOLECCION;
                    }
                    return true;
                })
                .filter(t -> {
                    LocalDate dia = t.getFechaHoraInicio().toLocalDate();
                    if (fechaInicio != null && dia.isBefore(fechaInicio)) {
                        return false;
                    }
                    if (fechaFin != null && dia.isAfter(fechaFin)) {
                        return false;
                    }
                    return true;
                })
                .map(TrasladoResponse::from)
                .toList();
    }

    /**
     * Obtiene un traslado por su ID.
     */
    @Transactional(readOnly = true)
    public TrasladoResponse obtenerPorId(Long id) {
        Traslado traslado = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Traslado no encontrado"));
        return TrasladoResponse.from(traslado);
    }

    /**
     * Consulta los operadores de traslado activos y calcula su disponibilidad
     * para el horario y fecha especificados.
     */
    @Transactional(readOnly = true)
    public List<OperadorDisponibilidadResponse> consultarDisponibilidad(
            Long trasladoId,
            LocalDateTime fechaHoraInicio,
            LocalDateTime fechaHoraFin
    ) {
        Traslado trasladoActual = null;
        if (trasladoId != null) {
            trasladoActual = repositorio.findById(trasladoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Traslado no encontrado"));

            if (fechaHoraInicio == null) {
                fechaHoraInicio = trasladoActual.getFechaHoraInicio();
            }
            if (fechaHoraFin == null) {
                fechaHoraFin = trasladoActual.getFechaHoraFin();
            }
        }

        if (fechaHoraInicio == null || fechaHoraFin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere fecha y horario para consultar disponibilidad");
        }

        List<Personal> operadores = personalRepositorio.findByTipoAndActivoTrue(TipoPersonal.OP_TRASLADO);

        LocalDateTime inicioFinal = fechaHoraInicio;
        LocalDateTime finFinal = fechaHoraFin;
        Long excluirId = trasladoId;

        LocalDateTime inicioDia = fechaHoraInicio.toLocalDate().atStartOfDay();
        LocalDateTime finDia = fechaHoraInicio.toLocalDate().atTime(23, 59, 59);

        return operadores.stream()
                .map(op -> {
                    String nombreCompleto = (op.getNombres() + " " + op.getApellidoPaterno()).trim();
                    String iniciales = obtenerIniciales(nombreCompleto);

                    // Revisar si tiene empalmes en el rango solicitado
                    List<Traslado> empalmes = repositorio.buscarEmpalmes(
                            op.getId(),
                            inicioFinal,
                            finFinal,
                            excluirId,
                            EstadoTraslado.CANCELADO
                    );

                    if (!empalmes.isEmpty()) {
                        Traslado conf = empalmes.get(0);
                        String motivo = String.format("Tiene %s de %02d:%02d a %02d:%02d",
                                conf.getFolio(),
                                conf.getFechaHoraInicio().getHour(),
                                conf.getFechaHoraInicio().getMinute(),
                                conf.getFechaHoraFin().getHour(),
                                conf.getFechaHoraFin().getMinute()
                        );
                        return new OperadorDisponibilidadResponse(
                                op.getId(),
                                op.getCodigo(),
                                nombreCompleto,
                                iniciales,
                                false,
                                motivo,
                                conf.getId(),
                                conf.getFolio()
                        );
                    }

                    // Sin conflicto en este bloque: revisar traslados del mismo día para dar contexto
                    List<Traslado> trasladosDelDia = repositorio.buscarPorOperadorYRango(
                            op.getId(),
                            inicioDia,
                            finDia,
                            EstadoTraslado.CANCELADO
                    ).stream()
                     .filter(t -> excluirId == null || !t.getId().equals(excluirId))
                     .toList();

                    String motivo;
                    if (trasladosDelDia.isEmpty()) {
                        motivo = "Sin traslados ese día";
                    } else if (trasladosDelDia.size() == 1) {
                        Traslado unico = trasladosDelDia.get(0);
                        motivo = String.format("1 traslado ese día · %02d:%02d – %02d:%02d",
                                unico.getFechaHoraInicio().getHour(),
                                unico.getFechaHoraInicio().getMinute(),
                                unico.getFechaHoraFin().getHour(),
                                unico.getFechaHoraFin().getMinute()
                        );
                    } else {
                        motivo = trasladosDelDia.size() + " traslados ese día";
                    }

                    return new OperadorDisponibilidadResponse(
                            op.getId(),
                            op.getCodigo(),
                            nombreCompleto,
                            iniciales,
                            true,
                            motivo,
                            null,
                            null
                    );
                })
                .sorted(Comparator.comparing(OperadorDisponibilidadResponse::disponible).reversed()
                        .thenComparing(OperadorDisponibilidadResponse::nombreCompleto))
                .toList();
    }

    /**
     * Asigna o cambia el responsable de un traslado.
     * Valida que el traslado no haya iniciado y que el operador no tenga empalmes de horario.
     */
    @Transactional
    public TrasladoResponse asignar(Long trasladoId, AsignarTrasladoRequest request) {
        Traslado traslado = repositorio.findById(trasladoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Traslado no encontrado"));

        if (!traslado.puedeAsignarse()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El traslado " + traslado.getFolio() + " ya inició en campo o no está disponible para asignación.");
        }

        Personal operador = personalRepositorio.findById(request.personalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operador no encontrado"));

        if (!operador.isActivo() || operador.getTipo() != TipoPersonal.OP_TRASLADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El empleado seleccionado no es un operador de traslado activo.");
        }

        // Validación de empalme de horarios (REGLA CRÍTICA)
        List<Traslado> empalmes = repositorio.buscarEmpalmes(
                operador.getId(),
                traslado.getFechaHoraInicio(),
                traslado.getFechaHoraFin(),
                traslado.getId(),
                EstadoTraslado.CANCELADO
        );

        if (!empalmes.isEmpty()) {
            Traslado conflicto = empalmes.get(0);
            String nombreOp = (operador.getNombres() + " " + operador.getApellidoPaterno()).trim();
            String horarioConflicto = String.format("%02d:%02d a %02d:%02d",
                    conflicto.getFechaHoraInicio().getHour(),
                    conflicto.getFechaHoraInicio().getMinute(),
                    conflicto.getFechaHoraFin().getHour(),
                    conflicto.getFechaHoraFin().getMinute()
            );

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Conflicto de horario: %s ya tiene asignado el traslado %s de %s.",
                            nombreOp, conflicto.getFolio(), horarioConflicto));
        }

        traslado.asignarResponsable(operador);
        Traslado guardado = repositorio.save(traslado);
        return TrasladoResponse.from(guardado);
    }

    private String obtenerIniciales(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) return "OP";
        String[] partes = nombreCompleto.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, partes.length); i++) {
            if (!partes[i].isEmpty()) {
                sb.append(Character.toUpperCase(partes[i].charAt(0)));
            }
        }
        return sb.toString();
    }
}

