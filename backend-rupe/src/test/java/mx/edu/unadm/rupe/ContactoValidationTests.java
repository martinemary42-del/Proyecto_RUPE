package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mx.edu.unadm.rupe.contacto.dto.MensajeContactoRequest;
import org.junit.jupiter.api.Test;

class ContactoValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rechazaCorreoInvalidoYTelefonoConFormatoIncorrecto() {
        MensajeContactoRequest request = new MensajeContactoRequest();
        request.setNombre("Usuario de prueba");
        request.setCorreo("correo-invalido");
        request.setTelefono("123");
        request.setAsunto("reporte");
        request.setMensaje("Necesito ayuda con un reporte.");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("correo", "telefono");
    }

    @Test
    void rechazaMensajeDeSoporteDemasiadoLargo() {
        MensajeContactoRequest request = new MensajeContactoRequest();
        request.setNombre("Usuario de prueba");
        request.setCorreo("persona@example.com");
        request.setTelefono("5512345678");
        request.setAsunto("seguimiento");
        request.setMensaje("a".repeat(501));

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("mensaje");
    }

    @Test
    void rechazaCorreoConDominioIncompleto() {
        MensajeContactoRequest request = new MensajeContactoRequest();
        request.setNombre("Usuario de prueba");
        request.setCorreo("jose@hotmail.co");
        request.setTelefono("5512345678");
        request.setAsunto("seguimiento");
        request.setMensaje("Necesito ayuda con un reporte.");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("correo");
    }
}
