package com.example.agente.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend;

    @Value("${resend.email.from}")
    private String fromEmail;

    public EmailService(@Value("${resend.api.key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public void enviarCodigoRecuperacion(String emailDestino, String codigo6Digitos) {
        String htmlContent = String.format(
            "<div style=\"font-family: Arial, sans-serif; background-color: #0a2305; color: #b0d1bc; padding: 40px; border-radius: 12px; max-width: 600px; margin: auto;\">" +
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                    "<h2 style=\"color: #ffffff; margin: 0; font-size: 26px;\">Whappify</h2>" +
                    "<p style=\"color: #b0d1bc; font-size: 14px; margin-top: 6px; opacity: 0.8;\">Tu asistente virtual</p>" +
                "</div>" +
                "<div style=\"background-color: #ffffff; padding: 32px; border-radius: 8px; border: 1px solid #7499c6;\">" +
                    "<h3 style=\"color: #355ab2; margin-top: 0; font-size: 20px;\">Restablece tu contraseña</h3>" +
                    "<p style=\"color: #355ab2; font-size: 15px; line-height: 1.5;\">Hola,</p>" +
                    "<p style=\"color: #355ab2; font-size: 15px; line-height: 1.5;\">Hemos recibido una solicitud para acceder a tu panel. Utiliza el siguiente código de verificación de 6 dígitos:</p>" +
                    "<div style=\"text-align: center; margin: 32px 0;\">" +
                        "<span style=\"background-color: #b0d1bc; color: #0a2305; padding: 16px 28px; font-size: 32px; font-weight: bold; letter-spacing: 8px; border-radius: 8px; border: 1px solid #7499c6; display: inline-block;\">%s</span>" +
                    "</div>" +
                    "<p style=\"color: #355ab2; font-size: 13px; margin-bottom: 0; opacity: 0.9;\">Este código expirará en <strong>5 minutos</strong>.</p>" +
                    "<p style=\"color: #355ab2; font-size: 13px; opacity: 0.9;\">Si no solicitaste este cambio, puedes ignorar este mensaje de forma segura.</p>" +
                "</div>" +
                "<div style=\"text-align: center; margin-top: 24px; color: #7499c6; font-size: 12px;\">" +
                    "<p>© 2026 Whappify. Todos los derechos reservados.</p>" +
                "</div>" +
            "</div>",
            codigo6Digitos
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(emailDestino)
                .subject("Código de recuperación de contraseña - Whappify")
                .html(htmlContent)
                .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("Correo enviado exitosamente con ID: " + data.getId());
        } catch (ResendException e) {
            System.err.println("Error al enviar el correo: " + e.getMessage());
        }
    }
}