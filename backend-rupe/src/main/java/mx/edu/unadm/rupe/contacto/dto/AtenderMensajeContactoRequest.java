package mx.edu.unadm.rupe.contacto.dto;

import jakarta.validation.constraints.Size;

public class AtenderMensajeContactoRequest {
    @Size(max = 500, message = "La respuesta no debe superar 500 caracteres.")
    private String respuestaAdmin;

    public String getRespuestaAdmin() { return respuestaAdmin; }
    public void setRespuestaAdmin(String respuestaAdmin) { this.respuestaAdmin = respuestaAdmin; }
}
