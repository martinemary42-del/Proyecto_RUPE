package mx.edu.unadm.rupe.admin.dto;

public class AdminCatalogoRequest {
    private String nombre;
    private Integer idPadre;
    private String extra;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getIdPadre() { return idPadre; }
    public void setIdPadre(Integer idPadre) { this.idPadre = idPadre; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}
