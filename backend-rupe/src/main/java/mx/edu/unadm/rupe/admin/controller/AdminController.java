package mx.edu.unadm.rupe.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import mx.edu.unadm.rupe.admin.dto.AdminBitacoraResponse;
import mx.edu.unadm.rupe.admin.dto.AdminCambiarEstatusUsuarioRequest;
import mx.edu.unadm.rupe.admin.dto.AdminCrearUsuarioRequest;
import mx.edu.unadm.rupe.admin.dto.AdminReporteResponse;
import mx.edu.unadm.rupe.admin.dto.AdminResumenResponse;
import mx.edu.unadm.rupe.admin.dto.AdminUsuarioResponse;
import mx.edu.unadm.rupe.admin.service.AdminAuditoriaExcelService;
import mx.edu.unadm.rupe.admin.service.AdminEstadisticasExcelService;
import mx.edu.unadm.rupe.admin.service.AdminService;
import mx.edu.unadm.rupe.contacto.dto.AtenderMensajeContactoRequest;
import mx.edu.unadm.rupe.contacto.dto.MensajeContactoResponse;
import mx.edu.unadm.rupe.contacto.service.MensajeContactoService;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final AdminEstadisticasExcelService excelService;
    private final AdminAuditoriaExcelService auditoriaExcelService;
    private final MensajeContactoService mensajeContactoService;
    private final BitacoraSeguridadService bitacoraService;

    public AdminController(AdminService adminService, AdminEstadisticasExcelService excelService,
            AdminAuditoriaExcelService auditoriaExcelService, MensajeContactoService mensajeContactoService,
            BitacoraSeguridadService bitacoraService) {
        this.adminService = adminService;
        this.excelService = excelService;
        this.auditoriaExcelService = auditoriaExcelService;
        this.mensajeContactoService = mensajeContactoService;
        this.bitacoraService = bitacoraService;
    }

    @GetMapping("/resumen")
    public AdminResumenResponse resumen(HttpSession session) {
        return adminService.resumen(session);
    }

    @GetMapping("/reportes")
    public List<AdminReporteResponse> reportes(HttpSession session) {
        return adminService.reportes(session);
    }

    @GetMapping("/usuarios")
    public List<AdminUsuarioResponse> usuarios(HttpSession session) {
        return adminService.usuarios(session);
    }

    @GetMapping("/bitacora")
    public List<AdminBitacoraResponse> bitacora(HttpSession session) {
        return adminService.bitacora(session);
    }

    @GetMapping("/bitacora/excel")
    public ResponseEntity<byte[]> exportarBitacoraExcel(HttpSession session, HttpServletRequest request) {
        byte[] archivo = auditoriaExcelService.generarBitacora(session);
        bitacoraService.registrar((Integer) session.getAttribute("idUsuario"), null,
            "EXPORTAR_EXCEL", "ADMIN_BITACORA", "EXITOSO",
            "Exportacion de bitacora de seguridad en Excel", request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bitacora-seguridad-rupe.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(archivo);
    }

    @GetMapping("/soporte")
    public List<MensajeContactoResponse> soporte(HttpSession session) {
        return mensajeContactoService.listarAdmin(session);
    }

    @GetMapping("/soporte/excel")
    public ResponseEntity<byte[]> exportarSoporteExcel(HttpSession session, HttpServletRequest request) {
        byte[] archivo = auditoriaExcelService.generarSoporte(session);
        bitacoraService.registrar((Integer) session.getAttribute("idUsuario"), null,
            "EXPORTAR_EXCEL", "ADMIN_SOPORTE", "EXITOSO",
            "Exportacion de mensajes de soporte en Excel", request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=soporte-contacto-rupe.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(archivo);
    }

    @PutMapping("/soporte/{idMensajeContacto}/atendido")
    public MensajeContactoResponse marcarSoporteAtendido(@PathVariable Integer idMensajeContacto,
            @RequestBody(required = false) AtenderMensajeContactoRequest body,
            HttpSession session, HttpServletRequest request) {
        return mensajeContactoService.marcarAtendido(idMensajeContacto, body, session, request);
    }

    @GetMapping("/estadisticas/excel")
    public ResponseEntity<byte[]> exportarEstadisticasExcel(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpSession session, HttpServletRequest request) {
        byte[] archivo = excelService.generarExcel(session, texto, estatus, desde, hasta);
        bitacoraService.registrar((Integer) session.getAttribute("idUsuario"), null,
            "EXPORTAR_EXCEL", "ADMIN_ESTADISTICAS", "EXITOSO",
            "Exportacion de estadisticas agregadas en Excel", request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=estadisticas-rupe.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(archivo);
    }

    @PostMapping("/usuarios/admin")
    public AdminUsuarioResponse crearAdministrador(@RequestBody AdminCrearUsuarioRequest request,
            HttpSession session, HttpServletRequest httpRequest) {
        return adminService.crearAdministrador(request, session, httpRequest);
    }

    @PutMapping("/usuarios/{idUsuario}/estatus")
    public AdminUsuarioResponse cambiarEstatus(@PathVariable Integer idUsuario,
            @RequestBody AdminCambiarEstatusUsuarioRequest body,
            HttpSession session, HttpServletRequest request) {
        return adminService.cambiarEstatusUsuario(idUsuario, body, session, request);
    }
}
