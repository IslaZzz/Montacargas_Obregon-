package mx.montacargasobregon.backend.avisos.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.usuarios.Usuario;

/**
 * Aviso de algo que pasó en la operación (traslados). Es para un usuario de la oficina
 * o para una persona del personal (app móvil): uno de los dos, nunca ambos.
 * El texto se guarda al crearlo, para que no cambie si después cambian los datos.
 */
@Entity
@Table(name = "avisos_operacion")
public class AvisoOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAvisoOperacion tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String detalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renta_id")
    private Renta renta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario; // Para la oficina

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id")
    private Personal personal; // Para la app móvil

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean leido;

    protected AvisoOperacion() {
    }

    private AvisoOperacion(TipoAvisoOperacion tipo, String titulo, String detalle, Renta renta,
                           Usuario usuario, Personal personal, LocalDateTime fecha) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.detalle = detalle;
        this.renta = renta;
        this.usuario = usuario;
        this.personal = personal;
        this.fecha = fecha;
    }

    public static AvisoOperacion paraUsuario(Usuario usuario, TipoAvisoOperacion tipo, String titulo,
                                             String detalle, Renta renta, LocalDateTime fecha) {
        return new AvisoOperacion(tipo, titulo, detalle, renta, usuario, null, fecha);
    }

    public static AvisoOperacion paraPersonal(Personal personal, TipoAvisoOperacion tipo, String titulo,
                                              String detalle, Renta renta, LocalDateTime fecha) {
        return new AvisoOperacion(tipo, titulo, detalle, renta, null, personal, fecha);
    }

    public Long getId() {
        return id;
    }

    public TipoAvisoOperacion getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDetalle() {
        return detalle;
    }

    public Renta getRenta() {
        return renta;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Personal getPersonal() {
        return personal;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean isLeido() {
        return leido;
    }
}