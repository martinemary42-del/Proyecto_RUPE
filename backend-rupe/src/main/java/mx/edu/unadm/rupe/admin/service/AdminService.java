package mx.edu.unadm.rupe.admin.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import mx.edu.unadm.rupe.admin.dto.AdminBitacoraResponse;
import mx.edu.unadm.rupe.admin.dto.AdminCambiarEstatusUsuarioRequest;
import mx.edu.unadm.rupe.admin.dto.AdminCrearUsuarioRequest;
import mx.edu.unadm.rupe.admin.dto.AdminReporteResponse;
import mx.edu.unadm.rupe.admin.dto.AdminResumenResponse;
import mx.edu.unadm.rupe.admin.dto.AdminUsuarioResponse;
import mx.edu.unadm.rupe.admin.dto.VisitaPaginaResponse;
import mx.edu.unadm.rupe.avistamiento.repository.AvistamientoRepository;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRol;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRolRepository;
import mx.edu.unadm.rupe.contacto.service.MensajeContactoService;
import mx.edu.unadm.rupe.mascota.repository.MascotaRepository;
import mx.edu.unadm.rupe.publico.repository.VisitaSitioRepository;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import mx.edu.unadm.rupe.reporte.repository.ReporteExtravioRepository;
import mx.edu.unadm.rupe.security.repository.BitacoraSeguridadRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UsuarioRepository usuarioRepository;
    private final MascotaRepository mascotaRepository;
    private final ReporteExtravioRepository reporteRepository;
    private final AvistamientoRepository avistamientoRepository;
    private final VisitaSitioRepository visitaRepository;
    private final BitacoraSeguridadRepository bitacoraRepository;
    private final BitacoraSeguridadService bitacoraService;
    private final MensajeContactoService mensajeContactoService;
    private final CatalogoRolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UsuarioRepository usuarioRepository, MascotaRepository mascotaRepository,
            ReporteExtravioRepository reporteRepository, AvistamientoRepository avistamientoRepository,
            VisitaSitioRepository visitaRepository, BitacoraSeguridadRepository bitacoraRepository,
            BitacoraSeguridadService bitacoraService,
            MensajeContactoService mensajeContactoService, CatalogoRolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.mascotaRepository = mascotaRepository;
        this.reporteRepository = reporteRepository;
        this.avistamientoRepository = avistamientoRepository;
        this.visitaRepository = visitaRepository;
        this.bitacoraRepository = bitacoraRepository;
        this.bitacoraService = bitacoraService;
        this.mensajeContactoService = mensajeContactoService;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminResumenResponse resumen(HttpSession session) {
        validarAdmin(session);
        var visitas = visitaRepository.findAll().stream()
            .sorted(Comparator.comparingLong(v -> -v.getTotal()))
            .map(v -> new VisitaPaginaResponse(v.getPagina(), v.getTotal()))
            .toList();
        long totalVisitas = visitas.stream().mapToLong(VisitaPaginaResponse::getTotal).sum();
        long pendientesRenovacion = reporteRepository
            .countByActivoTrueAndEstatusNombreIgnoreCaseAndRequiereRenovacionTrue("ACTIVO")
            + reporteRepository.countByActivoTrueAndEstatusNombreIgnoreCaseAndFechaVencimientoBefore("ACTIVO", LocalDate.now());
        return new AdminResumenResponse(
            usuarioRepository.countByActivoTrue(),
            usuarioRepository.countByActivoFalse(),
            // Conteo historico para admin: incluye registros con borrado logico para trazabilidad y estadistica.
            mascotaRepository.count(),
            reporteRepository.countByActivoTrueAndEstatusNombreIgnoreCase("ACTIVO"),
            reporteRepository.countByActivoTrueAndTipoReporteIgnoreCase("EXTRAVIO"),
            reporteRepository.countByActivoTrueAndTipoReporteIgnoreCase("ROBO"),
            pendientesRenovacion,
            reporteRepository.countByActivoTrueAndEstatusNombreIgnoreCase("RECUPERADO"),
            avistamientoRepository.countByActivoTrue(),
            avistamientoRepository.countByActivoTrueAndValidadoDuenoFalse(),
            totalVisitas,
            usuarioRepository.countByFechaBloqueoAfter(LocalDateTime.now()),
            bitacoraRepository.countByAccion("DESACTIVAR_CUENTA_PROPIA"),
            mensajeContactoService.contarTotal(),
            mensajeContactoService.contarPendientes(),
            mensajeContactoService.contarAtendidos(),
            visitas
        );
    }

    @Transactional(readOnly = true)
    public List<AdminReporteResponse> reportes(HttpSession session) {
        validarAdmin(session);
        return reporteRepository.findAllByOrderByFechaRegistroDesc()
            .stream()
            .map(this::construirReporteAdmin)
            .toList();
    }

    private AdminReporteResponse construirReporteAdmin(ReporteExtravio reporte) {
        long avistamientos = avistamientoRepository.countByReporteIdReporteAndActivoTrue(reporte.getIdReporte());
        return new AdminReporteResponse(reporte, avistamientos);
    }

    @Transactional(readOnly = true)
    public List<AdminUsuarioResponse> usuarios(HttpSession session) {
        validarAdmin(session);
        return usuarioRepository.findAllByOrderByFechaRegistroDesc()
            .stream()
            .map(AdminUsuarioResponse::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBitacoraResponse> bitacora(HttpSession session) {
        validarAdmin(session);
        return bitacoraRepository.findAllByOrderByFechaHoraDesc()
            .stream()
            .map(AdminBitacoraResponse::new)
            .toList();
    }

    @Transactional
    public AdminUsuarioResponse crearAdministrador(AdminCrearUsuarioRequest request,
            HttpSession session, HttpServletRequest httpRequest) {
        validarAdmin(session);
        String nombre = limpiarNombre(request.getNombreCompleto());
        String correo = limpiarCorreo(request.getCorreo());
        String telefono = limpiarTelefono(request.getTelefono());
        validarPassword(request.getPassword());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }

        CatalogoRol rolAdmin = rolRepository.findByNombre("ADMINISTRADOR")
            .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR."));
        Usuario usuario = new Usuario();
        usuario.setRol(rolAdmin);
        usuario.setNombreCompleto(nombre);
        usuario.setCorreo(correo);
        usuario.setTelefono(telefono);
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setFechaRegistro(LocalDateTime.now());
        Usuario guardado = usuarioRepository.save(usuario);

        bitacoraService.registrar((Integer) session.getAttribute("idUsuario"), correo,
            "CREAR_ADMINISTRADOR", "ADMIN_USUARIOS", "EXITOSO",
            "Alta de administrador secundario", httpRequest);
        return new AdminUsuarioResponse(guardado);
    }

    @Transactional
    public AdminUsuarioResponse cambiarEstatusUsuario(Integer idUsuario, AdminCambiarEstatusUsuarioRequest body,
            HttpSession session, HttpServletRequest request) {
        validarAdmin(session);
        if (body == null || body.getActivo() == null) {
            throw new IllegalArgumentException("Indica el nuevo estatus del usuario.");
        }
        boolean activo = body.getActivo();
        String motivo = limpiarMotivoCambioEstatus(body.getMotivo());

        Integer idAdmin = (Integer) session.getAttribute("idUsuario");
        if (idAdmin != null && idAdmin.equals(idUsuario) && !activo) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta administrativa.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if ("admin@rupe.local".equalsIgnoreCase(usuario.getCorreo()) && !activo) {
            throw new IllegalArgumentException("La cuenta principal del sistema no se puede desactivar.");
        }
        usuario.setActivo(activo);
        usuario.setFechaActualizacion(LocalDateTime.now());

        Usuario guardado = usuarioRepository.save(usuario);
        bitacoraService.registrar(idAdmin, usuario.getCorreo(),
            activo ? "REACTIVAR_USUARIO" : "DESACTIVAR_USUARIO",
            "ADMIN_USUARIOS", "EXITOSO",
            "Cambio de estatus administrativo del usuario " + idUsuario + ". Motivo: " + motivo, request);

        return new AdminUsuarioResponse(guardado);
    }

    private String limpiarMotivoCambioEstatus(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Escribe el motivo del cambio de estatus.");
        }
        String motivo = valor.trim().replaceAll("\\s+", " ");
        if (motivo.length() < 10 || motivo.length() > 300) {
            throw new IllegalArgumentException("El motivo debe tener entre 10 y 300 caracteres.");
        }
        return motivo;
    }

    private String limpiarNombre(String valor) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Escribe el nombre completo.");
        String limpio = valor.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 3 || limpio.length() > 120) throw new IllegalArgumentException("El nombre debe tener entre 3 y 120 caracteres.");
        return limpio;
    }

    private String limpiarCorreo(String valor) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Escribe el correo.");
        String correo = valor.trim().toLowerCase();
        if (!correo.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("El correo no tiene un formato válido.");
        }
        return correo;
    }

    private String limpiarTelefono(String valor) {
        if (valor == null || valor.isBlank()) return "";
        String telefono = valor.trim();
        if (!telefono.matches("^[0-9]{10}$")) throw new IllegalArgumentException("El teléfono debe tener 10 dígitos.");
        return telefono;
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 40) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 40 caracteres.");
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$")) {
            throw new IllegalArgumentException("La contraseña debe incluir mayúscula, minúscula, número y carácter especial.");
        }
    }

    private void validarAdmin(HttpSession session) {
        Object rol = session.getAttribute("rol");
        if (!"ADMINISTRADOR".equals(rol)) {
            throw new IllegalArgumentException("Acceso restringido a administradores");
        }
    }
}
