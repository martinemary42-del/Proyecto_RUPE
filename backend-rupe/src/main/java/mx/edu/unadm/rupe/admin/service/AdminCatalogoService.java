package mx.edu.unadm.rupe.admin.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.text.Normalizer;
import java.util.List;
import mx.edu.unadm.rupe.admin.dto.AdminCatalogoRequest;
import mx.edu.unadm.rupe.admin.dto.AdminCatalogoResponse;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColor;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstado;
import mx.edu.unadm.rupe.catalogo.model.CatalogoMunicipio;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRaza;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColorRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstadoRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstatusRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoMunicipioRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRazaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRolRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoTipoMascotaRepository;
import mx.edu.unadm.rupe.security.service.BitacoraSeguridadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCatalogoService {
    private final CatalogoTipoMascotaRepository tipoRepository;
    private final CatalogoRazaRepository razaRepository;
    private final CatalogoColorRepository colorRepository;
    private final CatalogoEstadoRepository estadoRepository;
    private final CatalogoMunicipioRepository municipioRepository;
    private final CatalogoColoniaRepository coloniaRepository;
    private final CatalogoEstatusRepository estatusRepository;
    private final CatalogoRolRepository rolRepository;
    private final BitacoraSeguridadService bitacoraService;

    public AdminCatalogoService(CatalogoTipoMascotaRepository tipoRepository,
            CatalogoRazaRepository razaRepository, CatalogoColorRepository colorRepository,
            CatalogoEstadoRepository estadoRepository, CatalogoMunicipioRepository municipioRepository,
            CatalogoColoniaRepository coloniaRepository, CatalogoEstatusRepository estatusRepository,
            CatalogoRolRepository rolRepository, BitacoraSeguridadService bitacoraService) {
        this.tipoRepository = tipoRepository;
        this.razaRepository = razaRepository;
        this.colorRepository = colorRepository;
        this.estadoRepository = estadoRepository;
        this.municipioRepository = municipioRepository;
        this.coloniaRepository = coloniaRepository;
        this.estatusRepository = estatusRepository;
        this.rolRepository = rolRepository;
        this.bitacoraService = bitacoraService;
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogoResponse> listar(String catalogo, HttpSession session) {
        validarAdmin(session);
        return switch (normalizarClave(catalogo)) {
            case "tipo" -> tipoRepository.findAllByOrderByNombreAsc().stream()
                .map(i -> respuesta(i.getIdTipoMascota(), "tipo", i.getNombre(), i.getActivo(), false, "MASCOTA.id_tipo_mascota", null, null, null))
                .toList();
            case "raza" -> razaRepository.findAllByOrderByNombreAsc().stream()
                .map(i -> respuesta(i.getIdRaza(), "raza", i.getNombre(), i.getActivo(), true, "MASCOTA.id_raza", i.getTipoMascota().getIdTipoMascota(), i.getTipoMascota().getNombre(), null))
                .toList();
            case "color" -> colorRepository.findAllByOrderByNombreAsc().stream()
                .map(i -> respuesta(i.getIdColor(), "color", i.getNombre(), i.getActivo(), true, "MASCOTA.id_color", null, null, null))
                .toList();
            case "estado" -> estadoRepository.findAllByOrderByNombreAsc().stream()
                .map(i -> respuesta(i.getIdEstado(), "estado", i.getNombre(), i.getActivo(), true, "REPORTE/AVISTAMIENTO.estado", null, null, null))
                .toList();
            case "municipio" -> municipioRepository.findAllByOrderByNombreAsc().stream()
                .map(i -> respuesta(i.getIdMunicipio(), "municipio", i.getNombre(), i.getActivo(), true, "CATALOGO_MUNICIPIO.id_estado", i.getEstado().getIdEstado(), i.getEstado().getNombre(), i.getZonaCobertura()))
                .toList();
            case "colonia" -> coloniaRepository.findAllByOrderByNombreAsc().stream()
                .map(i -> respuesta(i.getIdColonia(), "colonia", i.getNombre(), i.getActivo(), true, "CATALOGO_COLONIA.id_municipio", i.getMunicipio().getIdMunicipio(), i.getMunicipio().getNombre(), i.getCodigoPostal()))
                .toList();
            case "estatus" -> estatusRepository.findAllByOrderByNombreAsc().stream()
                .map(i -> respuesta(i.getIdEstatus(), "estatus", i.getNombre(), i.getActivo(), false, "REPORTE/AVISTAMIENTO.id_estatus", null, null, i.getDescripcion()))
                .toList();
            case "rol" -> rolRepository.findAllByOrderByNombreAsc().stream()
                .map(i -> respuesta(i.getIdRol(), "rol", i.getNombre(), i.getActivo(), false, "USUARIO.id_rol", null, null, i.getDescripcion()))
                .toList();
            default -> throw new IllegalArgumentException("Catalogo no valido.");
        };
    }

    @Transactional
    public AdminCatalogoResponse crear(String catalogo, AdminCatalogoRequest request,
            HttpSession session, HttpServletRequest httpRequest) {
        validarAdmin(session);
        String clave = normalizarClave(catalogo);
        String nombre = limpiarNombre(request.getNombre());
        AdminCatalogoResponse creado = switch (clave) {
            case "raza" -> crearRaza(nombre, request);
            case "color" -> crearColor(nombre);
            case "estado" -> crearEstado(nombre);
            case "municipio" -> crearMunicipio(nombre, request);
            case "colonia" -> crearColonia(nombre, request);
            default -> throw new IllegalArgumentException("Este catálogo es solo de consulta para proteger la lógica del sistema.");
        };
        bitacoraService.registrar((Integer) session.getAttribute("idUsuario"), null,
            "CREAR_CATALOGO", "ADMIN_CATALOGOS", "EXITOSO",
            "Alta en catálogo " + clave + ": " + nombre, httpRequest);
        return creado;
    }

    @Transactional
    public AdminCatalogoResponse cambiarEstatus(String catalogo, Integer id, boolean activo,
            HttpSession session, HttpServletRequest httpRequest) {
        validarAdmin(session);
        String clave = normalizarClave(catalogo);
        AdminCatalogoResponse respuesta = switch (clave) {
            case "raza" -> { CatalogoRaza item = razaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Registro no encontrado.")); item.setActivo(activo); yield convertir(razaRepository.save(item)); }
            case "color" -> { CatalogoColor item = colorRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Registro no encontrado.")); item.setActivo(activo); yield convertir(colorRepository.save(item)); }
            case "estado" -> { CatalogoEstado item = estadoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Registro no encontrado.")); item.setActivo(activo); yield convertir(estadoRepository.save(item)); }
            case "municipio" -> { CatalogoMunicipio item = municipioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Registro no encontrado.")); item.setActivo(activo); yield convertir(municipioRepository.save(item)); }
            case "colonia" -> { CatalogoColonia item = coloniaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Registro no encontrado.")); item.setActivo(activo); yield convertir(coloniaRepository.save(item)); }
            default -> throw new IllegalArgumentException("Este catálogo es solo de consulta.");
        };
        bitacoraService.registrar((Integer) session.getAttribute("idUsuario"), null,
            activo ? "ACTIVAR_CATALOGO" : "DESACTIVAR_CATALOGO", "ADMIN_CATALOGOS", "EXITOSO",
            "Cambio de estado en catálogo " + clave + " id " + id, httpRequest);
        return respuesta;
    }

    private AdminCatalogoResponse crearRaza(String nombre, AdminCatalogoRequest request) {
        var tipo = request.getIdPadre() != null
            ? tipoRepository.findById(request.getIdPadre()).orElseThrow(() -> new IllegalArgumentException("Selecciona un tipo de mascota valido."))
            : tipoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .filter(t -> normalizarTexto(t.getNombre()).equals("perro"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el tipo Perro."));
        validarDuplicado(razaRepository.findAllByOrderByNombreAsc().stream()
            .filter(r -> r.getTipoMascota().getIdTipoMascota().equals(tipo.getIdTipoMascota()))
            .map(CatalogoRaza::getNombre).toList(), nombre, "raza");
        CatalogoRaza item = new CatalogoRaza();
        item.setTipoMascota(tipo);
        item.setNombre(nombre);
        item.setActivo(true);
        return convertir(razaRepository.save(item));
    }

    private AdminCatalogoResponse crearColor(String nombre) {
        validarDuplicado(colorRepository.findAllByOrderByNombreAsc().stream().map(CatalogoColor::getNombre).toList(), nombre, "color");
        CatalogoColor item = new CatalogoColor();
        item.setNombre(nombre);
        item.setActivo(true);
        return convertir(colorRepository.save(item));
    }

    private AdminCatalogoResponse crearEstado(String nombre) {
        validarDuplicado(estadoRepository.findAllByOrderByNombreAsc().stream().map(CatalogoEstado::getNombre).toList(), nombre, "estado");
        CatalogoEstado item = new CatalogoEstado();
        item.setNombre(nombre);
        item.setActivo(true);
        return convertir(estadoRepository.save(item));
    }

    private AdminCatalogoResponse crearMunicipio(String nombre, AdminCatalogoRequest request) {
        CatalogoEstado estado = estadoRepository.findById(request.getIdPadre())
            .orElseThrow(() -> new IllegalArgumentException("Selecciona un estado valido."));
        validarDuplicado(municipioRepository.findAllByOrderByNombreAsc().stream()
            .filter(m -> m.getEstado().getIdEstado().equals(estado.getIdEstado()))
            .map(CatalogoMunicipio::getNombre).toList(), nombre, "municipio/alcaldía");
        CatalogoMunicipio item = new CatalogoMunicipio();
        item.setEstado(estado);
        item.setNombre(nombre);
        item.setZonaCobertura(limpiarOpcional(request.getExtra(), "Cobertura administrada"));
        item.setActivo(true);
        return convertir(municipioRepository.save(item));
    }

    private AdminCatalogoResponse crearColonia(String nombre, AdminCatalogoRequest request) {
        CatalogoMunicipio municipio = municipioRepository.findById(request.getIdPadre())
            .orElseThrow(() -> new IllegalArgumentException("Selecciona un municipio o alcaldia valido."));
        validarDuplicado(coloniaRepository.findAllByOrderByNombreAsc().stream()
            .filter(c -> c.getMunicipio().getIdMunicipio().equals(municipio.getIdMunicipio()))
            .map(CatalogoColonia::getNombre).toList(), nombre, "colonia");
        String cp = limpiarOpcional(request.getExtra(), null);
        if (cp != null && !cp.matches("\\d{5}")) {
            throw new IllegalArgumentException("El código postal debe tener 5 dígitos.");
        }
        CatalogoColonia item = new CatalogoColonia();
        item.setMunicipio(municipio);
        item.setNombre(nombre);
        item.setCodigoPostal(cp);
        item.setActivo(true);
        return convertir(coloniaRepository.save(item));
    }

    private AdminCatalogoResponse convertir(CatalogoRaza i) { return respuesta(i.getIdRaza(), "raza", i.getNombre(), i.getActivo(), true, "MASCOTA.id_raza", i.getTipoMascota().getIdTipoMascota(), i.getTipoMascota().getNombre(), null); }
    private AdminCatalogoResponse convertir(CatalogoColor i) { return respuesta(i.getIdColor(), "color", i.getNombre(), i.getActivo(), true, "MASCOTA.id_color", null, null, null); }
    private AdminCatalogoResponse convertir(CatalogoEstado i) { return respuesta(i.getIdEstado(), "estado", i.getNombre(), i.getActivo(), true, "REPORTE/AVISTAMIENTO.estado", null, null, null); }
    private AdminCatalogoResponse convertir(CatalogoMunicipio i) { return respuesta(i.getIdMunicipio(), "municipio", i.getNombre(), i.getActivo(), true, "CATALOGO_MUNICIPIO.id_estado", i.getEstado().getIdEstado(), i.getEstado().getNombre(), i.getZonaCobertura()); }
    private AdminCatalogoResponse convertir(CatalogoColonia i) { return respuesta(i.getIdColonia(), "colonia", i.getNombre(), i.getActivo(), true, "CATALOGO_COLONIA.id_municipio", i.getMunicipio().getIdMunicipio(), i.getMunicipio().getNombre(), i.getCodigoPostal()); }

    private AdminCatalogoResponse respuesta(Integer id, String catalogo, String nombre, Boolean activo,
            Boolean editable, String relacion, Integer idPadre, String padre, String extra) {
        return new AdminCatalogoResponse(id, catalogo, nombre, activo, editable, relacion, idPadre, padre, extra);
    }

    private void validarDuplicado(List<String> existentes, String nombre, String catalogo) {
        String normalizado = normalizarTexto(nombre);
        String parecido = existentes.stream()
            .filter(existente -> esNombreParecido(normalizarTexto(existente), normalizado))
            .findFirst()
            .orElse(null);
        if (parecido != null) {
            throw new IllegalArgumentException("Revisa el registro: se parece a '" + parecido + "', que ya existe en " + catalogo + ".");
        }
    }

    private boolean esNombreParecido(String existente, String nuevo) {
        if (existente.equals(nuevo)) return true;
        if (nuevo.length() < 4 || Math.abs(existente.length() - nuevo.length()) > 2) return false;
        int limite = Math.max(existente.length(), nuevo.length()) >= 8 ? 2 : 1;
        return distanciaEdicion(existente, nuevo) <= limite;
    }

    private int distanciaEdicion(String a, String b) {
        int[][] matriz = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) matriz[i][0] = i;
        for (int j = 0; j <= b.length(); j++) matriz[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                matriz[i][j] = Math.min(
                    Math.min(matriz[i - 1][j] + 1, matriz[i][j - 1] + 1),
                    matriz[i - 1][j - 1] + costo
                );
            }
        }
        return matriz[a.length()][b.length()];
    }

    private String limpiarNombre(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Escribe un nombre para el catalogo.");
        }
        String limpio = valor.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 2 || limpio.length() > 120) {
            throw new IllegalArgumentException("El nombre debe tener entre 2 y 120 caracteres.");
        }
        if (!limpio.matches("[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9/ .'-]+")) {
            throw new IllegalArgumentException("El nombre contiene caracteres no permitidos.");
        }
        return limpio;
    }

    private String limpiarOpcional(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim().replaceAll("\\s+", " ");
    }

    private String normalizarClave(String valor) {
        return normalizarTexto(valor).replace(" ", "");
    }

    private String normalizarTexto(String valor) {
        if (valor == null) return "";
        String texto = Normalizer.normalize(valor.trim().replaceAll("\\s+", " ").toLowerCase(), Normalizer.Form.NFD);
        return texto.replaceAll("\\p{M}", "");
    }

    private void validarAdmin(HttpSession session) {
        if (!"ADMINISTRADOR".equals(session.getAttribute("rol"))) {
            throw new IllegalArgumentException("Acceso restringido a administradores");
        }
    }
}
