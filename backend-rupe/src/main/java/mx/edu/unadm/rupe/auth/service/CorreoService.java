package mx.edu.unadm.rupe.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {
    private final JavaMailSender mailSender;

    @Value("${rupe.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${rupe.mail.from:no-reply@rupe.local}")
    private String mailFrom;

    @Value("${rupe.frontend.public-base-url:http://localhost:5500}")
    private String publicBaseUrl;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean estaActivo() {
        return mailEnabled;
    }

    public void enviarRecuperacionPassword(String destinatario, String token) {
        if (!mailEnabled || destinatario == null || token == null) {
            return;
        }

        // El correo solo contiene el enlace temporal; no envia contrasenas ni datos personales sensibles.
        String base = publicBaseUrl.endsWith("/")
            ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
            : publicBaseUrl;
        String enlace = base + "/validar-recuperacion.html?token=" + token;

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(mailFrom);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Recuperacion de contrasena RUPE");
        mensaje.setText("""
            Hola.

            Recibimos una solicitud para recuperar tu contrasena en RUPE.
            Usa el siguiente enlace temporal:

            %s

            El enlace expira en 20 minutos. Si no solicitaste este cambio, ignora este mensaje.

            RUPE - Registro Unico de Perritos Extraviados
            """.formatted(enlace));
        mailSender.send(mensaje);
    }
}
