package mx.montacargasobregon.backend.rentas;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Arma rentas para las pruebas. Toda renta sale de una cotización (HU-37): aquí se arma una mínima,
 * sin equipos, con el cliente, el periodo y el contacto de la renta.
 */
public final class RentaDePrueba {

    private RentaDePrueba() {
    }

    public static Renta nueva(String folio, Cliente cliente, LocalDate inicio, LocalDate fin,
                              String direccionObra, String contactoObra, String telefonoContacto) {
        Cotizacion cotizacion = new Cotizacion("COT-" + folio, cliente, null,
                inicio.atTime(9, 0), fin.atTime(17, 0),
                new DatosDireccion("Calle 200", "1500", "Centro", "Cd. Obregón", "Sonora", "85000"),
                new DatosContactoSitio(contactoObra, telefonoContacto),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Renta renta = new Renta(folio, cotizacion);
        // Cada prueba revisa su propio texto de dirección
        ReflectionTestUtils.setField(renta, "direccionObra", direccionObra);
        return renta;
    }
}
