# Manual de Configuración y Pruebas del Frontend (Angular)

Este manual te guiará para configurar y levantar el panel de administración web (Dashboard y Registro) del proyecto Recepcionista Virtual en tu entorno local.

---

## 🛠️ Requisitos Previos

Antes de iniciar, asegúrate de tener instalado en tu máquina:
* **Node.js** (Versión 18.0 o superior recomendada).
* **Angular CLI** (Opcional, instalado automáticamente mediante dependencias locales).

---

## 🚀 Pasos para Instalar y Levantar el Frontend

### Paso 1: Instalar dependencias (Primera vez)
Abre tu terminal, dirígete a la carpeta `frontend` y ejecuta la instalación de paquetes:
```bash
cd frontend
npm install
```
*(Espera a que finalice la descarga de la carpeta `node_modules`).*

### Paso 2: Levantar el Servidor de Desarrollo
Para arrancar la interfaz web, tienes dos opciones:

#### Opción A: Pedírselo a tu Asistente Antigravity (Recomendado)
Para no tener que lidiar con consolas adicionales, escribe lo siguiente en el chat de tu asistente coding AI:
* *"Antigravity, por favor levanta mi frontend Angular"* o *"Antigravity, corre el comando npm start en la carpeta frontend"*.
* Antigravity iniciará el servidor de desarrollo de Angular en segundo plano.

#### Opción B: Ejecución Manual en Terminal
Si prefieres hacerlo tú mismo, corre el comando npm en la carpeta `frontend`:
```bash
npm start
```

Una vez que el compilador termine (toma alrededor de 10-15 segundos), el servidor estará listo y escuchando en:
🔗 **URL Local**: [http://localhost:4200](http://localhost:4200)

---

## 🧭 Ligas y Rutas Clave de Pruebas

Con el servidor local encendido y tu navegador web abierto en `http://localhost:4200`, puedes probar los siguientes flujos de onboarding:

1. **Registro e Inicio de Negocio**:
   * 🔗 **Ruta**: `/register` -> [http://localhost:4200/register](http://localhost:4200/register)
   * **Qué probar**: Rellena los datos para crear un nuevo usuario propietario. Activa la pestaña **"Nuevo Negocio"** para dar de alta una empresa de prueba configurando su WhatsApp Phone ID, dirección, teléfono de soporte y enlace de Maps.
2. **Inicio de Sesión (Login)**:
   * 🔗 **Ruta**: `/login` -> [http://localhost:4200/login](http://localhost:4200/login)
   * **Qué probar**: Inicia sesión con el usuario que acabas de registrar para generar tu token JWT seguro.
3. **Dashboard de Administración**:
   * 🔗 **Ruta**: `/dashboard` -> [http://localhost:4200/dashboard](http://localhost:4200/dashboard)
   * **Qué probar**:
     * **Pestaña Citas**: Visualizarás los agendamientos que realices desde WhatsApp en tiempo real.
     * **Pestaña Horarios**: Activa/desactiva días de atención y cambia la hora de apertura/cierre.
     * **Pestaña Perfil**: Modifica los datos del negocio, su prompt especializado y prueba la vinculación simulada de Stripe.

---

## 💡 Notas Técnicas y Conectividad
* **Dependencia del Backend**: El frontend de Angular se comunica con la API REST del backend que corre en el puerto `8080` (utiliza un proxy de desarrollo para redirigir las peticiones `/api/*`). **Asegúrate de tener el backend de Spring Boot levantado** para que la autenticación, carga de citas y guardado de horarios funcionen.
* **Recarga en Caliente (Hot Reload)**: El servidor del frontend detecta automáticamente cualquier cambio que realices en el código HTML, TS o CSS de Angular y recargará la página del navegador web de inmediato sin necesidad de reiniciar la terminal.
