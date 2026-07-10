package com.example.agente.agent;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.*;
import com.google.cloud.vertexai.api.*;
import com.example.agente.service.ServicioSkill;
import com.example.agente.service.CitaService;
import com.example.agente.dto.CitaRequestDTO;
import com.example.agente.dto.CitaResponseDTO;
import com.example.agente.dto.ServicioDTO;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agente cognitivo de Antigravity que encapsula las interacciones con Vertex
 * AI.
 * Utiliza el transporte oficial REST/HTTP para evitar problemas de conectividad
 * gRPC.
 * Mantiene la memoria de la conversación (historial) por número de teléfono.
 */
public class AntigravityAgent {

    private final String systemPrompt;
    private final float temperature;
    private final ServicioSkill servicioSkill;
    private final CitaService citaService;
    private VertexAI vertexAI;
    private GenerativeModel model;

    // Caché en memoria para almacenar las sesiones de chat activas por número de teléfono
    private final Map<String, ChatSession> activeSessions = new ConcurrentHashMap<>();

    public AntigravityAgent(String systemPrompt, float temperature, ServicioSkill servicioSkill, CitaService citaService) {
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.servicioSkill = servicioSkill;
        this.citaService = citaService;
        initVertexAI();
    }

    private void initVertexAI() {
        try {
            // 1. Cargar las credenciales desde el classpath (src/main/resources)
            InputStream is = new ClassPathResource("antigravity-credentials.json").getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

            String projectId = "hackaton-recepcionist-agent";

            // 2. Configurar Vertex AI forzando el transporte HTTP REST unificado
            this.vertexAI = new VertexAI.Builder()
                    .setProjectId(projectId)
                    .setLocation("us-central1")
                    .setCredentials(credentials)
                    .setTransport(com.google.cloud.vertexai.Transport.REST)
                    .build();

            // 3. Declaraciones de funciones (Skills) para Gemini

            // Skill 1: Catálogo de servicios
            FunctionDeclaration obtenerCatalogoServiciosDecl = FunctionDeclaration.newBuilder()
                    .setName("obtenerCatalogoServicios")
                    .setDescription(
                            "Obtiene la lista de servicios activos de una empresa o negocio a partir de su ID. Úsalo obligatoriamente cuando el cliente pregunte por los servicios disponibles, precios o duración de un negocio en particular.")
                    .setParameters(Schema.newBuilder()
                            .setType(Type.OBJECT)
                            .putProperties("empresaId", Schema.newBuilder()
                                     .setType(Type.STRING)
                                     .setDescription(
                                             "El ID de la empresa o negocio en formato UUID (cadena de texto estándar)")
                                     .build())
                            .addRequired("empresaId")
                            .build())
                    .build();

            // Skill 2: Consultar disponibilidad
            FunctionDeclaration consultarDisponibilidadDecl = FunctionDeclaration.newBuilder()
                    .setName("consultarDisponibilidad")
                    .setDescription(
                            "Consulta los horarios disponibles para agendar una cita de un servicio específico en una fecha determinada. "
                            + "Úsalo cuando el cliente quiera saber qué horarios hay libres para un servicio en una fecha. "
                            + "Necesitas el ID del servicio (obtenido previamente del catálogo) y la fecha en formato YYYY-MM-DD. "
                            + "Si el cliente no ha especificado un servicio, primero consulta el catálogo.")
                    .setParameters(Schema.newBuilder()
                            .setType(Type.OBJECT)
                            .putProperties("empresaId", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID de la empresa en formato UUID")
                                    .build())
                            .putProperties("servicioId", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID del servicio en formato UUID (obtenido del catálogo de servicios)")
                                    .build())
                            .putProperties("fecha", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("La fecha solicitada en formato YYYY-MM-DD (ejemplo: 2026-07-01)")
                                    .build())
                            .addRequired("empresaId")
                            .addRequired("servicioId")
                            .addRequired("fecha")
                            .build())
                    .build();

            // Skill 3: Agendar cita
            FunctionDeclaration agendarCitaDecl = FunctionDeclaration.newBuilder()
                    .setName("agendarCita")
                    .setDescription(
                            "Agenda una nueva cita para el cliente. Úsalo cuando el cliente confirme que desea agendar en un horario específico. "
                            + "Necesitas: el ID de la empresa, el ID del servicio, la fecha y hora de inicio en formato ISO (YYYY-MM-DDTHH:MM), "
                            + "y el número de teléfono del cliente (proporcionado automáticamente por el contexto del sistema o metadatos). "
                            + "IMPORTANTE: Antes de agendar, asegúrate de que el cliente haya elegido un servicio y un horario disponible. "
                            + "Si no ha elegido, pregúntale primero.")
                    .setParameters(Schema.newBuilder()
                            .setType(Type.OBJECT)
                            .putProperties("empresaId", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID de la empresa en formato UUID")
                                    .build())
                            .putProperties("servicioId", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID del servicio en formato UUID")
                                    .build())
                            .putProperties("fechaHoraInicio", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("Fecha y hora de inicio de la cita en formato ISO 8601 (ejemplo: 2026-07-01T10:00)")
                                    .build())
                            .putProperties("telefonoCliente", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El número de teléfono WhatsApp del cliente (proporcionado automáticamente por el contexto del sistema)")
                                    .build())
                            .addRequired("empresaId")
                            .addRequired("servicioId")
                            .addRequired("fechaHoraInicio")
                            .addRequired("telefonoCliente")
                            .build())
                    .build();

            // Skill 4: Cancelar cita
            FunctionDeclaration cancelarCitaDecl = FunctionDeclaration.newBuilder()
                    .setName("cancelarCita")
                    .setDescription(
                            "Cancela una cita existente del cliente. Úsalo cuando el cliente solicite cancelar una de sus citas. "
                            + "Necesitas el ID de la cita que el cliente quiere cancelar. "
                            + "IMPORTANTE: Antes de cancelar, muestra las citas del cliente usando 'obtenerMisCitas' para que elija cuál cancelar. "
                            + "Pide confirmación explícita al cliente antes de ejecutar la cancelación.")
                    .setParameters(Schema.newBuilder()
                            .setType(Type.OBJECT)
                            .putProperties("idCita", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID de la cita a cancelar en formato UUID")
                                    .build())
                            .putProperties("empresaId", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID de la empresa en formato UUID")
                                    .build())
                            .addRequired("idCita")
                            .addRequired("empresaId")
                            .build())
                    .build();

            // Skill 5: Obtener mis citas
            FunctionDeclaration obtenerMisCitasDecl = FunctionDeclaration.newBuilder()
                    .setName("obtenerMisCitas")
                    .setDescription(
                            "Obtiene las citas futuras del cliente que está escribiendo. "
                            + "Úsalo cuando el cliente pregunte por sus citas agendadas, quiera ver su agenda, "
                            + "o cuando necesites mostrarle sus citas antes de cancelar una. "
                            + "Necesitas el teléfono del cliente (proporcionado automáticamente) y el ID de la empresa.")
                    .setParameters(Schema.newBuilder()
                            .setType(Type.OBJECT)
                            .putProperties("empresaId", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID de la empresa en formato UUID")
                                    .build())
                            .putProperties("telefonoCliente", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El número de teléfono WhatsApp del cliente (proporcionado automáticamente por el contexto del sistema)")
                                    .build())
                            .addRequired("empresaId")
                            .addRequired("telefonoCliente")
                            .build())
                    .build();

            // Skill 6: Obtener horarios de atención general del negocio
            FunctionDeclaration obtenerHorariosAtencionDecl = FunctionDeclaration.newBuilder()
                    .setName("obtenerHorariosAtencion")
                    .setDescription(
                            "Obtiene los días de la semana y horas en que el negocio está abierto o cerrado. "
                            + "Úsalo cuando el cliente pregunte por los horarios de apertura, cierre, "
                            + "o qué días y horas atiende el negocio.")
                    .setParameters(Schema.newBuilder()
                            .setType(Type.OBJECT)
                            .putProperties("empresaId", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID de la empresa en formato UUID")
                                    .build())
                            .addRequired("empresaId")
                            .build())
                    .build();

            Tool tool = Tool.newBuilder()
                    .addFunctionDeclarations(obtenerCatalogoServiciosDecl)
                    .addFunctionDeclarations(consultarDisponibilidadDecl)
                    .addFunctionDeclarations(agendarCitaDecl)
                    .addFunctionDeclarations(cancelarCitaDecl)
                    .addFunctionDeclarations(obtenerMisCitasDecl)
                    .addFunctionDeclarations(obtenerHorariosAtencionDecl)
                    .build();

            // 4. Inicializar el modelo Gemini con la configuración requerida
            this.model = new GenerativeModel("gemini-2.5-flash", vertexAI)
                    .withSystemInstruction(ContentMaker.fromString(systemPrompt))
                    .withGenerationConfig(GenerationConfig.newBuilder()
                            .setTemperature(temperature)
                            .build())
                    .withTools(Collections.singletonList(tool));

        } catch (IOException e) {
            throw new RuntimeException("Error al inicializar Vertex AI con el archivo de credenciales", e);
        }
    }

