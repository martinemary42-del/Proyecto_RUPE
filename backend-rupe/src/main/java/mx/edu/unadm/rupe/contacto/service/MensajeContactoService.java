package mx.edu.unadm.rupe.contacto.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import mx.edu.unadm.rupe.contacto.dto.AtenderMensajeContactoRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import mx.edu.unadm.rupe.contacto.dto.MensajeContactoRequest;
import mx.edu.unadm.rupe.contacto.dto.MensajeContactoResponse;
import mx.edu.unadm.rupe.contacto.model.MensajeContacto;
import mx.edu.unadm.rupe.contacto.repository.MensajeContactoRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.security.service.TurnstileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MensajeContactoService {
    private final MensajeContactoRepository repository;
    private final BitacoraSeguridadService bitacoraService;
    private final TurnstileService turnstileService;

    public MensajeContactoService(MensajeContactoRepository repository, BitacoraSeguridadService bitacoraService,
            TurnstileService turnstileService) {
        this.repository = repository;
        this.bitacoraService = bitacoraService;
        this.turnstileService = turnstileService;
    }

    @Transactional
    public MensajeContactoResponse registrar(MensajeContactoRequest request, HttpServletRequest httpRequest) {
        // Formulario publico: se valida CAPTCHA antes de guardar para disminuir mensajes automatizados.
        turnstileService.validarToken(request.getTurnstileToken());
        MensajeContacto mensaje = new MensajeContacto();
        mensaje.setNombre(limpiar(request.getNombre(), 100));
        mensaje.setCorreo(limpiarCorreo(request.getCorreo()));
        mensaje.setTelefono(limpiarTelefono(request.getTelefono()));
        mensaje.setAsunto(limpiar(request.getAsunto(), 60));
        mensaje.setMensaje(limpiar(request.getMensaje(), 500));
        mensaje.setIp(obtenerIp(httpRequest));
        MensajeContacto guardado = repository.save(mensaje);
        bitacoraService.registrar(null, guardado.getCorreo(), "MENSAJE_CONTACTO", "CONTACTO", "EXITOSO",
            "Mensaje publico recibido desde formulario de contacto", httpRequest);
        return new MensajeContactoResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<MensajeContactoResponse> listarAdmin(HttpSession session) {
        validarAdmin(session);
        return repository.findAllByOrderByFechaRegistroDesc().stream()
            .map(MensajeContactoResponse::new)
            .toList();
    }

    @Transactional
    public MensajeContactoResponse marcarAtendido(Integer idMensajeContacto, AtenderMensajeContactoRequest body,
            HttpSession session, HttpServletRequest request) {
        validarAdmin(session);
        MensajeContacto mensaje = repository.findById(idMensajeContacto)
            .orElseThrow(() -> new IllegalArgumentException("Mensaje de contacto no encontrado."));
        // Cerrar soporte exige dejar evidencia de la respuesta o accion administrativa.
        mensaje.setEstatus("ATENDIDO");
        mensaje.setFechaAtencion(LocalDateTime.now());
        if (body == null || body.getRespuestaAdmin() == null || body.getRespuestaAdmin().isBlank()) {
            throw new IllegalArgumentException("Escribe la respuesta o accion realizada antes de cerrar soporte.");
        }
        String respuestaAdmin = limpiar(body.getRespuestaAdmin(), 500);
        if (respuestaAdmin.length() < 10) {
            throw new IllegalArgumentException("La respuesta de soporte debe tener al menos 10 caracteres.");
        }
        mensaje.setRespuestaAdmin(respuestaAdmin);
        MensajeContacto guardado = repository.save(mensaje);
        bitacoraService.registrar((Integer) session.getAttribute("idUsuario"), guardado.getCorreo(),
            "ATENDER_MENSAJE_CONTACTO", "ADMIN_SOPORTE", "EXITOSO",
            "Mensaje de contacto marcado como atendido", request);
        return new MensajeContactoResponse(guardado);
    }

    public long contarPendientes() {
        return repository.countByEstatusIgnoreCase("PENDIENTE");
    }

    public long contarAtendidos() {
        return repository.countByEstatusIgnoreCase("ATENDIDO");
    }

    public long contarTotal() {
        return repository.count();
    }

    private void validarAdmin(HttpSession session) {
        if (!"ADMINISTRADOR".equals(session.getAttribute("rol"))) {
            throw new IllegalArgumentException("Acceso restringido a administradores");
        }
    }

    private String limpiar(String valor, int maximo) {
        // Normaliza espacios y aplica el limite usado por formularios y columnas de base de datos.
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Completa los campos obligatorios.");
        String limpio = valor.trim().replaceAll("\\s+", " ");
        if (limpio.length() > maximo) throw new IllegalArgumentException("Uno de los campos supera la longitud permitida.");
        return limpio;
    }

    private String limpiarCorreo(String valor) {
        String correo = limpiar(valor, 120).toLowerCase(Locale.ROOT);
        if (!correo.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("El correo no tiene un formato valido.");
        }
        return correo;
    }

    private String limpiarTelefono(String valor) {
        if (valor == null || valor.isBlank()) return "";
        String telefono = valor.trim();
        if (!telefono.matches("^[0-9]{10}$")) throw new IllegalArgumentException("El telefono debe tener 10 digitos.");
        return telefono;
    }

    private String obtenerIp(HttpServletRequest request) {
        if (request == null) return "desconocida";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
