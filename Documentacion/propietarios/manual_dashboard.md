# Manual del Dashboard para Propietarios

Este manual describe el funcionamiento general del panel de administración (Dashboard) de **Whappify** y proporciona una guía práctica para que los propietarios creen un **Prompt Especializado** eficaz para su agente virtual.

---

## 🖥️ Aspectos Generales del Dashboard

El Dashboard es el centro de control del negocio. Permite gestionar toda la interacción con los clientes antes de que las citas se concreten en WhatsApp.

1. **Perfil del Negocio:**
   * Configuración de datos básicos: Nombre del local, dirección física, teléfono de contacto y enlace de Google Maps.
   * **Descripción/Directivas del Negocio:** Un campo de texto abierto sumamente importante. Aquí es donde se ingresa el **Prompt Especializado** que el agente de inteligencia artificial leerá para entender el negocio y sus políticas.

2. **Gestión de Servicios:**
   * Creación, edición y eliminación de servicios.
   * Configuración de nombre del servicio, descripción amigable, duración en minutos y precio de lista.
   * El sistema genera automáticamente identificadores únicos (IDs) para que el agente valide la disponibilidad en tiempo real.

3. **Configuración de Horarios (Agenda):**
   * Definición de las horas de apertura y cierre para cada día de la semana.
   * Si un día se configura como "Cerrado" o no se le asigna horario, el agente de WhatsApp bloqueará automáticamente cualquier intento de agendamiento para esa fecha.

4. **Monitoreo de Citas:**
   * Vista de citas agendadas por clientes, estados (Confirmadas, Canceladas), y datos de contacto de los clientes de WhatsApp.

---

## 🧠 Guía para Crear tu Prompt Especializado (Directivas del Negocio)

El campo **"Descripción/Directivas del Negocio"** es la personalidad de tu recepcionista virtual. Para que tu agente de respuestas altamente precisas y evite confusiones, debes responder a las siguientes preguntas y redactar las respuestas en este campo:

### 📋 Cuestionario de Definición del Agente

1. **¿A qué se dedica tu negocio y cuáles son tus especialidades principales?**
   * *Ejemplo:* "Somos un spa boutique enfocado en masajes relajantes, tratamientos faciales orgánicos y bienestar integral." o "Somos un taller mecánico especializado en transmisiones automáticas y afinación de motores."
2. **¿Cuál es el tono de comunicación?**
   * *Ejemplo:* "¿Quieres que hable de 'tú' o de 'usted'? ¿Debe ser sumamente formal y elegante, o casual, alegre y amigable con muchos emojis?"
3. **¿Cuáles son tus políticas de tolerancia y cancelación?**
   * *Ejemplo:* "Hay una tolerancia de 15 minutos de retraso. Las cancelaciones deben hacerse con al menos 2 horas de anticipación."
4. **¿Qué debe saber o traer el cliente antes de su cita?**
   * *Ejemplo:* "Para el servicio de coctelería a domicilio, el cliente debe tener una mesa lista de 2 metros. Para servicios de spa, venir con ropa cómoda y sin maquillaje."
5. **¿Cuáles son los métodos de pago aceptados en el local?**
   * *Ejemplo:* "Aceptamos efectivo, transferencias bancarias y tarjetas Visa/Mastercard. No aceptamos American Express."
6. **¿Qué servicios o preguntas frecuentes adicionales debes cubrir?**
   * *Ejemplo:* "¿Cuentas con estacionamiento o valet parking? ¿El local es pet-friendly? ¿Tienes área de niños?"
7. **¿Cómo debe manejar el agente las solicitudes especiales o quejas?**
   * *Ejemplo:* "Si el cliente pide un descuento especial o quiere reportar una queja, proporciónale el Teléfono de Soporte del Local y dile que un humano le atenderá a la brevedad."

---

### 📝 Ejemplo de un Prompt Especializado Exitoso

> "Somos un servicio de barra móvil de coctelería artesanal premium para eventos sociales como bodas, graduaciones, XV años y reuniones privadas.
> Actúa como asistente de 'Coctelería Express'. Trata al cliente de 'tú' de forma muy alegre y usa emojis relacionados con bebidas (🍸, 🍹).
>
> **Políticas del Local:**
> - Tolerancia máxima de 10 minutos por retraso.
> - Si preguntan por estacionamiento, aclara que tenemos estacionamiento gratuito dentro de la plaza comercial.
> - Métodos de pago: Efectivo y transferencia (proporcionar datos si el cliente lo pide).
>
> **Indicaciones de Cita:**
> - Recordar siempre al cliente que para eventos a domicilio necesitamos acceso a conexión eléctrica de 110v a menos de 5 metros del área de barra."
