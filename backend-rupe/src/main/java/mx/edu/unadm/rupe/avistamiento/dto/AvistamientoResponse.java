package mx.edu.unadm.rupe.avistamiento.dto;

import java.time.LocalDate;
import mx.edu.unadm.rupe.avistamiento.model.Avistamiento;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;

public class AvistamientoResponse {
    private Integer idAvistamiento;
    private Integer idReporte;
    private String folioAvistamiento;
    private String folio;
    private String nombreMascota;
    private String estatus;
    private Boolean vinculadoReporte;
    private LocalDate fechaAvistamiento;
    private String estado;
    private String municipio;
    private String colonia;
    private String codigoPostal;
    private String referencias;
    private String descripcion;
    private String fotoMascotaUrl;
    private String fotoAvistamientoUrl;
    private Boolean resguardado;
    private Boolean validadoDueno;
    private String comentarioValidacion;
    private String nombreResguardante;
    private String correoResguardante;
    private String telefonoResguardante;
    private String mensaje;

    public AvistamientoResponse(Avistamiento avistamiento, String mensaje) {
        this(avistamiento, mensaje, null);
    }

    public AvistamientoResponse(Avistamiento avistamiento, String mensaje, String fotoMascotaUrl) {
        ReporteExtravio reporte = avistamiento.getReporte();
        this.idAvistamiento = avistamiento.getIdAvistamiento();
        this.idReporte = reporte != null ? reporte.getIdReporte() : null;
        this.folioAvistamiento = resolverFolioAvistamiento(avistamiento);
        this.folio = reporte != null ? reporte.getFolio() : null;
        this.nombreMascota = reporte != null ? reporte.getMascota().getNombre() : null;
        this.estatus = avistamiento.getEstatus().getNombre();
        this.vinculadoReporte = reporte != null;
        this.fechaAvistamiento = avistamiento.getFechaAvistamiento();
        this.estado = avistamiento.getEstado();
        this.municipio = avistamiento.getMunicipio();
        this.colonia = avistamiento.getColonia();
        this.codigoPostal = avistamiento.getCodigoPostal();
        this.referencias = avistamiento.getReferencias();
        this.descripcion = avistamiento.getDescripcion();
        this.fotoMascotaUrl = fotoMascotaUrl;
        this.fotoAvistamientoUrl = avistamiento.getFotoAvistamiento();
        this.resguardado = avistamiento.getResguardado();
        this.validadoDueno = avistamiento.getValidadoDueno();
        this.comentarioValidacion = avistamiento.getComentarioValidacion();
        if (Boolean.TRUE.equals(avistamiento.getResguardado()) && Boolean.TRUE.equals(avistamiento.getValidadoDueno())) {
            this.nombreResguardante = avistamiento.getNombreResguardante();
            this.correoResguardante = avistamiento.getCorreoResguardante();
            this.telefonoResguardante = avistamiento.getTelefonoResguardante();
        }
        this.mensaje = mensaje;
    }

    public Integer getIdAvistamiento() { return idAvistamiento; }
    public Integer getIdReporte() { return idReporte; }
    public String getFolioAvistamiento() { return folioAvistamiento; }
    public String getFolio() { return folio; }
    public String getNombreMascota() { return nombreMascota; }
    public String getEstatus() { return estatus; }
    public Boolean getVinculadoReporte() { return vinculadoReporte; }
    public LocalDate getFechaAvistamiento() { return fechaAvistamiento; }
    public String getEstado() { return estado; }
    public String getMunicipio() { return municipio; }
    public String getColonia() { return colonia; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getReferencias() { return referencias; }
    public String getDescripcion() { return descripcion; }
    public String getFotoMascotaUrl() { return fotoMascotaUrl; }
    public String getFotoAvistamientoUrl() { return fotoAvistamientoUrl; }
    public Boolean getResguardado() { return resguardado; }
    public Boolean getValidadoDueno() { return validadoDueno; }
    public String getComentarioValidacion() { return comentarioValidacion; }
    public String getNombreResguardante() { return nombreResguardante; }
    public String getCorreoResguardante() { return correoResguardante; }
    public String getTelefonoResguardante() { return telefonoResguardante; }
    public String getMensaje() { return mensaje; }

    private String resolverFolioAvistamiento(Avistamiento avistamiento) {
        if (avistamiento.getFolioAvistamiento() != null && !avistamiento.getFolioAvistamiento().isBlank()) {
            return avistamiento.getFolioAvistamiento();
        }
        int anio = avistamiento.getFechaRegistro() != null
                ? avistamiento.getFechaRegistro().getYear()
                : LocalDate.now().getYear();
        return "AV-" + anio + "-" + String.format("%06d", avistamiento.getIdAvistamiento());
    }
}
