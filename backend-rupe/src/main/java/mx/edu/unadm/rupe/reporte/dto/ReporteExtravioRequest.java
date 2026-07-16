package mx.edu.unadm.rupe.reporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ReporteExtravioRequest {
    @NotNull
    private Integer idMascota;
    @NotNull
    private LocalDate fechaExtravio;
    @Pattern(regexp = "^$|^(EXTRAVIO|ROBO)$", message = "Selecciona un tipo de reporte valido.")
    private String tipoReporte = "EXTRAVIO";
    @NotBlank
    private String idEstado;
    @NotBlank
    private String idMunicipio;
    @NotBlank
    private String idColonia;
    @Pattern(regexp = "^$|^[0-9]{5}$", message = "El codigo postal debe tener 5 digitos.")
    private String cp;
    @Size(max = 120, message = "La calle no debe superar 120 caracteres.")
    private String calle;
    @Size(max = 20, message = "El numero no debe superar 20 caracteres.")
    private String numero;
    @NotBlank
    @Size(max = 500, message = "Las referencias no deben superar 500 caracteres.")
    private String referencias;
    @NotBlank
    @Size(max = 1000, message = "La descripcion no debe superar 1000 caracteres.")
    private String descripcion;

    public Integer getIdMascota() { return idMascota; }
    public void setIdMascota(Integer idMascota) { this.idMascota = idMascota; }
    public LocalDate getFechaExtravio() { return fechaExtravio; }
    public void setFechaExtravio(LocalDate fechaExtravio) { this.fechaExtravio = fechaExtravio; }
    public String getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(String tipoReporte) { this.tipoReporte = tipoReporte; }
    public String getIdEstado() { return idEstado; }
    public void setIdEstado(String idEstado) { this.idEstado = idEstado; }
    public String getIdMunicipio() { return idMunicipio; }
    public void setIdMunicipio(String idMunicipio) { this.idMunicipio = idMunicipio; }
    public String getIdColonia() { return idColonia; }
    public void setIdColonia(String idColonia) { this.idColonia = idColonia; }
    public String getCp() { return cp; }
    public void setCp(String cp) { this.cp = cp; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getReferencias() { return referencias; }
    public void setReferencias(String referencias) { this.referencias = referencias; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
