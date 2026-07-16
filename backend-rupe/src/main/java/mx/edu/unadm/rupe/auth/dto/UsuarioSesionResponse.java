package mx.edu.unadm.rupe.auth.dto;

import mx.edu.unadm.rupe.usuario.model.Usuario;

public class UsuarioSesionResponse {

    private Integer idUsuario;
    private String nombreCompleto;
    private String correo;
    private String rol;

    public UsuarioSesionResponse(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.correo = usuario.getCorreo();
        this.rol = usuario.getRol().getNombre();
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }
}
