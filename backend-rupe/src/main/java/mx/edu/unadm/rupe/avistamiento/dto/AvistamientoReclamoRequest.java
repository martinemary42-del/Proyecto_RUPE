package mx.edu.unadm.rupe.avistamiento.dto;

import jakarta.validation.constraints.NotNull;

public class AvistamientoReclamoRequest {
    @NotNull
    private Integer idReporte;

    public Integer getIdReporte() { return idReporte; }
    public void setIdReporte(Integer idReporte) { this.idReporte = idReporte; }
}
