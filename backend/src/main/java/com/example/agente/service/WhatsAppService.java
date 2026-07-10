package com.example.agente.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class WhatsAppService {

    @Value("${whatsapp.api.token:}")
    private String apiToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void enviarMensajeTexto(String to, String messageBody, String businessPhoneId) {
        enviarMensajeTexto(to, messageBody, businessPhoneId, null);
    }

    public static String normalizarNumero(String telefono) {
        if (telefono == null) return null;
        
        // Quitar todos los caracteres no numéricos
        String num = telefono.replaceAll("[^0-9]", "");
        
        // Si tiene 10 dígitos (México sin clave de país), anteponer "52"
        if (num.length() == 10) {
            num = "52" + num;
        }
        
        // Si tiene el prefijo de celular de México "521" (13 dígitos), quitar el "1" para Sandbox
        if (num.startsWith("521") && num.length() == 13) {
            num = "52" + num.substring(3);
        }
        
        return num;
    }

    public void enviarMensajeTexto(String to, String messageBody, String businessPhoneId, String customToken) {
        if (to == null || messageBody == null || businessPhoneId == null) {
            System.err.println("[WhatsAppService] Error: Parámetros nulos al enviar mensaje.");
            return;
        }

        String targetNumber = normalizarNumero(to);
        System.out.println("[WhatsAppService] Número original: " + to + " | Normalizado: " + targetNumber);

        String tokenToUse = (customToken != null && !customToken.trim().isEmpty()) ? customToken : this.apiToken;

        if (tokenToUse == null || tokenToUse.trim().isEmpty() || "CAMBIAR_POR_TOKEN_REAL".equals(tokenToUse)) {
            System.out.println("[WhatsAppService] [MOCK] Omitiendo envío real de WhatsApp (token de API no configurado).");
            System.out.println("  Para: " + targetNumber);
            System.out.println("  ID Teléfono Negocio: " + businessPhoneId);
            System.out.println("  Contenido: " + messageBody);
            return;
        }

        System.out.println("[WhatsAppService] Enviando mensaje real a " + targetNumber + " desde " + businessPhoneId + "...");

        try {
            String url = "https://graph.facebook.com/v20.0/" + businessPhoneId + "/messages";
            
            // Reemplazar saltos de línea y escapar comillas dobles para que el JSON sea válido
            String escapedMessage = messageBody
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String jsonPayload = String.format(
                    "{\"messaging_product\":\"whatsapp\",\"recipient_type\":\"individual\",\"to\":\"%s\",\"type\":\"text\",\"text\":{\"body\":\"%s\"}}",
                    targetNumber, escapedMessage
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + tokenToUse)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            System.out.println("[WhatsAppService] Mensaje enviado con éxito. Código HTTP: " + response.statusCode());
                        } else {
                            System.err.println("[WhatsAppService] Error al enviar mensaje. Código HTTP: " + response.statusCode());
                            System.err.println("[WhatsAppService] Detalle del error: " + response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("[WhatsAppService] Excepción asíncrona al enviar mensaje a WhatsApp: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("[WhatsAppService] Error al preparar la petición HTTP de WhatsApp: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
