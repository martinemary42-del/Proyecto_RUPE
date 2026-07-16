package mx.edu.unadm.rupe.publico.controller;

import jakarta.validation.Valid;
import java.util.List;
import mx.edu.unadm.rupe.publico.dto.PublicoAvistamientoResponse;
import mx.edu.unadm.rupe.publico.dto.PublicoPaginaResponse;
import mx.edu.unadm.rupe.publico.dto.PublicoReporteRecienteResponse;
import mx.edu.unadm.rupe.publico.dto.PublicoResumenResponse;
import mx.edu.unadm.rupe.publico.dto.VisitaRequest;
import mx.edu.unadm.rupe.publico.service.PublicoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final PublicoService publicoService;

    public PublicoController(PublicoService publicoService) {
        this.publicoService = publicoService;
    }

    @GetMapping("/resumen")
    public PublicoResumenResponse resumen() {
        return publicoService.resumen();
    }

    @PostMapping("/visita")
    public void registrarVisita(@Valid @RequestBody VisitaRequest request) {
        publicoService.registrarVisita(request.getPagina());
    }

    @GetMapping("/reportes-recientes")
    public List<PublicoReporteRecienteResponse> reportesRecientes() {
        return publicoService.reportesRecientes();
    }

    @GetMapping("/reportes-activos")
    public List<PublicoReporteRecienteResponse> reportesActivos() {
        return publicoService.reportesActivos();
    }

    @GetMapping("/reportes-activos-paginados")
    public PublicoPaginaResponse<PublicoReporteRecienteResponse> reportesActivosPaginados(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio,
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String raza,
            @RequestParam(required = false) String senas,
            @RequestParam(required = false) String sexo,
            @RequestParam(required = false) String zona) {
        String busqueda = texto != null ? texto : folio;
        return publicoService.reportesActivosPaginados(pagina, tamanio, busqueda, raza, senas, sexo, zona);
    }

    @GetMapping("/avistamientos-sin-folio")
    public List<PublicoAvistamientoResponse> avistamientosSinFolio() {
        return publicoService.avistamientosSinFolio();
    }

    @GetMapping("/avistamientos-sin-folio-paginados")
    public PublicoPaginaResponse<PublicoAvistamientoResponse> avistamientosSinFolioPaginados(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String senas,
            @RequestParam(required = false) String zona,
            @RequestParam(required = false) String tipo) {
        return publicoService.avistamientosSinFolioPaginados(pagina, tamanio, texto, senas, zona, tipo);
    }
}
