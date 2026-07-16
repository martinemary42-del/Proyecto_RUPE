package mx.edu.unadm.rupe.notificacion.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import mx.edu.unadm.rupe.notificacion.dto.NotificacionResponse;
import mx.edu.unadm.rupe.notificacion.service.NotificacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping("/mis")
    public List<NotificacionResponse> consultarMisNotificaciones(HttpSession session) {
        return notificacionService.consultarMisNotificaciones(session);
    }

    @PutMapping("/{idNotificacion}/leida")
    public NotificacionResponse marcarLeida(@PathVariable Integer idNotificacion, HttpSession session,
            HttpServletRequest request) {
        return notificacionService.marcarLeida(idNotificacion, session, request);
    }
}
