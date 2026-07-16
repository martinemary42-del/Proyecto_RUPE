package mx.edu.unadm.rupe.security.repository;

import java.util.List;
import mx.edu.unadm.rupe.security.model.BitacoraSeguridad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraSeguridadRepository extends JpaRepository<BitacoraSeguridad, Integer> {
    List<BitacoraSeguridad> findAllByOrderByFechaHoraDesc();

    long countByAccion(String accion);
}
