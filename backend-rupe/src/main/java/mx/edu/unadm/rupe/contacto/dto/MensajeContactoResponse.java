package mx.edu.unadm.rupe.contacto.dto;

import java.time.LocalDateTime;
import mx.edu.unadm.rupe.contacto.model.MensajeContacto;

public class MensajeContactoResponse {
    private final Integer idMensajeContacto;
    private final String nombre;
    private final String correo;
    private final String telefono;
    private final String asunto;
    private final String mensaje;
    private final String estatus;
    private final String respuestaAdmin;
    private final LocalDateTime fechaRegistro;
    private final LocalDateTime fechaAtencion;

    public MensajeContactoResponse(MensajeContacto mensajeContacto) {
        this.idMensajeContacto = mensajeContacto.getIdMensajeContacto();
        this.nombre = mensajeContacto.getNombre();
        this.correo = mensajeContacto.getCorreo();
        this.telefono = mensajeContacto.getTelefono();
        this.asunto = mensajeContacto.getAsunto();
        this.mensaje = mensajeContacto.getMensaje();
        this.estatus = mensajeContacto.getEstatus();
        this.respuestaAdmin = mensajeContacto.getRespuestaAdmin();
        this.fechaRegistro = mensajeContacto.getFechaRegistro();
        this.fechaAtencion = mensajeContacto.getFechaAtencion();
    }

    public Integer getIdMensajeContacto() { return idMensajeContacto; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public String getEstatus() { return estatus; }
    public String getRespuestaAdmin() { return respuestaAdmin; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getFechaAtencion() { return fechaAtencion; }
}
