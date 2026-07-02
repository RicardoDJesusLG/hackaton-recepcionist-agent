# 🚀 1. CONFIGURACIÓN INICIAL DEL PROYECTO

¡Bienvenido al equipo de desarrollo del Asistente de Agendamiento Inteligente! 

Este repositorio está optimizado para que **no tengas que escribir ningún comando en la consola ni configurar servidores manualmente**. Antigravity, tu asistente virtual de desarrollo en este IDE, puede leer la documentación y encargarse de todo el trabajo de terminal por ti.

Sigue las breves instrucciones a continuación para poner en marcha el proyecto de inmediato.

---

## 🔑 Lo único que debes configurar manualmente

Antes de pedirle a Antigravity que levante los servicios, debes enlazar tu Sandbox de WhatsApp:

1. **Configurar tu Sandbox en Meta**:
   - Inicia sesión en tu cuenta en [Meta Developers Console](https://developers.facebook.com/).
   - Abre tu aplicación de prueba y copia tu **Token de acceso temporal de 24 horas** y tu **Phone Number ID**.
2. **Pegar las credenciales en tu properties**:
   - Abre el archivo local: `backend/src/main/resources/application.properties`
   - En la **línea 26**, pega tu token temporal en la propiedad:
     `whatsapp.api.token=TU_TOKEN_TEMPORAL`
   - *(Recuerda que este token de Meta vence cada 24 horas).*

---

## ⚡ Prompt Maestro para levantar todo automáticamente

Una vez que guardaste tus credenciales de Meta en el properties, abre el chat de tu asistente **Antigravity** en este IDE, copia el siguiente bloque de texto y envíaselo:

```text
Antigravity, por favor lee las instrucciones en 'Documentacion/vERSION ACTUAL/manual_rapido.md' y 'Documentacion/vERSION ACTUAL/Manual_Frontend.md'. A partir de ellas, realiza de forma autónoma las siguientes acciones en segundo plano:
1. Levanta los contenedores de Docker (docker-compose up -d) para iniciar la base de datos PostgreSQL.
2. Inicia el servidor del backend de Spring Boot en la carpeta /backend.
3. Levanta una instancia de localtunnel apuntando al puerto 8080 para generar una Callback URL pública e indícame cuál es la URL generada.
4. Levanta el servidor de desarrollo de Angular (npm start) en la carpeta /frontend. E indicame la url para acceder al frontend junto con la clave de acceso al mismo
```

---

## 🖥️ Qué servicios se levantarán de forma automática

Cuando le envíes el prompt anterior a Antigravity y confirmes la ejecución, el agente pondrá en marcha de forma paralela:

1. **Base de Datos PostgreSQL (Puerto `5432`)**:
   - Arranca el contenedor de Docker con la base de datos `agendamiento_db` e inicializa las tablas.
2. **Servidor Backend Spring Boot (Puerto `8080`)**:
   - Compila e inicia la lógica del API REST del chat, la conexión al modelo Gemini de Vertex AI y el enrutador de webhooks.
3. **Servidor Frontend Angular (Puerto `4200`)**:
   - Compila y despliega el Dashboard administrativo en caliente. Podrás acceder directamente abriendo en tu navegador: [http://localhost:4200/](http://localhost:4200/).
4. **Localtunnel (Redirección HTTPS)**:
   - Expone tu puerto 8080 al exterior y te dará la URL pública (ej: `https://xxx.loca.lt`) que deberás pegar en la Callback URL de tu Sandbox de Meta Developer para recibir los mensajes de WhatsApp en tiempo real.

---

## 🔄 Nota sobre Cambios en el Código Java
* Cada vez que realices una modificación en los archivos `.java` del backend, debes indicarle a Antigravity: *"Antigravity, por favor reinicia el backend"* o hacerlo manualmente deteniendo la tarea en la consola y volviendo a ejecutar para que tus cambios se compilen y apliquen en el webhook.
