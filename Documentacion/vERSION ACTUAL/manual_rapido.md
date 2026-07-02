# Guía Rápida de Pruebas Locales (WhatsApp Webhook)

Esta guía rápida está diseñada para que cualquier desarrollador o colaborador del equipo configure su entorno de pruebas local de WhatsApp en menos de 5 minutos.

---

## 📋 Resumen de Pasos a Realizar

Sigue esta lista ordenada de tareas para iniciar tu entorno:

### 1. Generar el Token de Meta (Dura 24 horas)
1. Entra a tu cuenta en [Meta Developers Console](https://developers.facebook.com/).
2. Abre tu aplicación de WhatsApp Sandbox y copia el **Token de acceso temporal**.
3. **⚠️ NOTA IMPORTANTE**: Este token tiene una vigencia estricta de **24 horas**. Si expira o el bot deja de responder al día siguiente, debes regresar a la consola de Meta, generar un token nuevo y volver a pegarlo.

### 2. Pegar el Token en tu Configuración
1. Abre el archivo de configuración del backend:
   `backend/src/main/resources/application.properties`
2. Ve a la **línea 26** y reemplaza el valor de la propiedad con tu token generado:
   ```properties
   whatsapp.api.token=PEGA_AQUI_TU_TOKEN_DE_META_DE_24_HORAS
   ```

### 3. Iniciar la Base de Datos Local
1. Abre tu terminal en la raíz del proyecto.
2. Inicia los servicios de Docker:
   ```bash
   docker-compose up -d
   ```

### 4. Compilar y Levantar el Backend
1. En tu terminal, ingresa a la carpeta del backend y ejecuta el servidor:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
2. **🔄 REINICIAR EL SERVIDOR**: Cada vez que realices una modificación en el código Java, debes detener el backend (`Ctrl + C` en la terminal) y volver a ejecutar el comando `./mvnw spring-boot:run` para que los cambios se vean reflejados en tus pruebas.

### 5. Levantar el Túnel Público de Conexión
Meta no puede enviar mensajes a tu `localhost`. Necesitamos una URL pública con HTTPS.
1. **Pídele a tu asistente Antigravity**:
   * Escríbele en el chat: *"Antigravity, por favor levanta un túnel de localtunnel para mi puerto 8080"*.
   * Antigravity se encargará de ejecutar el comando `npx localtunnel --port 8080` en background y te entregará una URL como `https://xxxxx.loca.lt`.
   * **⚠️ NOTA**: Debes solicitarle a Antigravity levantar este túnel **cada vez que inicies una nueva sesión de pruebas**, ya que las URLs públicas son efímeras y expiran si se desconectan.

### 6. Configurar la URL en Meta
1. Ve a la sección **WhatsApp** > **Configuración** de tu panel de Meta Developer.
2. Haz clic en **Editar** Webhook.
3. Pega la URL del túnel que te entregó Antigravity agregando el path del webhook al final:
   `https://xxxxx.loca.lt/api/v1/whatsapp/webhook`
4. En Token de Verificación escribe: `MiTokenSecretoDelHackathon2026` y guarda los cambios.
5. Asegúrate de dar clic en **Suscritos** para el campo `messages`.

---

## 💬 Cómo Probar en WhatsApp
1. Agrega el número de tu celular personal en la sección de **"Números autorizados"** del panel de Meta.
2. Envía un mensaje de WhatsApp (ej. *"Hola"*) al número de prueba que te asignó Meta.
3. El webhook redirigirá el mensaje a tu backend local, tu agente Antigravity lo procesará y te llegará la respuesta directamente en tu WhatsApp en pocos segundos.
