package mx.edu.unadm.rupe.admin.dto;

import java.util.List;

public class AdminResumenResponse {
    private final long usuariosActivos;
    private final long usuariosInactivos;
    private final long mascotasRegistradas;
    private final long reportesActivos;
    private final long reportesExtravio;
    private final long reportesRobo;
    private final long reportesPendientesRenovacion;
    private final long mascotasRecuperadas;
    private final long avistamientos;
    private final long avistamientosPendientesValidacion;
    private final long visitasTotales;
    private final long usuariosBloqueados;
    private final long usuariosBajaPropia;
    private final long mensajesContactoTotal;
    private final long mensajesContactoPendientes;
    private final long mensajesContactoAtendidos;
    private final List<VisitaPaginaResponse> visitasPorPagina;

    public AdminResumenResponse(long usuariosActivos, long usuariosInactivos, long mascotasRegistradas,
            long reportesActivos, long reportesExtravio, long reportesRobo, long reportesPendientesRenovacion,
            long mascotasRecuperadas, long avistamientos, long avistamientosPendientesValidacion,
            long visitasTotales, long usuariosBloqueados, long usuariosBajaPropia,
            long mensajesContactoTotal, long mensajesContactoPendientes,
            long mensajesContactoAtendidos, List<VisitaPaginaResponse> visitasPorPagina) {
        this.usuariosActivos = usuariosActivos;
        this.usuariosInactivos = usuariosInactivos;
        this.mascotasRegistradas = mascotasRegistradas;
        this.reportesActivos = reportesActivos;
        this.reportesExtravio = reportesExtravio;
        this.reportesRobo = reportesRobo;
        this.reportesPendientesRenovacion = reportesPendientesRenovacion;
        this.mascotasRecuperadas = mascotasRecuperadas;
        this.avistamientos = avistamientos;
        this.avistamientosPendientesValidacion = avistamientosPendientesValidacion;
        this.visitasTotales = visitasTotales;
        this.usuariosBloqueados = usuariosBloqueados;
        this.usuariosBajaPropia = usuariosBajaPropia;
        this.mensajesContactoTotal = mensajesContactoTotal;
        this.mensajesContactoPendientes = mensajesContactoPendientes;
        this.mensajesContactoAtendidos = mensajesContactoAtendidos;
        this.visitasPorPagina = visitasPorPagina;
    }

    public long getUsuariosActivos() { return usuariosActivos; }
    public long getUsuariosInactivos() { return usuariosInactivos; }
    public long getMascotasRegistradas() { return mascotasRegistradas; }
    public long getReportesActivos() { return reportesActivos; }
    public long getReportesExtravio() { return reportesExtravio; }
    public long getReportesRobo() { return reportesRobo; }
    public long getReportesPendientesRenovacion() { return reportesPendientesRenovacion; }
    public long getMascotasRecuperadas() { return mascotasRecuperadas; }
    public long getAvistamientos() { return avistamientos; }
    public long getAvistamientosPendientesValidacion() { return avistamientosPendientesValidacion; }
    public long getVisitasTotales() { return visitasTotales; }
    public long getUsuariosBloqueados() { return usuariosBloqueados; }
    public long getUsuariosBajaPropia() { return usuariosBajaPropia; }
    public long getMensajesContactoTotal() { return mensajesContactoTotal; }
    public long getMensajesContactoPendientes() { return mensajesContactoPendientes; }
    public long getMensajesContactoAtendidos() { return mensajesContactoAtendidos; }
    public List<VisitaPaginaResponse> getVisitasPorPagina() { return visitasPorPagina; }
}
