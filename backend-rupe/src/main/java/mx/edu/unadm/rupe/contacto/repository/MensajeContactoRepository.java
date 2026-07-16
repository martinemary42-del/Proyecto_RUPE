package mx.edu.unadm.rupe.contacto.repository;

import java.util.List;
import mx.edu.unadm.rupe.contacto.model.MensajeContacto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Integer> {
    List<MensajeContacto> findAllByOrderByFechaRegistroDesc();
    long countByEstatusIgnoreCase(String estatus);
}
