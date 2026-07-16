package mx.edu.unadm.rupe.publico.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VisitaRequest {
    @Size(max = 120, message = "La pagina no debe superar 120 caracteres.")
    @Pattern(regexp = "^$|^[A-Za-z0-9_./?=&%-]+$", message = "La pagina contiene caracteres no permitidos.")
    private String pagina;

    public String getPagina() { return pagina; }
    public void setPagina(String pagina) { this.pagina = pagina; }
}
