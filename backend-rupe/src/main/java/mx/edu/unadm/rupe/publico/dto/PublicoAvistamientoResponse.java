package mx.edu.unadm.rupe.publico.dto;

import java.time.LocalDate;

public class PublicoAvistamientoResponse {
    private Integer idAvistamiento;
    private String folioAvistamiento;
    private LocalDate fechaAvistamiento;
    private String municipio;
    private String colonia;
    private String referencias;
    private String descripcion;
    private Boolean resguardado;
    private String fotoAvistamientoUrl;

    public PublicoAvistamientoResponse(Integer idAvistamiento, String folioAvistamiento, LocalDate fechaAvistamiento,
            String municipio, String colonia, String referencias, String descripcion,
            Boolean resguardado, String fotoAvistamientoUrl) {
        this.idAvistamiento = idAvistamiento;
        this.folioAvistamiento = folioAvistamiento;
        this.fechaAvistamiento = fechaAvistamiento;
        this.municipio = municipio;
        this.colonia = colonia;
        this.referencias = referencias;
        this.descripcion = descripcion;
        this.resguardado = resguardado;
        this.fotoAvistamientoUrl = fotoAvistamientoUrl;
    }

    public Integer getIdAvistamiento() { return idAvistamiento; }
    public String getFolioAvistamiento() { return folioAvistamiento; }
    public LocalDate getFechaAvistamiento() { return fechaAvistamiento; }
    public String getMunicipio() { return municipio; }
    public String getColonia() { return colonia; }
    public String getReferencias() { return referencias; }
    public String getDescripcion() { return descripcion; }
    public Boolean getResguardado() { return resguardado; }
    public String getFotoAvistamientoUrl() { return fotoAvistamientoUrl; }
}
