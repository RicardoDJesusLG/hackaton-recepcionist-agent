package com.example.agente.controller;

import com.example.agente.agent.AntigravityAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppWebhookController {

    private final AntigravityAgent antigravityAgent;

    @Value("${whatsapp.verification.token}")
    private String tokenDeVerificacion;

    // Inyección de dependencias mediante constructor
    public WhatsAppWebhookController(AntigravityAgent antigravityAgent) {
        this.antigravityAgent = antigravityAgent;
    }

    /**
     * Handshake obligatorio de Meta (GET).
     * Valida que tu webhook es real y seguro.
     */
    @GetMapping("/webhook")
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && tokenDeVerificacion.equals(token)) {
            System.out.println("¡Handshake exitoso! Webhook verificado con Meta.");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(challenge);
        } else {
            System.err.println("Fallo la verificacion. Token no coincide.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Token invalido");
        }
    }

    /**
     * Interceptor de Mensajes (POST)
     * Aqui llegaran las peticiones en tiempo real cuando un cliente escriba a Whatsapp.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirMensaje(@RequestBody Map<String, Object> payload) {
        System.out.println("\n --- MENSAJE ENTRANTE DETECTADO ---");
        System.out.println(payload);
        System.out.println("----------------------------------\n");

        // Extraer el texto del mensaje de forma segura
        String userMessage = null;
        try {
            List<Map<String, Object>> entry = (List<Map<String, Object>>) payload.get("entry");
            if (entry != null && !entry.isEmpty()) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get(0).get("changes");
                if (changes != null && !changes.isEmpty()) {
                    Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
                    if (value != null) {
                        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                        if (messages != null && !messages.isEmpty()) {
                            Map<String, Object> message = messages.get(0);
                            if (message != null && "text".equals(message.get("type"))) {
                                Map<String, Object> text = (Map<String, Object>) message.get("text");
                                if (text != null) {
                                    userMessage = (String) text.get("body");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al analizar el payload estructurado de WhatsApp: " + e.getMessage());
        }

        // Invocar el ciclo de pensamiento del agente si se detecta un mensaje
        if (userMessage != null) {
            System.out.println("[WhatsAppWebhookController] Mensaje de texto recibido: " + userMessage);
            String agentResponse = antigravityAgent.chat(userMessage);
            System.out.println("[WhatsAppWebhookController] Respuesta del agente Antigravity: " + agentResponse);
        } else {
            System.out.println("[WhatsAppWebhookController] Payload recibido no contiene un mensaje de texto simple.");
        }

        // Retornamos de inmediato un 200 OK para cumplir con la latencia que exige Meta
        return ResponseEntity.ok().build();
    }

}
