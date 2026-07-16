package mx.edu.unadm.rupe.auth.dto;

public class RecuperacionPasswordResponse {
    private final String mensaje;
    private final String tokenDesarrollo;

    public RecuperacionPasswordResponse(String mensaje, String tokenDesarrollo) {
        this.mensaje = mensaje;
        this.tokenDesarrollo = tokenDesarrollo;
    }

    public String getMensaje() { return mensaje; }
    public String getTokenDesarrollo() { return tokenDesarrollo; }
}
