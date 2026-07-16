package mx.edu.unadm.rupe.publico.repository;

import java.util.Optional;
import mx.edu.unadm.rupe.publico.model.VisitaSitio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitaSitioRepository extends JpaRepository<VisitaSitio, Integer> {
    Optional<VisitaSitio> findByPagina(String pagina);
}
