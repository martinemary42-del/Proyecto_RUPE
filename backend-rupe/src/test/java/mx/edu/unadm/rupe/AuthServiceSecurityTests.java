package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import mx.edu.unadm.rupe.auth.dto.LoginRequest;
import mx.edu.unadm.rupe.auth.service.AuthService;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRol;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRolRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.security.service.TurnstileService;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceSecurityTests {
    private UsuarioRepository usuarioRepository;
    private TurnstileService turnstileService;
    private BitacoraSeguridadService bitacoraService;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void configurar() {
        usuarioRepository = mock(UsuarioRepository.class);
        CatalogoRolRepository rolRepository = mock(CatalogoRolRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        turnstileService = mock(TurnstileService.class);
        bitacoraService = mock(BitacoraSeguridadService.class);
        authService = new AuthService(usuarioRepository, rolRepository, passwordEncoder, turnstileService, bitacoraService);
        ReflectionTestUtils.setField(authService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(authService, "captchaAfterAttempts", 3);
        ReflectionTestUtils.setField(authService, "lockMinutes", 15);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void solicitaCaptchaCuandoLaCuentaTieneTresIntentosFallidos() {
        Usuario usuario = usuario("dueno@rupe.test", 3);
        when(usuarioRepository.findByCorreoIgnoreCase("dueno@rupe.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(login("dueno@rupe.test"), mock(HttpSession.class), mock(HttpServletRequest.class)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CAPTCHA");

        verify(turnstileService).validarToken("captcha-prueba");
        assertThat(usuario.getIntentosFallidos()).isEqualTo(4);
    }

    @Test
    void bloqueaTemporalmenteDespuesDeCincoIntentosFallidos() {
        Usuario usuario = usuario("dueno@rupe.test", 4);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(usuarioRepository.findByCorreoIgnoreCase("dueno@rupe.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(login("dueno@rupe.test"), mock(HttpSession.class), request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bloqueada");

        assertThat(usuario.getIntentosFallidos()).isEqualTo(5);
        assertThat(usuario.getFechaBloqueo()).isNotNull();
        verify(bitacoraService).registrar(eq(usuario.getIdUsuario()), eq(usuario.getCorreo()), eq("LOGIN"),
            eq("AUTH"), eq("BLOQUEADO"), anyString(), eq(request));
    }

    private LoginRequest login(String correo) {
        LoginRequest request = new LoginRequest();
        request.setCorreo(correo);
        request.setPassword("incorrecta");
        request.setCaptchaToken("captcha-prueba");
        return request;
    }

    private Usuario usuario(String correo, int intentosFallidos) {
        CatalogoRol rol = new CatalogoRol();
        rol.setNombre("DUENO");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setRol(rol);
        usuario.setNombreCompleto("Usuario prueba");
        usuario.setCorreo(correo);
        usuario.setPasswordHash("hash");
        usuario.setIntentosFallidos(intentosFallidos);
        usuario.setActivo(true);
        return usuario;
    }
}
