package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import mx.edu.unadm.rupe.security.filter.SessionAuthorizationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SessionAuthorizationFilterTests {
    private final SessionAuthorizationFilter filter = new SessionAuthorizationFilter(new ObjectMapper());

    @Test
    void permiteRutaPublicaSinSesion() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/publico/resumen");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void bloqueaRutaPrivadaSinSesion() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/mascotas/mis");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Inicia sesion");
    }

    @Test
    void bloqueaRutaAdminConRolDueno() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/resumen");
        request.getSession(true).setAttribute("idUsuario", 10);
        request.getSession().setAttribute("rol", "DUENO");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("No tienes permisos");
    }

    @Test
    void permiteRutaAdminConRolAdministrador() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/resumen");
        request.getSession(true).setAttribute("idUsuario", 1);
        request.getSession().setAttribute("rol", "ADMINISTRADOR");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
