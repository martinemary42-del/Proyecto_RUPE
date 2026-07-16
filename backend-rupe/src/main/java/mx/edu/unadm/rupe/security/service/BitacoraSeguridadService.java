package mx.edu.unadm.rupe.security.service;

import jakarta.servlet.http.HttpServletRequest;
import mx.edu.unadm.rupe.security.model.BitacoraSeguridad;
import mx.edu.unadm.rupe.security.repository.BitacoraSeguridadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BitacoraSeguridadService {
    private final BitacoraSeguridadRepository repository;

    public BitacoraSeguridadService(BitacoraSeguridadRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Integer idUsuario, String correo, String accion, String modulo,
            String resultado, String descripcion, HttpServletRequest request) {
        BitacoraSeguridad bitacora = new BitacoraSeguridad();
        bitacora.setIdUsuario(idUsuario);
        bitacora.setCorreo(correo);
        bitacora.setAccion(accion);
        bitacora.setModulo(modulo);
        bitacora.setResultado(resultado);
        bitacora.setDescripcion(descripcion);
        bitacora.setIp(obtenerIp(request));
        repository.save(bitacora);
    }

    private String obtenerIp(HttpServletRequest request) {
        if (request == null) return "desconocida";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
