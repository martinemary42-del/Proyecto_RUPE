package mx.edu.unadm.rupe.mascota.service;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColor;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRaza;
import mx.edu.unadm.rupe.catalogo.model.CatalogoTipoMascota;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColorRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRazaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoTipoMascotaRepository;
import mx.edu.unadm.rupe.mascota.dto.MascotaRegistroRequest;
import mx.edu.unadm.rupe.mascota.dto.MascotaResponse;
import mx.edu.unadm.rupe.mascota.model.Fotografia;
import mx.edu.unadm.rupe.mascota.model.Mascota;
import mx.edu.unadm.rupe.mascota.repository.FotografiaRepository;
import mx.edu.unadm.rupe.mascota.repository.MascotaRepository;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MascotaService {

    private static final long MAX_FOTO_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final MascotaRepository mascotaRepository;
    private final FotografiaRepository fotografiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoTipoMascotaRepository tipoMascotaRepository;
    private final CatalogoRazaRepository razaRepository;
    private final CatalogoColorRepository colorRepository;
    private final CatalogoColoniaRepository coloniaRepository;

    public MascotaService(MascotaRepository mascotaRepository, FotografiaRepository fotografiaRepository,
            UsuarioRepository usuarioRepository, CatalogoTipoMascotaRepository tipoMascotaRepository,
            CatalogoRazaRepository razaRepository, CatalogoColorRepository colorRepository,
            CatalogoColoniaRepository coloniaRepository) {
        this.mascotaRepository = mascotaRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoMascotaRepository = tipoMascotaRepository;
        this.razaRepository = razaRepository;
        this.colorRepository = colorRepository;
        this.coloniaRepository = coloniaRepository;
    }

    @Transactional
    public MascotaResponse registrar(MascotaRegistroRequest request, MultipartFile fotoPrincipal, HttpSession session) {
        Usuario usuario = obtenerUsuarioDeSesion(session);
        String nombre = limpiar(request.getNombre());
        CatalogoTipoMascota tipoMascota = obtenerTipoMascota(request.getIdTipoMascota());
        CatalogoRaza raza = obtenerRaza(request.getIdRaza());
        CatalogoColor color = obtenerColor(request.getIdColor());
        CatalogoColonia colonia = obtenerColonia(request.getIdColonia());
        validarCoherenciaCatalogos(tipoMascota, raza, colonia, request);
        validarRegistroDuplicado(usuario, nombre, raza.getIdRaza(), request.getConfirmarDuplicado());

        Mascota mascota = new Mascota();
        mascota.setUsuario(usuario);
        mascota.setNombre(nombre);
        mascota.setSexo(limpiar(request.getSexo()));
        mascota.setTipoMascota(tipoMascota);
        mascota.setEdadAproximada(limpiarOpcional(request.getEdad()));
        mascota.setRaza(raza);
        mascota.setMezclaRaza(limpiarOpcional(request.getMezcla()));
        mascota.setColor(color);
        mascota.setDescripcionColor(limpiarOpcional(request.getDescripcionColor()));
        mascota.setSenasParticulares(limpiar(request.getSenas()));
        mascota.setCondicionMedica(limpiarOpcional(request.getCondicionMedica()));
        mascota.setCollarPlaca(limpiar(request.getCollarPlaca()));
        mascota.setColoniaReferencia(colonia);
        mascota.setCalle(limpiarOpcional(request.getCalle()));
        mascota.setNumero(limpiarOpcional(request.getNumero()));
        mascota.setFechaRegistro(LocalDateTime.now());

        Mascota guardada = mascotaRepository.save(mascota);
        guardarFotoSiExiste(guardada, fotoPrincipal);

        return new MascotaResponse(guardada, "Perrito registrado correctamente", obtenerFotoPrincipalUrl(guardada));
    }


    @Transactional
    public MascotaResponse actualizar(Integer idMascota, MascotaRegistroRequest request, MultipartFile fotoPrincipal,
            HttpSession session) {
        Usuario usuario = obtenerUsuarioDeSesion(session);
        Mascota mascota = mascotaRepository.findById(idMascota)
            .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        if (!mascota.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new IllegalArgumentException("La mascota no pertenece al usuario activo");
        }

        CatalogoTipoMascota tipoMascota = obtenerTipoMascota(request.getIdTipoMascota());
        CatalogoRaza raza = obtenerRaza(request.getIdRaza());
        CatalogoColor color = obtenerColor(request.getIdColor());
        CatalogoColonia colonia = obtenerColonia(request.getIdColonia());
        validarCoherenciaCatalogos(tipoMascota, raza, colonia, request);

        mascota.setNombre(limpiar(request.getNombre()));
        mascota.setSexo(limpiar(request.getSexo()));
        mascota.setTipoMascota(tipoMascota);
        mascota.setEdadAproximada(limpiarOpcional(request.getEdad()));
        mascota.setRaza(raza);
        mascota.setMezclaRaza(limpiarOpcional(request.getMezcla()));
        mascota.setColor(color);
        mascota.setDescripcionColor(limpiarOpcional(request.getDescripcionColor()));
        mascota.setSenasParticulares(limpiar(request.getSenas()));
        mascota.setCondicionMedica(limpiarOpcional(request.getCondicionMedica()));
        mascota.setCollarPlaca(limpiar(request.getCollarPlaca()));
        mascota.setColoniaReferencia(colonia);
        mascota.setCalle(limpiarOpcional(request.getCalle()));
        mascota.setNumero(limpiarOpcional(request.getNumero()));
        mascota.setFechaActualizacion(LocalDateTime.now());

        Mascota actualizada = mascotaRepository.save(mascota);
        guardarFotoSiExiste(actualizada, fotoPrincipal);

        return new MascotaResponse(actualizada, "Perrito actualizado correctamente", obtenerFotoPrincipalUrl(actualizada));
    }

    @Transactional(readOnly = true)
    public MascotaResponse consultarDetalle(Integer idMascota, HttpSession session) {
        Usuario usuario = obtenerUsuarioDeSesion(session);
        Mascota mascota = mascotaRepository.findById(idMascota)
            .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        if (!mascota.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new IllegalArgumentException("La mascota no pertenece al usuario activo");
        }
        return new MascotaResponse(mascota, "Consulta correcta", obtenerFotoPrincipalUrl(mascota));
    }

    @Transactional(readOnly = true)
    public List<MascotaResponse> consultarMisMascotas(HttpSession session) {
        Usuario usuario = obtenerUsuarioDeSesion(session);
        return mascotaRepository.findByUsuarioIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(usuario.getIdUsuario())
            .stream()
            .map(mascota -> new MascotaResponse(mascota, "Consulta correcta", obtenerFotoPrincipalUrl(mascota)))
            .toList();
    }

    @Transactional
    public MascotaResponse desactivar(Integer idMascota, HttpSession session) {
        Usuario usuario = obtenerUsuarioDeSesion(session);
        Mascota mascota = mascotaRepository.findById(idMascota)
            .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        if (!mascota.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new IllegalArgumentException("La mascota no pertenece al usuario activo");
        }
        if (Boolean.FALSE.equals(mascota.getActivo())) {
            return new MascotaResponse(mascota, "El registro ya estaba desactivado", obtenerFotoPrincipalUrl(mascota));
        }

        mascota.setActivo(false);
        mascota.setFechaActualizacion(LocalDateTime.now());
        Mascota desactivada = mascotaRepository.save(mascota);
        return new MascotaResponse(desactivada, "Registro desactivado correctamente", obtenerFotoPrincipalUrl(desactivada));
    }


    private String obtenerFotoPrincipalUrl(Mascota mascota) {
        return fotografiaRepository
            .findFirstByMascotaIdMascotaAndEsPrincipalTrueAndActivoTrueOrderByFechaRegistroDesc(mascota.getIdMascota())
            .map(foto -> "/api/publico/mascotas/" + mascota.getIdMascota() + "/foto")
            .orElse(null);
    }

    private void validarRegistroDuplicado(Usuario usuario, String nombre, Integer idRaza, Boolean confirmarDuplicado) {
        // Evita registros repetidos accidentales incluso si el registro anterior tuvo borrado logico.
        // Esto conserva trazabilidad historica sin exponer datos de otros usuarios ni reactivar registros.
        boolean existeCoincidencia = mascotaRepository
            .existsByUsuarioIdUsuarioAndNombreIgnoreCaseAndRazaIdRaza(
                usuario.getIdUsuario(), nombre, idRaza);
        if (existeCoincidencia && !Boolean.TRUE.equals(confirmarDuplicado)) {
            throw new IllegalStateException("Ya existe en tu historial un perrito con el mismo nombre y raza. Puede estar activo o borrado de la vista. Confirma solo si se trata de otra mascota.");
        }
    }

    private CatalogoTipoMascota obtenerTipoMascota(String id) {
        return tipoMascotaRepository.findById(parsearId(id, "tipo de mascota"))
            .filter(catalogo -> Boolean.TRUE.equals(catalogo.getActivo()))
            .orElseThrow(() -> new IllegalArgumentException("Selecciona un tipo de mascota valido"));
    }

    private CatalogoRaza obtenerRaza(String id) {
        return razaRepository.findById(parsearId(id, "raza"))
            .filter(catalogo -> Boolean.TRUE.equals(catalogo.getActivo()))
            .orElseThrow(() -> new IllegalArgumentException("Selecciona una raza valida"));
    }

    private CatalogoColor obtenerColor(String id) {
        return colorRepository.findById(parsearId(id, "color"))
            .filter(catalogo -> Boolean.TRUE.equals(catalogo.getActivo()))
            .orElseThrow(() -> new IllegalArgumentException("Selecciona un color valido"));
    }

    private CatalogoColonia obtenerColonia(String id) {
        return coloniaRepository.findById(parsearId(id, "colonia"))
            .filter(catalogo -> Boolean.TRUE.equals(catalogo.getActivo()))
            .orElseThrow(() -> new IllegalArgumentException("Selecciona una colonia valida"));
    }

    private void validarCoherenciaCatalogos(CatalogoTipoMascota tipoMascota, CatalogoRaza raza,
            CatalogoColonia colonia, MascotaRegistroRequest request) {
        // Se valida la cadena de catalogos para que el frontend no pueda enviar combinaciones alteradas.
        if (!raza.getTipoMascota().getIdTipoMascota().equals(tipoMascota.getIdTipoMascota())) {
            throw new IllegalArgumentException("La raza no corresponde al tipo de mascota seleccionado");
        }
        Integer idMunicipio = parsearId(request.getIdMunicipio(), "municipio");
        Integer idEstado = parsearId(request.getIdEstado(), "estado");
        if (!colonia.getMunicipio().getIdMunicipio().equals(idMunicipio)
                || !colonia.getMunicipio().getEstado().getIdEstado().equals(idEstado)) {
            throw new IllegalArgumentException("La colonia no corresponde al estado y municipio seleccionados");
        }
    }

    private Integer parsearId(String valor, String campo) {
        String limpio = limpiar(valor);
        try {
            return Integer.valueOf(limpio);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Selecciona un valor valido para " + campo);
        }
    }

    private Usuario obtenerUsuarioDeSesion(HttpSession session) {
        // La sesion define la propiedad de los datos; no se aceptan ids de usuario enviados desde frontend.
        Object idUsuario = session.getAttribute("idUsuario");
        if (!(idUsuario instanceof Integer)) {
            throw new IllegalArgumentException("Inicia sesion para continuar");
        }
        return usuarioRepository.findById((Integer) idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void guardarFotoSiExiste(Mascota mascota, MultipartFile fotoPrincipal) {
        if (fotoPrincipal == null || fotoPrincipal.isEmpty()) {
            return;
        }

        validarFoto(fotoPrincipal);

        try {
            String extension = obtenerExtension(fotoPrincipal.getOriginalFilename());
            String nombreArchivo = "mascota-" + mascota.getIdMascota() + extension;

            desactivarFotosPrincipales(mascota);

            Fotografia fotografia = new Fotografia();
            fotografia.setMascota(mascota);
            fotografia.setNombreArchivo(nombreArchivo);
            fotografia.setRutaArchivo("/api/publico/mascotas/" + mascota.getIdMascota() + "/foto");
            fotografia.setContenido(fotoPrincipal.getBytes());
            fotografia.setTipoContenido(fotoPrincipal.getContentType());
            fotografia.setEsPrincipal(true);
            fotografia.setActivo(true);
            fotografiaRepository.save(fotografia);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la fotografia");
        }
    }


    @Transactional(readOnly = true)
    public FotoMascotaData obtenerFotoPrincipal(Integer idMascota) {
        Fotografia foto = fotografiaRepository
            .findFirstByMascotaIdMascotaAndEsPrincipalTrueAndActivoTrueOrderByFechaRegistroDesc(idMascota)
            .filter(f -> f.getContenido() != null && f.getContenido().length > 0)
            .orElseThrow(() -> new IllegalArgumentException("Fotografia no encontrada"));
        String tipo = foto.getTipoContenido() != null ? foto.getTipoContenido() : "image/jpeg";
        return new FotoMascotaData(foto.getContenido(), tipo, foto.getNombreArchivo());
    }

    public record FotoMascotaData(byte[] contenido, String tipoContenido, String nombreArchivo) {}


    private void desactivarFotosPrincipales(Mascota mascota) {
        fotografiaRepository.findByMascotaIdMascotaAndEsPrincipalTrueAndActivoTrue(mascota.getIdMascota())
            .forEach(foto -> {
                foto.setEsPrincipal(false);
                foto.setActivo(false);
                fotografiaRepository.save(foto);
            });
    }

    private void validarFoto(MultipartFile foto) {
        // Defensa del backend: aunque el frontend comprima/valide, aqui se limita tipo y peso real del archivo.
        if (foto.getSize() > MAX_FOTO_BYTES) {
            throw new IllegalArgumentException("La fotografia no debe superar 5 MB");
        }
        String contentType = foto.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Solo se permiten fotografias JPG, PNG o WEBP");
        }
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return ".jpg";
        }
        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case ".jpeg", ".jpg", ".png", ".webp" -> extension;
            default -> ".jpg";
        };
    }

    private String limpiar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Completa los campos obligatorios");
        }
        return valor.trim();
    }

    private String limpiarOpcional(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }
}
