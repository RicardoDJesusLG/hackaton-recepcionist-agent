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
        
        // NOTA: Para el sandbox de Meta Developers, NO debemos quitar el "1" del prefijo "521",
        // ya que Meta valida estrictamente contra la lista de números autorizados en el sandbox,
        // la cual suele registrar los números móviles de México con el formato "521...".
        // Si removemos el "1", Meta rechazará el mensaje con un código HTTP 400.
        
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

        enviarMensajeTextoReal(targetNumber, messageBody, businessPhoneId, tokenToUse, false);
    }

    private void enviarMensajeTextoReal(String targetNumber, String messageBody, String businessPhoneId, String tokenToUse, boolean esReintento) {
        System.out.println("[WhatsAppService] Enviando mensaje real a " + targetNumber + " desde " + businessPhoneId + " (esReintento: " + esReintento + ")...");

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

                            // Si falla por número no incluido en la lista del sandbox, y es número de México, intentamos el formato alterno
                            if (!esReintento && response.statusCode() == 400 && response.body().contains("131030") && targetNumber.startsWith("52")) {
                                String alternateNumber = obtenerNumeroAlternoMexico(targetNumber);
                                if (alternateNumber != null) {
                                    System.out.println("[WhatsAppService] Reintento autocurativo: Intentando enviar con formato alterno de México: " + alternateNumber);
                                    enviarMensajeTextoReal(alternateNumber, messageBody, businessPhoneId, tokenToUse, true);
                                }
                            }
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

    private static String obtenerNumeroAlternoMexico(String num) {
        if (num == null) return null;
        if (num.startsWith("521") && num.length() == 13) {
            // Cambiar de 521XXXXXXXXXX (con 1) a 52XXXXXXXXXX (sin 1)
            return "52" + num.substring(3);
        } else if (num.startsWith("52") && num.length() == 12 && !num.startsWith("521")) {
            // Cambiar de 52XXXXXXXXXX (sin 1) a 521XXXXXXXXXX (con 1)
            return "521" + num.substring(2);
        }
        return null;
    }

    public boolean suscribirAppAWaba(String phoneId, String token) {
        if (phoneId == null || phoneId.trim().isEmpty() || token == null || token.trim().isEmpty() 
                || "CAMBIAR_POR_TOKEN_REAL".equals(token) || token.contains("...") || token.contains("****")) {
            return false;
        }

        System.out.println("[WhatsAppService] Iniciando auto-vinculación síncrona para phoneId: " + phoneId);

        try {
            String getPhoneUrl = "https://graph.facebook.com/v20.0/" + phoneId + "?fields=whatsapp_business_account";
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getPhoneUrl))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

            if (getResponse.statusCode() != 200) {
                System.err.println("[WhatsAppService] Error al obtener detalles del teléfono (" + getResponse.statusCode() + "): " + getResponse.body());
                return false;
            }
            
            com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(getResponse.body());
            com.fasterxml.jackson.databind.JsonNode wabaNode = rootNode.path("whatsapp_business_account");
            if (wabaNode.isMissingNode() || wabaNode.path("id").isMissingNode()) {
                System.err.println("[WhatsAppService] No se encontró el nodo whatsapp_business_account.id en la respuesta: " + getResponse.body());
                return false;
            }
            String wabaId = wabaNode.path("id").asText();
            System.out.println("[WhatsAppService] WABA ID detectado automáticamente: " + wabaId);

            String subscribeUrl = "https://graph.facebook.com/v20.0/" + wabaId + "/subscribed_apps";
            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(URI.create(subscribeUrl))
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

            if (postResponse.statusCode() == 200 || postResponse.statusCode() == 201) {
                System.out.println("[WhatsAppService] Auto-suscripción exitosa a Meta: " + postResponse.body());
                return true;
            } else {
                System.err.println("[WhatsAppService] Error en la auto-suscripción a Meta (" + postResponse.statusCode() + "): " + postResponse.body());
                return false;
            }

        } catch (Exception e) {
            System.err.println("[WhatsAppService] Excepción en flujo de auto-suscripción síncrona: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

