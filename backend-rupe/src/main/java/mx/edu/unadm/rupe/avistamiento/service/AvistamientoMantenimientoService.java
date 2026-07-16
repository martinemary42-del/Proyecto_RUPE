package mx.edu.unadm.rupe.avistamiento.service;

import java.time.LocalDateTime;
import mx.edu.unadm.rupe.avistamiento.repository.AvistamientoRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvistamientoMantenimientoService {

    private static final int DIAS_VISIBILIDAD_SIN_RESGUARDO = 45;

    private final AvistamientoRepository avistamientoRepository;
    private final BitacoraSeguridadService bitacoraService;

    public AvistamientoMantenimientoService(AvistamientoRepository avistamientoRepository,
            BitacoraSeguridadService bitacoraService) {
        this.avistamientoRepository = avistamientoRepository;
        this.bitacoraService = bitacoraService;
    }

    @Transactional
    @Scheduled(initialDelay = 60000, fixedDelay = 86400000)
    public void desactivarAvistamientosVencidosSinResguardo() {
        // Limpieza logica: conserva historial en BD, pero retira de consulta publica avisos ciudadanos vencidos.
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(DIAS_VISIBILIDAD_SIN_RESGUARDO);
        int desactivados = avistamientoRepository.desactivarSinFolioVencidosSinResguardo(fechaLimite);
        if (desactivados > 0) {
            bitacoraService.registrar(null, null, "BAJA_LOGICA_AVISTAMIENTOS", "AVISTAMIENTOS", "EXITOSO",
                "Se desactivaron automaticamente " + desactivados
                    + " avistamiento(s) sin folio y sin resguardo por superar 45 dias.",
                null);
        }
    }
}
