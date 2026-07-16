package mx.edu.unadm.rupe.usuario.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    List<Usuario> findAllByOrderByFechaRegistroDesc();
    long countByActivoTrue();
    long countByActivoFalse();
    long countByFechaBloqueoAfter(LocalDateTime fecha);
}
