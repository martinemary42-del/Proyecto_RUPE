package mx.edu.unadm.rupe.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import mx.edu.unadm.rupe.auth.dto.LoginRequest;
import mx.edu.unadm.rupe.auth.dto.RegistroRequest;
import mx.edu.unadm.rupe.auth.dto.UsuarioSesionResponse;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRol;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRolRepository;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import mx.edu.unadm.rupe.security.service.TurnstileService;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final CatalogoRolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final TurnstileService turnstileService;
    private final BitacoraSeguridadService bitacoraService;

    @Value("${rupe.security.max-login-attempts}")
    private int maxLoginAttempts;

    @Value("${rupe.security.captcha-after-attempts:3}")
    private int captchaAfterAttempts;

    @Value("${rupe.security.lock-minutes}")
    private int lockMinutes;

    public AuthService(UsuarioRepository usuarioRepository, CatalogoRolRepository rolRepository,
            PasswordEncoder passwordEncoder, TurnstileService turnstileService, BitacoraSeguridadService bitacoraService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.turnstileService = turnstileService;
        this.bitacoraService = bitacoraService;
    }

    @Transactional
    public UsuarioSesionResponse registrar(RegistroRequest request, HttpServletRequest servletRequest) {
        turnstileService.validarToken(request.getCaptchaToken());

        String correo = request.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo");
        }

        CatalogoRol rolDueno = rolRepository.findByNombre("DUENO")
            .orElseThrow(() -> new IllegalStateException("No existe el rol DUENO"));

        Usuario usuario = new Usuario();
        usuario.setRol(rolDueno);
        usuario.setNombreCompleto(request.getNombreCompleto().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(request.getTelefono());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setFechaRegistro(LocalDateTime.now());

        Usuario guardado = usuarioRepository.save(usuario);
        bitacoraService.registrar(guardado.getIdUsuario(), guardado.getCorreo(), "REGISTRO_USUARIO", "AUTH", "EXITOSO", "Registro de cuenta DUENO", servletRequest);
        return new UsuarioSesionResponse(guardado);
    }

    @Transactional
    public UsuarioSesionResponse login(LoginRequest request, HttpSession session, HttpServletRequest servletRequest) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.getCorreo().trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Correo o contraseña incorrectos"));

        validarCuentaActiva(usuario);
        validarBloqueo(usuario);
        validarCaptchaSiHayIntentosFallidos(usuario, request);

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario, servletRequest);
        }

        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        bitacoraService.registrar(usuario.getIdUsuario(), usuario.getCorreo(), "LOGIN", "AUTH", "EXITOSO", "Inicio de sesion correcto", servletRequest);

        session.setAttribute("idUsuario", usuario.getIdUsuario());
        session.setAttribute("rol", usuario.getRol().getNombre());

        return new UsuarioSesionResponse(usuario);
    }

    public UsuarioSesionResponse sesionActual(HttpSession session) {
        Object idUsuario = session.getAttribute("idUsuario");
        if (idUsuario == null) {
            throw new IllegalArgumentException("No existe una sesion activa");
        }

        Usuario usuario = usuarioRepository.findById((Integer) idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return new UsuarioSesionResponse(usuario);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private void validarCuentaActiva(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("La cuenta se encuentra desactivada");
        }
    }

    private void validarBloqueo(Usuario usuario) {
        LocalDateTime bloqueo = usuario.getFechaBloqueo();
        if (bloqueo != null && bloqueo.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cuenta bloqueada temporalmente. Intenta mas tarde");
        }
    }

    private void validarCaptchaSiHayIntentosFallidos(Usuario usuario, LoginRequest request) {
        if (usuario.getIntentosFallidos() >= captchaAfterAttempts) {
            turnstileService.validarToken(request.getCaptchaToken());
        }
    }

    private void registrarIntentoFallido(Usuario usuario, HttpServletRequest servletRequest) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);

        if (intentos >= maxLoginAttempts) {
            usuario.setFechaBloqueo(LocalDateTime.now().plusMinutes(lockMinutes));
            usuarioRepository.save(usuario);
            bitacoraService.registrar(usuario.getIdUsuario(), usuario.getCorreo(), "LOGIN", "AUTH", "BLOQUEADO", "Cuenta bloqueada por intentos fallidos", servletRequest);
            throw new IllegalArgumentException("Cuenta bloqueada temporalmente por varios intentos fallidos. Intenta mas tarde");
        }

        usuarioRepository.save(usuario);
        bitacoraService.registrar(usuario.getIdUsuario(), usuario.getCorreo(), "LOGIN", "AUTH", "FALLIDO", "Contraseña incorrecta", servletRequest);
        if (intentos >= captchaAfterAttempts) {
            throw new IllegalArgumentException("Se requiere CAPTCHA para continuar con el inicio de sesion");
        }
        throw new IllegalArgumentException("Correo o contraseña incorrectos");
    }
}
