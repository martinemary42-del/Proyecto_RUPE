package mx.edu.unadm.rupe.avistamiento.service;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoReclamoRequest;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoRequest;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoResponse;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoValidacionRequest;
import mx.edu.unadm.rupe.avistamiento.model.Avistamiento;
import mx.edu.unadm.rupe.avistamiento.repository.AvistamientoRepository;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstatus;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstatusRepository;
import mx.edu.unadm.rupe.mascota.repository.FotografiaRepository;
import mx.edu.unadm.rupe.notificacion.service.NotificacionService;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import mx.edu.unadm.rupe.reporte.repository.ReporteExtravioRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.security.service.TurnstileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvistamientoService {

    private static final long MAX_FOTO_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final AvistamientoRepository avistamientoRepository;
    private final ReporteExtravioRepository reporteRepository;
    private final CatalogoEstatusRepository estatusRepository;
    private final CatalogoColoniaRepository coloniaRepository;
    private final TurnstileService turnstileService;
    private final NotificacionService notificacionService;
    private final FotografiaRepository fotografiaRepository;
    private final BitacoraSeguridadService bitacoraService;

    public AvistamientoService(AvistamientoRepository avistamientoRepository,
            ReporteExtravioRepository reporteRepository, CatalogoEstatusRepository estatusRepository,
            TurnstileService turnstileService, NotificacionService notificacionService,
            FotografiaRepository fotografiaRepository, BitacoraSeguridadService bitacoraService,
            CatalogoColoniaRepository coloniaRepository) {
        this.avistamientoRepository = avistamientoRepository;
        this.reporteRepository = reporteRepository;
        this.estatusRepository = estatusRepository;
        this.coloniaRepository = coloniaRepository;
        this.turnstileService = turnstileService;
        this.notificacionService = notificacionService;
        this.fotografiaRepository = fotografiaRepository;
        this.bitacoraService = bitacoraService;
    }

    @Transactional(readOnly = true)
    public List<AvistamientoResponse> consultarMisAvistamientos(HttpSession session) {
        // Se consulta por el id de sesion para que el dueño vea solo avisos asociados a sus reportes.
        Object idUsuario = session.getAttribute("idUsuario");
        if (idUsuario == null) {
            throw new IllegalArgumentException("Inicia sesion para continuar");
        }
        return avistamientoRepository
            .findByReporteMascotaUsuarioIdUsuarioAndActivoTrueOrderByFechaRegistroDesc((Integer) idUsuario)
            .stream()
            .map(avistamiento -> new AvistamientoResponse(avistamiento, null, obtenerFotoMascota(avistamiento)))
            .toList();
    }


    @Transactional
    public AvistamientoResponse reclamarAvistamiento(Integer idAvistamiento, AvistamientoReclamoRequest request,
            HttpSession session, HttpServletRequest servletRequest) {
        Object idUsuario = session.getAttribute("idUsuario");
        if (!(idUsuario instanceof Integer)) {
            throw new IllegalArgumentException("Inicia sesion para continuar");
        }

        Avistamiento avistamiento = avistamientoRepository.findById(idAvistamiento)
            .orElseThrow(() -> new IllegalArgumentException("Avistamiento no encontrado"));
        if (avistamiento.getReporte() != null) {
            throw new IllegalArgumentException("Este avistamiento ya esta asociado a un reporte");
        }

        ReporteExtravio reporte = reporteRepository.findById(request.getIdReporte())
            .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        if (!reporte.getMascota().getUsuario().getIdUsuario().equals((Integer) idUsuario)) {
            throw new IllegalArgumentException("El reporte seleccionado no pertenece al usuario activo");
        }
        if (!"ACTIVO".equalsIgnoreCase(reporte.getEstatus().getNombre())) {
            throw new IllegalArgumentException("Solo puedes asociar avistamientos a reportes activos");
        }

        avistamiento.setReporte(reporte);
        Avistamiento guardado = avistamientoRepository.save(avistamiento);
        notificacionService.crearPorAvistamiento(guardado);
        bitacoraService.registrar((Integer) idUsuario, null, "RECLAMAR_AVISTAMIENTO", "AVISTAMIENTOS", "EXITOSO",
            "Avistamiento " + guardado.getFolioAvistamiento() + " asociado a reporte " + reporte.getFolio(),
            servletRequest);
        return new AvistamientoResponse(guardado, "Avistamiento asociado correctamente al historial del reporte", obtenerFotoMascota(guardado));
    }

    @Transactional
    public AvistamientoResponse validarPorDueno(Integer idAvistamiento, AvistamientoValidacionRequest request,
            HttpSession session, HttpServletRequest servletRequest) {
        Object idUsuario = session.getAttribute("idUsuario");
        if (idUsuario == null) {
            throw new IllegalArgumentException("Inicia sesion para continuar");
        }

        // La validacion queda reservada al dueño del reporte, no a un id recibido desde el navegador.
        Avistamiento avistamiento = avistamientoRepository
            .findByIdAvistamientoAndReporteMascotaUsuarioIdUsuario(idAvistamiento, (Integer) idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("Avistamiento no encontrado"));

        String decision = request.getDecision().trim().toLowerCase(Locale.ROOT);
        if (!decision.equals("si") && !decision.equals("no")) {
            throw new IllegalArgumentException("Selecciona una decision valida");
        }

        String estatusNombre = decision.equals("si") ? "VALIDADO" : "DESCARTADO";
        CatalogoEstatus estatus = estatusRepository.findByNombre(estatusNombre)
            .orElseThrow(() -> new IllegalStateException("No existe el estatus " + estatusNombre));

        avistamiento.setEstatus(estatus);
        avistamiento.setValidadoDueno(decision.equals("si"));
        avistamiento.setFechaValidacion(LocalDateTime.now());
        avistamiento.setComentarioValidacion(limpiarOpcional(request.getComentario()));

        Avistamiento guardado = avistamientoRepository.save(avistamiento);
        bitacoraService.registrar((Integer) idUsuario, null, "VALIDAR_AVISTAMIENTO", "AVISTAMIENTOS", "EXITOSO",
            "Decision del dueño sobre avistamiento " + guardado.getFolioAvistamiento() + ": " + estatusNombre,
            servletRequest);
        return new AvistamientoResponse(guardado, "Validacion registrada correctamente", obtenerFotoMascota(guardado));
    }

    @Transactional
    public AvistamientoResponse registrar(AvistamientoRequest request, MultipartFile fotoAvistamiento) {
        // Los avistamientos son publicos, por eso siempre pasan por CAPTCHA antes de guardar.
        turnstileService.validarToken(request.getTurnstileToken());

        validarFotoObligatoria(fotoAvistamiento);
        validarFechaPermitida(request.getFecha(), "La fecha del avistamiento debe estar dentro de los ultimos 15 dias y no puede ser futura");

        CatalogoEstatus estatus = estatusRepository.findByNombre("PENDIENTE")
            .orElseThrow(() -> new IllegalStateException("No existe el estatus PENDIENTE"));

        ReporteExtravio reporte = null;
        if (request.getFolio() != null && !request.getFolio().isBlank()) {
            reporte = reporteRepository.findByFolioAndActivoTrue(request.getFolio().trim())
                .orElseThrow(() -> new IllegalArgumentException("No se encontro un reporte activo con ese folio"));
            String estatusReporte = reporte.getEstatus().getNombre();
            if (!"ACTIVO".equalsIgnoreCase(estatusReporte)) {
                throw new IllegalArgumentException("El reporte ya fue cerrado o recuperado y no recibe nuevos avistamientos");
            }
        }

        boolean estaResguardado = "si".equalsIgnoreCase(request.getResguardado());
        validarContactoResguardante(estaResguardado, request);
        CatalogoColonia colonia = obtenerColonia(request.getIdColonia());
        validarCoherenciaUbicacion(colonia, request.getIdMunicipio(), request.getIdEstado());

        Avistamiento avistamiento = new Avistamiento();
        avistamiento.setReporte(reporte);
        avistamiento.setEstatus(estatus);
        avistamiento.setFechaAvistamiento(request.getFecha());
        avistamiento.setColonia(colonia);
        avistamiento.setCalle(limpiarOpcional(request.getCalle()));
        avistamiento.setNumero(limpiarOpcional(request.getNumero()));
        avistamiento.setReferencias(limpiar(request.getReferencias()));
        // La descripcion contiene senas visibles del perrito y alimenta la busqueda publica sin folio.
        avistamiento.setDescripcion(limpiar(request.getDescripcion()));
        avistamiento.setResguardado(estaResguardado);
        avistamiento.setNombreResguardante(limpiarOpcional(request.getNombreResguardante()));
        avistamiento.setCorreoResguardante(limpiarOpcional(request.getCorreoResguardante()));
        avistamiento.setTelefonoResguardante(limpiarOpcional(request.getTelefonoResguardante()));

        Avistamiento guardado = avistamientoRepository.save(avistamiento);
        asignarFolioAvistamiento(guardado);
        guardarFotoEnBase(guardado, fotoAvistamiento);
        guardado.setFotoAvistamiento("/api/publico/avistamientos/" + guardado.getIdAvistamiento() + "/foto");
        guardado = avistamientoRepository.save(guardado);
        notificacionService.crearPorAvistamiento(guardado);
        return new AvistamientoResponse(guardado, "Avistamiento registrado correctamente");
    }


    @Transactional(readOnly = true)
    public FotoAvistamientoData obtenerFotoAvistamiento(Integer idAvistamiento) {
        Avistamiento avistamiento = avistamientoRepository.findById(idAvistamiento)
            .filter(a -> Boolean.TRUE.equals(a.getActivo()))
            .orElseThrow(() -> new IllegalArgumentException("Fotografia no encontrada"));

        if (avistamiento.getFotoContenido() == null || avistamiento.getFotoContenido().length == 0) {
            throw new IllegalArgumentException("Fotografia no encontrada");
        }

        String tipo = avistamiento.getFotoTipoContenido() != null ? avistamiento.getFotoTipoContenido() : "image/jpeg";
        String nombre = avistamiento.getFotoNombreArchivo() != null ? avistamiento.getFotoNombreArchivo() : "avistamiento.jpg";
        return new FotoAvistamientoData(avistamiento.getFotoContenido(), tipo, nombre);
    }

    public record FotoAvistamientoData(byte[] contenido, String tipoContenido, String nombreArchivo) {}


    private String obtenerFotoMascota(Avistamiento avistamiento) {
        if (avistamiento.getReporte() == null || avistamiento.getReporte().getMascota() == null) {
            return null;
        }
        Integer idMascota = avistamiento.getReporte().getMascota().getIdMascota();
        return fotografiaRepository
            .findFirstByMascotaIdMascotaAndEsPrincipalTrueAndActivoTrueOrderByFechaRegistroDesc(idMascota)
            .map(foto -> normalizarRutaFoto(foto.getRutaArchivo()))
            .orElse(null);
    }

    private String normalizarRutaFoto(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        String limpia = ruta.replace("\\", "/");
        int indiceUploads = limpia.indexOf("/uploads/");
        if (indiceUploads >= 0) {
            return limpia.substring(indiceUploads);
        }
        if (limpia.startsWith("uploads/")) {
            return "/" + limpia;
        }
        return limpia;
    }

    private void validarFotoObligatoria(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("Agrega una fotografia clara del perrito para registrar el avistamiento");
        }
        validarFoto(foto);
    }

    private void guardarFotoEnBase(Avistamiento avistamiento, MultipartFile foto) {
        try {
            String extension = obtenerExtension(foto.getOriginalFilename());
            avistamiento.setFotoContenido(foto.getBytes());
            avistamiento.setFotoTipoContenido(foto.getContentType());
            avistamiento.setFotoNombreArchivo("avistamiento-" + avistamiento.getIdAvistamiento() + extension);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la fotografia del avistamiento");
        }
    }

    private void validarFoto(MultipartFile foto) {
        if (foto.getSize() > MAX_FOTO_BYTES) {
            throw new IllegalArgumentException("La fotografia no debe superar 5 MB");
        }
        String contentType = foto.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Solo se permiten fotografias JPG, PNG o WEBP");
        }
    }

    private CatalogoColonia obtenerColonia(String id) {
        return coloniaRepository.findById(parsearId(id, "colonia"))
            .filter(catalogo -> Boolean.TRUE.equals(catalogo.getActivo()))
            .orElseThrow(() -> new IllegalArgumentException("Selecciona una colonia valida"));
    }

    private void validarCoherenciaUbicacion(CatalogoColonia colonia, String idMunicipioValor, String idEstadoValor) {
        // El avistamiento guarda solo id_colonia; la zona completa se resuelve desde catalogos.
        Integer idMunicipio = parsearId(idMunicipioValor, "municipio");
        Integer idEstado = parsearId(idEstadoValor, "estado");
        if (!colonia.getMunicipio().getIdMunicipio().equals(idMunicipio)
                || !colonia.getMunicipio().getEstado().getIdEstado().equals(idEstado)) {
            throw new IllegalArgumentException("La colonia no corresponde al estado y municipio seleccionados");
        }
    }

    private Integer parsearId(String valor, String campo) {
        String limpio = limpiar(valor);
        try {
            return Integer.valueOf(limpio);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Selecciona un valor valido para " + campo);
        }
    }

    private void asignarFolioAvistamiento(Avistamiento avistamiento) {
        if (avistamiento.getFolioAvistamiento() != null && !avistamiento.getFolioAvistamiento().isBlank()) {
            return;
        }
        int anio = avistamiento.getFechaRegistro() != null ? avistamiento.getFechaRegistro().getYear() : LocalDate.now().getYear();
        avistamiento.setFolioAvistamiento("AV-" + anio + "-" + String.format("%06d", avistamiento.getIdAvistamiento()));
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return ".jpg";
        }
        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case ".jpeg", ".jpg", ".png", ".webp" -> extension;
            default -> ".jpg";
        };
    }

    private void validarContactoResguardante(boolean resguardado, AvistamientoRequest request) {
        // Si alguien resguarda al perrito, el dueño necesita datos verificables para contactarlo.
        String nombre = limpiarOpcional(request.getNombreResguardante());
        String correo = limpiarOpcional(request.getCorreoResguardante());
        String telefono = limpiarOpcional(request.getTelefonoResguardante());

        if (resguardado && (nombre == null || correo == null || telefono == null)) {
            throw new IllegalArgumentException("Si el perrito esta en resguardo, captura nombre, correo y telefono de contacto");
        }
        if (correo != null && !correo.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{3,}$")) {
            throw new IllegalArgumentException("Captura un correo de contacto valido. Ejemplo: nombre@dominio.com");
        }
        if (telefono != null && !telefono.matches("^\\d{10}$")) {
            throw new IllegalArgumentException("El telefono debe contener 10 digitos numericos");
        }
    }

    private void validarFechaPermitida(LocalDate fecha, String mensaje) {
        LocalDate hoy = LocalDate.now();
        if (fecha == null || fecha.isBefore(hoy.minusDays(15)) || fecha.isAfter(hoy)) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private String limpiar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Completa los campos obligatorios");
        }
        return valor.trim();
    }

    private String limpiarOpcional(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }
}
