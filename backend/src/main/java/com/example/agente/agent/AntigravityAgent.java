package com.example.agente.agent;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.*;
import com.google.cloud.vertexai.api.*;
import com.example.agente.service.ServicioSkill;
import com.example.agente.dto.ServicioDTO;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agente cognitivo de Antigravity que encapsula las interacciones con Vertex
 * AI.
 * Utiliza el transporte oficial REST/HTTP para evitar problemas de conectividad
 * gRPC.
 */
public class AntigravityAgent {

    private final String systemPrompt;
    private final float temperature;
    private final ServicioSkill servicioSkill;
    private VertexAI vertexAI;
    private GenerativeModel model;

    public AntigravityAgent(String systemPrompt, float temperature, ServicioSkill servicioSkill) {
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.servicioSkill = servicioSkill;
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

            // 3. Declaración de la función (Skill de catálogo) para Gemini
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

            Tool tool = Tool.newBuilder()
                    .addFunctionDeclarations(obtenerCatalogoServiciosDecl)
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

    /**
     * Envía un mensaje al agente cognitivo y obtiene su respuesta final.
     * Si el modelo solicita llamar a una herramienta (Function Calling), se ejecuta
     * localmente.
     *
     * @param userMessage El mensaje enviado por el usuario desde WhatsApp.
     * @return La respuesta del agente en texto plano.
     */
    public String chat(String userMessage) {
        try {
            ChatSession chatSession = new ChatSession(model);
            GenerateContentResponse response = chatSession.sendMessage(userMessage);

            // Verificar si el modelo solicitó invocar una función externa (Function
            // Calling)
            List<FunctionCall> functionCalls = ResponseHandler.getFunctionCalls(response);
            if (!functionCalls.isEmpty()) {
                FunctionCall call = functionCalls.get(0);

                if ("obtenerCatalogoServicios".equals(call.getName())) {
                    String empresaIdStr = call.getArgs().getFieldsOrThrow("empresaId").getStringValue();
                    System.out.println(
                            "[AntigravityAgent] Function Calling detectado para obtenerCatalogoServicios de empresaId: "
                                    + empresaIdStr);

                    UUID empresaId;
                    try {
                        empresaId = UUID.fromString(empresaIdStr.trim());
                    } catch (IllegalArgumentException e) {
                        return "Lo siento, el ID del negocio proporcionado no tiene un formato válido.";
                    }

                    // Ejecutar la consulta en la base de datos a través de la capa de servicio
                    List<ServicioDTO> catalogo = servicioSkill.obtenerCatalogoServicios(empresaId);

                    // Formatear el resultado en texto legible para que el modelo lo procese
                    String catalogoString = formatCatalogo(catalogo);

                    // Empaquetar la respuesta estructural para Vertex AI
                    Struct responseStruct = Struct.newBuilder()
                            .putFields("resultado", Value.newBuilder().setStringValue(catalogoString).build())
                            .build();

                    // Devolver el resultado de la función al modelo para que genere su respuesta
                    // final empática
                    response = chatSession.sendMessage(
                            ContentMaker.fromMultiModalData(
                                    PartMaker.fromFunctionResponse("obtenerCatalogoServicios", responseStruct)));
                }
            }

            return ResponseHandler.getText(response);
        } catch (Exception e) {
            System.err.println("[AntigravityAgent] Error durante el procesamiento del chat: " + e.getMessage());
            e.printStackTrace();
            return "Lo siento, en este momento no puedo procesar tu solicitud. Por favor intenta de nuevo más tarde.";
        }
    }

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
                    .append(" | Duración: ").append(dto.duracionMinutos()).append(" minutos\n");
        }
        return sb.toString();
    }
}