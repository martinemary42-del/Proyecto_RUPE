package mx.edu.unadm.rupe.mascota.repository;

import java.util.List;
import java.util.Optional;
import mx.edu.unadm.rupe.mascota.model.Fotografia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FotografiaRepository extends JpaRepository<Fotografia, Integer> {
    Optional<Fotografia> findFirstByMascotaIdMascotaAndEsPrincipalTrueAndActivoTrueOrderByFechaRegistroDesc(Integer idMascota);
    List<Fotografia> findByMascotaIdMascotaAndEsPrincipalTrueAndActivoTrue(Integer idMascota);
}
