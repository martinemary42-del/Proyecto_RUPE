package mx.edu.unadm.rupe.catalogo.repository;

import java.util.List;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoColoniaRepository extends JpaRepository<CatalogoColonia, Integer> {
    List<CatalogoColonia> findByActivoTrueOrderByNombreAsc();
    List<CatalogoColonia> findAllByOrderByNombreAsc();
}
