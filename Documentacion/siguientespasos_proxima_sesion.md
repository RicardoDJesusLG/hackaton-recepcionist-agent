# Recordatorios para la Siguiente Sesión de Trabajo

Este documento sirve como bitácora y recordatorio de los pendientes acordados para nuestra próxima sesión de desarrollo.

---

## 📌 Pendiente Principal: Revisión del Plan de Acción de Google Calendar

En la siguiente sesión de trabajo, el objetivo principal será analizar y dar los primeros pasos para la integración con la **Google Calendar API**.

### 🗓️ Detalles de la Implementación de Google Calendar:
* **Decisión de Arquitectura:** Se optó por la **Alternativa B (Integración oficial mediante OAuth 2.0 y Google Calendar API)** para permitir la sincronización en tiempo real de las citas agendadas de forma bidireccional y directa en el calendario del propietario.
* **Costo de la Google Calendar API:**
  * **$0 USD / $0 MXN (Es 100% gratuita):** Google no realiza ningún cobro por crear, modificar o eliminar eventos a través de su API para calendarios de usuarios finales, siempre y cuando se esté dentro del límite de uso general del proyecto de Google Cloud (el cual es sumamente generoso, del orden de millones de peticiones al día, por lo que es prácticamente imposible que una aplicación comercial estándar llegue a tener algún costo).

---

## 💬 Resumen de Costos y Decisiones de WhatsApp (Meta)

* **Recordatorio de Costos por Recordatorios:**
  * Meta cobra por ventanas de 24 horas. Para México, una ventana de tipo *Utility* (donde entran los recordatorios automáticos) cuesta **~$0.30 - $0.35 MXN**.
  * **Optimización:** Si configuramos los recordatorios a las **24 horas** y a las **2 horas** antes de la cita, ambos mensajes caen dentro de la misma ventana de 24 horas, por lo que el costo total de recordatorios por cita agendada se optimiza a solo **~$0.30 MXN**.

---

## 🛠️ Pasos de Desarrollo para la Siguiente Sesión:

1. **Configuración en Google Cloud Console:**
   * Crear la pantalla de consentimiento de OAuth 2.0.
   * Generar las credenciales `Client ID` y `Client Secret` de Google.
2. **Desarrollo en el Backend (Spring Boot):**
   * Crear el modelo para almacenar los tokens OAuth de cada empresa.
   * Crear las rutas redirigidas para completar el flujo de autorización OAuth 2.0.
   * Desarrollar la sincronización en segundo plano con la API de Google Calendar al agendar o cancelar citas.
