package mx.edu.unadm.rupe.reporte.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;

public class ReporteExtravioResponse {
    private Integer idReporte;
    private Integer idMascota;
    private String folio;
    private String nombreMascota;
    private String razaMascota;
    private String colorMascota;
    private String senasMascota;
    private String fotoPrincipalUrl;
    private String estatus;
    private String tipoReporte;
    private LocalDate fechaExtravio;
    private String estado;
    private String municipio;
    private String colonia;
    private String codigoPostal;
    private String calle;
    private String numero;
    private String referencias;
    private String descripcionHechos;
    private String qrUrl;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaRecuperacion;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaUltimaRenovacion;
    private Integer renovaciones;
    private Boolean requiereRenovacion;
    private Long diasParaVencer;
    private String mensaje;

    public ReporteExtravioResponse(ReporteExtravio reporte, String mensaje) {
        this(reporte, mensaje, null);
    }

    public ReporteExtravioResponse(ReporteExtravio reporte, String mensaje, String fotoPrincipalUrl) {
        this.idReporte = reporte.getIdReporte();
        this.idMascota = reporte.getMascota().getIdMascota();
        this.folio = reporte.getFolio();
        this.nombreMascota = reporte.getMascota().getNombre();
        this.razaMascota = reporte.getMascota().getRaza();
        this.colorMascota = reporte.getMascota().getColor();
        this.senasMascota = reporte.getMascota().getSenasParticulares();
        this.fotoPrincipalUrl = fotoPrincipalUrl;
        this.estatus = reporte.getEstatus().getNombre();
        this.tipoReporte = reporte.getTipoReporte();
        this.fechaExtravio = reporte.getFechaExtravio();
        this.estado = reporte.getEstado();
        this.municipio = reporte.getMunicipio();
        this.colonia = reporte.getColonia();
        this.codigoPostal = reporte.getCodigoPostal();
        this.calle = reporte.getCalle();
        this.numero = reporte.getNumero();
        this.referencias = reporte.getReferencias();
        this.descripcionHechos = reporte.getDescripcionHechos();
        this.qrUrl = reporte.getQrUrl();
        this.activo = reporte.getActivo();
        this.fechaRegistro = reporte.getFechaRegistro();
        this.fechaRecuperacion = esReporteRecuperado(reporte) ? reporte.getFechaActualizacion() : null;
        this.fechaVencimiento = reporte.getFechaVencimiento();
        this.fechaUltimaRenovacion = reporte.getFechaUltimaRenovacion();
        this.renovaciones = reporte.getRenovaciones();
        this.requiereRenovacion = calcularRequiereRenovacion(reporte);
        this.diasParaVencer = reporte.getFechaVencimiento() != null
            ? java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), reporte.getFechaVencimiento())
            : null;
        this.mensaje = mensaje;
    }

    public Integer getIdReporte() { return idReporte; }
    public Integer getIdMascota() { return idMascota; }
    public String getFolio() { return folio; }
    public String getNombreMascota() { return nombreMascota; }
    public String getRazaMascota() { return razaMascota; }
    public String getColorMascota() { return colorMascota; }
    public String getSenasMascota() { return senasMascota; }
    public String getFotoPrincipalUrl() { return fotoPrincipalUrl; }
    public String getEstatus() { return estatus; }
    public String getTipoReporte() { return tipoReporte; }
    public LocalDate getFechaExtravio() { return fechaExtravio; }
    public String getEstado() { return estado; }
    public String getMunicipio() { return municipio; }
    public String getColonia() { return colonia; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getCalle() { return calle; }
    public String getNumero() { return numero; }
    public String getReferencias() { return referencias; }
    public String getDescripcionHechos() { return descripcionHechos; }
    public String getQrUrl() { return qrUrl; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getFechaRecuperacion() { return fechaRecuperacion; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public LocalDateTime getFechaUltimaRenovacion() { return fechaUltimaRenovacion; }
    public Integer getRenovaciones() { return renovaciones; }
    public Boolean getRequiereRenovacion() { return requiereRenovacion; }
    public Long getDiasParaVencer() { return diasParaVencer; }
    public String getMensaje() { return mensaje; }

    private boolean calcularRequiereRenovacion(ReporteExtravio reporte) {
        if (!Boolean.TRUE.equals(reporte.getActivo()) || esReporteRecuperado(reporte)) {
            return false;
        }
        if (Boolean.TRUE.equals(reporte.getRequiereRenovacion())) {
            return true;
        }
        return reporte.getFechaVencimiento() != null && reporte.getFechaVencimiento().isBefore(LocalDate.now());
    }

    private boolean esReporteRecuperado(ReporteExtravio reporte) {
        String estatusActual = reporte.getEstatus().getNombre();
        return "RECUPERADO".equalsIgnoreCase(estatusActual) || "CERRADO".equalsIgnoreCase(estatusActual);
    }
}
