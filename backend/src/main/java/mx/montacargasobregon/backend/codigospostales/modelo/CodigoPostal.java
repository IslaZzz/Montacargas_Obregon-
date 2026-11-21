package mx.montacargasobregon.backend.codigospostales.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "codigos_postales", indexes = @Index(name = "idx_codigos_postales_codigo", columnList = "codigo_postal"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodigoPostal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_postal", nullable = false, length = 5)
    private String codigoPostal;

    @Column(nullable = false, length = 100)
    private String colonia;

    @Column(length = 100)
    private String ciudad;

    @Column(nullable = false, length = 100)
    private String municipio;

    @Column(nullable = false, length = 50)
    private String estado;

    public CodigoPostal(String codigoPostal, String colonia, String ciudad, String municipio, String estado) {
        this.codigoPostal = codigoPostal;
        this.colonia = colonia;
        this.ciudad = ciudad;
        this.municipio = municipio;
        this.estado = estado;
    }
}
