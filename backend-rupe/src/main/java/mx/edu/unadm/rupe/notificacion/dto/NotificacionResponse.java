package mx.edu.unadm.rupe.notificacion.dto;

import java.time.LocalDateTime;
import mx.edu.unadm.rupe.notificacion.model.Notificacion;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;

public class NotificacionResponse {
    private Integer idNotificacion;
    private Integer idReporte;
    private String folio;
    private String tipo;
    private String asunto;
    private String mensaje;
    private String estatus;
    private LocalDateTime fechaEnvio;

    public NotificacionResponse(Notificacion notificacion) {
        ReporteExtravio reporte = notificacion.getReporte();
        this.idNotificacion = notificacion.getIdNotificacion();
        this.idReporte = reporte.getIdReporte();
        this.folio = reporte.getFolio();
        this.tipo = notificacion.getTipo();
        this.asunto = notificacion.getAsunto();
        this.mensaje = notificacion.getMensaje();
        this.estatus = notificacion.getEstatusEnvio();
        this.fechaEnvio = notificacion.getFechaEnvio();
    }

    public Integer getIdNotificacion() { return idNotificacion; }
    public Integer getIdReporte() { return idReporte; }
    public String getFolio() { return folio; }
    public String getTipo() { return tipo; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public String getEstatus() { return estatus; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
}
