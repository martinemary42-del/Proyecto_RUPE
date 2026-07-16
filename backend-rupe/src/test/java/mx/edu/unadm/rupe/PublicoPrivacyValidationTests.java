package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import mx.edu.unadm.rupe.publico.dto.PublicoAvistamientoResponse;
import mx.edu.unadm.rupe.publico.dto.PublicoReporteRecienteResponse;
import mx.edu.unadm.rupe.publico.dto.VisitaRequest;
import org.junit.jupiter.api.Test;

class PublicoPrivacyValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void dtoPublicosNoExponenDatosPersonales() {
        List<String> camposReporte = nombresCampos(PublicoReporteRecienteResponse.class);
        List<String> camposAvistamiento = nombresCampos(PublicoAvistamientoResponse.class);

        assertThat(camposReporte).doesNotContain("correo", "telefono", "propietario", "nombrePropietario");
        assertThat(camposAvistamiento).doesNotContain("correoResguardante", "telefonoResguardante", "nombreResguardante");
    }

    @Test
    void rechazaVisitaConPaginaNoPermitida() {
        VisitaRequest request = new VisitaRequest();
        request.setPagina("../<script>alert(1)</script>");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("pagina");
    }

    private List<String> nombresCampos(Class<?> tipo) {
        return Arrays.stream(tipo.getDeclaredFields())
            .map(java.lang.reflect.Field::getName)
            .toList();
    }
}
