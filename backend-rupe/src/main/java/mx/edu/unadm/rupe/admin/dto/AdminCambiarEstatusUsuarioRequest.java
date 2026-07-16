package mx.edu.unadm.rupe.admin.dto;

public class AdminCambiarEstatusUsuarioRequest {
    private Boolean activo;
    private String motivo;

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
