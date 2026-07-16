package mx.edu.unadm.rupe.config;

import java.util.List;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColor;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstado;
import mx.edu.unadm.rupe.catalogo.model.CatalogoEstatus;
import mx.edu.unadm.rupe.catalogo.model.CatalogoMunicipio;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRaza;
import mx.edu.unadm.rupe.catalogo.model.CatalogoRol;
import mx.edu.unadm.rupe.catalogo.model.CatalogoTipoMascota;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColorRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstadoRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstatusRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoMunicipioRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRazaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRolRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoTipoMascotaRepository;
import mx.edu.unadm.rupe.usuario.model.Usuario;
import mx.edu.unadm.rupe.usuario.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner inicializarDatosBase(CatalogoRolRepository rolRepository,
            CatalogoTipoMascotaRepository tipoRepository,
            CatalogoRazaRepository razaRepository,
            CatalogoColorRepository colorRepository,
            CatalogoEstadoRepository estadoRepository, CatalogoEstatusRepository estatusRepository,
            CatalogoMunicipioRepository municipioRepository,
            CatalogoColoniaRepository coloniaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            crearRolSiNoExiste(rolRepository, "ADMINISTRADOR", "Gestiona usuarios, reportes, catalogos y estadisticas");
            crearRolSiNoExiste(rolRepository, "DUENO", "Registra perritos y reportes de extravio");
            crearRolSiNoExiste(rolRepository, "CIUDADANO", "Reporta avistamientos o resguardos");
            crearAdministradorInicial(rolRepository, usuarioRepository, passwordEncoder);

            CatalogoTipoMascota perro = crearTipoSiNoExiste(tipoRepository, "Perro");
            for (String raza : List.of("Criollo/Mestizo", "Labrador", "Golden Retriever", "Chihuahua", "Poodle",
                    "Schnauzer", "Pastor Aleman", "Pitbull", "Husky", "Otro")) {
                crearRazaSiNoExiste(razaRepository, perro, raza);
            }

            for (String color : List.of("Negro", "Blanco", "Cafe", "Gris", "Dorado", "Pinto/Manchado",
                    "Atigrado", "Otro")) {
                crearColorSiNoExiste(colorRepository, color);
            }

            for (String estatus : List.of("ACTIVO", "PENDIENTE", "VALIDADO", "RECUPERADO", "DESCARTADO", "CERRADO")) {
                crearEstatusSiNoExiste(estatusRepository, estatus);
            }

            CatalogoEstado cdmx = crearEstadoSiNoExiste(estadoRepository, "Ciudad de Mexico");
            CatalogoEstado edomex = crearEstadoSiNoExiste(estadoRepository, "Estado de Mexico");

            CatalogoMunicipio coyoacan = crearMunicipioSiNoExiste(municipioRepository, cdmx, "Coyoacan", "CDMX completa");
            CatalogoMunicipio benito = crearMunicipioSiNoExiste(municipioRepository, cdmx, "Benito Juarez", "CDMX completa");
            CatalogoMunicipio iztapalapa = crearMunicipioSiNoExiste(municipioRepository, cdmx, "Iztapalapa", "CDMX completa");
            CatalogoMunicipio tlalpan = crearMunicipioSiNoExiste(municipioRepository, cdmx, "Tlalpan", "CDMX completa");
            CatalogoMunicipio naucalpan = crearMunicipioSiNoExiste(municipioRepository, edomex, "Naucalpan", "Zona conurbada");
            CatalogoMunicipio ecatepec = crearMunicipioSiNoExiste(municipioRepository, edomex, "Ecatepec", "Zona conurbada");
            CatalogoMunicipio neza = crearMunicipioSiNoExiste(municipioRepository, edomex, "Nezahualcoyotl", "Zona conurbada");

            crearColoniaSiNoExiste(coloniaRepository, coyoacan, "Del Carmen", "04100");
            crearColoniaSiNoExiste(coloniaRepository, coyoacan, "Copilco Universidad", "04360");
            crearColoniaSiNoExiste(coloniaRepository, coyoacan, "Pedregal de Santo Domingo", "04369");
            crearColoniaSiNoExiste(coloniaRepository, benito, "Narvarte", "03020");
            crearColoniaSiNoExiste(coloniaRepository, benito, "Del Valle Centro", "03100");
            crearColoniaSiNoExiste(coloniaRepository, benito, "Portales Norte", "03303");
            crearColoniaSiNoExiste(coloniaRepository, iztapalapa, "Santa Cruz Meyehualco", "09290");
            crearColoniaSiNoExiste(coloniaRepository, iztapalapa, "San Lorenzo Tezonco", "09790");
            crearColoniaSiNoExiste(coloniaRepository, iztapalapa, "Lomas Estrella", "09890");
            crearColoniaSiNoExiste(coloniaRepository, tlalpan, "Centro de Tlalpan", "14000");
            crearColoniaSiNoExiste(coloniaRepository, tlalpan, "San Miguel Topilejo", "14500");
            crearColoniaSiNoExiste(coloniaRepository, tlalpan, "Pedregal de San Nicolas", "14100");
            crearColoniaSiNoExiste(coloniaRepository, naucalpan, "Ciudad Satelite", "53100");
            crearColoniaSiNoExiste(coloniaRepository, naucalpan, "Echegaray", "53300");
            crearColoniaSiNoExiste(coloniaRepository, naucalpan, "Lomas Verdes", "53120");
            crearColoniaSiNoExiste(coloniaRepository, ecatepec, "San Cristobal Centro", "55000");
            crearColoniaSiNoExiste(coloniaRepository, ecatepec, "Ciudad Azteca", "55120");
            crearColoniaSiNoExiste(coloniaRepository, ecatepec, "Jardines de Morelos", "55070");
            crearColoniaSiNoExiste(coloniaRepository, neza, "Benito Juarez", "57000");
            crearColoniaSiNoExiste(coloniaRepository, neza, "Las Aguilas", "57900");
            crearColoniaSiNoExiste(coloniaRepository, neza, "Impulsora", "57130");
        };
    }

    private void crearRolSiNoExiste(CatalogoRolRepository rolRepository, String nombre, String descripcion) {
        rolRepository.findByNombre(nombre).orElseGet(() -> {
            CatalogoRol rol = new CatalogoRol();
            rol.setNombre(nombre);
            rol.setDescripcion(descripcion);
            rol.setActivo(true);
            return rolRepository.save(rol);
        });
    }

    private void crearAdministradorInicial(CatalogoRolRepository rolRepository, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        String correoAdmin = "admin@rupe.local";
        if (usuarioRepository.existsByCorreoIgnoreCase(correoAdmin)) {
            return;
        }
        CatalogoRol rolAdmin = rolRepository.findByNombre("ADMINISTRADOR")
            .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR"));
        Usuario admin = new Usuario();
        admin.setRol(rolAdmin);
        admin.setNombreCompleto("Administrador RUPE");
        admin.setCorreo(correoAdmin);
        admin.setTelefono("5500000000");
        admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
        usuarioRepository.save(admin);
    }

    private CatalogoTipoMascota crearTipoSiNoExiste(CatalogoTipoMascotaRepository tipoRepository, String nombre) {
        return tipoRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(item -> item.getNombre().equalsIgnoreCase(nombre))
            .findFirst()
            .orElseGet(() -> {
                CatalogoTipoMascota item = new CatalogoTipoMascota();
                item.setNombre(nombre);
                return tipoRepository.save(item);
            });
    }

    private void crearRazaSiNoExiste(CatalogoRazaRepository razaRepository, CatalogoTipoMascota tipo, String nombre) {
        boolean existe = razaRepository.findByActivoTrueOrderByNombreAsc().stream()
            .anyMatch(item -> item.getNombre().equalsIgnoreCase(nombre));
        if (!existe) {
            CatalogoRaza item = new CatalogoRaza();
            item.setTipoMascota(tipo);
            item.setNombre(nombre);
            razaRepository.save(item);
        }
    }

    private void crearColorSiNoExiste(CatalogoColorRepository colorRepository, String nombre) {
        boolean existe = colorRepository.findByActivoTrueOrderByNombreAsc().stream()
            .anyMatch(item -> item.getNombre().equalsIgnoreCase(nombre));
        if (!existe) {
            CatalogoColor item = new CatalogoColor();
            item.setNombre(nombre);
            colorRepository.save(item);
        }
    }

    private void crearEstatusSiNoExiste(CatalogoEstatusRepository estatusRepository, String nombre) {
        estatusRepository.findByNombre(nombre).orElseGet(() -> {
            CatalogoEstatus item = new CatalogoEstatus();
            item.setNombre(nombre);
            item.setDescripcion("Estatus " + nombre.toLowerCase());
            return estatusRepository.save(item);
        });
    }

    private CatalogoEstado crearEstadoSiNoExiste(CatalogoEstadoRepository estadoRepository, String nombre) {
        return estadoRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(item -> item.getNombre().equalsIgnoreCase(nombre))
            .findFirst()
            .orElseGet(() -> {
                CatalogoEstado item = new CatalogoEstado();
                item.setNombre(nombre);
                return estadoRepository.save(item);
            });
    }

    private CatalogoMunicipio crearMunicipioSiNoExiste(CatalogoMunicipioRepository municipioRepository,
            CatalogoEstado estado, String nombre, String zona) {
        return municipioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(item -> item.getEstado().getIdEstado().equals(estado.getIdEstado())
                && item.getNombre().equalsIgnoreCase(nombre))
            .findFirst()
            .orElseGet(() -> {
                CatalogoMunicipio item = new CatalogoMunicipio();
                item.setEstado(estado);
                item.setNombre(nombre);
                item.setZonaCobertura(zona);
                return municipioRepository.save(item);
            });
    }

    private void crearColoniaSiNoExiste(CatalogoColoniaRepository coloniaRepository, CatalogoMunicipio municipio,
            String nombre, String cp) {
        boolean existe = coloniaRepository.findByActivoTrueOrderByNombreAsc().stream()
            .anyMatch(item -> item.getMunicipio().getIdMunicipio().equals(municipio.getIdMunicipio())
                && item.getNombre().equalsIgnoreCase(nombre));
        if (!existe) {
            CatalogoColonia item = new CatalogoColonia();
            item.setMunicipio(municipio);
            item.setNombre(nombre);
            item.setCodigoPostal(cp);
            coloniaRepository.save(item);
        }
    }
}
