package mx.montacargasobregon.backend.movil.fotos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FotoRepository extends JpaRepository<Foto, Long> {

    // Sin el contenido, para no cargar las imágenes al listar
    @Query("""
        select new mx.montacargasobregon.backend.movil.fotos.FotoResponse(f.id, f.etiqueta, f.subidaEn)
        from Foto f
        where f.origen = :origen and f.referenciaId = :referenciaId
        order by f.id
        """)
    List<FotoResponse> listar(@Param("origen") OrigenFoto origen, @Param("referenciaId") Long referenciaId);

    @Query("select f.etiqueta from Foto f where f.origen = :origen and f.referenciaId = :referenciaId")
    List<String> etiquetas(@Param("origen") OrigenFoto origen, @Param("referenciaId") Long referenciaId);

    Optional<Foto> findByOrigenAndReferenciaIdAndEtiqueta(OrigenFoto origen, Long referenciaId, String etiqueta);
}