package mx.edu.unadm.rupe.contacto.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje_contacto")
public class MensajeContacto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje_contacto")
    private Integer idMensajeContacto;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 60)
    private String asunto;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false, length = 30)
    private String estatus = "PENDIENTE";

    @Column(length = 80)
    private String ip;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    @Column(name = "respuesta_admin", length = 500)
    private String respuestaAdmin;

    public Integer getIdMensajeContacto() { return idMensajeContacto; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public String getEstatus() { return estatus; }
    public String getIp() { return ip; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getFechaAtencion() { return fechaAtencion; }
    public String getRespuestaAdmin() { return respuestaAdmin; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public void setEstatus(String estatus) { this.estatus = estatus; }
    public void setIp(String ip) { this.ip = ip; }
    public void setFechaAtencion(LocalDateTime fechaAtencion) { this.fechaAtencion = fechaAtencion; }
    public void setRespuestaAdmin(String respuestaAdmin) { this.respuestaAdmin = respuestaAdmin; }
}
