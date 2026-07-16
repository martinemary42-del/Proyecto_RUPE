package mx.edu.unadm.rupe.publico.dto;

public class PublicoResumenResponse {
    private long perritosRegistrados;
    private long reportesActivos;
    private long perritosRecuperados;
    private long avistamientosCiudadanos;

    public PublicoResumenResponse(long perritosRegistrados, long reportesActivos,
            long perritosRecuperados, long avistamientosCiudadanos) {
        this.perritosRegistrados = perritosRegistrados;
        this.reportesActivos = reportesActivos;
        this.perritosRecuperados = perritosRecuperados;
        this.avistamientosCiudadanos = avistamientosCiudadanos;
    }

    public long getPerritosRegistrados() { return perritosRegistrados; }
    public long getReportesActivos() { return reportesActivos; }
    public long getPerritosRecuperados() { return perritosRecuperados; }
    public long getAvistamientosCiudadanos() { return avistamientosCiudadanos; }
}
