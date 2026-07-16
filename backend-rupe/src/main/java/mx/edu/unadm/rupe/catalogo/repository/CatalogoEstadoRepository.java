package mx.edu.unadm.rupe.catalogo.repository;

import java.util.List;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoEstadoRepository extends JpaRepository<CatalogoEstado, Integer> {
    List<CatalogoEstado> findByActivoTrueOrderByNombreAsc();
    List<CatalogoEstado> findAllByOrderByNombreAsc();
}
