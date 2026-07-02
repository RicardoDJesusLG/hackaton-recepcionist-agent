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
        if (to == null || messageBody == null || businessPhoneId == null) {
            System.err.println("[WhatsAppService] Error: Parámetros nulos al enviar mensaje.");
            return;
        }

        // Normalización para números de México (quitar el '1' después del código de país '52' en ambiente de Sandbox)
        String targetNumber = to;
        if (targetNumber.startsWith("521") && targetNumber.length() == 13) {
            targetNumber = "52" + targetNumber.substring(3);
            System.out.println("[WhatsAppService] Detectado número de México con prefijo '521'. Normalizando a '" + targetNumber + "' para compatibilidad con Sandbox.");
        }

        if (apiToken == null || apiToken.trim().isEmpty() || "CAMBIAR_POR_TOKEN_REAL".equals(apiToken)) {
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
                    .header("Authorization", "Bearer " + apiToken)
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
