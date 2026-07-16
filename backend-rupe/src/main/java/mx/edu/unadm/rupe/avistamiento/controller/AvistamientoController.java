package mx.edu.unadm.rupe.avistamiento.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoReclamoRequest;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoRequest;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoResponse;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoValidacionRequest;
import mx.edu.unadm.rupe.avistamiento.service.AvistamientoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/avistamientos")
public class AvistamientoController {

    private final AvistamientoService avistamientoService;

    public AvistamientoController(AvistamientoService avistamientoService) {
        this.avistamientoService = avistamientoService;
    }

    @GetMapping("/mis")
    public List<AvistamientoResponse> consultarMisAvistamientos(HttpSession session) {
        return avistamientoService.consultarMisAvistamientos(session);
    }


    @PutMapping("/{idAvistamiento}/validacion")
    public AvistamientoResponse validarPorDueno(@PathVariable Integer idAvistamiento,
            @Valid @RequestBody AvistamientoValidacionRequest request, HttpSession session,
            HttpServletRequest httpRequest) {
        return avistamientoService.validarPorDueno(idAvistamiento, request, session, httpRequest);
    }

    @PutMapping("/{idAvistamiento}/reclamar")
    public AvistamientoResponse reclamarAvistamiento(@PathVariable Integer idAvistamiento,
            @Valid @RequestBody AvistamientoReclamoRequest request, HttpSession session,
            HttpServletRequest httpRequest) {
        return avistamientoService.reclamarAvistamiento(idAvistamiento, request, session, httpRequest);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvistamientoResponse registrar(@Valid @ModelAttribute AvistamientoRequest request,
            @RequestPart(value = "foto_avistamiento", required = false) MultipartFile fotoAvistamiento) {
        return avistamientoService.registrar(request, fotoAvistamiento);
    }
}
