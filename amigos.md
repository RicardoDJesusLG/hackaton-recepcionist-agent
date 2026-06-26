# Manual de Colaboración: Agente Recepcionista Antigravity 

Este manual contiene las instrucciones detalladas para que puedas entender el flujo del proyecto, configurar tu entorno local tras realizar un `pull` y probar la integración con el agente cognitivo de Vertex AI y el Webhook de WhatsApp sin fricciones.

---

## 1. Descripción y Objetivo

El objetivo de esta implementación es construir un **Agente de Recepción y Agendamiento Autónomo** para WhatsApp. 
Este agente está integrado con **Vertex AI** (utilizando el modelo `gemini-1.5-flash`) y se conecta a una base de datos local PostgreSQL para responder preguntas sobre los catálogos de servicios, precios y duraciones de negocios reales utilizando **Function Calling (Llamada a Funciones locales)**.

---

## 2. Estructura y Cambios Realizados

Los cambios principales están encapsulados en el módulo `/backend`:

*   **[AntigravityAgent.java](file:///c:/Users/mejor/Documents/Desarrollo/hackatonfriends/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/agent/AntigravityAgent.java):** 
    *   Carga la Service Account de GCP desde los recursos del classpath.
    *   Inicializa la conexión con Vertex AI usando la región `us-central1` y transporte HTTP/REST.
    *   Declara la función `obtenerCatalogoServicios` (Skill de catálogo) para que Gemini decida cuándo consultar la base de datos de manera autónoma.
    *   Interpreta la respuesta de Gemini, ejecuta la consulta SQL local en caso de Function Calling y le devuelve los datos formateados a Gemini para generar una respuesta empática.
*   **[WhatsAppWebhookController.java](file:///c:/Users/mejor/Documents/Desarrollo/hackatonfriends/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/WhatsAppWebhookController.java):**
    *   `GET /api/v1/whatsapp/webhook`: Valida el handshake obligatorio exigido por Meta usando el token de verificación.
    *   `POST /api/v1/whatsapp/webhook`: Recibe los mensajes entrantes de los usuarios en formato JSON (payload oficial de WhatsApp Cloud API), extrae el texto del mensaje y lo envía al flujo conversacional del agente Antigravity.
*   **[application.properties](file:///c:/Users/mejor/Documents/Desarrollo/hackatonfriends/hackaton-recepcionist-agent/backend/src/main/resources/application.properties):** 
    *   Contiene la configuración de conexión PostgreSQL, configuración optimizada de Hikari Connection Pool (`maximum-pool-size=5`, `connection-timeout=40000`) y el token de seguridad del Webhook (`MiTokenSecretoDelHackathon2026`).
*   **[.gitignore](file:///c:/Users/mejor/Documents/Desarrollo/hackatonfriends/hackaton-recepcionist-agent/.gitignore):**
    *   Ignora el archivo de credenciales de Google Cloud (`antigravity-credentials.json`) para evitar fugas accidentales de seguridad en GitHub.

---

## 3. Configuración Inicial (Post-Pull)

Para poder ejecutar el proyecto en tu máquina local después de hacer `git pull`, sigue estos pasos:

1.  **Ejecutar la Base de Datos:**
    Asegúrate de tener levantado el contenedor de PostgreSQL (usando Docker Compose en la raíz o tu cliente local en el puerto `5432`).
2.  **Agregar Credenciales de Vertex AI:**
    Consigue el archivo de credenciales de la cuenta de servicio de Google Cloud (`antigravity-credentials.json`) de tu equipo y colócalo en la siguiente ruta:
    `backend/src/main/resources/antigravity-credentials.json`
    *Nota: Este archivo está ignorado por Git, por lo que nunca se subirá al repositorio.*

---

## 4. Ejecución del Servidor

Navega a la carpeta de backend y levanta la aplicación:

```powershell
cd backend
.\mvnw.cmd clean spring-boot:run
```
*(Espera a ver el mensaje `Tomcat started on port 8080` en tu consola).*

---

## 5. Instrucciones Paso a Paso para Pruebas e Interacción

Para validar el flujo completo sin necesidad de un cliente real de WhatsApp, abre una terminal de **PowerShell** independiente y ejecuta las siguientes pruebas:

### Prueba A: Validación de Webhook (Handshake GET)
Valida que el handshake requerido por Meta funcione.

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=MiTokenSecretoDelHackathon2026&hub.challenge=RetoWhatsApp123" -Method Get
```
*   **Esperado:** Debe responder en texto plano: `RetoWhatsApp123`
*   **Log en servidor:** `¡Handshake exitoso! Webhook verificado con Meta.`

---

### Prueba B: Mensaje de Texto Simple (POST)
Valida la recepción de un mensaje de texto simple que despierta al agente de Vertex AI.

```powershell
$bodyJson = @{
    entry = @(
        @{
            changes = @(
                @{
                    value = @{
                        messages = @(
                            @{
                                type = "text"
                                text = @{
                                    body = "Hola, ¿qué servicios tienen disponibles?"
                                }
                            }
                        )
                    }
                }
            )
        }
    )
} | ConvertTo-Json -Depth 10

# Evita problemas de decodificación ANSI/UTF-8 para caracteres especiales como "¿"
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyJson)

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/whatsapp/webhook" -Method Post -Body $bodyBytes -ContentType "application/json; charset=utf-8"
```
*   **Esperado:** Devuelve `200 OK` (vacío).
*   **En consola del servidor:** Verás los logs de entrada del mensaje y la respuesta empática del Agente Antigravity en tiempo real.

---

### Prueba C: Consulta con Llamada a Base de Datos (Function Calling)
Valida que el modelo de IA reconozca que el cliente pregunta por una empresa en particular, active la herramienta Java y traiga la información desde PostgreSQL.

*(Reemplaza el UUID `a5e7c10b-d24f-4d92-bf3b-ec81512411e7` por un ID de empresa válido que tengas en tu base de datos local).*

```powershell
$bodyJson = @{
    entry = @(
        @{
            changes = @(
                @{
                    value = @{
                        messages = @(
                            @{
                                type = "text"
                                text = @{
                                    body = "Hola, ¿cuáles son los servicios del negocio a5e7c10b-d24f-4d92-bf3b-ec81512411e7?"
                                }
                            }
                        )
                    }
                }
            )
        }
    )
} | ConvertTo-Json -Depth 10

$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyJson)

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/whatsapp/webhook" -Method Post -Body $bodyBytes -ContentType "application/json; charset=utf-8"
```
*   **En consola del servidor:** Verás la traza: `[AntigravityAgent] Function Calling detectado para obtenerCatalogoServicios de empresaId: a5e7c10b-d24f-4d92-bf3b-ec81512411e7`. Posterior a esto, se listará el catálogo extraído del PostgreSQL y el agente dará una respuesta final conteniendo dichos datos estructurados.

---

## 6. Reglas de Desarrollo Importantes (¡Evita errores comunes!)

1.  **Región/Ubicación de Vertex AI:** 
    Asegúrate de mantener siempre `.setLocation("us-central1")` en el builder de Vertex AI. **No uses `"global"`**, ya que los endpoints directos de generación de contenido (`generateContent`) a través del SDK no están disponibles en la ruta global de la API REST de Google Cloud.
2.  **Versión del Modelo en Código:**
    Usa `"gemini-1.5-flash"` como modelo estable en el constructor de `GenerativeModel`. Aunque la consola de GCP pueda mostrar `"gemini-3.5-flash"` para la plataforma visual de agentes, el SDK crudo requiere usar la versión pública habilitada para peticiones directas en tu región.
3.  **Reinicios del Servidor:**
    Si modificas cualquier clase Java, detén tu servidor con **`Ctrl + C`** y vuelve a compilar y ejecutar con `.\mvnw.cmd clean spring-boot:run`. De lo contrario, el servidor seguirá ejecutando en memoria las clases antiguas.
4.  **Pruebas Unitarias Rápidas:**
    Puedes ejecutar todas las pruebas del backend localmente antes de hacer un push ejecutando:
    ```powershell
    .\mvnw.cmd test
    ```
