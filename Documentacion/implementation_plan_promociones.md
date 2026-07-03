# Plan de Implementación: Promociones Dinámicas y Corrección de Prompt Especializado

Este plan detalla los cambios requeridos para corregir la omisión de `descripcion_negocio` en el agente cognitivo de WhatsApp, y para agregar soporte estructurado de promociones que el administrador del negocio puede activar, desactivar, redactar o eliminar libremente.

## Proposed Changes

### Backend (Java)

---

#### [MODIFY] [Empresa.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/model/Empresa.java)
- Añadir las columnas `promocionActiva` (`promocion_activa` boolean) y `promocionDescripcion` (`promocion_descripcion` TEXT).

#### [MODIFY] [DashboardController.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/DashboardController.java)
- En `obtenerDetalleEmpresa`, incluir `promocionActiva` y `promocionDescripcion` en la respuesta JSON.
- En `actualizarEmpresa`, leer e incluir estos dos campos del payload y guardarlos en la base de datos.

#### [MODIFY] [AntigravityAgent.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/agent/AntigravityAgent.java)
- Actualizar la firma de `chat(...)` para aceptar `descripcionNegocio`, `promocionActiva` y `promocionDescripcion`.
- Formatear el contexto del sistema inyectado a Gemini para incluir la descripción y reglas del negocio, y las promociones vigentes con directivas claras para calcular descuentos dinámicamente si corresponde.

#### [MODIFY] [WhatsAppWebhookController.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/WhatsAppWebhookController.java)
- Pasar `descripcionNegocio`, `promocionActiva` y `promocionDescripcion` desde la entidad `Empresa` en la llamada a `antigravityAgent.chat(...)`.

---

### Frontend (Angular)

---

#### [MODIFY] [dashboard.component.ts](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/dashboard/dashboard.component.ts)
- Añadir las propiedades `promocionActiva: false` y `promocionDescripcion: ''` al objeto de inicialización de `empresa`.
- Implementar una función `eliminarPromocion()` que limpie los campos en local y dispare el guardado del formulario para impactar los cambios inmediatamente.

#### [MODIFY] [dashboard.component.html](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/dashboard/dashboard.component.html)
- En la pestaña "Perfil de Negocio", añadir una tarjeta dedicada de "Promoción del Negocio" con:
  - Un switch/toggle para activar/desactivar la promoción (`empresa.promocionActiva`).
  - Un campo de texto (`textarea`) para describir la promoción (`empresa.promocionDescripcion`).
  - Un botón de "Eliminar Promoción" que llame a `eliminarPromocion()`.

---

## Verification Plan

### Manual Verification
1. Iniciar los servicios y acceder al frontend.
2. Ir a la pestaña "Perfil de Negocio" en el dashboard.
3. Activar una promoción ficticia escribiendo *"20% de descuento en todos los servicios de corte hoy"* y activar el switch. Guardar.
4. Interactuar con el bot simulando una pregunta de costos del catálogo, y validar que aplique el descuento directamente en la conversación de WhatsApp.
5. Hacer clic en "Eliminar Promoción", y validar que se limpie de la base de datos e interfaz, y el bot deje de ofrecerla.
