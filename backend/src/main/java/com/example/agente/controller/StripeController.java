package com.example.agente.controller;

import com.example.agente.model.Empresa;
import com.example.agente.repository.EmpresaRepository;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stripe")
public class StripeController {

    private final EmpresaRepository empresaRepository;
    private final String stripeApiKey;
    private final String webhookSecret;

    public StripeController(EmpresaRepository empresaRepository,
                            @Value("${stripe.api.key:}") String stripeApiKey,
                            @Value("${stripe.webhook.secret:}") String webhookSecret) {
        this.empresaRepository = empresaRepository;
        this.stripeApiKey = stripeApiKey;
        this.webhookSecret = webhookSecret;
    }

    /**
     * Endpoint para generar un link de pago seguro en Stripe.
     * POST /api/v1/stripe/checkout
     */
    /**
     * Endpoint para generar un link de acceso al portal de clientes (Billing Portal) de Stripe.
     * POST /api/v1/stripe/portal
     */
    @PostMapping("/portal")
    public ResponseEntity<?> crearPortalSession(@RequestBody Map<String, String> request) {
        String idNegocioStr = request.get("idNegocio");

        if (idNegocioStr == null || idNegocioStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de negocio requerido"));
        }

        UUID idNegocio;
        try {
            idNegocio = UUID.fromString(idNegocioStr.trim());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de negocio no válido"));
        }

        Optional<Empresa> empresaOpt = empresaRepository.findById(idNegocio);
        if (empresaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Empresa no encontrada"));
        }

        Empresa empresa = empresaOpt.get();

        if (stripeApiKey == null || stripeApiKey.trim().isEmpty() || "CAMBIAR_POR_LLAVE_REAL".equals(stripeApiKey)) {
            // Modo mock: Simular portal de Stripe y poner inactiva para probar el flujo localmente
            System.out.println("[StripeController] [MOCK] Generando portal de gestión simulado para: " + idNegocio);
            empresa.setSuscripcionActiva(false);
            empresaRepository.save(empresa);

            String mockUrl = "http://localhost:4200/dashboard?payment=cancel&mock=true&idNegocio=" + idNegocio;
            return ResponseEntity.ok(Map.of("url", mockUrl));
        }

        try {
            Stripe.apiKey = stripeApiKey;
            if (empresa.getStripeCustomerId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El negocio no tiene un Customer ID de Stripe registrado"));
            }

            com.stripe.param.billingportal.SessionCreateParams portalParams = 
                com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(empresa.getStripeCustomerId())
                    .setReturnUrl("http://localhost:4200/dashboard")
                    .build();

            com.stripe.model.billingportal.Session portalSession = 
                    com.stripe.model.billingportal.Session.create(portalParams);

            return ResponseEntity.ok(Map.of("url", portalSession.getUrl()));

        } catch (Exception e) {
            System.err.println("[StripeController] Error al crear sesión del portal de Stripe: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al conectar con Stripe: " + e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> crearCheckoutSession(@RequestBody Map<String, String> request) {
        String idNegocioStr = request.get("idNegocio");
        String plan = request.get("plan");

        if (idNegocioStr == null || idNegocioStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de negocio requerido"));
        }

        UUID idNegocio;
        try {
            idNegocio = UUID.fromString(idNegocioStr.trim());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de negocio no válido"));
        }

        Optional<Empresa> empresaOpt = empresaRepository.findById(idNegocio);
        if (empresaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Empresa no encontrada"));
        }

        if (plan == null || plan.trim().isEmpty()) {
            plan = "BASIC";
        }
        plan = plan.trim().toUpperCase();

        long unitAmount = 29900L; // $299.00 MXN
        String productName = "Membresía Recepción Básica";
        String productDesc = "Suscripción mensual al bot WhatsApp con límite de 60 citas/mes y soporte en horario laboral.";

        if ("PREMIUM".equals(plan)) {
            unitAmount = 49900L; // $499.00 MXN
            productName = "Membresía Recepción Premium";
            productDesc = "Suscripción mensual al bot WhatsApp ilimitada con soporte prioritario 24/7 y cobro a clientes.";
        } else {
            plan = "BASIC";
        }

        // Modo MOCK si no hay llave de Stripe configurada
        if (stripeApiKey == null || stripeApiKey.trim().isEmpty() || "CAMBIAR_POR_LLAVE_REAL".equals(stripeApiKey)) {
            System.out.println("[StripeController] [MOCK] Generando sesión simulada para el negocio: " + idNegocio + " en plan: " + plan);
            
            // Activar de una vez la suscripción en base de datos para facilitar pruebas locales
            Empresa empresa = empresaOpt.get();
            empresa.setSuscripcionActiva(true);
            empresa.setPlanSuscripcion(plan);
            empresa.setFechaInicioSuscripcion(java.time.LocalDateTime.now());
            empresa.setFechaFinSuscripcion(java.time.LocalDateTime.now().plusDays(30));
            empresaRepository.save(empresa);
            System.out.println("[StripeController] [MOCK] Suscripción activada exitosamente en BD para " + empresa.getNombre() + " en plan: " + plan);

            String mockUrl = "http://localhost:4200/dashboard?payment=success&mock=true&idNegocio=" + idNegocio + "&plan=" + plan;
            return ResponseEntity.ok(Map.of("url", mockUrl));
        }

        // Integración Real con Stripe
        try {
            Stripe.apiKey = stripeApiKey;

            String successUrl = "http://localhost:4200/dashboard?payment=success&idNegocio=" + idNegocio + "&plan=" + plan;
            String cancelUrl = "http://localhost:4200/dashboard?payment=cancel";

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("mxn")
                                                    .setUnitAmount(unitAmount)
                                                    .setRecurring(
                                                            SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                    .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                                                    .build()
                                                    )
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(productName)
                                                                    .setDescription(productDesc)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("idNegocio", idNegocio.toString())
                    .putMetadata("plan", plan)
                    .build();

            Session session = Session.create(params);
            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (Exception e) {
            System.err.println("[StripeController] Error al crear sesión de checkout: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el pago con Stripe: " + e.getMessage()));
        }
    }

    /**
     * Webhook de Stripe para recibir notificaciones asíncronas de pagos exitosos.
     * POST /api/v1/stripe/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> recibirWebhook(@RequestBody String payload,
                                                 @RequestHeader("Stripe-Signature") String sigHeader) {
        
        Event event;

        // Si tenemos firma del webhook configurada, validar con el SDK oficial
        if (webhookSecret != null && !webhookSecret.trim().isEmpty() && !"CAMBIAR_POR_FIRMA_REAL".equals(webhookSecret)) {
            try {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } catch (Exception e) {
                System.err.println("[StripeController] Error de validación de firma en Webhook de Stripe: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");
            }
        } else {
            // Modo Desarrollo: Parsear sin firma
            try {
                event = Event.GSON.fromJson(payload, Event.class);
            } catch (Exception e) {
                System.err.println("[StripeController] Error al parsear JSON de Webhook: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payload inválido");
            }
        }

        System.out.println("[StripeWebhook] Evento recibido: " + event.getType());

        String eventType = event.getType();

        if ("checkout.session.completed".equals(eventType)) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Optional<Session> sessionOpt = deserializer.getObject().map(obj -> (Session) obj);

            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();
                String idNegocioStr = session.getMetadata().get("idNegocio");
                String planStr = session.getMetadata().get("plan");

                if (planStr == null) {
                    planStr = "BASIC";
                }

                if (idNegocioStr != null) {
                    try {
                        UUID idNegocio = UUID.fromString(idNegocioStr);
                        Optional<Empresa> empresaOpt = empresaRepository.findById(idNegocio);
                        
                        if (empresaOpt.isPresent()) {
                            Empresa empresa = empresaOpt.get();
                            empresa.setSuscripcionActiva(true);
                            empresa.setPlanSuscripcion(planStr.toUpperCase());
                            
                            // Guardar datos de Stripe
                            empresa.setStripeCustomerId(session.getCustomer());
                            empresa.setStripeSubscriptionId(session.getSubscription());

                            // Intentar recuperar fechas de la suscripción real
                            String subscriptionId = session.getSubscription();
                            if (subscriptionId != null && !subscriptionId.isEmpty()) {
                                try {
                                    com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
                                    if (subscription != null) {
                                        if (subscription.getCurrentPeriodStart() != null) {
                                            empresa.setFechaInicioSuscripcion(
                                                java.time.LocalDateTime.ofInstant(
                                                    java.time.Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), 
                                                    java.time.ZoneId.systemDefault()
                                                )
                                            );
                                        }
                                        if (subscription.getCurrentPeriodEnd() != null) {
                                            empresa.setFechaFinSuscripcion(
                                                java.time.LocalDateTime.ofInstant(
                                                    java.time.Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), 
                                                    java.time.ZoneId.systemDefault()
                                                )
                                            );
                                        }
                                    }
                                } catch (Exception ex) {
                                    System.err.println("[StripeWebhook] Error al obtener detalles de la suscripción de Stripe: " + ex.getMessage());
                                }
                            }

                            empresaRepository.save(empresa);
                            System.out.println("[StripeWebhook] ¡Suscripción activada con éxito para la empresa: " 
                                    + empresa.getNombre() + " en plan: " + planStr + "!");
                        } else {
                            System.err.println("[StripeWebhook] Empresa no encontrada con ID: " + idNegocio);
                        }
                    } catch (Exception e) {
                        System.err.println("[StripeWebhook] Error al activar la empresa en BD: " + e.getMessage());
                    }
                } else {
                    System.err.println("[StripeWebhook] Sesión completada no tiene metadata de 'idNegocio'.");
                }
            }
        } else if ("customer.subscription.deleted".equals(eventType)) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Optional<com.stripe.model.Subscription> subOpt = deserializer.getObject().map(obj -> (com.stripe.model.Subscription) obj);
            if (subOpt.isPresent()) {
                com.stripe.model.Subscription subscription = subOpt.get();
                String subscriptionId = subscription.getId();
                Optional<Empresa> empresaOpt = empresaRepository.findByStripeSubscriptionId(subscriptionId);
                if (empresaOpt.isPresent()) {
                    Empresa empresa = empresaOpt.get();
                    empresa.setSuscripcionActiva(false);
                    empresaRepository.save(empresa);
                    System.out.println("[StripeWebhook] Suscripción cancelada para la empresa: " + empresa.getNombre());
                }
            }
        } else if ("customer.subscription.updated".equals(eventType)) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Optional<com.stripe.model.Subscription> subOpt = deserializer.getObject().map(obj -> (com.stripe.model.Subscription) obj);
            if (subOpt.isPresent()) {
                com.stripe.model.Subscription subscription = subOpt.get();
                String subscriptionId = subscription.getId();
                Optional<Empresa> empresaOpt = empresaRepository.findByStripeSubscriptionId(subscriptionId);
                if (empresaOpt.isPresent()) {
                    Empresa empresa = empresaOpt.get();
                    boolean active = "active".equals(subscription.getStatus());
                    empresa.setSuscripcionActiva(active);
                    if (subscription.getCurrentPeriodStart() != null) {
                        empresa.setFechaInicioSuscripcion(
                            java.time.LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochSecond(subscription.getCurrentPeriodStart()), 
                                java.time.ZoneId.systemDefault()
                            )
                        );
                    }
                    if (subscription.getCurrentPeriodEnd() != null) {
                        empresa.setFechaFinSuscripcion(
                            java.time.LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), 
                                java.time.ZoneId.systemDefault()
                            )
                        );
                    }
                    empresaRepository.save(empresa);
                    System.out.println("[StripeWebhook] Suscripción actualizada para la empresa: " + empresa.getNombre() + " (Activa: " + active + ")");
                }
            }
        }

        return ResponseEntity.ok("Recibido");
    }
}
