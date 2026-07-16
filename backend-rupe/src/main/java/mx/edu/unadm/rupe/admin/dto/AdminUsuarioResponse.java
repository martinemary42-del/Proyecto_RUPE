package mx.edu.unadm.rupe.admin.dto;

import java.time.LocalDateTime;
import mx.edu.unadm.rupe.usuario.model.Usuario;

public class AdminUsuarioResponse {
    private final Integer idUsuario;
    private final String nombreVisible;
    private final String correoEnmascarado;
    private final String rol;
    private final Boolean activo;
    private final Boolean bloqueado;
    private final LocalDateTime ultimoAcceso;
    private final LocalDateTime fechaRegistro;
    private final Boolean administradorPrincipal;

    public AdminUsuarioResponse(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.nombreVisible = construirNombreVisible(usuario.getNombreCompleto(), usuario.getIdUsuario());
        this.correoEnmascarado = enmascararCorreo(usuario.getCorreo());
        this.rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "SIN_ROL";
        this.activo = Boolean.TRUE.equals(usuario.getActivo());
        this.bloqueado = usuario.getFechaBloqueo() != null && usuario.getFechaBloqueo().isAfter(LocalDateTime.now());
        this.ultimoAcceso = usuario.getUltimoAcceso();
        this.fechaRegistro = usuario.getFechaRegistro();
        this.administradorPrincipal = "admin@rupe.local".equalsIgnoreCase(usuario.getCorreo());
    }

    private String construirNombreVisible(String nombre, Integer idUsuario) {
        if (nombre == null || nombre.isBlank()) {
            return "Usuario #" + idUsuario;
        }

        String[] partes = nombre.trim().split("\\s+");
        String iniciales = partes[0].substring(0, 1).toUpperCase();
        if (partes.length > 1) {
            iniciales += partes[1].substring(0, 1).toUpperCase();
        }
        return "Usuario #" + idUsuario + " (" + iniciales + ")";
    }

    private String enmascararCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return "sin-correo";
        }

        int arroba = correo.indexOf('@');
        if (arroba <= 1) {
            return "***" + (arroba >= 0 ? correo.substring(arroba) : "");
        }

        String inicio = correo.substring(0, Math.min(2, arroba));
        String dominio = arroba >= 0 ? correo.substring(arroba) : "";
        return inicio + "***" + dominio;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getCorreoEnmascarado() {
        return correoEnmascarado;
    }

    public String getRol() {
        return rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Boolean getBloqueado() {
        return bloqueado;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public Boolean getAdministradorPrincipal() {
        return administradorPrincipal;
    }
}
