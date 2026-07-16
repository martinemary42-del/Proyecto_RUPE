package mx.edu.unadm.rupe.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import mx.edu.unadm.rupe.reporte.model.ReporteExtravio;

public class AdminReporteResponse {
    private final Integer idReporte;
    private final String folio;
    private final String mascota;
    private final String raza;
    private final String zona;
    private final String estatus;
    private final String tipoReporte;
    private final LocalDate fechaExtravio;
    private final LocalDateTime fechaRegistro;
    private final LocalDate fechaVencimiento;
    private final Boolean requiereRenovacion;
    private final Long diasParaVencer;
    private final Integer renovaciones;
    private final Long avistamientos;

    public AdminReporteResponse(ReporteExtravio reporte, long avistamientos) {
        this.idReporte = reporte.getIdReporte();
        this.folio = reporte.getFolio();
        this.mascota = reporte.getMascota() != null ? reporte.getMascota().getNombre() : "Sin dato";
        this.raza = reporte.getMascota() != null ? reporte.getMascota().getRaza() : "Sin dato";
        this.zona = construirZona(reporte);
        this.estatus = reporte.getEstatus() != null ? reporte.getEstatus().getNombre() : "SIN_ESTATUS";
        this.tipoReporte = reporte.getTipoReporte() == null ? "EXTRAVIO" : reporte.getTipoReporte();
        this.fechaExtravio = reporte.getFechaExtravio();
        this.fechaRegistro = reporte.getFechaRegistro();
        this.fechaVencimiento = reporte.getFechaVencimiento();
        this.requiereRenovacion = calcularRequiereRenovacion(reporte);
        this.diasParaVencer = reporte.getFechaVencimiento() != null
            ? ChronoUnit.DAYS.between(LocalDate.now(), reporte.getFechaVencimiento())
            : null;
        this.renovaciones = reporte.getRenovaciones() == null ? 0 : reporte.getRenovaciones();
        this.avistamientos = avistamientos;
    }

    private String construirZona(ReporteExtravio reporte) {
        return String.join(", ",
            valor(reporte.getColonia()),
            valor(reporte.getMunicipio()),
            valor(reporte.getEstado())
        ).replaceAll("^(,\\s*)+|(,\\s*)+$", "");
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "Sin dato" : texto.trim();
    }

    private Boolean calcularRequiereRenovacion(ReporteExtravio reporte) {
        if (!Boolean.TRUE.equals(reporte.getActivo())) return false;
        if (!"ACTIVO".equalsIgnoreCase(estatus)) return false;
        if (Boolean.TRUE.equals(reporte.getRequiereRenovacion())) return true;
        return reporte.getFechaVencimiento() != null && reporte.getFechaVencimiento().isBefore(LocalDate.now());
    }

    public Integer getIdReporte() { return idReporte; }
    public String getFolio() { return folio; }
    public String getMascota() { return mascota; }
    public String getRaza() { return raza; }
    public String getZona() { return zona; }
    public String getEstatus() { return estatus; }
    public String getTipoReporte() { return tipoReporte; }
    public LocalDate getFechaExtravio() { return fechaExtravio; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public Boolean getRequiereRenovacion() { return requiereRenovacion; }
    public Long getDiasParaVencer() { return diasParaVencer; }
    public Integer getRenovaciones() { return renovaciones; }
    public Long getAvistamientos() { return avistamientos; }
}
