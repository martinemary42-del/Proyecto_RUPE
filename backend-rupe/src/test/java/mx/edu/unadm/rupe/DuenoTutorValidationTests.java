package mx.edu.unadm.rupe;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import mx.edu.unadm.rupe.avistamiento.dto.AvistamientoRequest;
import mx.edu.unadm.rupe.mascota.dto.MascotaRegistroRequest;
import mx.edu.unadm.rupe.reporte.dto.ReporteExtravioRequest;
import org.junit.jupiter.api.Test;

class DuenoTutorValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rechazaMascotaConCodigoPostalInvalidoYSenasDemasiadoLargas() {
        MascotaRegistroRequest request = mascotaValida();
        request.setCp("123");
        request.setSenas("a".repeat(501));

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("cp", "senas");
    }

    @Test
    void rechazaReporteConDescripcionDemasiadoLarga() {
        ReporteExtravioRequest request = reporteValido();
        request.setDescripcion("a".repeat(1001));

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("descripcion");
    }

    @Test
    void rechazaAvistamientoConContactoInvalido() {
        AvistamientoRequest request = avistamientoValido();
        request.setCp("ABCDE");
        request.setCorreoResguardante("correo-invalido");
        request.setTelefonoResguardante("555");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("cp", "correoResguardante", "telefonoResguardante");
    }

    @Test
    void rechazaAvistamientoSinSenasUtilesParaBusqueda() {
        AvistamientoRequest request = avistamientoValido();
        request.setDescripcion("cafe");

        var errores = validator.validate(request);

        assertThat(errores).extracting("propertyPath").map(Object::toString)
            .contains("descripcion");
    }

    private MascotaRegistroRequest mascotaValida() {
        MascotaRegistroRequest request = new MascotaRegistroRequest();
        request.setNombre("Firulais");
        request.setSexo("Macho");
        request.setIdTipoMascota("Perro");
        request.setIdRaza("Criollo");
        request.setIdColor("Cafe");
        request.setSenas("Mancha blanca en el pecho");
        request.setCollarPlaca("Si");
        request.setIdEstado("CDMX");
        request.setIdMunicipio("Benito Juarez");
        request.setIdColonia("Del Valle");
        request.setCp("03100");
        return request;
    }

    private ReporteExtravioRequest reporteValido() {
        ReporteExtravioRequest request = new ReporteExtravioRequest();
        request.setIdMascota(1);
        request.setFechaExtravio(LocalDate.now());
        request.setIdEstado("CDMX");
        request.setIdMunicipio("Benito Juarez");
        request.setIdColonia("Del Valle");
        request.setReferencias("Cerca del parque");
        request.setDescripcion("Se extravio durante el paseo.");
        request.setCp("03100");
        return request;
    }

    private AvistamientoRequest avistamientoValido() {
        AvistamientoRequest request = new AvistamientoRequest();
        request.setFecha(LocalDate.now());
        request.setIdEstado("CDMX");
        request.setIdMunicipio("Benito Juarez");
        request.setIdColonia("Del Valle");
        request.setReferencias("Frente a una tienda");
        request.setDescripcion("Perrito cafe con pecho blanco y collar rojo");
        request.setCp("03100");
        return request;
    }
}
