package mx.edu.unadm.rupe.contacto.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import mx.edu.unadm.rupe.contacto.dto.MensajeContactoRequest;
import mx.edu.unadm.rupe.contacto.dto.MensajeContactoResponse;
import mx.edu.unadm.rupe.contacto.service.MensajeContactoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacto")
public class ContactoController {
    private final MensajeContactoService service;

    public ContactoController(MensajeContactoService service) {
        this.service = service;
    }

    @PostMapping
    public MensajeContactoResponse registrar(@Valid @RequestBody MensajeContactoRequest request,
            HttpServletRequest httpRequest) {
        return service.registrar(request, httpRequest);
    }
}
