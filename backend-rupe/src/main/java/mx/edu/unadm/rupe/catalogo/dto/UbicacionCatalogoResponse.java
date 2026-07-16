package mx.edu.unadm.rupe.catalogo.dto;

import java.util.List;

public record UbicacionCatalogoResponse(List<EstadoResponse> estados) {
    public record EstadoResponse(Integer id, String nombre, List<MunicipioResponse> municipios) {}
    public record MunicipioResponse(Integer id, String nombre, String zonaCobertura, List<ColoniaResponse> colonias) {}
    public record ColoniaResponse(Integer id, String nombre, String codigoPostal) {}
}
