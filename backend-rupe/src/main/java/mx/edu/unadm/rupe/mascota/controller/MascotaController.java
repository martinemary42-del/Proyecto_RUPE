package mx.edu.unadm.rupe.mascota.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import mx.edu.unadm.rupe.mascota.dto.MascotaRegistroRequest;
import mx.edu.unadm.rupe.mascota.dto.MascotaResponse;
import mx.edu.unadm.rupe.mascota.service.MascotaService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/mascotas")
public class MascotaController {

    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MascotaResponse registrar(@Valid @ModelAttribute MascotaRegistroRequest request,
            @RequestPart(value = "foto_principal", required = false) MultipartFile fotoPrincipal,
            HttpSession session) {
        return mascotaService.registrar(request, fotoPrincipal, session);
    }


    @PutMapping(value = "/{idMascota}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MascotaResponse actualizar(@PathVariable Integer idMascota,
            @Valid @ModelAttribute MascotaRegistroRequest request,
            @RequestPart(value = "foto_principal", required = false) MultipartFile fotoPrincipal,
            HttpSession session) {
        return mascotaService.actualizar(idMascota, request, fotoPrincipal, session);
    }

    @GetMapping("/{idMascota}")
    public MascotaResponse consultarDetalle(@PathVariable Integer idMascota, HttpSession session) {
        return mascotaService.consultarDetalle(idMascota, session);
    }

    @GetMapping("/mis")
    public List<MascotaResponse> consultarMisMascotas(HttpSession session) {
        return mascotaService.consultarMisMascotas(session);
    }

    @PutMapping("/{idMascota}/desactivar")
    public MascotaResponse desactivar(@PathVariable Integer idMascota, HttpSession session) {
        return mascotaService.desactivar(idMascota, session);
    }
}
