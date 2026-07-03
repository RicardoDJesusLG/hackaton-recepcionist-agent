package com.example.agente.controller;

import com.example.agente.agent.AntigravityAgent;
import com.example.agente.model.Empresa;
import com.example.agente.repository.EmpresaRepository;
import com.example.agente.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppWebhookController {

    private final AntigravityAgent antigravityAgent;
    private final WhatsAppService whatsAppService;
    private final EmpresaRepository empresaRepository;

    @Value("${whatsapp.verification.token}")
    private String tokenDeVerificacion;

    // Inyección de dependencias mediante constructor
    public WhatsAppWebhookController(AntigravityAgent antigravityAgent, 
                                     WhatsAppService whatsAppService,
                                     EmpresaRepository empresaRepository) {
        this.antigravityAgent = antigravityAgent;
        this.whatsAppService = whatsAppService;
        this.empresaRepository = empresaRepository;
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

        // Extraer el texto del mensaje y metadatos de forma segura
        String userMessage = null;
        String customerPhone = null;
        String businessPhoneId = null;

        try {
            List<Map<String, Object>> entry = (List<Map<String, Object>>) payload.get("entry");
            if (entry != null && !entry.isEmpty()) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get(0).get("changes");
                if (changes != null && !changes.isEmpty()) {
                    Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
                    if (value != null) {
                        // Extraer id de teléfono del negocio
                        Map<String, Object> metadata = (Map<String, Object>) value.get("metadata");
                        if (metadata != null) {
                            businessPhoneId = (String) metadata.get("phone_number_id");
                        }
                        // Extraer mensaje y número del cliente
                        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                        if (messages != null && !messages.isEmpty()) {
                            Map<String, Object> message = messages.get(0);
                            if (message != null) {
                                customerPhone = (String) message.get("from");
                                if ("text".equals(message.get("type"))) {
                                    Map<String, Object> text = (Map<String, Object>) message.get("text");
                                    if (text != null) {
                                        userMessage = (String) text.get("body");
                                    }
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
        if (userMessage != null && customerPhone != null && businessPhoneId != null) {
            // Normalizar número del cliente (quitar el '1' para prefijo celular en México) de inmediato
            if (customerPhone.startsWith("521") && customerPhone.length() == 13) {
                customerPhone = "52" + customerPhone.substring(3);
            }
            
            System.out.println("[WhatsAppWebhookController] Mensaje de texto recibido de " + customerPhone + ": " + userMessage);
            
            // Validar la suscripción de la empresa asociada
            Optional<Empresa> empresaOpt = empresaRepository.findByWhatsappPhoneId(businessPhoneId);
            String customToken = empresaOpt.map(Empresa::getWhatsappToken).orElse(null);

            if (empresaOpt.isPresent() && !empresaOpt.get().getSuscripcionActiva()) {
                System.out.println("[WhatsAppWebhookController] Mensaje bloqueado. Empresa " + 
                        empresaOpt.get().getNombre() + " tiene la suscripción inactiva.");
                
                String blockMessage = "Lo sentimos, el servicio de recepción de este negocio se encuentra "
                        + "temporalmente suspendido por falta de pago. Si eres el dueño del negocio, "
                        + "por favor ingresa a tu Panel de Administración para reactivarlo.";
                whatsAppService.enviarMensajeTexto(customerPhone, blockMessage, businessPhoneId, customToken);
                return ResponseEntity.ok().build();
            }

            // Invocar al agente con contexto de la empresa si existe
            String agentResponse;
            if (empresaOpt.isPresent()) {
                Empresa empresa = empresaOpt.get();
                agentResponse = antigravityAgent.chat(
                    userMessage, 
                    empresa.getId().toString(), 
                    empresa.getNombre(), 
                    empresa.getTelefonoContacto(), 
                    empresa.getDireccion(), 
                    empresa.getMapsLink(), 
                    empresa.getDescripcionNegocio(),
                    empresa.getPromocionActiva(),
                    empresa.getPromocionDescripcion(),
                    customerPhone
                );
            } else {
                agentResponse = antigravityAgent.chat(userMessage);
            }
            System.out.println("[WhatsAppWebhookController] Respuesta del agente Antigravity: " + agentResponse);

            // Enviar mensaje de vuelta usando el servicio
            whatsAppService.enviarMensajeTexto(customerPhone, agentResponse, businessPhoneId, customToken);
        } else {
            System.out.println("[WhatsAppWebhookController] Payload recibido no contiene la información mínima para responder.");
        }

        // Retornamos de inmediato un 200 OK para cumplir con la latencia que exige Meta
        return ResponseEntity.ok().build();
    }

}
