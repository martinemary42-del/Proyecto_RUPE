package mx.edu.unadm.rupe.notificacion.repository;

import java.util.List;
import java.util.Optional;
import mx.edu.unadm.rupe.notificacion.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findByReporteMascotaUsuarioIdUsuarioOrderByFechaEnvioDesc(Integer idUsuario);

    Optional<Notificacion> findByIdNotificacionAndReporteMascotaUsuarioIdUsuario(Integer idNotificacion, Integer idUsuario);
}
