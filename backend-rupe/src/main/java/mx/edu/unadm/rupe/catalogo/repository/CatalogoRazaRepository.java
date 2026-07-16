package mx.edu.unadm.rupe.catalogo.repository;

import java.util.List;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRaza;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoRazaRepository extends JpaRepository<CatalogoRaza, Integer> {
    List<CatalogoRaza> findByActivoTrueOrderByNombreAsc();
    List<CatalogoRaza> findAllByOrderByNombreAsc();
}
