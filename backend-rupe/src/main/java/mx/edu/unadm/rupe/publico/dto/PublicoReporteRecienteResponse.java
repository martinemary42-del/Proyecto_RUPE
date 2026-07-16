package mx.edu.unadm.rupe.publico.dto;

import java.time.LocalDate;

public class PublicoReporteRecienteResponse {
    private Integer idReporte;
    private String folio;
    private String nombreMascota;
    private String razaMascota;
    private String senasParticulares;
    private String estatus;
    private String municipio;
    private String colonia;
    private LocalDate fechaExtravio;
    private String fotoPrincipalUrl;

    public PublicoReporteRecienteResponse(Integer idReporte, String folio, String nombreMascota,
            String razaMascota, String senasParticulares, String estatus, String municipio, String colonia,
            LocalDate fechaExtravio, String fotoPrincipalUrl) {
        this.idReporte = idReporte;
        this.folio = folio;
        this.nombreMascota = nombreMascota;
        this.razaMascota = razaMascota;
        this.senasParticulares = senasParticulares;
        this.estatus = estatus;
        this.municipio = municipio;
        this.colonia = colonia;
        this.fechaExtravio = fechaExtravio;
        this.fotoPrincipalUrl = fotoPrincipalUrl;
    }

    public Integer getIdReporte() { return idReporte; }
    public String getFolio() { return folio; }
    public String getNombreMascota() { return nombreMascota; }
    public String getRazaMascota() { return razaMascota; }
    public String getSenasParticulares() { return senasParticulares; }
    public String getEstatus() { return estatus; }
    public String getMunicipio() { return municipio; }
    public String getColonia() { return colonia; }
    public LocalDate getFechaExtravio() { return fechaExtravio; }
    public String getFotoPrincipalUrl() { return fotoPrincipalUrl; }
}
