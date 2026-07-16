package mx.edu.unadm.rupe.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mx.edu.unadm.rupe.auth.dto.LoginRequest;
import mx.edu.unadm.rupe.auth.dto.RegistroRequest;
import mx.edu.unadm.rupe.auth.dto.RecuperacionPasswordRequest;
import mx.edu.unadm.rupe.auth.dto.RecuperacionPasswordResponse;
import mx.edu.unadm.rupe.auth.dto.RestablecerPasswordRequest;
import mx.edu.unadm.rupe.auth.dto.UsuarioSesionResponse;
import mx.edu.unadm.rupe.auth.service.AuthService;
import mx.edu.unadm.rupe.auth.service.PasswordRecoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordRecoveryService recoveryService;

    public AuthController(AuthService authService, PasswordRecoveryService recoveryService) {
        this.authService = authService;
        this.recoveryService = recoveryService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioSesionResponse> registrar(@Valid @RequestBody RegistroRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.registrar(request, servletRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioSesionResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.login(request, session, servletRequest));
    }

    @GetMapping("/sesion")
    public ResponseEntity<UsuarioSesionResponse> sesionActual(HttpSession session) {
        return ResponseEntity.ok(authService.sesionActual(session));
    }

    @PostMapping("/recuperacion-password")
    public ResponseEntity<RecuperacionPasswordResponse> solicitarRecuperacion(@Valid @RequestBody RecuperacionPasswordRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(recoveryService.solicitar(request, servletRequest));
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request,
            HttpServletRequest servletRequest) {
        recoveryService.restablecer(request, servletRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.noContent().build();
    }
}
