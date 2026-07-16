package mx.edu.unadm.rupe.avistamiento.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import mx.edu.unadm.rupe.avistamiento.model.Avistamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvistamientoRepository extends JpaRepository<Avistamiento, Integer> {

    List<Avistamiento> findByReporteMascotaUsuarioIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(Integer idUsuario);

    Optional<Avistamiento> findByIdAvistamientoAndReporteMascotaUsuarioIdUsuario(Integer idAvistamiento, Integer idUsuario);

    long countByActivoTrue();

    long countByActivoTrueAndValidadoDuenoFalse();

    long countByReporteIdReporteAndActivoTrue(Integer idReporte);

    @Query("""
        select a from Avistamiento a
        where a.reporte is null
          and a.activo = true
          and (a.resguardado = true or a.fechaRegistro >= :fechaMinimaSinResguardo)
        order by a.fechaRegistro desc
        """)
    List<Avistamiento> buscarPublicosSinFolio(@Param("fechaMinimaSinResguardo") java.time.LocalDateTime fechaMinimaSinResguardo);

    Page<Avistamiento> findByReporteIsNullAndActivoTrue(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Avistamiento a
           set a.activo = false
         where a.reporte is null
           and a.activo = true
           and a.resguardado = false
           and a.fechaRegistro < :fechaLimite
        """)
    int desactivarSinFolioVencidosSinResguardo(@Param("fechaLimite") java.time.LocalDateTime fechaLimite);

    @Query("""
        select a from Avistamiento a
        join a.colonia col
        join col.municipio mun
        join mun.estado edo
        where a.reporte is null
          and a.activo = true
          and (a.resguardado = true or a.fechaRegistro >= :fechaMinimaSinResguardo)
          and (:texto is null or :texto = '' or lower(a.folioAvistamiento) like lower(concat('%', :texto, '%')) or lower(a.descripcion) like lower(concat('%', :texto, '%')) or lower(a.referencias) like lower(concat('%', :texto, '%')))
          and (:senas is null or :senas = '' or lower(a.descripcion) like lower(concat('%', :senas, '%')) or lower(a.referencias) like lower(concat('%', :senas, '%')))
          and (:zona is null or :zona = '' or lower(edo.nombre) like lower(concat('%', :zona, '%')) or lower(mun.nombre) like lower(concat('%', :zona, '%')) or lower(col.nombre) like lower(concat('%', :zona, '%')))
          and (:resguardado is null or a.resguardado = :resguardado)
        """)
    Page<Avistamiento> buscarPublicosSinFolioPaginados(@Param("texto") String texto,
            @Param("senas") String senas, @Param("zona") String zona,
            @Param("resguardado") Boolean resguardado,
            @Param("fechaMinimaSinResguardo") java.time.LocalDateTime fechaMinimaSinResguardo,
            Pageable pageable);
}
