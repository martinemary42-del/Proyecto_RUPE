package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import mx.edu.unadm.rupe.avistamiento.model.Avistamiento;
import mx.edu.unadm.rupe.avistamiento.repository.AvistamientoRepository;
import mx.edu.unadm.rupe.mascota.repository.FotografiaRepository;
import mx.edu.unadm.rupe.mascota.repository.MascotaRepository;
import mx.edu.unadm.rupe.publico.repository.VisitaSitioRepository;
import mx.edu.unadm.rupe.publico.service.PublicoService;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import mx.edu.unadm.rupe.reporte.repository.ReporteExtravioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class PublicoBusquedaAvistamientoTests {

    @Test
    void busquedaPublicaDeReportesEnviaSenasParticularesAlRepositorio() {
        ReporteExtravioRepository reporteRepository = mock(ReporteExtravioRepository.class);
        PublicoService service = servicioPublico(reporteRepository, mock(AvistamientoRepository.class));
        when(reporteRepository.buscarPublicosPaginados(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(Page.<ReporteExtravio>empty());

        // Las senas permiten localizar coincidencias sin exponer datos personales del propietario.
        service.reportesActivosPaginados(0, 12, "RUPE", "mestizo", "collar rojo", "Macho", "Coyoacan");

        verify(reporteRepository).buscarPublicosPaginados(
            eq("ACTIVO"), eq("RUPE"), eq("mestizo"), eq("collar rojo"),
            eq("Macho"), eq("Coyoacan"), any(Pageable.class));
    }

    @Test
    void avistamientosSinResguardoUsanVentanaPublicaDeCuarentaYCincoDias() {
        AvistamientoRepository avistamientoRepository = mock(AvistamientoRepository.class);
        PublicoService service = servicioPublico(mock(ReporteExtravioRepository.class), avistamientoRepository);
        when(avistamientoRepository.buscarPublicosSinFolioPaginados(
                any(), any(), any(), any(), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(Page.<Avistamiento>empty());

        LocalDateTime antes = LocalDateTime.now().minusDays(45).minusSeconds(2);
        service.avistamientosSinFolioPaginados(0, 12, "parque", "mancha blanca", "Toluca", "avistamiento");
        LocalDateTime despues = LocalDateTime.now().minusDays(45).plusSeconds(2);

        ArgumentCaptor<LocalDateTime> fechaMinima = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(avistamientoRepository).buscarPublicosSinFolioPaginados(
            eq("parque"), eq("mancha blanca"), eq("Toluca"), eq(false), fechaMinima.capture(), any(Pageable.class));

        // Regla de privacidad y vigencia: sin resguardo expira a 45 dias; con resguardo permanece activo.
        assertThat(fechaMinima.getValue()).isAfter(antes).isBefore(despues);
    }

    private PublicoService servicioPublico(ReporteExtravioRepository reporteRepository,
            AvistamientoRepository avistamientoRepository) {
        return new PublicoService(
            mock(MascotaRepository.class),
            reporteRepository,
            avistamientoRepository,
            mock(FotografiaRepository.class),
            mock(VisitaSitioRepository.class)
        );
    }
}
