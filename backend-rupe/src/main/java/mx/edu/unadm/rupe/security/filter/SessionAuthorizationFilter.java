package mx.edu.unadm.rupe.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthorizationFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    public SessionAuthorizationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ruta = request.getRequestURI();

        if (!ruta.startsWith("/api/") || esRutaPublica(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        Object idUsuario = session != null ? session.getAttribute("idUsuario") : null;
        Object rol = session != null ? session.getAttribute("rol") : null;

        // Toda ruta privada requiere una sesion creada por /api/auth/login.
        if (!(idUsuario instanceof Integer)) {
            escribirError(response, HttpStatus.UNAUTHORIZED, "Inicia sesion para continuar");
            return;
        }

        // El modulo administrativo solo permite usuarios con rol ADMINISTRADOR.
        if (ruta.startsWith("/api/admin/") && !"ADMINISTRADOR".equals(rol)) {
            escribirError(response, HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este modulo");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean esRutaPublica(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        String metodo = request.getMethod();

        return ruta.startsWith("/api/auth/")
            || ruta.startsWith("/api/publico/")
            || ruta.equals("/api/salud")
            || ruta.startsWith("/uploads/")
            || ruta.startsWith("/api/catalogos/")
            || ruta.equals("/api/contacto")
            || (ruta.equals("/api/avistamientos") && "POST".equalsIgnoreCase(metodo));
    }

    private void escribirError(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("mensaje", mensaje));
    }
}
