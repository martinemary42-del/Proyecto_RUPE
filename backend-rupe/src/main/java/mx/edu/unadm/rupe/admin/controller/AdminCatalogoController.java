package mx.edu.unadm.rupe.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import mx.edu.unadm.rupe.admin.dto.AdminCatalogoRequest;
import mx.edu.unadm.rupe.admin.dto.AdminCatalogoResponse;
import mx.edu.unadm.rupe.admin.service.AdminAuditoriaExcelService;
import mx.edu.unadm.rupe.admin.service.AdminCatalogoService;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/catalogos")
public class AdminCatalogoController {
    private final AdminCatalogoService service;
    private final AdminAuditoriaExcelService excelService;
    private final BitacoraSeguridadService bitacoraService;

    public AdminCatalogoController(AdminCatalogoService service, AdminAuditoriaExcelService excelService,
            BitacoraSeguridadService bitacoraService) {
        this.service = service;
        this.excelService = excelService;
        this.bitacoraService = bitacoraService;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportarExcel(HttpSession session, HttpServletRequest request) {
        byte[] archivo = excelService.generarCatalogos(session);
        bitacoraService.registrar((Integer) session.getAttribute("idUsuario"), null,
            "EXPORTAR_EXCEL", "ADMIN_CATALOGOS", "EXITOSO",
            "Exportacion de catalogos en Excel", request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=catalogos-rupe.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(archivo);
    }

    @GetMapping("/{catalogo}")
    public List<AdminCatalogoResponse> listar(@PathVariable String catalogo, HttpSession session) {
        return service.listar(catalogo, session);
    }

    @PostMapping("/{catalogo}")
    public AdminCatalogoResponse crear(@PathVariable String catalogo, @RequestBody AdminCatalogoRequest request,
            HttpSession session, HttpServletRequest httpRequest) {
        return service.crear(catalogo, request, session, httpRequest);
    }

    @PutMapping("/{catalogo}/{id}/estatus")
    public AdminCatalogoResponse cambiarEstatus(@PathVariable String catalogo, @PathVariable Integer id,
            @RequestParam boolean activo, HttpSession session, HttpServletRequest httpRequest) {
        return service.cambiarEstatus(catalogo, id, activo, session, httpRequest);
    }
}
