package mx.edu.unadm.rupe.usuario.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.usuario.dto.PerfilResponse;
import mx.edu.unadm.rupe.usuario.dto.PerfilUpdateRequest;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final BitacoraSeguridadService bitacoraService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            BitacoraSeguridadService bitacoraService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.bitacoraService = bitacoraService;
    }

    @Transactional(readOnly = true)
    public PerfilResponse consultarPerfil(HttpSession session) {
        return new PerfilResponse(obtenerUsuarioSesion(session));
    }

    @Transactional
    public PerfilResponse actualizarPerfil(PerfilUpdateRequest request, HttpSession session) {
        Usuario usuario = obtenerUsuarioSesion(session);
        String correo = request.getCorreo().trim().toLowerCase();

        usuarioRepository.findByCorreoIgnoreCase(correo).ifPresent(existente -> {
            if (!existente.getIdUsuario().equals(usuario.getIdUsuario())) {
                throw new IllegalArgumentException("El correo ya esta registrado en otra cuenta");
            }
        });

        usuario.setNombreCompleto(request.getNombreCompleto().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(limpiarOpcional(request.getTelefono()));

        if (request.getPasswordNueva() != null && !request.getPasswordNueva().isBlank()) {
            validarCambioPassword(usuario, request);
            usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        }

        usuario.setFechaActualizacion(LocalDateTime.now());
        return new PerfilResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desactivarCuentaPropia(HttpSession session, HttpServletRequest request) {
        Usuario usuario = obtenerUsuarioSesion(session);
        usuario.setActivo(false);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Se conserva evidencia de auditoria sin borrar el historial del usuario.
        bitacoraService.registrar(usuario.getIdUsuario(), usuario.getCorreo(),
            "DESACTIVAR_CUENTA_PROPIA", "USUARIOS", "EXITOSO",
            "El usuario desactivo su propia cuenta desde el modulo de perfil.", request);
        session.invalidate();
    }

    private void validarCambioPassword(Usuario usuario, PerfilUpdateRequest request) {
        if (request.getPasswordActual() == null || request.getPasswordActual().isBlank()) {
            throw new IllegalArgumentException("Ingresa tu contrasena actual para cambiarla");
        }
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La contrasena actual no es correcta");
        }
        if (request.getPasswordNueva().length() < 8) {
            throw new IllegalArgumentException("La nueva contrasena debe tener al menos 8 caracteres");
        }
    }

    private Usuario obtenerUsuarioSesion(HttpSession session) {
        Object idUsuario = session.getAttribute("idUsuario");
        if (idUsuario == null) {
            throw new IllegalArgumentException("Inicia sesion para continuar");
        }
        return usuarioRepository.findById((Integer) idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private String limpiarOpcional(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }
}