    public String chat(String userMessage, String empresaId, String empresaNombre, String customerPhone) {
        return chat(userMessage, empresaId, empresaNombre, "No registrado", customerPhone);
    }

    public String chat(String userMessage, String empresaId, String empresaNombre, String telefonoContacto, String customerPhone) {
        return chat(userMessage, empresaId, empresaNombre, telefonoContacto, null, customerPhone);
    }

    public String chat(String userMessage, String empresaId, String empresaNombre, String telefonoContacto, String direccion, String customerPhone) {
        return chat(userMessage, empresaId, empresaNombre, telefonoContacto, direccion, null, customerPhone);
    }

    public String chat(String userMessage, String empresaId, String empresaNombre, String telefonoContacto, String direccion, String mapsLink, String customerPhone) {
        return chat(userMessage, empresaId, empresaNombre, telefonoContacto, direccion, mapsLink, null, customerPhone);
    }

    /**
     * Sobrecarga completa que inyecta el contexto de la empresa, descripción del negocio, promociones y teléfono del cliente,
     * utilizando una sesión persistente para retener la memoria del chat.
     */
    public String chat(String userMessage, String empresaId, String empresaNombre, String telefonoContacto, String direccion, String mapsLink, String descripcionNegocio, String customerPhone) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy, HH:mm:ss", new java.util.Locale("es", "MX"));
        String fechaHoraActual = java.time.LocalDateTime.now().format(formatter);
        
