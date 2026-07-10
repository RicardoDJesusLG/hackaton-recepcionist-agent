package com.example.agente.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo electrónico simple utilizando SMTP.
     * Si falla o no está configurado, imprime en consola como fallback de desarrollo.
     */
    public void enviarCorreo(String to, String subject, String body) {
        if (fromEmail == null || fromEmail.trim().isEmpty() || fromEmail.contains("tu-correo-temporal")) {
            System.out.println("======================================================================");
            System.out.println("[Fallback de Correo] SMTP no configurado. Simulación de envío:");
            System.out.println("Para: " + to);
            System.out.println("Asunto: " + subject);
            System.out.println("Mensaje: " + body);
            System.out.println("======================================================================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("[EmailService] Correo enviado exitosamente a " + to);
        } catch (Exception e) {
            System.err.println("[EmailService] Error al enviar el correo SMTP a " + to + ": " + e.getMessage());
            System.out.println("======================================================================");
            System.out.println("[Fallback de Correo] Imprimiendo el mensaje debido a un error SMTP:");
            System.out.println("Para: " + to);
            System.out.println("Asunto: " + subject);
            System.out.println("Mensaje: " + body);
            System.out.println("======================================================================");
        }
    }
}
