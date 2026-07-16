package mx.edu.unadm.rupe.publico.dto;

import java.util.List;

public class PublicoPaginaResponse<T> {
    private final List<T> contenido;
    private final int pagina;
    private final int tamanio;
    private final long totalElementos;
    private final int totalPaginas;
    private final boolean primera;
    private final boolean ultima;

    public PublicoPaginaResponse(List<T> contenido, int pagina, int tamanio,
            long totalElementos, int totalPaginas, boolean primera, boolean ultima) {
        this.contenido = contenido;
        this.pagina = pagina;
        this.tamanio = tamanio;
        this.totalElementos = totalElementos;
        this.totalPaginas = totalPaginas;
        this.primera = primera;
        this.ultima = ultima;
    }

    public List<T> getContenido() { return contenido; }
    public int getPagina() { return pagina; }
    public int getTamanio() { return tamanio; }
    public long getTotalElementos() { return totalElementos; }
    public int getTotalPaginas() { return totalPaginas; }
    public boolean isPrimera() { return primera; }
    public boolean isUltima() { return ultima; }
}
