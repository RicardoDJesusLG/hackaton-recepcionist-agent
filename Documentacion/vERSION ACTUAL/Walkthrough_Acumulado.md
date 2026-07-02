# Bitácora de Cambios Acumulados (Walkthrough General)

Este documento detalla todas las modificaciones, diseños de arquitectura e integraciones que hemos implementado en conjunto para transformar la aplicación de agendamiento en un SaaS funcional e inteligente.

---

## 📅 1. Conexión e Integración con Meta Business API
* **Handshake de Verificación (Webhook GET)**:
  - Implementamos el endpoint de suscripción en [WhatsAppWebhookController.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/WhatsAppWebhookController.java) que responde con el challenge de texto plano cuando Meta valida la suscripción mediante el token secreto (`MiTokenSecretoDelHackathon2026`).
* **Consumo de Mensajes (Webhook POST)**:
  - Estructuramos el mapeo de payloads de tipo `whatsapp_business_account` para extraer el `from` (número del cliente), `body` (mensaje del usuario), y `phone_number_id` (identificador del teléfono del negocio).
* **Envío Asíncrono de Respuestas**:
  - Creamos el servicio asíncrono [WhatsAppService.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/service/WhatsAppService.java) que realiza peticiones HTTP POST a la Graph API de Meta (`https://graph.facebook.com/v20.0/{businessPhoneId}/messages`) utilizando un cliente HTTP asíncrono para enviar las respuestas generadas por el agente de forma inmediata sin bloquear el hilo principal.

---

## 🗄️ 2. Evolución del Modelo de Datos (Base de Datos)
* **Tabla `empresas`**:
  - Incorporamos campos dinámicos de `direccion` y `descripcion_negocio` (usado por la IA como contexto).
  - Añadimos la columna `suscripcion_activa` (boolean) para bloquear el webhook a empresas morosas.
  - Añadimos las columnas `telefono_contacto` y `maps_link` para que el bot pueda proporcionar soporte telefónico y enlaces GPS de Google Maps cuando el cliente los solicite.
* **Tabla `agenda_config`**:
  - Creada para almacenar los horarios generales de apertura y cierre por día de la semana (0 = Domingo, 6 = Sábado).
  - Añadimos una restricción de unicidad (`UniqueConstraint`) en la combinación de `(empresa_id, dia_semana)` para evitar registros duplicados por día.
* **Tabla `owners`**:
  - Se añadió la relación uno-a-uno o pertenencia mediante `empresa_id` (UUID) para mapear qué administrador tiene control sobre qué negocio.
* **Tabla `citas`**:
  - Control transaccional en `CitaService` para evitar solapamientos: antes de registrar una nueva cita, el sistema consulta las citas activas de ese día y evalúa que `inicio_solicitado < fin_existente` y `fin_solicitado > inicio_existente`.

---

## ⚙️ 3. Backend e Inteligencia del Chat (API & Agente)
* **Memoria Conversacional Persistente**:
  - Implementamos una caché en memoria basada en `ConcurrentHashMap` dentro de [AntigravityAgent.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/agent/AntigravityAgent.java) vinculada al número telefónico del cliente (`customerPhone`). Esto evita que Gemini pierda el contexto de la plática al cambiar de turno en WhatsApp.
* **Normalización de Teléfonos (Compatibilidad México)**:
  - Implementamos una regla de limpieza en [WhatsAppWebhookController.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/WhatsAppWebhookController.java) que convierte el prefijo de celular mexicano `521` enviado por Meta a `52` (formato estándar de 12 dígitos) de inmediato. Esto garantiza que las búsquedas en la base de datos de usuarios y citas coincidan perfectamente.
* **Inyección de Metadatos del Sistema**:
  - Diseñamos un formateador en `AntigravityAgent.chat(...)` que concatena un bloque técnico invisible al inicio de cada mensaje del usuario:
    `[Contexto del sistema - Fecha y Hora actual: YYYY-MM-DDTHH:mm, Empresa: 'Nombre' (ID: UUID), Teléfono de Soporte: X, Dirección: Y, Enlace de Google Maps: Z, Cliente Tel: W]`
  - Esto le proporciona a Gemini los UUIDs reales e información del sistema en cada turno de conversación, eliminando la alucinación de IDs y años (evita agendamientos en años incorrectos como 2024).
* **Function Calling (Herramientas de Gemini)**:
  - Declaramos e implementamos 6 funciones que el modelo puede invocar de forma autónoma:
    1. `obtenerCatalogoServicios`: Listado de servicios del negocio.
    2. `obtenerHorariosAtencion`: Horarios generales de la empresa.
    3. `consultarDisponibilidad`: Slots libres por día y servicio.
    4. `agendarCita`: Reservar cita validando solapamientos.
    5. `cancelarCita`: Cancelación lógica en BD.
    6. `obtenerMisCitas`: Citas pendientes del cliente.
* **Control de Errores e Instrucciones Estrictas**:
  - Se configuró el System Prompt en [AntigravityConfig.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/config/AntigravityConfig.java) para prohibir al bot inventar UUIDs ficticios de servicios y obligarle a reportar los errores del backend (como solapamientos o ID no encontrado) en lugar de mentir sobre la confirmación de la cita.

---

## 💻 4. Frontend Angular (Dashboard y Onboarding)
* **Registro de Negocios Integrado**:
  - Implementamos un switch selector en [register.component.html](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/register/register.component.html) ("Nuevo Negocio" vs "Vincular Existente") que permite a los dueños registrar su cuenta y crear su empresa (con dirección, WhatsApp Phone ID, teléfono de soporte y Google Maps link) en un solo paso.
* **Dashboard Tabulado**:
  - **Pestaña Citas**: Grid interactivo que lista todas las citas agendadas por WhatsApp en tiempo real con badges de estado (CONFIRMADA, CANCELADA) y botones de cancelación rápida.
  - **Pestaña Perfil**: Configuración de información del local, prompt del bot y un módulo de facturación de Stripe para cobro recurrente del SaaS.
  - **Pestaña Horarios**: Un planificador visual con switches de apertura y selectores de horas (de 9:00 AM a 6:00 PM) para habilitar o deshabilitar días completos en la agenda de la base de datos de manera intuitiva.
