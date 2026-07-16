package mx.edu.unadm.rupe.mascota.repository;

import java.util.List;
import mx.edu.unadm.rupe.mascota.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MascotaRepository extends JpaRepository<Mascota, Integer> {
    List<Mascota> findByUsuarioIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(Integer idUsuario);

    boolean existsByUsuarioIdUsuarioAndNombreIgnoreCaseAndRazaIdRaza(
        Integer idUsuario, String nombre, Integer idRaza);

    long countByActivoTrue();
}
