package mx.edu.unadm.rupe.catalogo.repository;

import java.util.List;
import java.util.Optional;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoRolRepository extends JpaRepository<CatalogoRol, Integer> {

    Optional<CatalogoRol> findByNombre(String nombre);
    List<CatalogoRol> findAllByOrderByNombreAsc();
}
