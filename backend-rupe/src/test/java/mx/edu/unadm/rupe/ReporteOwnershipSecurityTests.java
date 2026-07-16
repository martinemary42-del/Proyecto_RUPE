package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Optional;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColor;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstado;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstatus;
import mx.edu.unadm.rupe.catalogo.model.CatalogoMunicipio;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRaza;
import mx.edu.unadm.rupe.catalogo.model.CatalogoTipoMascota;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstatusRepository;
import mx.edu.unadm.rupe.mascota.model.Mascota;
import mx.edu.unadm.rupe.mascota.repository.FotografiaRepository;
import mx.edu.unadm.rupe.mascota.repository.MascotaRepository;
import mx.edu.unadm.rupe.reporte.dto.ReporteExtravioRequest;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;
import mx.edu.unadm.rupe.reporte.repository.ReporteExtravioRepository;
import mx.edu.unadm.rupe.reporte.service.ReporteExtravioService;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import org.junit.jupiter.api.Test;

class ReporteOwnershipSecurityTests {

    @Test
    void registrarRechazaMascotaDeOtroUsuarioYNoGuardaReporte() {
        ReporteExtravioRepository reporteRepository = mock(ReporteExtravioRepository.class);
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        BitacoraSeguridadService bitacoraService = mock(BitacoraSeguridadService.class);
        ReporteExtravioService service = servicioReportes(reporteRepository, mascotaRepository, bitacoraService);

        when(mascotaRepository.findById(15)).thenReturn(Optional.of(mascotaDeUsuario(15, 200)));

        assertThatThrownBy(() -> service.registrar(requestReporteValido(15), sesionUsuario(100), mock(HttpServletRequest.class)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece");

        verify(reporteRepository, never()).save(any());
        verify(bitacoraService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void renovarReporteDelDuenoRegistraBitacora() {
        ReporteExtravioRepository reporteRepository = mock(ReporteExtravioRepository.class);
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        BitacoraSeguridadService bitacoraService = mock(BitacoraSeguridadService.class);
        ReporteExtravioService service = servicioReportes(reporteRepository, mascotaRepository, bitacoraService);
        ReporteExtravio reporte = reporteActivoDeUsuario(30, 100);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(reporteRepository.findById(30)).thenReturn(Optional.of(reporte));
        when(reporteRepository.save(any(ReporteExtravio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.renovarBusqueda(30, sesionUsuario(100), request);

        verify(reporteRepository).save(reporte);
        verify(bitacoraService).registrar(eq(100), eq(null), eq("RENOVAR_REPORTE"), eq("REPORTES"),
            eq("EXITOSO"), anyString(), eq(request));
    }

    private ReporteExtravioService servicioReportes(ReporteExtravioRepository reporteRepository,
            MascotaRepository mascotaRepository, BitacoraSeguridadService bitacoraService) {
        CatalogoEstatusRepository estatusRepository = mock(CatalogoEstatusRepository.class);
        return new ReporteExtravioService(reporteRepository, mascotaRepository, estatusRepository,
            mock(FotografiaRepository.class), bitacoraService, mock(CatalogoColoniaRepository.class));
    }

    private ReporteExtravioRequest requestReporteValido(Integer idMascota) {
        ReporteExtravioRequest request = new ReporteExtravioRequest();
        request.setIdMascota(idMascota);
        request.setFechaExtravio(LocalDate.now());
        request.setTipoReporte("EXTRAVIO");
        request.setIdEstado("CDMX");
        request.setIdMunicipio("Coyoacan");
        request.setIdColonia("Del Carmen");
        request.setReferencias("Cerca del parque");
        request.setDescripcion("Se extravio durante el paseo");
        return request;
    }

    private ReporteExtravio reporteActivoDeUsuario(Integer idReporte, Integer idUsuario) {
        ReporteExtravio reporte = new ReporteExtravio();
        reporte.setIdReporte(idReporte);
        reporte.setFolio("RUPE-2026-000001");
        reporte.setMascota(mascotaDeUsuario(10, idUsuario));
        reporte.setEstatus(estatus("ACTIVO"));
        reporte.setFechaExtravio(LocalDate.now());
        reporte.setTipoReporte("EXTRAVIO");
        reporte.setColonia(colonia(1, "Del Carmen", municipio(1, "Coyoacan", estado(1, "CDMX"))));
        reporte.setReferencias("Cerca del parque");
        reporte.setDescripcionHechos("Se extravio durante el paseo");
        reporte.setRenovaciones(0);
        reporte.setRequiereRenovacion(false);
        return reporte;
    }

    private Mascota mascotaDeUsuario(Integer idMascota, Integer idUsuario) {
        Mascota mascota = new Mascota();
        mascota.setIdMascota(idMascota);
        mascota.setNombre("Max");
        CatalogoTipoMascota tipo = tipoMascota(1, "Perro");
        mascota.setTipoMascota(tipo);
        mascota.setRaza(raza(1, "Mestizo", tipo));
        mascota.setColor(color(1, "Cafe"));
        mascota.setUsuario(usuario(idUsuario));
        mascota.setActivo(true);
        return mascota;
    }

    private CatalogoEstatus estatus(String nombre) {
        CatalogoEstatus estatus = new CatalogoEstatus();
        estatus.setNombre(nombre);
        return estatus;
    }

    private CatalogoTipoMascota tipoMascota(Integer id, String nombre) {
        CatalogoTipoMascota tipo = new CatalogoTipoMascota();
        tipo.setIdTipoMascota(id);
        tipo.setNombre(nombre);
        tipo.setActivo(true);
        return tipo;
    }

    private CatalogoRaza raza(Integer id, String nombre, CatalogoTipoMascota tipo) {
        CatalogoRaza raza = new CatalogoRaza();
        raza.setIdRaza(id);
        raza.setNombre(nombre);
        raza.setTipoMascota(tipo);
        raza.setActivo(true);
        return raza;
    }

    private CatalogoColor color(Integer id, String nombre) {
        CatalogoColor color = new CatalogoColor();
        color.setIdColor(id);
        color.setNombre(nombre);
        color.setActivo(true);
        return color;
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

    private Usuario usuario(Integer idUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setCorreo("usuario" + idUsuario + "@example.com");
        usuario.setNombreCompleto("Usuario " + idUsuario);
        return usuario;
    }

    private HttpSession sesionUsuario(Integer idUsuario) {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("idUsuario")).thenReturn(idUsuario);
        return session;
    }
}
