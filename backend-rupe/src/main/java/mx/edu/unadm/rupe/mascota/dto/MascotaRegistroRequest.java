package mx.edu.unadm.rupe.mascota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MascotaRegistroRequest {

    @NotBlank
    @Size(max = 80, message = "El nombre no debe superar 80 caracteres.")
    private String nombre;
    @NotBlank
    private String sexo;
    @NotBlank
    private String idTipoMascota;
    private String edad;
    @NotBlank
    private String idRaza;
    private String mezcla;
    @NotBlank
    private String idColor;
    @Size(max = 120, message = "La descripcion de color no debe superar 120 caracteres.")
    private String descripcionColor;
    @NotBlank
    @Size(max = 500, message = "Las señas particulares no deben superar 500 caracteres.")
    private String senas;
    @Size(max = 500, message = "La condicion medica no debe superar 500 caracteres.")
    private String condicionMedica;
    @NotBlank
    private String collarPlaca;
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
    private Boolean confirmarDuplicado = false;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getIdTipoMascota() { return idTipoMascota; }
    public void setIdTipoMascota(String idTipoMascota) { this.idTipoMascota = idTipoMascota; }
    public String getEdad() { return edad; }
    public void setEdad(String edad) { this.edad = edad; }
    public String getIdRaza() { return idRaza; }
    public void setIdRaza(String idRaza) { this.idRaza = idRaza; }
    public String getMezcla() { return mezcla; }
    public void setMezcla(String mezcla) { this.mezcla = mezcla; }
    public String getIdColor() { return idColor; }
    public void setIdColor(String idColor) { this.idColor = idColor; }
    public String getDescripcionColor() { return descripcionColor; }
    public void setDescripcionColor(String descripcionColor) { this.descripcionColor = descripcionColor; }
    public String getSenas() { return senas; }
    public void setSenas(String senas) { this.senas = senas; }
    public String getCondicionMedica() { return condicionMedica; }
    public void setCondicionMedica(String condicionMedica) { this.condicionMedica = condicionMedica; }
    public String getCollarPlaca() { return collarPlaca; }
    public void setCollarPlaca(String collarPlaca) { this.collarPlaca = collarPlaca; }
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
    public Boolean getConfirmarDuplicado() { return confirmarDuplicado; }
    public void setConfirmarDuplicado(Boolean confirmarDuplicado) { this.confirmarDuplicado = confirmarDuplicado; }
}
