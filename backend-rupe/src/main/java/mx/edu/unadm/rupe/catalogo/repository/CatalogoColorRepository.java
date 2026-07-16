package mx.edu.unadm.rupe.catalogo.repository;

import java.util.List;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoColorRepository extends JpaRepository<CatalogoColor, Integer> {
    List<CatalogoColor> findByActivoTrueOrderByNombreAsc();
    List<CatalogoColor> findAllByOrderByNombreAsc();
}
