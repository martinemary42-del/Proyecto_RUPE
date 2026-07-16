package mx.edu.unadm.rupe.reporte.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstatus;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstatusRepository;
import mx.edu.unadm.rupe.mascota.model.Mascota;
import mx.edu.unadm.rupe.mascota.repository.MascotaRepository;
import mx.edu.unadm.rupe.mascota.repository.FotografiaRepository;
import mx.edu.unadm.rupe.reporte.dto.ReporteExtravioRequest;
import mx.edu.unadm.rupe.reporte.dto.ReporteExtravioResponse;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import mx.edu.unadm.rupe.reporte.repository.ReporteExtravioRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteExtravioService {

    private final ReporteExtravioRepository reporteRepository;
    private final MascotaRepository mascotaRepository;
    private final CatalogoEstatusRepository estatusRepository;
    private final CatalogoColoniaRepository coloniaRepository;
    private final FotografiaRepository fotografiaRepository;
    private final BitacoraSeguridadService bitacoraService;

    @Value("${rupe.frontend.public-base-url:http://localhost:5500}")
    private String publicBaseUrl;

    public ReporteExtravioService(ReporteExtravioRepository reporteRepository, MascotaRepository mascotaRepository,
            CatalogoEstatusRepository estatusRepository, FotografiaRepository fotografiaRepository,
            BitacoraSeguridadService bitacoraService, CatalogoColoniaRepository coloniaRepository) {
        this.reporteRepository = reporteRepository;
        this.mascotaRepository = mascotaRepository;
        this.estatusRepository = estatusRepository;
        this.coloniaRepository = coloniaRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.bitacoraService = bitacoraService;
    }

    @Transactional
    public ReporteExtravioResponse registrar(ReporteExtravioRequest request, HttpSession session,
            HttpServletRequest servletRequest) {
        Integer idUsuario = obtenerIdUsuario(session);
        Mascota mascota = mascotaRepository.findById(request.getIdMascota())
            .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        if (!mascota.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new IllegalArgumentException("La mascota no pertenece al usuario activo");
        }

        validarFechaPermitida(request.getFechaExtravio(), "La fecha de extravio debe estar dentro de los ultimos 15 dias y no puede ser futura");

        // Una mascota solo puede tener un reporte activo, sin importar si es extravio o robo.
        reporteRepository.findFirstByMascotaIdMascotaAndActivoTrueAndEstatusNombreIgnoreCaseOrderByFechaRegistroDesc(
                mascota.getIdMascota(), "ACTIVO")
            .ifPresent(reporteActivo -> {
                throw new IllegalArgumentException("Ya existe un reporte activo para esta mascota con folio " + reporteActivo.getFolio() + ". Cierra o marca como recuperado el reporte actual antes de generar otro por extravio o robo.");
            });

        CatalogoEstatus estatus = estatusRepository.findByNombre("ACTIVO")
            .orElseThrow(() -> new IllegalStateException("No existe el estatus ACTIVO"));
        CatalogoColonia colonia = obtenerColonia(request.getIdColonia());
        validarCoherenciaUbicacion(colonia, request.getIdMunicipio(), request.getIdEstado());

        ReporteExtravio reporte = new ReporteExtravio();
        reporte.setMascota(mascota);
        reporte.setEstatus(estatus);
        reporte.setFolio(generarFolio());
        reporte.setQrUrl("/reportar-avistamiento.html?folio=" + reporte.getFolio());
        reporte.setFechaExtravio(request.getFechaExtravio());
        reporte.setTipoReporte(resolverTipoReporte(request.getTipoReporte()));
        reporte.setColonia(colonia);
        reporte.setCalle(limpiarOpcional(request.getCalle()));
        reporte.setNumero(limpiarOpcional(request.getNumero()));
        reporte.setReferencias(limpiar(request.getReferencias()));
        reporte.setDescripcionHechos(limpiar(request.getDescripcion()));
        reporte.setFechaVencimiento(LocalDate.now().plusDays(30));
        reporte.setFechaUltimaRenovacion(LocalDateTime.now());
        reporte.setRenovaciones(0);
        reporte.setRequiereRenovacion(false);

        ReporteExtravio guardado = reporteRepository.save(reporte);
        bitacoraService.registrar(idUsuario, null, "CREAR_REPORTE", "REPORTES", "EXITOSO",
            "Reporte " + guardado.getFolio() + " creado como " + guardado.getTipoReporte(), servletRequest);
        return construirRespuesta(guardado, "Reporte de extravio registrado correctamente");
    }


    @Transactional(readOnly = true)
    public List<ReporteExtravioResponse> consultarMisReportes(HttpSession session) {
        Integer idUsuario = obtenerIdUsuario(session);
        return reporteRepository.findByMascotaUsuarioIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(idUsuario)
            .stream()
            .map(reporte -> construirRespuesta(reporte, "Consulta correcta"))
            .toList();
    }


    @Transactional(readOnly = true)
    public ReporteExtravioResponse consultarDetalle(Integer idReporte, HttpSession session) {
        Integer idUsuario = obtenerIdUsuario(session);
        ReporteExtravio reporte = reporteRepository.findById(idReporte)
            .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        if (!reporte.getMascota().getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new IllegalArgumentException("El reporte no pertenece al usuario activo");
        }
        return construirRespuesta(reporte, "Consulta correcta");
    }

    @Transactional
    public ReporteExtravioResponse renovarBusqueda(Integer idReporte, HttpSession session,
            HttpServletRequest servletRequest) {
        ReporteExtravio reporte = obtenerReporteDelDueno(idReporte, session);
        String estatusActual = reporte.getEstatus().getNombre();
        if (!"ACTIVO".equalsIgnoreCase(estatusActual)) {
            throw new IllegalArgumentException("Solo se pueden renovar reportes activos.");
        }

        // Renovar extiende la busqueda 30 dias y limpia la marca de publicacion vencida.
        reporte.setFechaVencimiento(LocalDate.now().plusDays(30));
        reporte.setFechaUltimaRenovacion(LocalDateTime.now());
        reporte.setRenovaciones((reporte.getRenovaciones() == null ? 0 : reporte.getRenovaciones()) + 1);
        reporte.setRequiereRenovacion(false);
        reporte.setFechaActualizacion(LocalDateTime.now());

        ReporteExtravio guardado = reporteRepository.save(reporte);
        bitacoraService.registrar(obtenerIdUsuario(session), null, "RENOVAR_REPORTE", "REPORTES", "EXITOSO",
            "Renovacion del reporte " + guardado.getFolio(), servletRequest);
        return construirRespuesta(guardado, "Busqueda renovada por 30 dias mas.");
    }

    @Transactional
    public ReporteExtravioResponse marcarRecuperado(Integer idReporte, HttpSession session,
            HttpServletRequest servletRequest) {
        ReporteExtravio reporte = obtenerReporteDelDueno(idReporte, session);
        String estatusActual = reporte.getEstatus().getNombre();
        if ("RECUPERADO".equalsIgnoreCase(estatusActual) || "CERRADO".equalsIgnoreCase(estatusActual)) {
            return construirRespuesta(reporte, "El reporte ya estaba cerrado o recuperado");
        }

        CatalogoEstatus recuperado = estatusRepository.findByNombre("RECUPERADO")
            .orElseThrow(() -> new IllegalStateException("No existe el estatus RECUPERADO"));
        reporte.setEstatus(recuperado);
        reporte.setFechaActualizacion(LocalDateTime.now());

        ReporteExtravio guardado = reporteRepository.save(reporte);
        bitacoraService.registrar(obtenerIdUsuario(session), null, "MARCAR_RECUPERADO", "REPORTES", "EXITOSO",
            "Cierre por recuperacion del reporte " + guardado.getFolio(), servletRequest);
        return construirRespuesta(guardado, "Busqueda cerrada correctamente. El perrito fue marcado como recuperado.");
    }

    private ReporteExtravioResponse construirRespuesta(ReporteExtravio reporte, String mensaje) {
        return new ReporteExtravioResponse(reporte, mensaje, obtenerFotoPrincipalUrl(reporte.getMascota()));
    }


    @Transactional
    public byte[] generarQr(Integer idReporte, HttpSession session, HttpServletRequest servletRequest) {
        ReporteExtravio reporte = obtenerReporteDelDueno(idReporte, session);
        try {
            // El QR apunta al formulario publico de avistamiento; no expone datos personales del dueño.
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(urlAvistamiento(reporte), BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            bitacoraService.registrar(obtenerIdUsuario(session), null, "GENERAR_QR", "REPORTES", "EXITOSO",
                "Generacion de QR para reporte " + reporte.getFolio(), servletRequest);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el codigo QR");
        }
    }

    @Transactional
    public byte[] generarCartelPdf(Integer idReporte, HttpSession session, HttpServletRequest servletRequest) {
        ReporteExtravio reporte = obtenerReporteDelDueno(idReporte, session);
        if (!"ACTIVO".equalsIgnoreCase(reporte.getEstatus().getNombre())) {
            throw new IllegalArgumentException("El cartel solo puede descargarse mientras el reporte esta activo. Este reporte ya fue cerrado o recuperado.");
        }
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // El cartel publica solo datos utiles para busqueda; la informacion privada del propietario se omite.
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 48;
                float y = 720;

                content.setNonStrokingColor(new Color(97, 18, 50));
                content.addRect(margin, y, 516, 46);
                content.fill();
                escribirTexto(content, PDType1Font.HELVETICA_BOLD, 24, Color.WHITE, "SE BUSCA", 250, y + 17);
                y -= 34;
                escribirTexto(content, PDType1Font.HELVETICA, 10, Color.DARK_GRAY,
                    "Registro Unico de Perritos Extraviados - RUPE", 190, y);
                y -= 24;

                String fotoUrl = obtenerFotoPrincipalUrl(reporte.getMascota());
                if (fotoUrl != null) {
                    Path fotoPath = Paths.get("uploads", "mascotas", fotoUrl.substring(fotoUrl.lastIndexOf("/") + 1))
                        .toAbsolutePath().normalize();
                    if (Files.exists(fotoPath)) {
                        PDImageXObject image = PDImageXObject.createFromFileByExtension(fotoPath.toFile(), document);
                        content.drawImage(image, margin + 56, y - 260, 400, 250);
                    }
                }
                y -= 292;

                escribirTexto(content, PDType1Font.HELVETICA_BOLD, 22, new Color(97, 18, 50),
                    reporte.getMascota().getNombre(), margin, y);
                y -= 28;
                escribirTexto(content, PDType1Font.HELVETICA_BOLD, 13, Color.BLACK,
                    "Folio: " + reporte.getFolio(), margin, y);
                y -= 24;

                escribirTexto(content, PDType1Font.HELVETICA, 12, Color.BLACK,
                    "Tipo de reporte: " + etiquetaTipoReporte(reporte.getTipoReporte()) + " | Fecha: " + reporte.getFechaExtravio(), margin, y);
                y -= 18;
                escribirTexto(content, PDType1Font.HELVETICA, 12, Color.BLACK,
                    "Zona: " + reporte.getColonia() + ", " + reporte.getMunicipio() + ", " + reporte.getEstado(), margin, y);
                y -= 18;
                escribirTexto(content, PDType1Font.HELVETICA, 12, Color.BLACK,
                    "Raza: " + reporte.getMascota().getRaza() + " | Color: " + reporte.getMascota().getColor(), margin, y);
                y -= 28;

                escribirTexto(content, PDType1Font.HELVETICA_BOLD, 12, Color.BLACK, "Referencias:", margin, y);
                y -= 16;
                escribirLineas(content, reporte.getReferencias(), margin, y, 90);
                y -= 70;

                escribirTexto(content, PDType1Font.HELVETICA_BOLD, 12, Color.BLACK, "Descripcion del reporte:", margin, y);
                y -= 16;
                escribirLineas(content, reporte.getDescripcionHechos(), margin, y, 72);

                byte[] qrBytes = generarQr(idReporte, session, servletRequest);
                PDImageXObject qr = LosslessFactory.createFromImage(document, javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(qrBytes)));
                content.drawImage(qr, 380, 122, 155, 155);
                escribirTexto(content, PDType1Font.HELVETICA_BOLD, 9, new Color(97, 18, 50),
                    "Escanea para reportar avistamiento", 370, 108);

                content.setNonStrokingColor(new Color(255, 248, 230));
                content.addRect(margin, 48, 516, 48);
                content.fill();
                escribirTexto(content, PDType1Font.HELVETICA_BOLD, 10, new Color(97, 18, 50),
                    "Aviso antifraude: RUPE es gratuito. No se solicitan recompensas, pagos ni depositos.", margin + 12, 74);
                escribirTexto(content, PDType1Font.HELVETICA, 9, Color.DARK_GRAY,
                    "Los datos personales del propietario no se publican en este cartel.", margin + 12, 60);
            }

            document.save(output);
            bitacoraService.registrar(obtenerIdUsuario(session), null, "DESCARGAR_CARTEL", "REPORTES", "EXITOSO",
                "Descarga de cartel PDF del reporte " + reporte.getFolio(), servletRequest);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el cartel PDF");
        }
    }

    private ReporteExtravio obtenerReporteDelDueno(Integer idReporte, HttpSession session) {
        // Control de acceso centralizado: cada accion sensible confirma que el reporte pertenece al usuario activo.
        Integer idUsuario = obtenerIdUsuario(session);
        ReporteExtravio reporte = reporteRepository.findById(idReporte)
            .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        if (!reporte.getMascota().getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new IllegalArgumentException("El reporte no pertenece al usuario activo");
        }
        return reporte;
    }

    private String urlAvistamiento(ReporteExtravio reporte) {
        // La URL publica se parametriza para que el QR funcione tanto localmente como en alojamiento.
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/reportar-avistamiento.html?folio=" + reporte.getFolio();
    }

    private String obtenerFotoPrincipalUrl(Mascota mascota) {
        return fotografiaRepository
            .findFirstByMascotaIdMascotaAndEsPrincipalTrueAndActivoTrueOrderByFechaRegistroDesc(
                mascota.getIdMascota())
            .map(foto -> "/uploads/mascotas/" + foto.getNombreArchivo())
            .orElse(null);
    }

    private CatalogoColonia obtenerColonia(String id) {
        return coloniaRepository.findById(parsearId(id, "colonia"))
            .filter(catalogo -> Boolean.TRUE.equals(catalogo.getActivo()))
            .orElseThrow(() -> new IllegalArgumentException("Selecciona una colonia valida"));
    }

    private void validarCoherenciaUbicacion(CatalogoColonia colonia, String idMunicipioValor, String idEstadoValor) {
        // El reporte solo guarda id_colonia; municipio, estado y codigo postal se obtienen por relacion.
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

    private void escribirTexto(PDPageContentStream content, PDType1Font font, int size, Color color,
            String text, float x, float y) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(sanitizarPdf(text));
        content.endText();
    }

    private void escribirLineas(PDPageContentStream content, String text, float x, float y, int maxChars)
            throws IOException {
        String valor = sanitizarPdf(text);
        int inicio = 0;
        int linea = 0;
        while (inicio < valor.length() && linea < 4) {
            int fin = Math.min(inicio + maxChars, valor.length());
            escribirTexto(content, PDType1Font.HELVETICA, 10, Color.DARK_GRAY, valor.substring(inicio, fin), x, y - (linea * 14));
            inicio = fin;
            linea++;
        }
    }

    private String sanitizarPdf(String text) {
        if (text == null || text.isBlank()) {
            return "Sin dato";
        }
        return text.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
            .replace("ñ", "n").replace("Ñ", "N");
    }

    private Integer obtenerIdUsuario(HttpSession session) {
        Object idUsuario = session.getAttribute("idUsuario");
        if (!(idUsuario instanceof Integer)) {
            throw new IllegalArgumentException("Inicia sesion para continuar");
        }
        return (Integer) idUsuario;
    }

    private String generarFolio() {
        // Folio legible para reportes y cartel: RUPE-año-consecutivo.
        int anio = LocalDate.now().getYear();
        String prefijo = "RUPE-" + anio + "-";
        long consecutivo = reporteRepository.countByFolioStartingWith(prefijo) + 1;
        return prefijo + String.format("%06d", consecutivo);
    }

    private void validarFechaPermitida(LocalDate fecha, String mensaje) {
        LocalDate hoy = LocalDate.now();
        if (fecha == null || fecha.isBefore(hoy.minusDays(15)) || fecha.isAfter(hoy)) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private String resolverTipoReporte(String tipo) {
        String normalizado = tipo == null || tipo.isBlank() ? "EXTRAVIO" : tipo.trim().toUpperCase();
        if (!normalizado.equals("EXTRAVIO") && !normalizado.equals("ROBO")) {
            throw new IllegalArgumentException("Selecciona un tipo de reporte valido.");
        }
        return normalizado;
    }

    private String etiquetaTipoReporte(String tipo) {
        return "ROBO".equalsIgnoreCase(tipo) ? "Robo de mascota" : "Extravío";
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


