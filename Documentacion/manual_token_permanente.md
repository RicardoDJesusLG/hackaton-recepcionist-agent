# Manual: Generación de Token de Acceso Permanente para WhatsApp Cloud API 🔑

Por defecto, los tokens de acceso que genera Meta en el panel de desarrolladores son temporales y expiran en **24 horas**. Para un entorno de producción o pruebas continuas, es necesario generar un **System User Token** (Token de Usuario del Sistema) el cual es permanente y nunca expira.

Aquí tienes el paso a paso detallado para crearlo:

---

## Requisitos Previos
1. Tener una cuenta en [Meta for Developers](https://developers.facebook.com/).
2. Contar con un **Administrador comercial (Business Manager)** verificado o activo en Meta Business Suite.
3. Haber asociado tu aplicación de WhatsApp a este Business Manager.

---

## Paso a Paso para la Generación

### Paso 1: Ir a la Configuración del Negocio
1. En el menú lateral izquierdo, bajo la sección **Usuarios (Users)**, haz clic en **Usuarios del sistema (System Users)**.
2. Haz clic en el botón **Agregar (Add)**.
3. Asigna un nombre descriptivo al usuario (ej: `Chatbot-Recepcionista-SystemUser`).
4. Selecciona el rol de usuario del sistema como **Administrador (Admin)**.
5. Haz clic en **Crear usuario del sistema**.

### Paso 3: Asignar Activos a tu Usuario del Sistema
Para que el usuario pueda interactuar con la API, debe tener permisos sobre la aplicación de WhatsApp:
1. Dentro del perfil del usuario del sistema que acabas de crear, haz clic en **Asignar activos (Assign Assets)**.
2. Ve a la pestaña **Apps** y selecciona tu aplicación de WhatsApp.
3. Activa los permisos de **Control total (Manage App)**.
4. Haz clic en **Guardar cambios**.

### Paso 4: Generar el Token Permanente
1. Selecciona nuevamente el usuario del sistema en la lista.
2. Haz clic en el botón **Generar nuevo token (Generate New Token)**.
3. Se abrirá una ventana emergente:
   - Selecciona tu aplicación de WhatsApp en el menú desplegable.
   - **IMPORTANTE:** En la lista de permisos, debes marcar obligatoriamente los siguientes dos alcances (scopes):
     - `whatsapp_business_messaging` (Permite enviar y recibir mensajes).
     - `whatsapp_business_management` (Permite administrar plantillas y configuraciones comerciales).
4. Haz clic en **Generar token (Generate Token)**.
5. Meta te mostrará un recuadro con una cadena de texto larga. **Copia este token de inmediato y guárdalo en un lugar seguro**, ya que por motivos de seguridad Meta no te lo volverá a mostrar.

---

## Integración en la Aplicación
Una vez que tienes el token permanente:

1. **En la base de datos (Dinámico por negocio - Multi-inquilino):**
   - Inicia sesión en tu Dashboard Administrativo (`http://localhost:4200/login`).
   - Ve a **Perfil del Negocio** y en el campo de **WhatsApp Access Token**, pega tu nuevo token permanente.
   - De esta forma, el chatbot responderá usando este token para este negocio de forma automática y en tiempo real.

2. **Como variable de entorno global (Opcional/Respaldo):**
   - En tu servidor o sistema operativo local, configura la siguiente variable:
     ```bash
     WHATSAPP_API_TOKEN=tu_token_permanente_aqui
     ```
   - El backend de Spring Boot leerá este token de forma segura sin exponerlo directamente en el código de tu archivo `application.properties`.
