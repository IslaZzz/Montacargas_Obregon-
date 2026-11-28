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

import mx.montacargasobregon.backend.cotizaciones.modelo.AutorizacionDescuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.ResultadoAutorizacion;
import mx.montacargasobregon.backend.usuarios.Usuario;

/** Aviso para quien pidió un descuento: gerencia ya respondió (autorizó o rechazó). */
@Entity
@Table(name = "avisos_descuento")
public class AvisoDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id")
    private Usuario destinatario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoAutorizacion resultado; // Autorizada o rechazada

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean leido;

    // Quién respondió y su comentario, copiados de la respuesta: al corregir una cotización rechazada
    // (HU-32) la solicitud se reemplaza y el aviso tiene que seguir diciendo lo de entonces.
    // Vacíos en los avisos creados antes de HU-32
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondida_por_id")
    private Usuario respondidaPor;

    @Column(length = 500)
    private String comentario;

    protected AvisoDescuento() {
    }

    /** Aviso de la respuesta que gerencia acaba de dar a la solicitud de autorización. */
    public AvisoDescuento(Usuario destinatario, Cotizacion cotizacion, AutorizacionDescuento respuesta) {
        this.destinatario = destinatario;
        this.cotizacion = cotizacion;
        this.resultado = respuesta.getResultado();
        this.fecha = respuesta.getFechaRespuesta();
        this.respondidaPor = respuesta.getRespondidaPor();
        this.comentario = respuesta.getComentario();
    }

    public Long getId() {
        return id;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public ResultadoAutorizacion getResultado() {
        return resultado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean isLeido() {
        return leido;
    }

    public Usuario getRespondidaPor() {
        return respondidaPor;
    }

    public String getComentario() {
        return comentario;
    }
}