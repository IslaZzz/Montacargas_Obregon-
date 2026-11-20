package mx.montacargasobregon.backend.movil.sesion;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.personal.modelo.Personal;

/**
 * Sesión de un mecánico u operador en la app móvil. Dura una jornada fija desde que entra
 * con su código (venceEn) y no se extiende con el uso. Solo se guarda el hash del token:
 * el token en claro viaja una única vez, en la respuesta de entrar.
 */
@Entity
@Table(name = "sesiones_movil",
        indexes = @Index(name = "idx_sesiones_movil_personal", columnList = "personal_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_sesiones_movil_token_hash", columnNames = "token_hash"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SesionMovil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personal_id", nullable = false)
    private Personal personal;

    // SHA-256 del token en hexadecimal (64 caracteres)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "vence_en", nullable = false)
    private Instant venceEn;

    @Column(nullable = false)
    private boolean revocada;

    public SesionMovil(Personal personal, String tokenHash, Instant creadoEn, Instant venceEn) {
        this.personal = personal;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.venceEn = venceEn;
        this.revocada = false;
    }

    public void revocar() {
        this.revocada = true;
    }
}

