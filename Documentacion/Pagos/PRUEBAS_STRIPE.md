# PRUEBAS_STRIPE.md — Guía Operativa de Pruebas con Stripe CLI (Modo Test)

Este documento detalla el procedimiento paso a paso para realizar pruebas del flujo de cobro por suscripción utilizando el simulador oficial de Stripe (Stripe CLI).

---

## 1. Levantar el Túnel del Webhook Local
Para redirigir los eventos de tu cuenta de Stripe (test) a tu servidor local de Spring Boot, ejecuta el siguiente comando en la consola:

```bash
./stripe listen --forward-to localhost:8080/api/v1/payments/webhook
```

Al inicializarse, la consola de Stripe CLI te devolverá una salida indicando el secreto para la firma criptográfica:
`Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxxxxxxxxxxxxx`

> 🔑 **Importante:** Copia esa firma y configúrala en tu `application.properties` bajo la propiedad `stripe.webhook.secret`.

---

## 2. Tarjetas de Prueba Oficiales de Stripe

Cuando la pasarela Stripe Checkout te solicite los datos de pago, utiliza las siguientes tarjetas para simular diferentes escenarios:

| Marca/Escenario | Número de Tarjeta | CVC | Fecha Expiración | Resultado Esperado |
| :--- | :--- | :--- | :--- | :--- |
| **Visa (Éxito)** | `4242 4242 4242 4242` | `123` | Cualquier fecha futura | ✅ Pago exitoso y activación automática |
| **Visa (Autenticación 3D Secure)** | `4000 0025 0000 3155` | `123` | Cualquier fecha futura | 🔐 Solicita verificación adicional |
| **Visa (Fondos Insuficientes)** | `4000 0000 0000 9995` | `123` | Cualquier fecha futura | ❌ Rechazado (mantiene membresía inactiva) |

---

## 3. Validación Operativa mediante Logs

### logs en Stripe CLI:
```bash
2026-07-05 01:10:00   --> checkout.session.completed [evt_1P...]
2026-07-05 01:10:00  <--  [200] POST http://localhost:8080/api/v1/payments/webhook
```

### logs en el Backend (Spring Boot):
```
[PaymentsWebhook] Evento verificado recibido: checkout.session.completed
[PaymentsWebhook] Estado de suscripción actualizado a true para: PetCo Banda
```
