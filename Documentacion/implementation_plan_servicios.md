# Plan de Implementación: Catálogo de Servicios (CRUD) y Seguridad del Prompt Especializado

Este plan detalla los cambios requeridos para construir la interfaz y la API de administración de servicios (Catálogo), permitiendo a los dueños de negocios crear, editar y eliminar sus servicios desde el Dashboard. También incluye el diseño de seguridad (Guardrails) para el prompt especializado y el modal de re-autenticación (doble factor por contraseña) en el frontend.

## Seguridad y Doble Factor de Autorización

> [!IMPORTANT]
> **Modal de Re-Autenticación (Confirmación Crítica):**
> Al modificar las directivas del bot (Prompt Especializado), se debe garantizar que un atacante con acceso a la sesión activa o una persona no autorizada no comprometa la IA.
> - Al hacer clic en "Guardar Configuración", si el campo `descripcionNegocio` ha sido modificado, el frontend detendrá la acción y mostrará un modal indicando:
>   *"Advertencia: Esto cambiará la información de la base de datos de manera permanente y modificará el comportamiento del agente de WhatsApp en tiempo real. Por favor, confirma tus credenciales para proceder."*
> - El modal solicitará **Usuario** y **Contraseña**.
> - Al presionar "Confirmar", se llamará al servicio de autenticación para validar las credenciales. Si son correctas, se enviará el PUT definitivo para actualizar los datos.

> [!WARNING]
> **Prompt Guardrail (Filtro de Inyección):**
> En el System Prompt maestro del backend, colocaremos el prompt especializado del negocio delimitado por `<descripcion_negocio>` e indicaremos al modelo Gemini que descarte cualquier instrucción que intente anular el comportamiento de recepción, regalar citas, cambiar de idioma, o insultar a los usuarios.

---

## Proposed Changes

### Backend (Java)

---

#### [MODIFY] [AntigravityConfig.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/config/AntigravityConfig.java) (o donde esté el System Prompt de inicialización)
- Reforzar el System Prompt maestro para incluir la regla de sanamiento y priorización de instrucciones de seguridad sobre las directivas libres del negocio.

#### [MODIFY] [ServicioRepository.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/repository/ServicioRepository.java)
- Añadir el método `List<Servicio> findByEmpresaId(UUID empresaId);` para poder listar todos los servicios de un negocio en su panel (activos e inactivos).

#### [MODIFY] [ServicioController.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/ServicioController.java)
- Añadir endpoints para crear (`POST`), actualizar (`PUT`) y eliminar (`DELETE`) servicios.
- Proteger estos endpoints requiriendo que la petición provenga del negocio autenticado (`empresaId` extraído del token JWT).

---

### Frontend (Angular)

---

#### [MODIFY] [auth.service.ts](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/auth.service.ts)
- Añadir un método `verificarCredenciales(username, password)` que retorne el observable del POST de login sin alterar el `localStorage`.

#### [MODIFY] [dashboard.component.html](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/dashboard/dashboard.component.html)
- Añadir una nueva pestaña en el menú de navegación: **"🏷️ Servicios"**.
- Diseñar la vista de la pestaña con:
  - Una tabla o grid listando los servicios existentes.
  - Un botón de **"Agregar Servicio"**.
  - Botones de **Editar** y **Eliminar** para cada servicio.
  - Un formulario modal o inline para crear/editar servicios.
- **Modal de Re-autenticación**: Diseñar una caja de diálogo oculta por defecto (usando `@if`) que solicite usuario/contraseña cuando se va a guardar el prompt especializado.

#### [MODIFY] [dashboard.component.ts](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/dashboard/dashboard.component.ts)
- Definir variables para almacenar la lista de servicios del negocio.
- Almacenar temporalmente el estado anterior del `descripcionNegocio` para detectar si cambió al hacer clic en guardar.
- Si cambió, levantar el modal de re-autenticación.
- Al ingresar los datos en el modal, consumir `verificarCredenciales` de `AuthService`. Si es exitoso, cerrar modal y enviar el PUT de actualización.

#### [MODIFY] [dashboard.service.ts](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/dashboard.service.ts)
- Añadir funciones de integración HTTP con `/api/v1/servicios` (`post`, `put`, `delete`).

---

## Verification Plan

### Manual Verification
1. Iniciar los servicios.
2. Cambiar la descripción en la pestaña de Perfil y dar clic en "Guardar".
3. Validar que aparezca el modal de re-autenticación indicando el cambio permanente.
4. Digitar credenciales incorrectas y validar el mensaje de error.
5. Digitar credenciales correctas y validar que se guarde el dato y se actualice el sistema.
