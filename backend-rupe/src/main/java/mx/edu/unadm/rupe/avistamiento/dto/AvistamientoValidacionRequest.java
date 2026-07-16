package mx.edu.unadm.rupe.avistamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AvistamientoValidacionRequest {

    @NotBlank(message = "Selecciona si la evidencia corresponde o no")
    private String decision;

    @Size(max = 500, message = "El comentario no debe superar 500 caracteres")
    private String comentario;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
