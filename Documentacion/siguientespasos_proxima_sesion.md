# Recordatorios para la Siguiente Sesión de Trabajo

Este documento sirve como bitácora y recordatorio de los pendientes acordados para nuestra próxima sesión de desarrollo.

---

## 📌 Estado de la Integración de Google Calendar (¡Completada! 🎉)

Se ha completado satisfactoriamente toda la integración de **Google Calendar API** con las siguientes características:
* **OAuth 2.0:** Flujo de autenticación completo y seguro.
* **Seguridad:** Redirección automática de Google al panel del frontend (`/dashboard?googleCalendar=success`) y limpieza de parámetros de URL.
* **Desvinculación:** Opción en tiempo real para borrar credenciales de la BD y desconectar la cuenta.
* **Estado en Producción (GCP):** Proyecto configurado como "En producción" en la Google Cloud Console, lo que permite a cualquier usuario vincular su calendario (omitiendo la advertencia de app no verificada).
* **Sincronización Bidireccional Asíncrona:** Agendar y cancelar citas actualiza de inmediato el calendario de Google en segundo plano sin retrasar las respuestas de la API.

---

## 💬 Resumen de Costos y Decisiones de WhatsApp (Meta)

* **Recordatorio de Costos por Recordatorios:**
  * Meta cobra por ventanas de 24 horas. Para México, una ventana de tipo *Utility* (donde entran los recordatorios automáticos) cuesta **~$0.30 - $0.35 MXN**.
  * **Optimización:** Si configuramos los recordatorios a las **24 horas** y a las **2 horas** antes de la cita, ambos mensajes caen dentro de la misma ventana de 24 horas, por lo que el costo total de recordatorios por cita agendada se optimiza a solo **~$0.30 MXN**.

---

## 🛠️ Pasos Propuestos para la Siguiente Sesión:

1. **Mensajería de WhatsApp con Plantillas (Meta):**
   * Configurar plantillas aprobadas por Meta para el envío de recordatorios automáticos (24h / 2h antes de la cita).
   * Programar las tareas automáticas (Cron Jobs) en Spring Boot para realizar el barrido de citas y enviar los recordatorios de WhatsApp.
2. **Pasarela de Pagos (Stripe):**
   * Configurar flujos de cobro de anticipos o pagos completos para servicios específicos.
   * Conectar Webhooks de Stripe para confirmar citas automáticamente al recibir el pago.
3. **Seguridad y Extracción de Secretos:**
   * Extraer las API Keys y tokens hardcodeados (Stripe, Google, WhatsApp, JWT y base de datos) del código fuente y configurarlas a través de variables de entorno para evitar subirlos expuestos al repositorio público.


