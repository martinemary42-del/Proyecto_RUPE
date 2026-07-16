package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoValidacionRequest;
import mx.edu.unadm.rupe.avistamiento.model.Avistamiento;
import mx.edu.unadm.rupe.avistamiento.repository.AvistamientoRepository;
import mx.edu.unadm.rupe.avistamiento.service.AvistamientoService;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstado;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstatus;
import mx.edu.unadm.rupe.catalogo.model.CatalogoMunicipio;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstatusRepository;
import mx.edu.unadm.rupe.mascota.model.Mascota;
import mx.edu.unadm.rupe.mascota.repository.FotografiaRepository;
import mx.edu.unadm.rupe.notificacion.model.Notificacion;
import mx.edu.unadm.rupe.notificacion.repository.NotificacionRepository;
import mx.edu.unadm.rupe.notificacion.service.NotificacionService;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import mx.edu.unadm.rupe.reporte.repository.ReporteExtravioRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.security.service.TurnstileService;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import org.junit.jupiter.api.Test;

class SeguimientoAvistamientoSecurityTests {

    @Test
    void validarAvistamientoDelDuenoRegistraBitacora() {
        AvistamientoRepository avistamientoRepository = mock(AvistamientoRepository.class);
        CatalogoEstatusRepository estatusRepository = mock(CatalogoEstatusRepository.class);
        NotificacionService notificacionService = mock(NotificacionService.class);
        BitacoraSeguridadService bitacoraService = mock(BitacoraSeguridadService.class);
        AvistamientoService service = new AvistamientoService(avistamientoRepository, mock(ReporteExtravioRepository.class),
            estatusRepository, mock(TurnstileService.class), notificacionService, mock(FotografiaRepository.class),
            bitacoraService, mock(CatalogoColoniaRepository.class));
        Avistamiento avistamiento = avistamientoDeUsuario(100);
        AvistamientoValidacionRequest request = new AvistamientoValidacionRequest();
        request.setDecision("si");
        request.setComentario("Coincide con mi perrito");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(avistamientoRepository.findByIdAvistamientoAndReporteMascotaUsuarioIdUsuario(20, 100))
            .thenReturn(Optional.of(avistamiento));
        when(estatusRepository.findByNombre("VALIDADO")).thenReturn(Optional.of(estatus("VALIDADO")));
        when(avistamientoRepository.save(any(Avistamiento.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.validarPorDueno(20, request, sesionUsuario(100), httpRequest);

        verify(bitacoraService).registrar(eq(100), eq(null), eq("VALIDAR_AVISTAMIENTO"), eq("AVISTAMIENTOS"),
            eq("EXITOSO"), anyString(), eq(httpRequest));
    }

    @Test
    void marcarNotificacionDeOtroUsuarioNoRegistraBitacora() {
        NotificacionRepository notificacionRepository = mock(NotificacionRepository.class);
        BitacoraSeguridadService bitacoraService = mock(BitacoraSeguridadService.class);
        NotificacionService service = new NotificacionService(notificacionRepository, bitacoraService);

        when(notificacionRepository.findByIdNotificacionAndReporteMascotaUsuarioIdUsuario(9, 100))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarLeida(9, sesionUsuario(100), mock(HttpServletRequest.class)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Notificacion no encontrada");
    }

    @Test
    void marcarNotificacionPropiaRegistraBitacora() {
        NotificacionRepository notificacionRepository = mock(NotificacionRepository.class);
        BitacoraSeguridadService bitacoraService = mock(BitacoraSeguridadService.class);
        NotificacionService service = new NotificacionService(notificacionRepository, bitacoraService);
        Notificacion notificacion = notificacionDeUsuario(100);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(notificacionRepository.findByIdNotificacionAndReporteMascotaUsuarioIdUsuario(9, 100))
            .thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.marcarLeida(9, sesionUsuario(100), request);

        verify(bitacoraService).registrar(eq(100), eq(null), eq("MARCAR_NOTIFICACION_LEIDA"), eq("NOTIFICACIONES"),
            eq("EXITOSO"), anyString(), eq(request));
    }

    private Avistamiento avistamientoDeUsuario(Integer idUsuario) {
        Avistamiento avistamiento = new Avistamiento();
        avistamiento.setReporte(reporteDeUsuario(idUsuario));
        avistamiento.setFolioAvistamiento("AV-2026-000020");
        avistamiento.setEstatus(estatus("PENDIENTE"));
        avistamiento.setFechaAvistamiento(LocalDate.now());
        avistamiento.setColonia(colonia(1, "Del Carmen", municipio(1, "Coyoacan", estado(1, "CDMX"))));
        avistamiento.setReferencias("Cerca del parque");
        avistamiento.setDescripcion("Perrito cafe con collar rojo");
        avistamiento.setActivo(true);
        return avistamiento;
    }

    private Notificacion notificacionDeUsuario(Integer idUsuario) {
        Notificacion notificacion = new Notificacion();
        notificacion.setReporte(reporteDeUsuario(idUsuario));
        notificacion.setTipo("avistamiento");
        notificacion.setDestinatario("dueno@example.com");
        notificacion.setAsunto("Nuevo avistamiento recibido");
        notificacion.setMensaje("Se registro un avistamiento relacionado.");
        notificacion.setFechaEnvio(LocalDateTime.now());
        notificacion.setEstatusEnvio("NO_LEIDA");
        return notificacion;
    }

    private ReporteExtravio reporteDeUsuario(Integer idUsuario) {
        ReporteExtravio reporte = new ReporteExtravio();
        reporte.setFolio("RUPE-2026-000001");
        reporte.setMascota(mascotaDeUsuario(idUsuario));
        reporte.setEstatus(estatus("ACTIVO"));
        reporte.setFechaExtravio(LocalDate.now());
        reporte.setTipoReporte("EXTRAVIO");
        reporte.setColonia(colonia(1, "Del Carmen", municipio(1, "Coyoacan", estado(1, "CDMX"))));
        reporte.setReferencias("Cerca del parque");
        reporte.setDescripcionHechos("Se extravio durante el paseo");
        return reporte;
    }

    private Mascota mascotaDeUsuario(Integer idUsuario) {
        Mascota mascota = new Mascota();
        mascota.setNombre("Max");
        mascota.setUsuario(usuario(idUsuario));
        return mascota;
    }

    private Usuario usuario(Integer idUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setCorreo("usuario" + idUsuario + "@example.com");
        usuario.setNombreCompleto("Usuario " + idUsuario);
        return usuario;
    }

    private CatalogoEstatus estatus(String nombre) {
        CatalogoEstatus estatus = new CatalogoEstatus();
        estatus.setNombre(nombre);
        return estatus;
    }

    private CatalogoEstado estado(Integer id, String nombre) {
        CatalogoEstado estado = new CatalogoEstado();
        estado.setIdEstado(id);
        estado.setNombre(nombre);
        estado.setActivo(true);
        return estado;
    }

    private CatalogoMunicipio municipio(Integer id, String nombre, CatalogoEstado estado) {
        CatalogoMunicipio municipio = new CatalogoMunicipio();
        municipio.setIdMunicipio(id);
        municipio.setNombre(nombre);
        municipio.setEstado(estado);
        municipio.setActivo(true);
        return municipio;
    }

    private CatalogoColonia colonia(Integer id, String nombre, CatalogoMunicipio municipio) {
        CatalogoColonia colonia = new CatalogoColonia();
        colonia.setIdColonia(id);
        colonia.setNombre(nombre);
        colonia.setCodigoPostal("04100");
        colonia.setMunicipio(municipio);
        colonia.setActivo(true);
        return colonia;
    }

    private HttpSession sesionUsuario(Integer idUsuario) {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("idUsuario")).thenReturn(idUsuario);
        return session;
    }
}
