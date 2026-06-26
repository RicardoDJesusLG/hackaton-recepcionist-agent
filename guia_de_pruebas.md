# Guía de Pruebas: Webhook de WhatsApp y Vertex AI

Este documento sirve como referencia rápida para probar localmente el flujo de integración de WhatsApp con el backend y el agente cognitivo de Vertex AI.

---

## 1. Prueba de Verificación (Handshake GET)

### Motivo de la Prueba
Meta (WhatsApp) requiere validar que el endpoint del Webhook sea real, seguro y responda con el mismo reto (`challenge`) enviado en los parámetros de la URL cuando se configura por primera vez. Esto evita registros fraudulentos de servidores.

### Parámetros Requeridos
* **Verify Token:** Debe coincidir con `whatsapp.verification.token` configurado en `application.properties` (`MiTokenSecretoDelHackathon2026`).
* **Challenge:** Una cadena aleatoria enviada por Meta que el servidor debe devolver idéntica en texto plano.

### Comandos de Ejecución

#### En Windows (PowerShell)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=MiTokenSecretoDelHackathon2026&hub.challenge=PruebaHandshake123" -Method Get
```

#### Con Curl (cmd / Unix)
```bash
curl "http://localhost:8080/api/v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=MiTokenSecretoDelHackathon2026&hub.challenge=PruebaHandshake123"
```

### Resultado Esperado
* **HTTP Status:** `200 OK`
* **Cuerpo de Respuesta:** `PruebaHandshake123`
* **Log en Consola:** `¡Handshake exitoso! Webhook verificado con Meta.`

---

## 2. Simulación de Mensaje de Texto (POST)

### Motivo de la Prueba
Simula la recepción de un mensaje de texto simple enviado por un usuario desde su teléfono móvil a través de WhatsApp. Sirve para validar que el `WhatsAppWebhookController` parsea correctamente el payload JSON de Meta, extrae el texto del mensaje y lo envía a la clase del agente cognitivo.

### Comandos de Ejecución

#### En Windows (PowerShell)
*(Usa codificación explícita UTF-8 para evitar errores con caracteres como `¿`)*
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

# Codificar a UTF-8 antes de enviar
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyJson)

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/whatsapp/webhook" -Method Post -Body $bodyBytes -ContentType "application/json; charset=utf-8"
```

#### Con Curl (cmd / Unix)
```bash
curl -X POST "http://localhost:8080/api/v1/whatsapp/webhook" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{
    "entry": [
      {
        "changes": [
          {
            "value": {
              "messages": [
                {
                  "type": "text",
                  "text": {
                    "body": "Hola, ¿qué servicios tienen disponibles?"
                  }
                }
              ]
            }
          }
        ]
      }
    ]
  }'
```

---

## 3. Prueba de Integración con Base de Datos (Function Calling)

### Motivo de la Prueba
Evalúa si Vertex AI puede activar la llamada a funciones locales (`Function Calling`) para interactuar con la base de datos PostgreSQL cuando el usuario consulte detalles específicos de un negocio. Valida:
1. Reconocimiento de la intención por parte de Gemini.
2. Invocación de la capa de servicio Java (`servicioSkill.obtenerCatalogoServicios(empresaId)`).
3. Respuesta empática final generada por el agente utilizando la información de la base de datos.

### Comandos de Ejecución

#### En Windows (PowerShell)
*(Asegúrate de cambiar el ID del negocio por un UUID válido existente en tu base de datos)*
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
                                    body = "Hola, ¿qué servicios tienen en el negocio a5e7c10b-d24f-4d92-bf3b-ec81512411e7?"
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

#### Con Curl (cmd / Unix)
```bash
curl -X POST "http://localhost:8080/api/v1/whatsapp/webhook" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{
    "entry": [
      {
        "changes": [
          {
            "value": {
              "messages": [
                {
                  "type": "text",
                  "text": {
                    "body": "Hola, ¿qué servicios tienen en el negocio a5e7c10b-d24f-4d92-bf3b-ec81512411e7?"
                  }
                }
              ]
            }
          }
        ]
      }
    ]
  }'
```
