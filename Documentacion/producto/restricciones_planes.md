# Restricciones y Características de los Planes de Suscripción

Este documento detalla los costos, características y restricciones de los planes de suscripción para los negocios afiliados a **Whappify**. Su propósito es servir de guía para el diseño y actualización de la interfaz de usuario (Frontend).

---

## Comparativa de Costos y Características Generales

| Característica / Plan | **Plan BASIC (Básico)** | **Plan PREMIUM** |
| :--- | :--- | :--- |
| **Costo Mensual** | **$299.00 MXN** / mes | **$499.00 MXN** / mes |
| **Moneda** | Pesos Mexicanos (MXN) | Pesos Mexicanos (MXN) |
| **Citas Mensuales** | Máximo **60 citas** | **Ilimitadas** |
| **Servicios Activos** | Máximo **3 servicios** | **Ilimitados** |
| **Google Maps (Ubicación)**| ❌ No disponible |  Disponible |
| **Pasarela de Pagos (Stripe)**| ❌ No disponible |  Disponible |
| **Soporte Técnico** | Soporte en horario laboral | Soporte prioritario 24/7 |

---

##  Restricciones y Reglas de Negocio en Detalle

### 1. Plan BASIC (Básico)
* **Límite Mensual de Citas:** El negocio puede recibir un máximo de **60 citas exitosas al mes**. Una vez superado este límite, no se permitirán agendar más citas hasta el siguiente ciclo de facturación.
* **Control Anti-Spam (WhatsApp):**
  * Los clientes de WhatsApp solo pueden tener un máximo de **3 citas activas o programadas a futuro** de manera simultánea.
  * Los clientes de WhatsApp tienen un límite de **3 citas agendadas para el mismo día**.
* **Catálogo de Servicios:** El negocio solo puede tener hasta **3 servicios activos** en su catálogo.
* **Geolocalización:** El bot de WhatsApp no compartirá enlaces directos a Google Maps.
* **Pagos en Línea:** El negocio no puede solicitar anticipos ni cobrar citas a través del bot de WhatsApp.

### 2. Plan PREMIUM
* **Límite Mensual de Citas:** Sin restricciones. El negocio puede recibir citas de forma **ilimitada**.
* **Control Anti-Spam Flexible:**
  * Los clientes pueden tener hasta **10 citas activas o programadas a futuro** de manera simultánea.
  * Los clientes pueden agendar hasta **10 citas para el mismo día** (ideal para clientes frecuentes o corporativos).
* **Catálogo de Servicios:** Sin límite de servicios activos en la plataforma.
* **Geolocalización:** El bot de WhatsApp enviará automáticamente el enlace de Google Maps del local cuando los clientes pregunten por la dirección o cómo llegar.
* **Pagos en Línea:** Permite la integración directa con Stripe para cobrar anticipos o pagos completos a los clientes antes de confirmar su cita.
