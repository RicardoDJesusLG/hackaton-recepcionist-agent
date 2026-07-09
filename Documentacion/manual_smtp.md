# Manual: Configuración de SMTP para Envío de Correos en Spring Boot 📬

Este manual describe cómo habilitar y configurar el servicio de correos del backend para enviar notificaciones de restablecimiento de contraseña mediante servidores **SMTP estándar** (ej: Gmail u Outlook).

---

## 1. Configuración General en Spring Boot

El backend utiliza la dependencia oficial `spring-boot-starter-mail`. Los parámetros de conexión se controlan a través del archivo `backend/src/main/resources/application.properties` en la siguiente sección:

```properties
# ============================================
# 5. CONFIGURACION DE SMTP (CORREO ELECTRONICO)
# ============================================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu-correo-real@gmail.com
spring.mail.password=tu-contrasena-de-aplicacion
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

> [!NOTE]
> **Modo Fallback para Desarrollo:**
> Si los campos `spring.mail.username` y `spring.mail.password` se dejan con sus valores predeterminados o de prueba, el servicio de correos detectará esto de forma automática y **no intentará enviar ningún correo real**, sino que imprimirá el contenido y el código de verificación directamente en la consola del backend para agilizar tus pruebas locales sin necesidad de configurar credenciales.

---

## 2. Configuración para Gmail (Google Workspace)

Google ya no permite usar tu contraseña de Gmail habitual para que aplicaciones de terceros envíen correos por motivos de seguridad. Debes generar una **Contraseña de aplicación (App Password)**.

### Paso a Paso en tu Cuenta de Google:
1. Ve a tu [Cuenta de Google](https://myaccount.google.com/).
2. En el menú lateral izquierdo, haz clic en **Seguridad (Security)**.
3. Asegúrate de tener activada la **Verificación en dos pasos (2-Step Verification)**. Si no está activa, actívala.
4. En el buscador superior de tu cuenta de Google, escribe **"Contraseñas de aplicaciones"** (o "App passwords") y accede al menú correspondiente.
5. Asigna un nombre a la aplicación (ej: `Chatbot-Recepcionista`).
6. Haz clic en **Crear (Create)**.
7. Google te mostrará una **contraseña de 16 caracteres** dentro de un recuadro amarillo. **Copia esta contraseña** (sin los espacios).
8. Pega esta clave de 16 caracteres en la propiedad:
   `spring.mail.password=tu_clave_de_16_caracteres`
9. Asegúrate de configurar `spring.mail.host=smtp.gmail.com` and `spring.mail.port=587`.

---

## 3. Configuración para Outlook / Office 365

Si utilizas una cuenta de Microsoft (Outlook o Hotmail):

### Parámetros de Propiedades:
- **Host:** `smtp.office365.com`
- **Puerto:** `587`
- **Propiedades adicionales:**
  ```properties
  spring.mail.host=smtp.office365.com
  spring.mail.port=587
  spring.mail.username=tu-correo@outlook.com
  spring.mail.password=tu-contrasena-o-clave-de-aplicacion
  spring.mail.properties.mail.smtp.auth=true
  spring.mail.properties.mail.smtp.starttls.enable=true
  ```

*Nota: Si tu cuenta de Outlook tiene activada la autenticación de dos factores, también deberás generar una contraseña de aplicación desde el panel de seguridad de Microsoft en lugar de usar tu contraseña estándar.*

---

## 4. Configuración para Hostinger / Zoho / Dominios Propios

Si utilizas un proveedor de correo corporativo para tu dominio propio:

### Parámetros Comunes:
- **Hostinger:** `smtp.hostinger.com` (Puerto `587` o `465` con SSL).
- **Zoho Mail:** `smtp.zoho.com` (Puerto `587`).
- **Username:** `noreply@tudominio.com` (Tu correo corporativo completo).
- **Password:** La contraseña asignada a esa cuenta de correo.

---

## 5. Buenas Prácticas de Seguridad para Producción

Para evitar exponer tus contraseñas del SMTP en el código fuente de tu repositorio de control de versiones, se recomienda usar **Variables de Entorno** en tu servidor de producción:

```properties
spring.mail.username=${SMTP_USERNAME:}
spring.mail.password=${SMTP_PASSWORD:}
```

De este modo, configuras `SMTP_USERNAME` y `SMTP_PASSWORD` directamente en las variables de entorno de tu servidor de hosting (ej. Render, Railway, AWS, Heroku) de forma totalmente segura.
