package mx.edu.unadm.rupe.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_seguridad")
public class BitacoraSeguridad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora_seguridad")
    private Integer idBitacoraSeguridad;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(length = 120)
    private String correo;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(nullable = false, length = 60)
    private String modulo;

    @Column(nullable = false, length = 30)
    private String resultado;

    @Column(length = 80)
    private String ip;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    public Integer getIdBitacoraSeguridad() { return idBitacoraSeguridad; }
    public Integer getIdUsuario() { return idUsuario; }
    public String getCorreo() { return correo; }
    public String getAccion() { return accion; }
    public String getModulo() { return modulo; }
    public String getResultado() { return resultado; }
    public String getIp() { return ip; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFechaHora() { return fechaHora; }

    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setAccion(String accion) { this.accion = accion; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public void setIp(String ip) { this.ip = ip; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
