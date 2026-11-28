package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.montacargasobregon.backend.comun.dto.DatosDireccionRequest;
import mx.montacargasobregon.backend.cotizaciones.modelo.TipoDescuento;

/**
 * Cuerpo de PUT /api/cotizaciones/5/correccion (HU-32): los datos de los pasos 2 a 4 de
 * «Nueva cotización» y el descuento. El cliente y la razón social no cambian.
 * Con descuentoTipo NINGUNO, valor y motivo llegan en null. Los límites del descuento que
 * dependen del subtotal los revisa el servicio.
 */
public record CorregirCotizacionRequest(
        @NotNull(message = "La fecha y hora de entrega es obligatoria") LocalDateTime fechaEntrega,
        @NotNull(message = "La fecha y hora de recolección es obligatoria") LocalDateTime fechaRecoleccion,
        @NotNull(message = "Escribe la dirección de la obra") @Valid DatosDireccionRequest direccionObra,
        @NotBlank(message = "Escribe quién recibe en la obra")
        @Size(max = 100, message = "El contacto admite máximo 100 caracteres") String contactoObra,
        @NotBlank(message = "Escribe el teléfono del contacto")
        @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos") String telefonoContacto,
        @NotEmpty(message = "Agrega al menos un equipo") List<@NotNull Long> equipoIds,
        @NotNull(message = "Elige si lleva descuento") TipoDescuento descuentoTipo,
        @DecimalMin(value = "0.00", message = "Descuento inválido")
        @Digits(integer = 10, fraction = 2, message = "Descuento inválido") BigDecimal descuentoValor,
        @Size(max = 200, message = "El motivo admite máximo 200 caracteres") String descuentoMotivo) {
}
