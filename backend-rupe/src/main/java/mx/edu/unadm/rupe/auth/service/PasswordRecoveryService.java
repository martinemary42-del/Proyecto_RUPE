package mx.edu.unadm.rupe.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import mx.edu.unadm.rupe.auth.dto.RecuperacionPasswordRequest;
import mx.edu.unadm.rupe.auth.dto.RecuperacionPasswordResponse;
import mx.edu.unadm.rupe.auth.dto.RestablecerPasswordRequest;
import mx.edu.unadm.rupe.auth.model.PasswordResetToken;
import mx.edu.unadm.rupe.auth.repository.PasswordResetTokenRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.security.service.TurnstileService;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TurnstileService turnstileService;
    private final BitacoraSeguridadService bitacoraService;
    private final CorreoService correoService;

    @Value("${rupe.security.expose-reset-token:false}")
    private boolean exposeResetToken;

    public PasswordRecoveryService(UsuarioRepository usuarioRepository, PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, TurnstileService turnstileService, BitacoraSeguridadService bitacoraService,
            CorreoService correoService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.turnstileService = turnstileService;
        this.bitacoraService = bitacoraService;
        this.correoService = correoService;
    }

    @Transactional
    public RecuperacionPasswordResponse solicitar(RecuperacionPasswordRequest request, HttpServletRequest servletRequest) {
        turnstileService.validarToken(request.getCaptchaToken());
        String correo = request.getCorreo().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo).orElse(null);
        String tokenVisible = null;
        if (usuario != null && Boolean.TRUE.equals(usuario.getActivo())) {
            PasswordResetToken token = new PasswordResetToken();
            token.setUsuario(usuario);
            token.setToken(UUID.randomUUID().toString());
            token.setFechaExpiracion(LocalDateTime.now().plusMinutes(20));
            tokenRepository.save(token);
            tokenVisible = token.getToken();
            correoService.enviarRecuperacionPassword(correo, tokenVisible);
            bitacoraService.registrar(usuario.getIdUsuario(), correo, "SOLICITAR_RECUPERACION", "AUTH", "EXITOSO",
                correoService.estaActivo()
                    ? "Token temporal de recuperacion generado y enviado por correo"
                    : "Token temporal de recuperacion generado en modo local", servletRequest);
        } else {
            bitacoraService.registrar(null, correo, "SOLICITAR_RECUPERACION", "AUTH", "FALLIDO",
                "Solicitud con correo inexistente o inactivo", servletRequest);
        }
        // En desarrollo se puede devolver el token para pruebas locales; en produccion debe viajar por correo.
        String tokenRespuesta = exposeResetToken ? tokenVisible : null;
        return new RecuperacionPasswordResponse(
            "Si el correo existe, se genero una solicitud de recuperacion. En produccion el token se enviara por correo.",
            tokenRespuesta
        );
    }

    @Transactional
    public void restablecer(RestablecerPasswordRequest request, HttpServletRequest servletRequest) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken().trim())
            .orElseThrow(() -> new IllegalArgumentException("Token invalido o vencido"));
        if (Boolean.TRUE.equals(token.getUsado()) || token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token invalido o vencido");
        }
        Usuario usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuario.setFechaActualizacion(LocalDateTime.now());
        token.setUsado(true);
        usuarioRepository.save(usuario);
        tokenRepository.save(token);
        bitacoraService.registrar(usuario.getIdUsuario(), usuario.getCorreo(), "RESTABLECER_PASSWORD", "AUTH", "EXITOSO",
            "Contraseña restablecida por token temporal", servletRequest);
    }
}
