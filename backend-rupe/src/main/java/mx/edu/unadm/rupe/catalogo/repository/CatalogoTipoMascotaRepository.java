package mx.edu.unadm.rupe.catalogo.repository;

import java.util.List;
import mx.edu.unadm.rupe.catalogo.model.CatalogoTipoMascota;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoTipoMascotaRepository extends JpaRepository<CatalogoTipoMascota, Integer> {
    List<CatalogoTipoMascota> findByActivoTrueOrderByNombreAsc();
    List<CatalogoTipoMascota> findAllByOrderByNombreAsc();
}
