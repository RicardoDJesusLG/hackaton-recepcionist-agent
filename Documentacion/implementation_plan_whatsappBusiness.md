# Implementación de WhatsApp Tokens Dinámicos (Modelo Multi-Tenant)

Este plan detalla los cambios requeridos para soportar múltiples empresas (clientes) utilizando sus propios tokens de acceso permanente de la API de WhatsApp, evitando tokens fijos en la configuración del servidor y permitiendo el aislamiento de cuentas (Modelo A). 

Además, se añadirá la interfaz gráfica necesaria en el frontend (Registro y Edición del Perfil de Negocio) para que el dueño del negocio pueda ingresar y actualizar su token de manera segura.

## User Review Required

> [!IMPORTANT]
> - La base de datos local de desarrollo (`agendamiento_db` en Docker) se actualizará automáticamente al iniciar el servidor de Spring Boot gracias a la propiedad `spring.jpa.hibernate.ddl-auto=update`. Sin embargo, para ambientes de producción o migraciones manuales futuras, se requerirá añadir la columna `whatsapp_token` de tipo `TEXT` (o `VARCHAR`) a la tabla `empresas`.
> - Las empresas que no tengan un `whatsapp_token` guardado en la base de datos usarán por defecto el token global configurado en `application.properties` (si existe), lo cual actúa como un mecanismo de fallback para el Sandbox/pruebas locales.
> - Por seguridad, el token en la base de datos se podrá actualizar desde el panel, pero para evitar exponerlo en texto plano en el navegador, se retornará enmascarado en las peticiones GET (ejemplo: `EAA...[ENMASCARADO]`).

## Proposed Changes

### Backend (Java)

---

#### [MODIFY] [Empresa.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/model/Empresa.java)
- Añadir la propiedad `whatsappToken` con su correspondiente anotación `@Column(name = "whatsapp_token", columnDefinition = "TEXT")`. Esto permitirá almacenar tokens de acceso permanentes de Meta de cualquier longitud.

#### [MODIFY] [WhatsAppService.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/service/WhatsAppService.java)
- Cambiar la firma del método `enviarMensajeTexto` para que acepte un parámetro adicional `customToken` de tipo `String`.
- En el cuerpo del método, determinar si `customToken` es válido; de lo contrario, usar el token global `apiToken` como fallback.
- Utilizar este token dinámico en el header `Authorization: Bearer <token>`.

#### [MODIFY] [WhatsAppWebhookController.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/WhatsAppWebhookController.java)
- En el método `recibirMensaje`, al procesar un mensaje entrante de una empresa registrada, extraer su `whatsappToken`.
- Pasar dicho token en las llamadas al método `whatsAppService.enviarMensajeTexto`.

#### [MODIFY] [AuthController.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/AuthController.java)
- Leer `whatsappToken` del payload JSON de registro en `registrarOwner` y asignarlo a la entidad `Empresa` si se está creando una nueva.

#### [MODIFY] [DashboardController.java](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/backend/src/main/java/com/example/agente/controller/DashboardController.java)
- En `actualizarEmpresa`, leer `whatsappToken` del cuerpo de la petición y actualizarlo si no está vacío o enmascarado.
- En `obtenerDetalleEmpresa`, retornar una copia de la empresa con el `whatsappToken` enmascarado (por ejemplo, mostrando solo los primeros 6 caracteres si existe) para proteger el valor real frente al frontend.

---

### Frontend (Angular)

---

#### [MODIFY] [register.component.html](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/register/register.component.html)
- Añadir un campo de entrada para `whatsappToken` de tipo `password` o `text` justo debajo de `WhatsApp Phone Number ID` para que sea obligatorio al registrar un nuevo negocio.

#### [MODIFY] [register.component.ts](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/register/register.component.ts)
- Definir la propiedad `whatsappToken = ''`.
- Validar que no esté vacío si `crearNuevaEmpresa` es verdadero.
- Añadir `whatsappToken` al payload de envío al backend.

#### [MODIFY] [dashboard.component.html](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/dashboard/dashboard.component.html)
- Añadir un campo de entrada para `whatsappToken` en la sección de configuración de la empresa, de modo que el dueño pueda cambiar o actualizar el token si expira.

#### [MODIFY] [dashboard.component.ts](file:///c:/Users/Ricardo/.gemini/antigravity/scratch/hackaton-recepcionist-agent/frontend/src/app/dashboard/dashboard.component.ts)
- Añadir la propiedad `whatsappToken` al objeto de inicialización de `empresa`.

---

## Verification Plan

### Manual Verification
1. Iniciar los servicios utilizando la base de datos Docker y el backend.
2. Registrar un nuevo usuario y negocio a través de la pantalla de Registro del frontend, ingresando el `whatsappPhoneId` y un token simulado en `whatsappToken`.
3. Validar que los campos se guarden en la base de datos y que al entrar al dashboard, la pestaña "Perfil de Negocio" muestre los campos y permita actualizarlos de manera exitosa.
4. Enviar un mensaje de prueba al webhook y comprobar en los logs del backend que se utiliza el token dinámico guardado para realizar el POST de respuesta a Meta.
