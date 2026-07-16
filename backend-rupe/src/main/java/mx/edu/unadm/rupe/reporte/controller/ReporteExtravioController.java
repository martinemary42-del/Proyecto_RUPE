package mx.edu.unadm.rupe.reporte.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import mx.edu.unadm.rupe.reporte.dto.ReporteExtravioRequest;
import mx.edu.unadm.rupe.reporte.dto.ReporteExtravioResponse;
import mx.edu.unadm.rupe.reporte.service.ReporteExtravioService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteExtravioController {

    private final ReporteExtravioService reporteService;

    public ReporteExtravioController(ReporteExtravioService reporteService) {
        this.reporteService = reporteService;
    }


    @GetMapping("/mis")
    public List<ReporteExtravioResponse> consultarMisReportes(HttpSession session) {
        return reporteService.consultarMisReportes(session);
    }


    @GetMapping("/{idReporte}")
    public ReporteExtravioResponse consultarDetalle(@PathVariable Integer idReporte, HttpSession session) {
        return reporteService.consultarDetalle(idReporte, session);
    }


    @GetMapping(value = "/{idReporte}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generarQr(@PathVariable Integer idReporte, HttpSession session,
            HttpServletRequest request) {
        return ResponseEntity.ok(reporteService.generarQr(idReporte, session, request));
    }

    @PutMapping("/{idReporte}/renovar")
    public ReporteExtravioResponse renovarBusqueda(@PathVariable Integer idReporte, HttpSession session,
            HttpServletRequest request) {
        return reporteService.renovarBusqueda(idReporte, session, request);
    }

    @PutMapping("/{idReporte}/recuperar")
    public ReporteExtravioResponse marcarRecuperado(@PathVariable Integer idReporte, HttpSession session,
            HttpServletRequest request) {
        return reporteService.marcarRecuperado(idReporte, session, request);
    }

    @GetMapping(value = "/{idReporte}/cartel", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarCartel(@PathVariable Integer idReporte, HttpSession session,
            HttpServletRequest request) {
        byte[] pdf = reporteService.generarCartelPdf(idReporte, session, request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("cartel-rupe-" + idReporte + ".pdf").build().toString())
            .body(pdf);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReporteExtravioResponse registrar(@Valid @ModelAttribute ReporteExtravioRequest request, HttpSession session,
            HttpServletRequest httpRequest) {
        return reporteService.registrar(request, session, httpRequest);
    }
}
