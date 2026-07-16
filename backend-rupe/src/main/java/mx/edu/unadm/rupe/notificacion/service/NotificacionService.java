package mx.edu.unadm.rupe.notificacion.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import mx.edu.unadm.rupe.avistamiento.model.Avistamiento;
import mx.edu.unadm.rupe.notificacion.dto.NotificacionResponse;
import mx.edu.unadm.rupe.notificacion.model.Notificacion;
import mx.edu.unadm.rupe.notificacion.repository.NotificacionRepository;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final BitacoraSeguridadService bitacoraService;

    public NotificacionService(NotificacionRepository notificacionRepository, BitacoraSeguridadService bitacoraService) {
        this.notificacionRepository = notificacionRepository;
        this.bitacoraService = bitacoraService;
    }

    @Transactional
    public void crearPorAvistamiento(Avistamiento avistamiento) {
        ReporteExtravio reporte = avistamiento.getReporte();
        if (reporte == null) {
            return;
        }

        boolean resguardo = Boolean.TRUE.equals(avistamiento.getResguardado());
        Notificacion notificacion = new Notificacion();
        notificacion.setReporte(reporte);
        notificacion.setTipo(resguardo ? "resguardo" : "avistamiento");
        notificacion.setDestinatario(reporte.getMascota().getUsuario().getCorreo());
        notificacion.setAsunto(resguardo ? "Posible resguardo reportado" : "Nuevo avistamiento recibido");
        notificacion.setMensaje(construirMensaje(avistamiento, reporte, resguardo));
        notificacion.setFechaEnvio(LocalDateTime.now());
        notificacion.setEstatusEnvio("NO_LEIDA");
        notificacionRepository.save(notificacion);
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> consultarMisNotificaciones(HttpSession session) {
        Integer idUsuario = obtenerIdUsuario(session);
        return notificacionRepository.findByReporteMascotaUsuarioIdUsuarioOrderByFechaEnvioDesc(idUsuario)
            .stream()
            .map(NotificacionResponse::new)
            .toList();
    }

    @Transactional
    public NotificacionResponse marcarLeida(Integer idNotificacion, HttpSession session, HttpServletRequest request) {
        Integer idUsuario = obtenerIdUsuario(session);
        Notificacion notificacion = notificacionRepository
            .findByIdNotificacionAndReporteMascotaUsuarioIdUsuario(idNotificacion, idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        notificacion.setEstatusEnvio("LEIDA");
        Notificacion guardada = notificacionRepository.save(notificacion);
        bitacoraService.registrar(idUsuario, null, "MARCAR_NOTIFICACION_LEIDA", "NOTIFICACIONES", "EXITOSO",
            "Notificacion " + idNotificacion + " marcada como leida", request);
        return new NotificacionResponse(guardada);
    }

    private String construirMensaje(Avistamiento avistamiento, ReporteExtravio reporte, boolean resguardo) {
        String tipo = resguardo ? "posible resguardo" : "avistamiento";
        return "Se registro un " + tipo + " relacionado con el folio " + reporte.getFolio()
            + " en " + avistamiento.getColonia() + ", " + avistamiento.getMunicipio()
            + ". Revisa el aviso para dar seguimiento.";
    }

    private Integer obtenerIdUsuario(HttpSession session) {
        Object idUsuario = session.getAttribute("idUsuario");
        if (idUsuario == null) {
            throw new IllegalArgumentException("Inicia sesion para continuar");
        }
        return (Integer) idUsuario;
    }
}
