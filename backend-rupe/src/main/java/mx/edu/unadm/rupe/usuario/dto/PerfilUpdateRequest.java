package mx.edu.unadm.rupe.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PerfilUpdateRequest {
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120, message = "El nombre no debe superar 120 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo valido")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.(com|com\\.mx|mx|org|net|edu|gob\\.mx)$", message = "Ingresa un correo valido")
    @Size(max = 120, message = "El correo no debe superar 120 caracteres")
    private String correo;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "El telefono debe tener 10 digitos")
    private String telefono;

    private String passwordActual;
    private String passwordNueva;

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getPasswordActual() { return passwordActual; }
    public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }
    public String getPasswordNueva() { return passwordNueva; }
    public void setPasswordNueva(String passwordNueva) { this.passwordNueva = passwordNueva; }
}
