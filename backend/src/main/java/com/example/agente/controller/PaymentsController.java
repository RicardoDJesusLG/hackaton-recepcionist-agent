package com.example.agente.controller;

import com.example.agente.model.Empresa;
import com.example.agente.repository.EmpresaRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
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
@RequestMapping("/api/v1/payments")
public class PaymentsController {

    private final EmpresaRepository empresaRepository;
    private final String webhookSecret;

    public PaymentsController(EmpresaRepository empresaRepository,
                              @Value("${stripe.webhook.secret}") String webhookSecret) {
        this.empresaRepository = empresaRepository;
        this.webhookSecret = webhookSecret;
    }

    /**
     * POST /api/v1/payments/create-checkout-session
     * Si la empresa ya está activa y tiene un ID de cliente de Stripe, redirige al Customer Portal.
     * Si no, crea una nueva sesión de checkout de Stripe en modo SUBSCRIPTION.
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> crearCheckoutSession(@RequestBody Map<String, String> request) {
        String empresaIdStr = request.get("empresaId");

        if (empresaIdStr == null || empresaIdStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'empresaId' es requerido"));
        }

        UUID empresaId;
        try {
            empresaId = UUID.fromString(empresaIdStr.trim());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "El ID de la empresa no es un UUID válido"));
        }

        Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
        if (empresaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Empresa no encontrada"));
        }

        Empresa empresa = empresaOpt.get();

        try {
            // Si ya tiene suscripción activa y Customer ID, redirigir al portal de gestión
            if (Boolean.TRUE.equals(empresa.getSuscripcionActiva()) && empresa.getStripeCustomerId() != null) {
                com.stripe.param.billingportal.SessionCreateParams portalParams = 
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(empresa.getStripeCustomerId())
                        .setReturnUrl("http://localhost:4200/dashboard")
                        .build();

                com.stripe.model.billingportal.Session portalSession = 
                        com.stripe.model.billingportal.Session.create(portalParams);

                return ResponseEntity.ok(Map.of("url", portalSession.getUrl()));
            }

            // Crear sesión de Checkout
            String successUrl = "http://localhost:4200/dashboard?payment=success&idNegocio=" + empresaId;
            String cancelUrl = "http://localhost:4200/dashboard?payment=cancel";

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("mxn")
                                                    .setUnitAmount(59900L) // $599.00 MXN
                                                    .setRecurring(
                                                            SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                    .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                                                    .build()
                                                    )
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Membresía Recepción Inteligente")
                                                                    .setDescription("Suscripción mensual al bot WhatsApp de recepción y agenda.")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("empresaId", empresaId.toString())
                    .setSubscriptionData(
                            SessionCreateParams.SubscriptionData.builder()
                                    .putMetadata("empresaId", empresaId.toString())
                                    .build()
                    );

            if (empresa.getStripeCustomerId() != null) {
                builder.setCustomer(empresa.getStripeCustomerId());
            } else {
                builder.setCustomerEmail(empresa.getNombre().toLowerCase().replaceAll("\\s+", "") + "@negocio.com");
            }

            Session session = Session.create(builder.build());
            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (Exception e) {
            System.err.println("[PaymentsController] Error al procesar sesión: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al conectar con Stripe: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/payments/webhook
     * Recibe los eventos de Stripe validados mediante firma estricta.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> recibirWebhook(@RequestBody String payload,
                                                 @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("[PaymentsWebhook] Falló la verificación de firma del webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma de webhook inválida");
        } catch (Exception e) {
            System.err.println("[PaymentsWebhook] Error al analizar el evento del webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error de procesamiento");
        }

        System.out.println("[PaymentsWebhook] Evento verificado recibido: " + event.getType());

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(event);
            case "customer.subscription.created" -> handleSubscriptionCreated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> System.out.println("[PaymentsWebhook] Evento no procesado: " + event.getType());
        }

        return ResponseEntity.ok("Verificado");
    }

    private void handleCheckoutCompleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<Session> sessionOpt = deserializer.getObject().map(obj -> (Session) obj);

        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            String empresaIdStr = session.getMetadata().get("empresaId");

            if (empresaIdStr != null) {
                actualizarEstadoEmpresa(empresaIdStr, session.getCustomer(), session.getSubscription(), true);
            }
        }
    }

    private void handleSubscriptionCreated(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<Subscription> subOpt = deserializer.getObject().map(obj -> (Subscription) obj);

        if (subOpt.isPresent()) {
            Subscription subscription = subOpt.get();
            String empresaIdStr = subscription.getMetadata().get("empresaId");
            if (empresaIdStr != null) {
                actualizarEstadoEmpresa(empresaIdStr, subscription.getCustomer(), subscription.getId(), true);
            }
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<Invoice> invoiceOpt = deserializer.getObject().map(obj -> (Invoice) obj);

        if (invoiceOpt.isPresent()) {
            Invoice invoice = invoiceOpt.get();
            String subscriptionId = invoice.getSubscription();

            if (subscriptionId != null) {
                Optional<Empresa> empresaOpt = empresaRepository.findByStripeSubscriptionId(subscriptionId);
                if (empresaOpt.isPresent()) {
                    Empresa empresa = empresaOpt.get();
                    empresa.setSuscripcionActiva(true);
                    
                    try {
                        Subscription subscription = Subscription.retrieve(subscriptionId);
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
                        System.err.println("[PaymentsWebhook] Error al obtener detalles de la suscripción tras renovación: " + ex.getMessage());
                    }

                    empresaRepository.save(empresa);
                    System.out.println("[PaymentsWebhook] Renovación de suscripción exitosa para: " + empresa.getNombre());
                }
            }
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<Subscription> subOpt = deserializer.getObject().map(obj -> (Subscription) obj);

        if (subOpt.isPresent()) {
            Subscription subscription = subOpt.get();
            String subscriptionId = subscription.getId();

            Optional<Empresa> empresaOpt = empresaRepository.findByStripeSubscriptionId(subscriptionId);
            if (empresaOpt.isPresent()) {
                Empresa empresa = empresaOpt.get();
                empresa.setSuscripcionActiva(false);
                empresaRepository.save(empresa);
                System.out.println("[PaymentsWebhook] Suscripción cancelada para la empresa: " + empresa.getNombre());
            }
        }
    }

    private void actualizarEstadoEmpresa(String empresaIdStr, String customerId, String subscriptionId, boolean activo) {
        try {
            UUID idNegocio = UUID.fromString(empresaIdStr);
            Optional<Empresa> empresaOpt = empresaRepository.findById(idNegocio);
            if (empresaOpt.isPresent()) {
                Empresa empresa = empresaOpt.get();
                empresa.setSuscripcionActiva(activo);
                empresa.setStripeCustomerId(customerId);
                empresa.setStripeSubscriptionId(subscriptionId);

                // Intentar recuperar fechas de la suscripción real
                if (subscriptionId != null && !subscriptionId.isEmpty()) {
                    try {
                        Subscription subscription = Subscription.retrieve(subscriptionId);
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
                        System.err.println("[PaymentsWebhook] Error al obtener detalles de la suscripción de Stripe: " + ex.getMessage());
                    }
                }

                empresaRepository.save(empresa);
                System.out.println("[PaymentsWebhook] Estado de suscripción actualizado a " + activo + " para: " + empresa.getNombre());
            }
        } catch (Exception e) {
            System.err.println("[PaymentsWebhook] Error al actualizar estado de la empresa: " + e.getMessage());
        }
    }
}
