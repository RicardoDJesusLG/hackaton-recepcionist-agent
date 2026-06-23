package com.example.agente.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppWebhookController {

    @Value("${whatsapp.verification.token}")
    private String tokenDeVerificacion;

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

        // Retornamos de inmediato un 200 OK para cumplir con la latencia que exige Meta
        return ResponseEntity.ok().build();
    }

}
