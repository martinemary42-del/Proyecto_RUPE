package mx.edu.unadm.rupe.publico.service;

import java.time.LocalDateTime;
import java.util.List;
import mx.edu.unadm.rupe.avistamiento.model.Avistamiento;
import mx.edu.unadm.rupe.avistamiento.repository.AvistamientoRepository;
import mx.edu.unadm.rupe.mascota.model.Mascota;
import mx.edu.unadm.rupe.mascota.repository.FotografiaRepository;
import mx.edu.unadm.rupe.mascota.repository.MascotaRepository;
import mx.edu.unadm.rupe.publico.dto.PublicoAvistamientoResponse;
import mx.edu.unadm.rupe.publico.dto.PublicoPaginaResponse;
import mx.edu.unadm.rupe.publico.dto.PublicoReporteRecienteResponse;
import mx.edu.unadm.rupe.publico.dto.PublicoResumenResponse;
import mx.edu.unadm.rupe.publico.model.VisitaSitio;
import mx.edu.unadm.rupe.publico.repository.VisitaSitioRepository;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import mx.edu.unadm.rupe.reporte.repository.ReporteExtravioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicoService {
    private static final int DIAS_VISIBILIDAD_AVISTAMIENTO_SIN_RESGUARDO = 45;

    private final MascotaRepository mascotaRepository;
    private final ReporteExtravioRepository reporteRepository;
    private final AvistamientoRepository avistamientoRepository;
    private final FotografiaRepository fotografiaRepository;
    private final VisitaSitioRepository visitaRepository;

    public PublicoService(MascotaRepository mascotaRepository, ReporteExtravioRepository reporteRepository,
            AvistamientoRepository avistamientoRepository, FotografiaRepository fotografiaRepository,
            VisitaSitioRepository visitaRepository) {
        this.mascotaRepository = mascotaRepository;
        this.reporteRepository = reporteRepository;
        this.avistamientoRepository = avistamientoRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.visitaRepository = visitaRepository;
    }

    @Transactional
    public void registrarVisita(String pagina) {
        String paginaLimpia = limpiarPagina(pagina);
        VisitaSitio visita = visitaRepository.findByPagina(paginaLimpia).orElseGet(() -> {
            VisitaSitio nueva = new VisitaSitio();
            nueva.setPagina(paginaLimpia);
            nueva.setTotal(0L);
            return nueva;
        });
        visita.setTotal(visita.getTotal() + 1);
        visita.setFechaActualizacion(LocalDateTime.now());
        visitaRepository.save(visita);
    }

    private String limpiarPagina(String pagina) {
        if (pagina == null || pagina.isBlank()) {
            return "index.html";
        }
        String limpia = pagina.trim().replace("\\", "/");
        int queryIndex = limpia.indexOf('?');
        if (queryIndex >= 0) {
            limpia = limpia.substring(0, queryIndex);
        }
        if (limpia.length() > 120) {
            limpia = limpia.substring(0, 120);
        }
        return limpia;
    }

    @Transactional(readOnly = true)
    public PublicoResumenResponse resumen() {
        return new PublicoResumenResponse(
            // Estadistica publica historica: el borrado logico oculta al dueño, pero no resta registros del sistema.
            mascotaRepository.count(),
            reporteRepository.countByActivoTrueAndEstatusNombreIgnoreCase("ACTIVO"),
            reporteRepository.countByActivoTrueAndEstatusNombreIgnoreCase("RECUPERADO"),
            avistamientoRepository.countByActivoTrue()
        );
    }

    @Transactional(readOnly = true)
    public List<PublicoReporteRecienteResponse> reportesRecientes() {
        return reporteRepository.findTop3ByActivoTrueAndEstatusNombreIgnoreCaseOrderByFechaRegistroDesc("ACTIVO")
            .stream()
            .map(this::construirReporteReciente)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicoReporteRecienteResponse> reportesActivos() {
        return reporteRepository.findByActivoTrueAndEstatusNombreIgnoreCaseOrderByFechaRegistroDesc("ACTIVO")
            .stream()
            .map(this::construirReporteReciente)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicoAvistamientoResponse> avistamientosSinFolio() {
        LocalDateTime fechaMinimaSinResguardo = LocalDateTime.now().minusDays(DIAS_VISIBILIDAD_AVISTAMIENTO_SIN_RESGUARDO);
        return avistamientoRepository.buscarPublicosSinFolio(fechaMinimaSinResguardo)
            .stream()
            .map(this::construirAvistamientoPublico)
            .toList();
    }


    @Transactional(readOnly = true)
    public PublicoPaginaResponse<PublicoReporteRecienteResponse> reportesActivosPaginados(int pagina, int tamanio,
            String texto, String raza, String senas, String sexo, String zona) {
        Pageable pageable = PageRequest.of(paginaSegura(pagina), tamanioSeguro(tamanio), Sort.by(Sort.Direction.DESC, "fechaRegistro"));
        Page<ReporteExtravio> resultado = reporteRepository.buscarPublicosPaginados(
            "ACTIVO", limpiarFiltro(texto), limpiarFiltro(raza), limpiarFiltro(senas), limpiarFiltro(sexo), limpiarFiltro(zona), pageable
        );
        return construirPagina(resultado.map(this::construirReporteReciente));
    }

    @Transactional(readOnly = true)
    public PublicoPaginaResponse<PublicoAvistamientoResponse> avistamientosSinFolioPaginados(int pagina, int tamanio,
            String texto, String senas, String zona, String tipo) {
        Pageable pageable = PageRequest.of(paginaSegura(pagina), tamanioSeguro(tamanio), Sort.by(Sort.Direction.DESC, "fechaRegistro"));
        LocalDateTime fechaMinimaSinResguardo = LocalDateTime.now().minusDays(DIAS_VISIBILIDAD_AVISTAMIENTO_SIN_RESGUARDO);
        // Regla publica: sin resguardo solo se muestran 45 dias; con resguardo permanecen visibles mientras esten activos.
        Page<Avistamiento> resultado = avistamientoRepository.buscarPublicosSinFolioPaginados(
            limpiarFiltro(texto), limpiarFiltro(senas), limpiarFiltro(zona), resolverFiltroResguardo(tipo), fechaMinimaSinResguardo, pageable
        );
        return construirPagina(resultado.map(this::construirAvistamientoPublico));
    }

    private <T> PublicoPaginaResponse<T> construirPagina(Page<T> pagina) {
        return new PublicoPaginaResponse<>(
            pagina.getContent(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages(),
            pagina.isFirst(),
            pagina.isLast()
        );
    }

    private int paginaSegura(int pagina) {
        return Math.max(pagina, 0);
    }

    private int tamanioSeguro(int tamanio) {
        if (tamanio < 1) return 12;
        return Math.min(tamanio, 24);
    }

    private String limpiarFiltro(String valor) {
        return valor == null ? null : valor.trim();
    }

    private Boolean resolverFiltroResguardo(String tipo) {
        if (tipo == null || tipo.isBlank() || "todos".equalsIgnoreCase(tipo)) return null;
        if ("resguardo".equalsIgnoreCase(tipo)) return true;
        if ("avistamiento".equalsIgnoreCase(tipo)) return false;
        return null;
    }

    private PublicoAvistamientoResponse construirAvistamientoPublico(Avistamiento avistamiento) {
        // Respuesta publica: se omiten nombre, correo y telefono del resguardante para proteger datos personales.
        return new PublicoAvistamientoResponse(
            avistamiento.getIdAvistamiento(),
            resolverFolioAvistamiento(avistamiento),
            avistamiento.getFechaAvistamiento(),
            avistamiento.getMunicipio(),
            avistamiento.getColonia(),
            avistamiento.getReferencias(),
            avistamiento.getDescripcion(),
            avistamiento.getResguardado(),
            avistamiento.getFotoAvistamiento()
        );
    }

    private String resolverFolioAvistamiento(Avistamiento avistamiento) {
        if (avistamiento.getFolioAvistamiento() != null && !avistamiento.getFolioAvistamiento().isBlank()) {
            return avistamiento.getFolioAvistamiento();
        }
        int anio = avistamiento.getFechaRegistro() != null ? avistamiento.getFechaRegistro().getYear() : java.time.LocalDate.now().getYear();
        return "AV-" + anio + "-" + String.format("%06d", avistamiento.getIdAvistamiento());
    }

    private PublicoReporteRecienteResponse construirReporteReciente(ReporteExtravio reporte) {
        Mascota mascota = reporte.getMascota();
        // Respuesta publica: solo datos utiles para busqueda, sin informacion privada del propietario.
        return new PublicoReporteRecienteResponse(
            reporte.getIdReporte(),
            reporte.getFolio(),
            mascota.getNombre(),
            mascota.getRaza(),
            mascota.getSenasParticulares(),
            reporte.getEstatus().getNombre(),
            reporte.getMunicipio(),
            reporte.getColonia(),
            reporte.getFechaExtravio(),
            obtenerFotoPrincipalUrl(mascota)
        );
    }

    private String obtenerFotoPrincipalUrl(Mascota mascota) {
        return fotografiaRepository
            .findFirstByMascotaIdMascotaAndEsPrincipalTrueAndActivoTrueOrderByFechaRegistroDesc(mascota.getIdMascota())
            .map(foto -> "/api/publico/mascotas/" + mascota.getIdMascota() + "/foto")
            .orElse(null);
    }
}
