package mx.edu.unadm.rupe.contacto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MensajeContactoRequest {
    @NotBlank(message = "Escribe tu nombre completo.")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Escribe tu correo electronico.")
    @Email(message = "El correo no tiene un formato valido.")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{3,}$",
        message = "Captura un correo electronico valido. Ejemplo: nombre@dominio.com")
    @Size(max = 120, message = "El correo no debe superar 120 caracteres.")
    private String correo;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "El telefono debe tener 10 digitos.")
    private String telefono;

    @NotBlank(message = "Selecciona un asunto.")
    @Size(max = 60, message = "El asunto no debe superar 60 caracteres.")
    private String asunto;

    @NotBlank(message = "Escribe un mensaje.")
    @Size(max = 500, message = "El mensaje no debe superar 500 caracteres.")
    private String mensaje;

    private String turnstileToken;

    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public String getTurnstileToken() { return turnstileToken; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public void setTurnstileToken(String turnstileToken) { this.turnstileToken = turnstileToken; }
}
