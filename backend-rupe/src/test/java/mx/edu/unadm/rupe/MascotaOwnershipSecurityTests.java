package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColor;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstado;
import mx.edu.unadm.rupe.catalogo.model.CatalogoMunicipio;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRaza;
import mx.edu.unadm.rupe.catalogo.model.CatalogoTipoMascota;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColorRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRazaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoTipoMascotaRepository;
import mx.edu.unadm.rupe.mascota.dto.MascotaRegistroRequest;
import mx.edu.unadm.rupe.mascota.model.Mascota;
import mx.edu.unadm.rupe.mascota.repository.FotografiaRepository;
import mx.edu.unadm.rupe.mascota.repository.MascotaRepository;
import mx.edu.unadm.rupe.mascota.service.MascotaService;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

class MascotaOwnershipSecurityTests {

    @Test
    void detalleRechazaMascotaDeOtroUsuario() {
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        MascotaService service = servicioMascotas(mascotaRepository, usuarioRepositoryConSesion());
        when(mascotaRepository.findById(15)).thenReturn(Optional.of(mascotaDeUsuario(15, 200)));

        // El id de la URL no basta: el backend valida que la mascota pertenezca al usuario en sesion.
        assertThatThrownBy(() -> service.consultarDetalle(15, sesionUsuario(100)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece");
    }

    @Test
    void actualizarRechazaMascotaDeOtroUsuarioYNoGuardaCambios() {
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        MascotaService service = servicioMascotas(mascotaRepository, usuarioRepositoryConSesion());
        when(mascotaRepository.findById(15)).thenReturn(Optional.of(mascotaDeUsuario(15, 200)));

        assertThatThrownBy(() -> service.actualizar(15, requestMascotaValida(), null, sesionUsuario(100)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece");

        verify(mascotaRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void desactivarRechazaMascotaDeOtroUsuarioYNoAplicaBajaLogica() {
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        MascotaService service = servicioMascotas(mascotaRepository, usuarioRepositoryConSesion());
        when(mascotaRepository.findById(15)).thenReturn(Optional.of(mascotaDeUsuario(15, 200)));

        assertThatThrownBy(() -> service.desactivar(15, sesionUsuario(100)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece");

        verify(mascotaRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void misMascotasConsultaSoloActivasDelUsuarioEnSesion() {
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        MascotaService service = servicioMascotas(mascotaRepository, usuarioRepositoryConSesion());
        when(mascotaRepository.findByUsuarioIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(100))
            .thenReturn(List.of());

        service.consultarMisMascotas(sesionUsuario(100));

        verify(mascotaRepository).findByUsuarioIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(100);
    }

    private MascotaService servicioMascotas(MascotaRepository mascotaRepository, UsuarioRepository usuarioRepository) {
        return new MascotaService(mascotaRepository, mock(FotografiaRepository.class), usuarioRepository,
            mock(CatalogoTipoMascotaRepository.class), mock(CatalogoRazaRepository.class),
            mock(CatalogoColorRepository.class), mock(CatalogoColoniaRepository.class));
    }

    private UsuarioRepository usuarioRepositoryConSesion() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(usuario(100)));
        return usuarioRepository;
    }

    private HttpSession sesionUsuario(Integer idUsuario) {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("idUsuario")).thenReturn(idUsuario);
        return session;
    }

    private Mascota mascotaDeUsuario(Integer idMascota, Integer idUsuario) {
        Mascota mascota = requestMascotaValidaComoEntidad();
        mascota.setIdMascota(idMascota);
        mascota.setUsuario(usuario(idUsuario));
        return mascota;
    }

    private Usuario usuario(Integer idUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setNombreCompleto("Usuario " + idUsuario);
        usuario.setCorreo("usuario" + idUsuario + "@example.com");
        return usuario;
    }

    private MascotaRegistroRequest requestMascotaValida() {
        MascotaRegistroRequest request = new MascotaRegistroRequest();
        request.setNombre("Max");
        request.setSexo("Macho");
        request.setIdTipoMascota("1");
        request.setIdRaza("1");
        request.setIdColor("1");
        request.setSenas("Mancha blanca en pecho");
        request.setCollarPlaca("Con collar");
        request.setIdEstado("1");
        request.setIdMunicipio("1");
        request.setIdColonia("1");
        return request;
    }

    private Mascota requestMascotaValidaComoEntidad() {
        MascotaRegistroRequest request = requestMascotaValida();
        Mascota mascota = new Mascota();
        mascota.setNombre(request.getNombre());
        mascota.setSexo(request.getSexo());
        CatalogoTipoMascota tipo = tipoMascota(1, "Perro");
        mascota.setTipoMascota(tipo);
        mascota.setRaza(raza(1, "Mestizo", tipo));
        mascota.setColor(color(1, "Cafe"));
        mascota.setSenasParticulares(request.getSenas());
        mascota.setCollarPlaca(request.getCollarPlaca());
        mascota.setColoniaReferencia(colonia(1, "Del Carmen", municipio(1, "Coyoacan", estado(1, "CDMX"))));
        mascota.setActivo(true);
        return mascota;
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
}
