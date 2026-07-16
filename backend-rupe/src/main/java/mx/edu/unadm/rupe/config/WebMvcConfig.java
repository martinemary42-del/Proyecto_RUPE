package mx.edu.unadm.rupe.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploads = Paths.get("uploads").toAbsolutePath().normalize();
        Path uploadsBackend = Paths.get("backend-rupe", "uploads").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
            // Se registran ambas rutas para que las fotos funcionen si Spring Boot se arranca
            // desde backend-rupe o desde la carpeta raiz del proyecto durante pruebas locales.
            .addResourceLocations(uploads.toUri().toString(), uploadsBackend.toUri().toString());
    }
}
