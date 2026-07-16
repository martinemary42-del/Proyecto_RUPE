package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;

import mx.edu.unadm.rupe.admin.dto.AdminBitacoraResponse;
import mx.edu.unadm.rupe.security.model.BitacoraSeguridad;
import org.junit.jupiter.api.Test;

class AdminBitacoraResponseTests {

    @Test
    void enmascaraCorreoAntesDeExponerloAlAdministrador() {
        BitacoraSeguridad bitacora = new BitacoraSeguridad();
        bitacora.setCorreo("persona@example.com");
        bitacora.setAccion("LOGIN");
        bitacora.setModulo("AUTH");
        bitacora.setResultado("EXITOSO");

        AdminBitacoraResponse response = new AdminBitacoraResponse(bitacora);

        assertThat(response.getCorreo()).isEqualTo("pe***@example.com");
    }
}
