package mx.montacargasobregon.backend.cartas.servicio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.modelo.DireccionFiscal;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Los datos de la renta que van en la carta responsiva, ya tomados del cliente, los equipos y la renta.
 * Cualquiera puede venir vacío: {@link #faltantes()} dice cuáles impiden generar la carta.
 */
public record DatosCarta(
        String folio,
        String cliente,
        String correo,
        String telefonoCliente,
        String direccionFiscal,
        LocalDate fechaEntrega,
        String direccionEntrega,
        String contactoObra,
        String telefonoContacto,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        List<EquipoCarta> equipos) {

    /** Un equipo de la renta como aparece en la carta. */
    public record EquipoCarta(String numeroUnidad, String marca, String modelo, String serie) {
    }

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_MX);

    // A quién le toca corregir cada dato que falta (se muestra junto al dato)
    static final String CLIENTE = "Cliente";
    static final String RENTA = "Renta";
    static final String EQUIPO = "Equipo";

    /** Toma los datos de la renta. Deja fuera los equipos cancelados. */
    public static DatosCarta de(Renta renta) {
        Cliente c = renta.getCliente();
        List<EquipoCarta> equipos = renta.getEquipos().stream()
                .filter(er -> er.getFase() != FaseEquipoRenta.CANCELADO)
                .map(er -> equipo(er.getEquipo()))
                .sorted(Comparator.comparing(EquipoCarta::numeroUnidad))
                .toList();

        return new DatosCarta(
                renta.getFolio(),
                nombreCliente(c),
                c.getCorreo(),
                c.getTelefono(),
                direccionFiscal(c),
                renta.getEntregaDesde() == null ? null : renta.getEntregaDesde().toLocalDate(),
                renta.getDireccionObra(),
                renta.getContactoObra(),
                renta.getTelefonoContacto(),
                renta.getFechaInicio(),
                renta.getFechaFin(),
                equipos);
    }

    private static EquipoCarta equipo(Equipo e) {
        return new EquipoCarta(e.getNumeroUnidad(), e.getModelo().getMarca(), e.getModelo().getNombre(),
                e.getNumeroSerie());
    }

    // La empresa si la tiene; si no, el nombre de la persona
    private static String nombreCliente(Cliente c) {
        if (vacio(c.getEmpresa())) {
            return unir(" ", c.getNombre(), c.getApellidoPaterno(), c.getApellidoMaterno());
        }
        return c.getEmpresa();
    }

    // La renta todavía no guarda con qué razón social se factura: se usa la primera del cliente.
    // No es obligatoria (por confirmar con el equipo): sin razón social la carta dice "No registrada".
    private static String direccionFiscal(Cliente c) {
        if (c.getRazonesSociales().isEmpty()) return null;
        RazonSocial razon = c.getRazonesSociales().get(0);
        DireccionFiscal d = razon.getDireccionFiscal();
        return "%s %s, Col. %s, %s, C.P. %s".formatted(d.getCalle(), d.getNumero(), d.getColonia(),
                d.getCiudad(), d.getCodigoPostal());
    }

    /** Días del periodo contratado: del 12 de octubre al 11 de noviembre son 30. */
    public long dias() {
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin);
    }

    /**
     * Los datos obligatorios que faltan, en el orden en que aparecen en la carta:
     * nombre del dato → a quién le toca (Cliente, Renta o Equipo). Vacío si se puede generar.
     */
    public Map<String, String> faltantes() {
        Map<String, String> faltan = new LinkedHashMap<>();
        if (vacio(cliente)) faltan.put("Nombre del cliente", CLIENTE);
        if (vacio(correo)) faltan.put("Correo electrónico del cliente", CLIENTE);
        if (vacio(telefonoCliente)) faltan.put("Teléfono del cliente", CLIENTE);

        if (fechaEntrega == null) faltan.put("Fecha de entrega", RENTA);
        if (vacio(direccionEntrega)) faltan.put("Dirección de entrega", RENTA);
        if (vacio(contactoObra)) faltan.put("Contacto en la obra", RENTA);
        if (vacio(telefonoContacto)) faltan.put("Teléfono del contacto en la obra", RENTA);
        if (fechaInicio == null) faltan.put("Fecha de inicio de la renta", RENTA);
        if (fechaFin == null) faltan.put("Fecha final de la renta", RENTA);

        if (equipos.isEmpty()) faltan.put("Equipos de la renta", RENTA);
        for (EquipoCarta e : equipos) {
            if (vacio(e.serie())) faltan.put("Número de serie de " + e.numeroUnidad(), EQUIPO);
        }
        return faltan;
    }

    /**
     * Los datos como se imprimen, para guardarlos con la carta y saber después qué cambió.
     * Si un dato falta queda vacío, para poder comparar aunque la renta haya quedado incompleta.
     */
    public Map<String, String> resumen() {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("Cliente", texto(cliente));
        datos.put("Correo del cliente", texto(correo));
        datos.put("Teléfono del cliente", texto(telefonoCliente));
        datos.put("Dirección fiscal", texto(direccionFiscal));
        datos.put("Fecha de entrega", fecha(fechaEntrega, FECHA_LARGA));
        datos.put("Dirección de entrega", texto(direccionEntrega));
        datos.put("Contacto en la obra", unir(" · ", contactoObra, telefonoContacto));
        datos.put("Fecha de inicio", fecha(fechaInicio, FECHA));
        datos.put("Fecha final", fecha(fechaFin, FECHA));
        datos.put("Periodo", fechaInicio == null || fechaFin == null ? ""
                : dias() == 1 ? "1 día" : dias() + " días");
        datos.put("Equipos", equipos.stream()
                .map(e -> "%s %s %s (serie %s)".formatted(e.numeroUnidad(), e.marca(), e.modelo(), texto(e.serie())))
                .collect(Collectors.joining(", ")));
        return datos;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.strip();
    }

    private static String fecha(LocalDate fecha, DateTimeFormatter formato) {
        return fecha == null ? "" : fecha.format(formato);
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String unir(String separador, String... partes) {
        return java.util.Arrays.stream(partes)
                .filter(p -> !vacio(p))
                .collect(Collectors.joining(separador));
    }
}
