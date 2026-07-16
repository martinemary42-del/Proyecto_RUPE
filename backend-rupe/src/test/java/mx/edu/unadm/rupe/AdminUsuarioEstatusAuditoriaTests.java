package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import mx.edu.unadm.rupe.admin.dto.AdminCambiarEstatusUsuarioRequest;
import mx.edu.unadm.rupe.admin.dto.AdminUsuarioResponse;
import mx.edu.unadm.rupe.admin.service.AdminService;
import mx.edu.unadm.rupe.avistamiento.repository.AvistamientoRepository;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRol;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRolRepository;
import mx.edu.unadm.rupe.contacto.service.MensajeContactoService;
import mx.edu.unadm.rupe.mascota.repository.MascotaRepository;
import mx.edu.unadm.rupe.publico.repository.VisitaSitioRepository;
import mx.edu.unadm.rupe.reporte.repository.ReporteExtravioRepository;
import mx.edu.unadm.rupe.security.repository.BitacoraSeguridadRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminUsuarioEstatusAuditoriaTests {

    @Test
    void cambiarEstatusRequiereMotivoAuditable() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BitacoraSeguridadService bitacoraService = mock(BitacoraSeguridadService.class);
        AdminService service = servicioAdmin(usuarioRepository, bitacoraService);
        AdminCambiarEstatusUsuarioRequest request = new AdminCambiarEstatusUsuarioRequest();
        request.setActivo(false);
        request.setMotivo("corto");

        // El motivo evita desactivaciones arbitrarias y deja contexto para revision posterior.
        assertThatThrownBy(() -> service.cambiarEstatusUsuario(8, request, sesionAdmin(), mock(HttpServletRequest.class)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("motivo");

        verify(usuarioRepository, never()).save(any());
        verify(bitacoraService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cambiarEstatusRegistraMotivoEnBitacoraSinExponerDatosEnRespuesta() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        BitacoraSeguridadService bitacoraService = mock(BitacoraSeguridadService.class);
        AdminService service = servicioAdmin(usuarioRepository, bitacoraService);
        Usuario usuario = usuario(8, "Tutora Prueba", "tutora@example.com", true);
        when(usuarioRepository.findById(8)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminCambiarEstatusUsuarioRequest request = new AdminCambiarEstatusUsuarioRequest();
        request.setActivo(false);
        request.setMotivo("Riesgo alto detectado en bitacora por intentos repetidos");

        AdminUsuarioResponse response = service.cambiarEstatusUsuario(8, request, sesionAdmin(), mock(HttpServletRequest.class));

        assertThat(response.getActivo()).isFalse();
        assertThat(response.getCorreoEnmascarado()).isEqualTo("tu***@example.com");
        verify(bitacoraService).registrar(eq(99), eq("tutora@example.com"), eq("DESACTIVAR_USUARIO"),
            eq("ADMIN_USUARIOS"), eq("EXITOSO"), contains("Riesgo alto detectado"), any());
    }

    private AdminService servicioAdmin(UsuarioRepository usuarioRepository, BitacoraSeguridadService bitacoraService) {
        return new AdminService(
            usuarioRepository,
            mock(MascotaRepository.class),
            mock(ReporteExtravioRepository.class),
            mock(AvistamientoRepository.class),
            mock(VisitaSitioRepository.class),
            mock(BitacoraSeguridadRepository.class),
            bitacoraService,
            mock(MensajeContactoService.class),
            mock(CatalogoRolRepository.class),
            mock(PasswordEncoder.class)
        );
    }

    private HttpSession sesionAdmin() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("rol")).thenReturn("ADMINISTRADOR");
        when(session.getAttribute("idUsuario")).thenReturn(99);
        return session;
    }

    private Usuario usuario(Integer id, String nombre, String correo, boolean activo) {
        CatalogoRol rol = new CatalogoRol();
        rol.setNombre("TUTOR");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreCompleto(nombre);
        usuario.setCorreo(correo);
        usuario.setActivo(activo);
        usuario.setRol(rol);
        return usuario;
    }
}
