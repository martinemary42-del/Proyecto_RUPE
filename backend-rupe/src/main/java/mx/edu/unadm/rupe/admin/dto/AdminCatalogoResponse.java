package mx.edu.unadm.rupe.admin.dto;

public class AdminCatalogoResponse {
    private final Integer id;
    private final String catalogo;
    private final String nombre;
    private final Boolean activo;
    private final Boolean editable;
    private final String relacion;
    private final Integer idPadre;
    private final String padre;
    private final String extra;

    public AdminCatalogoResponse(Integer id, String catalogo, String nombre, Boolean activo,
            Boolean editable, String relacion, Integer idPadre, String padre, String extra) {
        this.id = id;
        this.catalogo = catalogo;
        this.nombre = nombre;
        this.activo = activo;
        this.editable = editable;
        this.relacion = relacion;
        this.idPadre = idPadre;
        this.padre = padre;
        this.extra = extra;
    }

    public Integer getId() { return id; }
    public String getCatalogo() { return catalogo; }
    public String getNombre() { return nombre; }
    public Boolean getActivo() { return activo; }
    public Boolean getEditable() { return editable; }
    public String getRelacion() { return relacion; }
    public Integer getIdPadre() { return idPadre; }
    public String getPadre() { return padre; }
    public String getExtra() { return extra; }
}
