package mx.edu.unadm.rupe.usuario.dto;

import mx.edu.unadm.rupe.usuario.model.Usuario;

public class PerfilResponse {
    private Integer idUsuario;
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private String rol;

    public PerfilResponse(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.correo = usuario.getCorreo();
        this.telefono = usuario.getTelefono();
        this.rol = usuario.getRol().getNombre();
    }

    public Integer getIdUsuario() { return idUsuario; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getRol() { return rol; }
}
