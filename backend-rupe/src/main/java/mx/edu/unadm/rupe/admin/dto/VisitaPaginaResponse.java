package mx.edu.unadm.rupe.admin.dto;

public class VisitaPaginaResponse {
    private final String pagina;
    private final long total;

    public VisitaPaginaResponse(String pagina, long total) {
        this.pagina = pagina;
        this.total = total;
    }

    public String getPagina() { return pagina; }
    public long getTotal() { return total; }
}
