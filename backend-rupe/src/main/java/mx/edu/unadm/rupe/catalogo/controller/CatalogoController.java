package mx.edu.unadm.rupe.catalogo.controller;

import java.util.List;
import mx.edu.unadm.rupe.catalogo.dto.CatalogoSimpleResponse;
import mx.edu.unadm.rupe.catalogo.dto.UbicacionCatalogoResponse;
import mx.edu.unadm.rupe.catalogo.model.CatalogoColonia;
import mx.edu.unadm.rupe.catalogo.model.CatalogoMunicipio;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColorRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoColoniaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstadoRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoEstatusRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoMunicipioRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoRazaRepository;
import mx.edu.unadm.rupe.catalogo.repository.CatalogoTipoMascotaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final CatalogoTipoMascotaRepository tipoRepository;
    private final CatalogoRazaRepository razaRepository;
    private final CatalogoColorRepository colorRepository;
    private final CatalogoEstadoRepository estadoRepository;
    private final CatalogoEstatusRepository estatusRepository;
    private final CatalogoMunicipioRepository municipioRepository;
    private final CatalogoColoniaRepository coloniaRepository;

    public CatalogoController(CatalogoTipoMascotaRepository tipoRepository, CatalogoRazaRepository razaRepository,
            CatalogoColorRepository colorRepository, CatalogoEstadoRepository estadoRepository, CatalogoEstatusRepository estatusRepository,
            CatalogoMunicipioRepository municipioRepository, CatalogoColoniaRepository coloniaRepository) {
        this.tipoRepository = tipoRepository;
        this.razaRepository = razaRepository;
        this.colorRepository = colorRepository;
        this.estadoRepository = estadoRepository;
        this.estatusRepository = estatusRepository;
        this.municipioRepository = municipioRepository;
        this.coloniaRepository = coloniaRepository;
    }

    @GetMapping("/tipos-mascota")
    public List<CatalogoSimpleResponse> tiposMascota() {
        return tipoRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(item -> new CatalogoSimpleResponse(item.getIdTipoMascota(), item.getNombre()))
            .toList();
    }

    @GetMapping("/razas")
    public List<CatalogoSimpleResponse> razas() {
        return razaRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(item -> new CatalogoSimpleResponse(item.getIdRaza(), item.getNombre()))
            .toList();
    }

    @GetMapping("/colores")
    public List<CatalogoSimpleResponse> colores() {
        return colorRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(item -> new CatalogoSimpleResponse(item.getIdColor(), item.getNombre()))
            .toList();
    }

    @GetMapping("/estatus")
    public List<CatalogoSimpleResponse> estatus() {
        return estatusRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(item -> new CatalogoSimpleResponse(item.getIdEstatus(), item.getNombre()))
            .toList();
    }

    @GetMapping("/ubicaciones")
    public UbicacionCatalogoResponse ubicaciones() {
        List<CatalogoMunicipio> municipios = municipioRepository.findByActivoTrueOrderByNombreAsc();
        List<CatalogoColonia> colonias = coloniaRepository.findByActivoTrueOrderByNombreAsc();

        List<UbicacionCatalogoResponse.EstadoResponse> estados = estadoRepository.findByActivoTrueOrderByNombreAsc()
            .stream()
            .map(estado -> new UbicacionCatalogoResponse.EstadoResponse(
                estado.getIdEstado(),
                estado.getNombre(),
                municipios.stream()
                    .filter(municipio -> municipio.getEstado().getIdEstado().equals(estado.getIdEstado()))
                    .map(municipio -> new UbicacionCatalogoResponse.MunicipioResponse(
                        municipio.getIdMunicipio(),
                        municipio.getNombre(),
                        municipio.getZonaCobertura(),
                        colonias.stream()
                            .filter(colonia -> colonia.getMunicipio().getIdMunicipio().equals(municipio.getIdMunicipio()))
                            .map(colonia -> new UbicacionCatalogoResponse.ColoniaResponse(
                                colonia.getIdColonia(),
                                colonia.getNombre(),
                                colonia.getCodigoPostal()))
                            .toList()))
                    .toList()))
            .toList();

        return new UbicacionCatalogoResponse(estados);
    }
}
