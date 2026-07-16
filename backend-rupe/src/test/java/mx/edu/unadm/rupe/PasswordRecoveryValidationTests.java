package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mx.edu.unadm.rupe.auth.dto.RecuperacionPasswordRequest;
import mx.edu.unadm.rupe.auth.dto.RestablecerPasswordRequest;
import org.junit.jupiter.api.Test;

class PasswordRecoveryValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rechazaSolicitudRecuperacionSinCaptcha() {
        RecuperacionPasswordRequest request = new RecuperacionPasswordRequest();
        request.setCorreo("persona@example.com");
        request.setCaptchaToken("");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("captchaToken");
    }

    @Test
    void rechazaRestablecimientoConPasswordDebil() {
        RestablecerPasswordRequest request = new RestablecerPasswordRequest();
        request.setToken("token-temporal");
        request.setPassword("password123");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("password");
    }

    @Test
    void aceptaRestablecimientoConPasswordRobusta() {
        RestablecerPasswordRequest request = new RestablecerPasswordRequest();
        request.setToken("token-temporal");
        request.setPassword("Rupe2026!");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .doesNotContain("password");
    }
}
