package mx.edu.unadm.rupe.reporte.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReporteExtravioRepository extends JpaRepository<ReporteExtravio, Integer> {
    long countByFolioStartingWith(String prefijo);
    List<ReporteExtravio> findAllByOrderByFechaRegistroDesc();
    List<ReporteExtravio> findByMascotaUsuarioIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(Integer idUsuario);
    Optional<ReporteExtravio> findByFolioAndActivoTrue(String folio);
    Optional<ReporteExtravio> findFirstByMascotaIdMascotaAndActivoTrueAndEstatusNombreIgnoreCaseOrderByFechaRegistroDesc(Integer idMascota, String estatus);
    long countByActivoTrueAndEstatusNombreIgnoreCase(String estatus);
    long countByActivoTrueAndTipoReporteIgnoreCase(String tipoReporte);
    long countByActivoTrueAndEstatusNombreIgnoreCaseAndFechaVencimientoBefore(String estatus, java.time.LocalDate fecha);
    long countByActivoTrueAndEstatusNombreIgnoreCaseAndRequiereRenovacionTrue(String estatus);
    List<ReporteExtravio> findTop3ByActivoTrueAndEstatusNombreIgnoreCaseOrderByFechaRegistroDesc(String estatus);
    List<ReporteExtravio> findByActivoTrueAndEstatusNombreIgnoreCaseOrderByFechaRegistroDesc(String estatus);
    Page<ReporteExtravio> findByActivoTrueAndEstatusNombreIgnoreCase(String estatus, Pageable pageable);
    Page<ReporteExtravio> findByActivoTrueAndEstatusNombreIgnoreCaseAndFolioContainingIgnoreCase(String estatus, String folio, Pageable pageable);

    @Query("""
        select r from ReporteExtravio r
        join r.mascota m
        join m.raza raza
        join m.color color
        join r.colonia col
        join col.municipio mun
        join mun.estado edo
        where r.activo = true
          and lower(r.estatus.nombre) = lower(:estatus)
          and (:texto is null or :texto = '' or lower(r.folio) like lower(concat('%', :texto, '%')) or lower(m.nombre) like lower(concat('%', :texto, '%')))
          and (:raza is null or :raza = '' or lower(raza.nombre) like lower(concat('%', :raza, '%')))
          and (:senas is null or :senas = '' or lower(m.senasParticulares) like lower(concat('%', :senas, '%')) or lower(color.nombre) like lower(concat('%', :senas, '%')) or lower(m.descripcionColor) like lower(concat('%', :senas, '%')) or lower(r.descripcionHechos) like lower(concat('%', :senas, '%')) or lower(r.referencias) like lower(concat('%', :senas, '%')))
          and (:sexo is null or :sexo = '' or lower(m.sexo) = lower(:sexo))
          and (:zona is null or :zona = '' or lower(edo.nombre) like lower(concat('%', :zona, '%')) or lower(mun.nombre) like lower(concat('%', :zona, '%')) or lower(col.nombre) like lower(concat('%', :zona, '%')))
        """)
    Page<ReporteExtravio> buscarPublicosPaginados(@Param("estatus") String estatus,
            @Param("texto") String texto, @Param("raza") String raza,
            @Param("senas") String senas, @Param("sexo") String sexo,
            @Param("zona") String zona, Pageable pageable);
}

