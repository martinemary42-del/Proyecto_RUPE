package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mx.edu.unadm.rupe.auth.dto.RegistroRequest;
import org.junit.jupiter.api.Test;

class RegistroValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rechazaPasswordSinPoliticaRobusta() {
        RegistroRequest request = registroValido();
        request.setPassword("password123");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("password");
    }

    @Test
    void aceptaPasswordConPoliticaRobusta() {
        RegistroRequest request = registroValido();
        request.setPassword("Rupe2026!");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .doesNotContain("password");
    }

    private RegistroRequest registroValido() {
        RegistroRequest request = new RegistroRequest();
        request.setNombreCompleto("Usuario de prueba");
        request.setCorreo("persona@example.com");
        request.setTelefono("5512345678");
        request.setPassword("Rupe2026!");
        request.setCaptchaToken("captcha-prueba");
        return request;
    }
}
