# Registro de Bugs y Errores Detectados

Este documento detalla los principales errores técnicos e inconsistencias detectadas durante la implementación e integración del agente cognitivo Antigravity con Vertex AI y el Webhook de WhatsApp, junto con sus explicaciones técnicas y soluciones correspondientes.

---

## 1. Error 404 de Endpoint Inexistente en Vertex AI

### Síntoma
El servidor arroja un error `404 Not Found` al intentar enviar un mensaje al agente cognitivo:
```text
Caused by: com.google.api.client.http.HttpResponseException: 404 Not Found
POST https://us-central1-aiplatform.googleapis.com:443/v1/projects/hackaton-recepcionist-agent/locations/us-central1/publishers/google/models/gemini-3.5-flash:generateContent
```

### Causa Raíz
* **Diferencia de Productos en Google Cloud (Plataforma de Agente vs. SDK de Vertex AI):**
  La consola de Google Cloud que muestra la **"Plataforma de agente" (Agent Platform / Agent Builder)** utiliza un motor de orquestación interno donde sí está disponible el modelo `gemini-3.5-flash` para construir playbooks y agentes visuales. 
  
  Sin embargo, el código Java de este backend está utilizando la clase `GenerativeModel` del **SDK de Vertex AI estándar**, el cual interactúa directamente con el endpoint de inferencia de modelos individuales de Google Cloud (`.../models/gemini-X-flash:generateContent`). 
  
  Para la API directa de inferencia de Vertex AI, el modelo `gemini-3.5-flash` requiere permisos de preview específicos, no está soportado de forma general en todos los endpoints REST directos, o su nombre difiere. Intentar llamarlo directamente por API REST genera un `404 Not Found` en la llamada HTTP del cliente.

### Solución
Restaurar los parámetros de Vertex AI de producción estables en [AntigravityAgent.java](file:///c:/Users/mejor/Documents/Desarrollo/hackatonfriends/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/agent/AntigravityAgent.java):
* **Location:** `"us-central1"`
* **Model ID:** `"gemini-1.5-flash"` (este es el modelo estándar general habilitado para llamadas de inferencia cruda de Vertex AI con Function Calling local).

---

## 2. Error de Decodificación UTF-8 (`Invalid UTF-8 start byte 0xbf`)

### Síntoma
La consola de Spring Boot registra la advertencia:
```text
2026-06-26T10:22:59.669-06:00  WARN 23996 --- [nio-8080-exec-1] .w.s.m.s.DefaultHandlerExceptionResolver : Resolved [org.springframework.http.converter.HttpMessageNotReadableException: JSON parse error: Invalid UTF-8 start byte 0xbf]
```

### Causa Raíz
Ocurre cuando se envían caracteres especiales en español como `¿` (cuyo código en codificación ANSI de Windows / ISO-8859-1 es `0xBF`) dentro de peticiones HTTP generadas desde PowerShell o cmd. Dado que Spring Boot espera estrictamente formato `UTF-8` en el cuerpo de la solicitud JSON, no puede interpretar el byte `0xBF` como un inicio válido de carácter UTF-8 y aborta el análisis del JSON.

### Solución
Forzar al cliente de pruebas (PowerShell, Curl) a utilizar codificación UTF-8. 
* En PowerShell, esto se soluciona convirtiendo explícitamente el cuerpo a formato de bytes UTF-8 antes de enviarlo:
  ```powershell
  $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyJson)
  Invoke-RestMethod -Uri $Uri -Method Post -Body $bodyBytes -ContentType "application/json; charset=utf-8"
  ```
* En peticiones curl, especificando el header `Content-Type: application/json; charset=utf-8`.

---

## 3. Error `./mvnw` no reconocido o no encontrado (Exit Code 127)

### Síntoma
Al ejecutar el comando `./mvnw clean spring-boot:run` en la raíz del proyecto, Windows arroja:
```text
bash: ./mvnw: No such file or directory
O bien en PowerShell:
El término './mvnw' no se reconoce como nombre de un cmdlet, función...
```

### Causa Raíz
* **Carpeta incorrecta:** El proyecto tiene una estructura donde la aplicación Maven se encuentra dentro del subdirectorio `/backend`, por lo que el script wrapper no existe en la carpeta raíz.
* **Extensión de archivo en Windows:** En Windows PowerShell, la sintaxis `./mvnw` busca un script de shell de Unix (`mvnw`). Para ejecutar el archivo de procesamiento de lotes de Windows se debe utilizar `.\mvnw.cmd`.

### Solución
Navegar primero a la carpeta de backend y ejecutar el wrapper de Windows:
```powershell
cd backend
.\mvnw.cmd clean spring-boot:run
```
