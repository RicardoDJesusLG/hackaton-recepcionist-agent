# Walkthrough: Cambios e Implementaciones Clave 🛠️

Este documento recopila de manera ordenada todos los desarrollos e integraciones realizadas en esta sesión de trabajo para la optimización de tu plataforma.

---

## 1. Prevención de Spam y Sabotaje en Reservas 🛡️
Se implementó un sistema rígido en el backend para evitar que un usuario sature la agenda del negocio agendando citas de forma masiva:

- **Límite Total de Citas Activas:** Se modificó `CitaService.java`. Antes de agendar una nueva cita, el sistema consulta en PostgreSQL si el usuario ya cuenta con **3 citas futuras/activas** (en estado `CONFIRMADA`). De ser así, se rechaza.
- **Límite de Citas Diarias:** Se colocó una segunda restricción de un máximo de **3 citas por día** para un mismo cliente, evitando bloqueos de agendas completas en un solo día.
- **Respuesta Amigable:** Cuando el backend lanza la excepción por superar el límite, el agente cognitivo de IA interpreta el mensaje y le responde al cliente en WhatsApp de manera amable explicándole que debe asistir o cancelar alguna cita antes de pedir más.

---

## 2. Promociones por Servicio (Protección contra Inyección de Prompt) 🎁
Se rediseñó la lógica de promociones globales para evitar que los usuarios puedan alterar precios mediante técnicas de inyección de prompt:

- **Eliminación de la Promo Global:** Se removieron los campos de texto libre generales de promociones.
- **Promoción Estricta por Servicio:** Ahora, en el catálogo, cada servicio cuenta con su propio control de promociones en el administrador.
- **Tipos de Promoción Soportados:** `NINGUNA`, `DESCUENTO_PORCENTAJE` (requiere un entero del 1 al 100), y `PERSONALIZADA` (valida caracteres especiales y restringe inyecciones mediante filtros de palabras prohibidas como "ignora", "sistema", "gratis a todos", etc.).
- **Guardrails en la IA:** El Prompt del Sistema del agente de Vertex AI fue actualizado con instrucciones explícitas para ignorar directivas extrañas dentro de los textos personalizados.

---

## 3. Migración de Autenticación a Correo Electrónico 📧
Para evitar conflictos con los números de teléfono asociados a las APIs de WhatsApp y facilitar la recuperación de cuentas:

- **Entidad `Owner`**: El campo `username` (que almacenaba el teléfono) fue reemplazado por `email`.
- **Formularios del Frontend**: Las interfaces de registro y login de Angular fueron actualizadas con validaciones específicas de formato de correo.
- **JWT**: El token JWT de seguridad ahora emite y valida el `email` del administrador como sujeto.
- **Base de Datos**: Se ejecutó una inicialización limpia mediante `ddl-auto=create` para migrar las tablas de PostgreSQL de forma automatizada y consistente.

---

## 4. Integración de Correo mediante SMTP Estándar 📬
Se abandonó el flujo manual y la dependencia de SendGrid en favor de una solución de SMTP integrada en Spring Boot:

- **Librería JavaMail**: Añadida a las dependencias en `pom.xml`.
- **EmailService con Fallback**:
  - Si el SMTP está configurado con credenciales temporales o por defecto, **imprime el contenido y el código del correo en la terminal del backend** (perfecto para desarrollo local y pruebas rápidas).
  - Si se introducen credenciales reales en `application.properties`, realiza el envío del correo real de manera fluida.
- **Flujo de Recuperación**: El endpoint `/forgot-password` ahora genera un código OTP de 6 dígitos que se envía al correo del dueño para reestablecer su contraseña.

---

## 5. Pruebas y Gestión E2E (WhatsApp Real) ✅
Se verificó el flujo completo con las credenciales permanentes de Meta Developer cargadas directamente en el panel de administrador de tu negocio:

- **Inyección Dinámica de Tokens:** El backend recupera el token de acceso de WhatsApp directamente de la base de datos de manera dinámica.
- **Prueba exitosa en caliente:** Mandaste el mensaje *"Hola"* a través de tu número de pruebas de WhatsApp, el backend procesó el webhook, consultó tus horarios de atención para la barbería, detectó el horario no disponible (a medianoche) y envió la respuesta de vuelta a tu celular de manera exitosa (Código HTTP 200).
