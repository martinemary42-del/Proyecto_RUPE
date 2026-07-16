package mx.edu.unadm.rupe.avistamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class AvistamientoRequest {
    private String folio;
    @NotNull
    private LocalDate fecha;
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
    @NotBlank(message = "Describe las senas visibles del perrito.")
    @Size(min = 10, max = 1000, message = "La descripcion debe tener entre 10 y 1000 caracteres.")
    private String descripcion;
    private String resguardado;
    @Size(max = 100, message = "El nombre de contacto no debe superar 100 caracteres.")
    private String nombreResguardante;
    @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$", message = "El correo de contacto no tiene formato valido.")
    private String correoResguardante;
    @Pattern(regexp = "^$|^[0-9]{10}$", message = "El telefono debe tener 10 digitos.")
    private String telefonoResguardante;
    private String turnstileToken;

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
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
    public String getResguardado() { return resguardado; }
    public void setResguardado(String resguardado) { this.resguardado = resguardado; }
    public String getNombreResguardante() { return nombreResguardante; }
    public void setNombreResguardante(String nombreResguardante) { this.nombreResguardante = nombreResguardante; }
    public String getCorreoResguardante() { return correoResguardante; }
    public void setCorreoResguardante(String correoResguardante) { this.correoResguardante = correoResguardante; }
    public String getTelefonoResguardante() { return telefonoResguardante; }
    public void setTelefonoResguardante(String telefonoResguardante) { this.telefonoResguardante = telefonoResguardante; }
    public String getTurnstileToken() { return turnstileToken; }
    public void setTurnstileToken(String turnstileToken) { this.turnstileToken = turnstileToken; }
}
