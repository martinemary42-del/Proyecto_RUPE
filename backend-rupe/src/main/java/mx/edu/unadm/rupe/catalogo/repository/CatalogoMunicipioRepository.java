package mx.edu.unadm.rupe.catalogo.repository;

import java.util.List;
import mx.edu.unadm.rupe.catalogo.model.CatalogoMunicipio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoMunicipioRepository extends JpaRepository<CatalogoMunicipio, Integer> {
    List<CatalogoMunicipio> findByActivoTrueOrderByNombreAsc();
    List<CatalogoMunicipio> findAllByOrderByNombreAsc();
}
