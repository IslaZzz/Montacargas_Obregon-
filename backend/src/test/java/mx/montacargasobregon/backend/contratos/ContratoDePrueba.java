package mx.montacargasobregon.backend.contratos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.test.util.ReflectionTestUtils;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.modelo.DireccionFiscal;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;

/**
 * La cotización COT-0158 del prototipo de HU-23: MC-07 y PT-05 del 12 al 16 de octubre, con traslados.
 * Total con IVA $9,448.20; su anticipo del 30 % es $2,834.46.
 */
public final class ContratoDePrueba {

    private ContratoDePrueba() {
    }

    public static Cotizacion cotizacion() {
        Cliente cliente = new Cliente("Ramón", "Gutiérrez", "Soto", "Constructora del Valle de Guasave",
                "6871234567", "compras@cdvguasave.mx", LocalDate.of(2026, 10, 1));
        RazonSocial razon = new RazonSocial("Constructora del Valle de Guasave S.A. de C.V.", "CVG150312KD4",
                new DireccionFiscal("Blvd. Central", "1250", "Centro", "Guasave", "81000"));
        cliente.agregarRazonSocial(razon);

        Categoria montacargas = new Categoria("Montacargas");
        Equipo mc07 = new Equipo("MC-07", "SER-0007", 2022, new Modelo("Montacargas eléctrico 3 t", "Toyota",
                "TOY-3T", null, new BigDecimal("1200.00"), montacargas));
        Equipo pt05 = new Equipo("PT-05", "SER-0205", 2023, new Modelo("Patín hidráulico 2.5 t", "Jungheinrich",
                "JUN-25", null, new BigDecimal("250.00"), montacargas));
        ReflectionTestUtils.setField(mc07, "id", 7L);
        ReflectionTestUtils.setField(pt05, "id", 5L);

        Cotizacion cotizacion = new Cotizacion("COT-0158", cliente, razon,
                LocalDateTime.of(2026, 10, 12, 9, 0), LocalDateTime.of(2026, 10, 16, 17, 0),
                new DatosDireccion("Carretera Guasave-Los Mochis", "km 4.5", "Parque Industrial", "Guasave",
                        "Sinaloa", "81040"),
                new DatosContactoSitio("Ing. Raúl Soto", "6872104455"),
                new BigDecimal("9050.00"), new BigDecimal("1303.20"), new BigDecimal("9448.20"));
        ReflectionTestUtils.setField(cotizacion, "id", 158L);
        cotizacion.agregarEquipo(new EquipoCotizacion(mc07, new BigDecimal("1200.00"), new BigDecimal("6000.00")));
        cotizacion.agregarEquipo(new EquipoCotizacion(pt05, new BigDecimal("250.00"), new BigDecimal("1250.00")));
        ReflectionTestUtils.setField(cotizacion, "trasladoEntrega", new BigDecimal("900.00"));
        ReflectionTestUtils.setField(cotizacion, "trasladoRecoleccion", new BigDecimal("900.00"));
        return cotizacion;
    }
}
