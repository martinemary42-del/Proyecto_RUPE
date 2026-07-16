package mx.edu.unadm.rupe.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RestablecerPasswordRequest {
    @NotBlank
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 40, message = "La contraseña debe tener entre 8 y 40 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
        message = "La contraseña debe incluir mayúscula, minúscula, número y carácter especial")
    private String password;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
