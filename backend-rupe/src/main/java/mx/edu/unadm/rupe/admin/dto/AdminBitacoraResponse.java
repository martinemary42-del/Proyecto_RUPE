package mx.edu.unadm.rupe.admin.dto;

import java.time.LocalDateTime;
import mx.edu.unadm.rupe.security.model.BitacoraSeguridad;

public class AdminBitacoraResponse {
    private final Integer idBitacoraSeguridad;
    private final Integer idUsuario;
    private final String correo;
    private final String accion;
    private final String modulo;
    private final String resultado;
    private final String ip;
    private final String descripcion;
    private final LocalDateTime fechaHora;

    public AdminBitacoraResponse(BitacoraSeguridad bitacora) {
        this.idBitacoraSeguridad = bitacora.getIdBitacoraSeguridad();
        this.idUsuario = bitacora.getIdUsuario();
        this.correo = enmascararCorreo(bitacora.getCorreo());
        this.accion = bitacora.getAccion();
        this.modulo = bitacora.getModulo();
        this.resultado = bitacora.getResultado();
        this.ip = bitacora.getIp();
        this.descripcion = bitacora.getDescripcion();
        this.fechaHora = bitacora.getFechaHora();
    }

    private String enmascararCorreo(String correo) {
        if (correo == null || correo.isBlank()) return "sistema";
        int arroba = correo.indexOf('@');
        if (arroba <= 1) return "***" + (arroba >= 0 ? correo.substring(arroba) : "");
        return correo.substring(0, Math.min(2, arroba)) + "***" + correo.substring(arroba);
    }

    public Integer getIdBitacoraSeguridad() { return idBitacoraSeguridad; }
    public Integer getIdUsuario() { return idUsuario; }
    public String getCorreo() { return correo; }
    public String getAccion() { return accion; }
    public String getModulo() { return modulo; }
    public String getResultado() { return resultado; }
    public String getIp() { return ip; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
