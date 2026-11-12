package mx.montacargasobregon.backend.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import mx.montacargasobregon.backend.clientes.modelo.DireccionFiscal;

class DireccionTest {

    @Test
    void enUnRenglonLlevaElEstadoCuandoLoTiene() {
        DatosDireccion obra = new DatosDireccion("Calle 200", "1500", "Centro", "Cd. Obregón", "Sonora", "85000");

        assertEquals("Calle 200 1500, Col. Centro, Cd. Obregón, Sonora, C.P. 85000", obra.enUnRenglon());
    }

    @Test
    void enUnRenglonSinEstadoNoLoPone() {
        // La dirección fiscal todavía no lleva estado
        DireccionFiscal fiscal = new DireccionFiscal("Calle Jalisco", "312", "Centro", "Navojoa", "85800");

        assertEquals("Calle Jalisco 312, Col. Centro, Navojoa, C.P. 85800", fiscal.enUnRenglon());
    }
}
