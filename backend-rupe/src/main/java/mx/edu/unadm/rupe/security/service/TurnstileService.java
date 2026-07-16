package mx.edu.unadm.rupe.security.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class TurnstileService {

    private final RestClient restClient;

    @Value("${rupe.turnstile.siteverify-url}")
    private String siteverifyUrl;

    @Value("${rupe.turnstile.secret-key}")
    private String secretKey;

    @Value("${rupe.turnstile.enabled:true}")
    private boolean enabled;

    public TurnstileService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void validarToken(String token) {
        if (!enabled) {
            return;
        }

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Completa la verificación CAPTCHA");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secretKey);
        body.add("response", token);

        TurnstileResponse response = restClient.post()
            .uri(siteverifyUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(TurnstileResponse.class);

        if (response == null || !response.success()) {
            throw new IllegalArgumentException("No fue posible validar el CAPTCHA");
        }
    }

    public record TurnstileResponse(
        boolean success,
        String hostname,
        String action,
        @JsonProperty("error-codes") List<String> errorCodes
    ) {
    }
}