        String contextMessage = String.format(
                "[Contexto del sistema - Fecha y Hora actual: %s, Empresa: '%s' (ID: %s), Teléfono de Soporte del Local: %s, Dirección del Local: %s, Enlace de Google Maps del Local: %s, Descripción/Directivas del Negocio: %s, Cliente Tel: %s]\n" +
                "Instrucción para el bot: Si en el catálogo de servicios algún servicio tiene promoción activa, aplícala y calcula de forma dinámica los costos con descuento cuando el cliente te pregunte por servicios o precios.\n" +
                "Mensaje del cliente: %s",
                fechaHoraActual, empresaNombre, empresaId, 
                (telefonoContacto != null && !telefonoContacto.trim().isEmpty()) ? telefonoContacto : "No registrado", 
                (direccion != null && !direccion.trim().isEmpty()) ? direccion : "No registrada", 
                (mapsLink != null && !mapsLink.trim().isEmpty()) ? mapsLink : "No registrado",
                (descripcionNegocio != null && !descripcionNegocio.trim().isEmpty()) ? descripcionNegocio : "No registrada",
                customerPhone, userMessage
        );
        
        return chat(contextMessage, customerPhone);
    }

    /**
     * Sobrecarga legacy sin teléfono del cliente (para empresas sin registro).
     */
    public String chat(String userMessage, String empresaId, String empresaNombre) {
        return chat(userMessage, empresaId, empresaNombre, "desconocido");
    }

    /**
     * Sobrecarga fallback
     */
    public String chat(String userMessage) {
        return chat(userMessage, "default_test_session");
    }

    /**
     * Procesa el mensaje recuperando la sesión de chat del cliente para conservar el historial.
     */
    public String chat(String userMessage, String customerPhone) {
        try {
            // Recuperar o inicializar sesión para este cliente
            ChatSession chatSession = activeSessions.computeIfAbsent(customerPhone, k -> new ChatSession(model));
            
            GenerateContentResponse response = chatSession.sendMessage(userMessage);

            // Ciclo de Function Calling: el modelo puede solicitar múltiples llamadas secuenciales o en paralelo
            int maxIterations = 5; 
            for (int i = 0; i < maxIterations; i++) {
                List<FunctionCall> functionCalls = ResponseHandler.getFunctionCalls(response);
                if (functionCalls.isEmpty()) {
                    break; // El modelo ya tiene su respuesta final
                }

                java.util.List<Part> parts = new java.util.ArrayList<>();
                for (FunctionCall call : functionCalls) {
                    String functionName = call.getName();
                    System.out.println("[AntigravityAgent] [" + customerPhone + "] Function Calling detectado: " + functionName);
                    Struct responseStruct = executeFunctionCall(functionName, call);
                    parts.add(PartMaker.fromFunctionResponse(functionName, responseStruct));
                }

                // Devolver el resultado de todas las funciones al modelo en una sola respuesta
                response = chatSession.sendMessage(
                        ContentMaker.fromMultiModalData(parts.toArray(new Part[0])));
            }

            return ResponseHandler.getText(response);
        } catch (Exception e) {
            System.err.println("[AntigravityAgent] Error durante el procesamiento del chat para " + customerPhone + ": " + e.getMessage());
            e.printStackTrace();
            return "Lo siento, en este momento no puedo procesar tu solicitud. Por favor intenta de nuevo más tarde.";
        }
    }

    /**
     * Router central de Function Calling. Ejecuta la función solicitada por el modelo
     * y devuelve el resultado empaquetado como Struct para Vertex AI.
     */
    private Struct executeFunctionCall(String functionName, FunctionCall call) {
        try {
            return switch (functionName) {
                case "obtenerCatalogoServicios" -> handleObtenerCatalogo(call);
                case "consultarDisponibilidad" -> handleConsultarDisponibilidad(call);
                case "agendarCita" -> handleAgendarCita(call);
                case "cancelarCita" -> handleCancelarCita(call);
                case "obtenerMisCitas" -> handleObtenerMisCitas(call);
                case "obtenerHorariosAtencion" -> handleObtenerHorariosAtencion(call);
                default -> Struct.newBuilder()
                        .putFields("error", Value.newBuilder().setStringValue("Función no reconocida: " + functionName).build())
                        .build();
            };
        } catch (Exception e) {
            System.err.println("[AntigravityAgent] Error ejecutando función " + functionName + ": " + e.getMessage());
            return Struct.newBuilder()
                    .putFields("error", Value.newBuilder().setStringValue("Error al ejecutar la operación: " + e.getMessage()).build())
                    .build();
        }
    }

    // ========================
    // HANDLERS DE CADA SKILL
    // ========================

    private Struct handleObtenerCatalogo(FunctionCall call) {
        String empresaIdStr = call.getArgs().getFieldsOrThrow("empresaId").getStringValue();
        System.out.println("[AntigravityAgent] obtenerCatalogoServicios de empresaId: " + empresaIdStr);

        UUID empresaId = UUID.fromString(empresaIdStr.trim());
        List<ServicioDTO> catalogo = servicioSkill.obtenerCatalogoServicios(empresaId);
        String catalogoString = formatCatalogo(catalogo);

        return Struct.newBuilder()
                .putFields("resultado", Value.newBuilder().setStringValue(catalogoString).build())
                .build();
    }

    private Struct handleConsultarDisponibilidad(FunctionCall call) {
        String empresaIdStr = call.getArgs().getFieldsOrThrow("empresaId").getStringValue();
        String servicioIdStr = call.getArgs().getFieldsOrThrow("servicioId").getStringValue();
        String fechaStr = call.getArgs().getFieldsOrThrow("fecha").getStringValue();

        System.out.println("[AntigravityAgent] consultarDisponibilidad: empresa=" + empresaIdStr
                + ", servicio=" + servicioIdStr + ", fecha=" + fechaStr);

        UUID empresaId = UUID.fromString(empresaIdStr.trim());
        UUID servicioId = UUID.fromString(servicioIdStr.trim());
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaStr.trim());
        } catch (DateTimeParseException e) {
            return Struct.newBuilder()
                    .putFields("error", Value.newBuilder().setStringValue(
                            "La fecha proporcionada no tiene un formato válido. Usa el formato YYYY-MM-DD (ejemplo: 2026-07-01).").build())
                    .build();
        }

        // Verificar si el día está CERRADO (no tiene configuración de agenda)
        int diaSemanaSql = fecha.getDayOfWeek().getValue() % 7;
        String[] diasNombres = {"Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
        java.util.Optional<com.example.agente.model.AgendaConfig> configOpt = 
                citaService.getAgendaConfigRepository().findByEmpresaIdAndDiaSemana(empresaId, diaSemanaSql);
        
        if (configOpt.isEmpty()) {
            String resultado = "CERRADO: El negocio NO abre los días " + diasNombres[diaSemanaSql] 
                    + ". Informa al cliente que el negocio está cerrado ese día y sugiérele elegir otro día de la semana en el que sí esté abierto.";
            System.out.println("[AntigravityAgent] Día cerrado detectado: " + diasNombres[diaSemanaSql]);
            return Struct.newBuilder()
                    .putFields("resultado", Value.newBuilder().setStringValue(resultado).build())
                    .build();
        }

        List<String> slots = citaService.obtenerDisponibilidad(empresaId, servicioId, fecha);

        String resultado;
        if (slots.isEmpty()) {
            resultado = "No hay horarios disponibles para la fecha " + fechaStr + ". Todos los horarios están ocupados. Sugiere al cliente probar otro día.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Horarios disponibles para el ").append(fechaStr).append(":\n");
            for (int i = 0; i < slots.size(); i++) {
                sb.append((i + 1)).append(". ").append(slots.get(i)).append("\n");
            }
            resultado = sb.toString();
        }

        return Struct.newBuilder()
                .putFields("resultado", Value.newBuilder().setStringValue(resultado).build())
                .build();
    }

    private Struct handleAgendarCita(FunctionCall call) {
        String empresaIdStr = call.getArgs().getFieldsOrThrow("empresaId").getStringValue();
        String servicioIdStr = call.getArgs().getFieldsOrThrow("servicioId").getStringValue();
        String fechaHoraInicio = call.getArgs().getFieldsOrThrow("fechaHoraInicio").getStringValue();
        String telefonoCliente = call.getArgs().getFieldsOrThrow("telefonoCliente").getStringValue();

        System.out.println("[AntigravityAgent] agendarCita: empresa=" + empresaIdStr
                + ", servicio=" + servicioIdStr + ", inicio=" + fechaHoraInicio + ", tel=" + telefonoCliente);

        UUID empresaId = UUID.fromString(empresaIdStr.trim());
        UUID servicioId = UUID.fromString(servicioIdStr.trim());

        CitaRequestDTO request = new CitaRequestDTO(empresaId, telefonoCliente.trim(), servicioId, fechaHoraInicio.trim());

        try {
            CitaResponseDTO response = citaService.agendarCita(request);
            String resultado = "✅ Cita agendada con éxito.\n"
                    + "Servicio: " + response.nombreServicio() + "\n"
                    + "Fecha y hora: " + response.fechaHoraInicio() + " a " + response.fechaHoraFin() + "\n"
                    + "Estado: " + response.estado() + "\n"
                    + "ID de cita: " + response.idCita();

            return Struct.newBuilder()
                    .putFields("resultado", Value.newBuilder().setStringValue(resultado).build())
                    .build();
        } catch (Exception e) {
            return Struct.newBuilder()
                    .putFields("error", Value.newBuilder().setStringValue(e.getMessage()).build())
                    .build();
        }
    }

    private Struct handleCancelarCita(FunctionCall call) {
        String idCitaStr = call.getArgs().getFieldsOrThrow("idCita").getStringValue();
        String empresaIdStr = call.getArgs().getFieldsOrThrow("empresaId").getStringValue();

        System.out.println("[AntigravityAgent] cancelarCita: cita=" + idCitaStr + ", empresa=" + empresaIdStr);

        UUID idCita = UUID.fromString(idCitaStr.trim());
        UUID empresaId = UUID.fromString(empresaIdStr.trim());

        try {
            citaService.cancelarCita(idCita, empresaId);
            return Struct.newBuilder()
                    .putFields("resultado", Value.newBuilder().setStringValue(
                            "✅ La cita ha sido cancelada exitosamente.").build())
                    .build();
        } catch (IllegalArgumentException e) {
            return Struct.newBuilder()
                    .putFields("error", Value.newBuilder().setStringValue(
                            "No se encontró la cita con el ID proporcionado o no pertenece a este negocio.").build())
                    .build();
        }
    }

    private Struct handleObtenerMisCitas(FunctionCall call) {
        String empresaIdStr = call.getArgs().getFieldsOrThrow("empresaId").getStringValue();
        String telefonoCliente = call.getArgs().getFieldsOrThrow("telefonoCliente").getStringValue();

        System.out.println("[AntigravityAgent] obtenerMisCitas: empresa=" + empresaIdStr + ", tel=" + telefonoCliente);

        UUID empresaId = UUID.fromString(empresaIdStr.trim());

        List<CitaResponseDTO> citas = citaService.obtenerCitasPorTelefono(telefonoCliente.trim(), empresaId);

        if (citas.isEmpty()) {
            return Struct.newBuilder()
                    .putFields("resultado", Value.newBuilder().setStringValue(
                            "No tienes citas agendadas próximamente en este negocio.").build())
                    .build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Tus citas agendadas:\n");
        for (int i = 0; i < citas.size(); i++) {
            CitaResponseDTO c = citas.get(i);
            sb.append((i + 1)).append(". ")
                    .append(c.nombreServicio())
                    .append(" — ").append(c.fechaHoraInicio()).append(" a ").append(c.fechaHoraFin())
                    .append(" (Estado: ").append(c.estado()).append(")")
                    .append(" [ID: ").append(c.idCita()).append("]\n");
        }

        return Struct.newBuilder()
                .putFields("resultado", Value.newBuilder().setStringValue(sb.toString()).build())
                .build();
    }

    private Struct handleObtenerHorariosAtencion(FunctionCall call) {
        String empresaIdStr = call.getArgs().getFieldsOrThrow("empresaId").getStringValue();
        System.out.println("[AntigravityAgent] obtenerHorariosAtencion de empresaId: " + empresaIdStr);

        UUID empresaId = UUID.fromString(empresaIdStr.trim());
        String horarios = citaService.obtenerHorariosAtencion(empresaId);

        return Struct.newBuilder()
                .putFields("resultado", Value.newBuilder().setStringValue(horarios).build())
                .build();
    }

    // ========================
    // FORMATEO
    // ========================

    private String formatCatalogo(List<ServicioDTO> catalogo) {
        if (catalogo.isEmpty()) {
            return "No se encontraron servicios disponibles para esta empresa.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Servicios Disponibles:\n");
        for (ServicioDTO dto : catalogo) {
            sb.append("- ").append(dto.nombre())
                    .append(": ").append(dto.descripcion() != null ? dto.descripcion() : "Sin descripción")
                    .append(" | Precio: $").append(dto.precio())
                    .append(" | Duración: ").append(dto.duracionMinutos()).append(" minutos")
                    .append(" | [servicioId interno: ").append(dto.id()).append("]\n");
        }
        return sb.toString();
    }
}