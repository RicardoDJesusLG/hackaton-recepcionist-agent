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
            final String originalCustomerPhone = customerPhone; // Guardar el número original para responder al destinatario exacto
            
            // Normalizar número del cliente (quitar el '1' para prefijo celular en México) de inmediato para la DB y sesión
            if (customerPhone.startsWith("521") && customerPhone.length() == 13) {
                customerPhone = "52" + customerPhone.substring(3);
            }
            
            System.out.println("[WhatsAppWebhookController] Mensaje de texto recibido de " + customerPhone + " (original: " + originalCustomerPhone + "): " + userMessage);
            
            // Validar la suscripción de la empresa asociada y loguear diagnósticos claros
            System.out.println("[WhatsAppWebhookController] Buscando empresa para el businessPhoneId recibido de Meta: " + businessPhoneId);
            Optional<Empresa> empresaOpt = empresaRepository.findByWhatsappPhoneId(businessPhoneId);
            String customToken = empresaOpt.map(Empresa::getWhatsappToken).orElse(null);

            if (empresaOpt.isEmpty()) {
                System.out.println("[WhatsAppWebhookController] ⚠️ ADVERTENCIA: No se encontró ninguna empresa en la base de datos con whatsappPhoneId = '" + businessPhoneId + "'.");
                System.out.println("  Verifica lo siguiente:");
                System.out.println("  1. ¿Registraste la empresa en el panel con este ID exacto?");
                System.out.println("  2. ¿Confundiste el 'Phone Number ID' (ID de teléfono - 15 dígitos) con el 'WhatsApp Business Account ID' o tu número de teléfono real?");
                System.out.println("  Se intentará responder usando el token por defecto del servidor, lo cual fallará si este webhook no proviene de tu Sandbox principal.");
            } else {
                Empresa e = empresaOpt.get();
                System.out.println("[WhatsAppWebhookController] Empresa encontrada: '" + e.getNombre() + "' (ID Interno: " + e.getId() + ", Suscripción Activa: " + e.getSuscripcionActiva() + ")");
                if (customToken == null || customToken.trim().isEmpty()) {
                    System.out.println("[WhatsAppWebhookController] ⚠️ ADVERTENCIA: La empresa '" + e.getNombre() + "' no tiene un whatsappToken configurado. Se usará el token por defecto del servidor.");
                } else {
                    System.out.println("[WhatsAppWebhookController] Utilizando token personalizado configurado para '" + e.getNombre() + "'.");
                }
            }

            if (empresaOpt.isPresent() && !empresaOpt.get().getSuscripcionActiva()) {
                System.out.println("[WhatsAppWebhookController] Mensaje bloqueado. Empresa " + 
                        empresaOpt.get().getNombre() + " tiene la suscripción inactiva.");
                
                String blockMessage = "Lo sentimos, el servicio de recepción de este negocio se encuentra "
                        + "temporalmente suspendido por falta de pago. Si eres el dueño del negocio, "
                        + "por favor ingresa a tu Panel de Administración para reactivarlo.";
                whatsAppService.enviarMensajeTexto(originalCustomerPhone, blockMessage, businessPhoneId, customToken);
                return ResponseEntity.ok().build();
            }

            final String finalCustomerPhone = customerPhone;
            final String finalUserMessage = userMessage;
            final String finalBusinessPhoneId = businessPhoneId;
            final String finalCustomToken = customToken;
            final Optional<Empresa> finalEmpresaOpt = empresaOpt;

            // Procesar de forma síncrona para garantizar que Cloud Run mantenga la CPU asignada durante la ejecución
            try {
                String agentResponse;
                if (finalEmpresaOpt.isPresent()) {
                    Empresa empresa = finalEmpresaOpt.get();
                    String mapsLink = empresa.getMapsLink();
                    if ("BASIC".equalsIgnoreCase(empresa.getPlanSuscripcion())) {
                        mapsLink = null;
                    }
                    agentResponse = antigravityAgent.chat(
                        finalUserMessage, 
                        empresa.getId().toString(), 
                        empresa.getNombre(), 
                        empresa.getTelefonoContacto(), 
                        empresa.getDireccion(), 
                        mapsLink, 
                        empresa.getDescripcionNegocio(),
                        finalCustomerPhone
                    );
                } else {
                    agentResponse = antigravityAgent.chat(finalUserMessage);
                }
                System.out.println("[WhatsAppWebhookController] Respuesta del agente Antigravity: " + agentResponse);

                // Enviar mensaje de vuelta usando el servicio al número original (con el '1' si venía así de Meta)
                whatsAppService.enviarMensajeTexto(originalCustomerPhone, agentResponse, finalBusinessPhoneId, finalCustomToken);
            } catch (Exception ex) {
                System.err.println("[WhatsAppWebhookController] Error al procesar mensaje de forma síncrona: " + ex.getMessage());
            }
        } else {
            System.out.println("[WhatsAppWebhookController] Payload recibido no contiene la información mínima para responder.");
        }

        // Retornamos 200 OK
        return ResponseEntity.ok().build();
    }

}
