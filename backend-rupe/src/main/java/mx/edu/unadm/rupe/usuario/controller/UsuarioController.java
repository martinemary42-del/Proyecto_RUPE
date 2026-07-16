package mx.edu.unadm.rupe.usuario.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import mx.edu.unadm.rupe.usuario.dto.PerfilResponse;
import mx.edu.unadm.rupe.usuario.dto.PerfilUpdateRequest;
import mx.edu.unadm.rupe.usuario.service.UsuarioService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil")
    public PerfilResponse consultarPerfil(HttpSession session) {
        return usuarioService.consultarPerfil(session);
    }

    @PutMapping("/perfil")
    public PerfilResponse actualizarPerfil(@Valid @RequestBody PerfilUpdateRequest request, HttpSession session) {
        return usuarioService.actualizarPerfil(request, session);
    }

    @DeleteMapping("/perfil")
    public Map<String, String> desactivarCuentaPropia(HttpSession session, HttpServletRequest request) {
        usuarioService.desactivarCuentaPropia(session, request);
        return Map.of("mensaje", "Cuenta desactivada correctamente.");
    }
}
