package mx.edu.unadm.rupe.catalogo.repository;

import java.util.List;
import java.util.Optional;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoEstatusRepository extends JpaRepository<CatalogoEstatus, Integer> {
    Optional<CatalogoEstatus> findByNombre(String nombre);
    List<CatalogoEstatus> findByActivoTrueOrderByNombreAsc();
    List<CatalogoEstatus> findAllByOrderByNombreAsc();
}
