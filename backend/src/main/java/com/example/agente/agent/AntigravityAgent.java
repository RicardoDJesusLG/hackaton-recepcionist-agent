package com.example.agente.agent;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.*;
import com.google.cloud.vertexai.api.*;
import com.example.agente.service.ServicioSkill;
import com.example.agente.dto.ServicioDTO;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agente cognitivo de Antigravity que encapsula las interacciones con Vertex AI (Gemini Flash).
 * Maneja de forma autónoma el Function Calling de la skill ServicioSkill.
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
            // Cargar credenciales desde el archivo local antigravity-credentials.json
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("antigravity-credentials.json"))
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

            String projectId = "hackaton-recepcionist-agent";
            this.vertexAI = new VertexAI.Builder()
                    .setProjectId(projectId)
                    .setLocation("us-central1")
                    .setCredentials(credentials)
                    .build();

            // Definición de la función para el Function Calling de Gemini
            FunctionDeclaration obtenerCatalogoServiciosDecl = FunctionDeclaration.newBuilder()
                    .setName("obtenerCatalogoServicios")
                    .setDescription("Obtiene la lista de servicios activos de una empresa o negocio a partir de su ID. Úsalo obligatoriamente cuando el cliente pregunte por los servicios disponibles, precios o duración de un negocio en particular.")
                    .setParameters(Schema.newBuilder()
                            .setType(Type.OBJECT)
                            .putProperties("empresaId", Schema.newBuilder()
                                    .setType(Type.STRING)
                                    .setDescription("El ID de la empresa o negocio en formato UUID (cadena de texto estándar)")
                                    .build())
                            .addRequired("empresaId")
                            .build())
                    .build();

            Tool tool = Tool.newBuilder()
                    .addFunctionDeclarations(obtenerCatalogoServiciosDecl)
                    .build();

            // Configurar el modelo cognitivo (Gemini 1.5 Flash) con su prompt del sistema y temperatura
            this.model = new GenerativeModel("gemini-1.5-flash", vertexAI)
                    .withSystemInstruction(ContentMaker.fromString(systemPrompt))
                    .withGenerationConfig(GenerationConfig.newBuilder()
                            .setTemperature(temperature)
                            .build())
                    .withTools(Collections.singletonList(tool));

        } catch (IOException e) {
            throw new RuntimeException("Error al inicializar Vertex AI con antigravity-credentials.json", e);
        }
    }

    /**
     * Envía un mensaje al agente cognitivo y obtiene su respuesta.
     * Si el agente requiere invocar una función para responder (Function Calling),
     * se ejecuta localmente y se retroalimenta al modelo de forma transparente.
     *
     * @param userMessage El mensaje enviado por el usuario.
     * @return La respuesta inteligente final del agente.
     */
    public String chat(String userMessage) {
        try {
            ChatSession chatSession = new ChatSession(model);
            GenerateContentResponse response = chatSession.sendMessage(userMessage);

            // Revisar si el modelo solicitó llamar a una herramienta (Function Calling)
            List<FunctionCall> functionCalls = ResponseHandler.getFunctionCalls(response);
            if (!functionCalls.isEmpty()) {
                FunctionCall call = functionCalls.get(0);
                if ("obtenerCatalogoServicios".equals(call.getName())) {
                    String empresaIdStr = call.getArgs().getFieldsOrThrow("empresaId").getStringValue();
                    System.out.println("[AntigravityAgent] Function Calling detectado para obtenerCatalogoServicios de empresaId: " + empresaIdStr);

                    UUID empresaId;
                    try {
                        empresaId = UUID.fromString(empresaIdStr.trim());
                    } catch (IllegalArgumentException e) {
                        return "Lo siento, el ID del negocio proporcionado no es válido.";
                    }

                    // Invocar la skill local
                    List<ServicioDTO> catalogo = servicioSkill.obtenerCatalogoServicios(empresaId);
                    
                    // Formatear el catálogo como cadena para entregárselo al modelo
                    String catalogoString = formatCatalogo(catalogo);

                    // Construir la respuesta estructurada de la función
                    Struct responseStruct = Struct.newBuilder()
                            .putFields("resultado", Value.newBuilder().setStringValue(catalogoString).build())
                            .build();

                    // Reenviar el resultado de la función al modelo para obtener su respuesta final
                    response = chatSession.sendMessage(
                            ContentMaker.fromMultiModalData(
                                    PartMaker.fromFunctionResponse("obtenerCatalogoServicios", responseStruct)
                            )
                    );
                }
            }

            return ResponseHandler.getText(response);
        } catch (Exception e) {
            System.err.println("[AntigravityAgent] Error en la interacción del chat: " + e.getMessage());
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
