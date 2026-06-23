package com.example.agente.config;

import com.example.agente.agent.AntigravityAgent;
import com.example.agente.service.ServicioSkill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración para inicializar el motor cognitivo de Antigravity.
 * Registra el Bean de AntigravityAgent con el prompt del sistema correspondiente.
 */
@Configuration
public class AntigravityConfig {

    @Bean
    public AntigravityAgent antigravityAgent(ServicioSkill servicioSkill) {
        // Prompt del sistema para la recepcionista virtual con directiva estricta contra alucinaciones
        String systemPrompt = "Actúas como una recepcionista virtual y asistente de agendamiento experta, atenta y profesional. "
                + "Tu objetivo es ayudar al usuario a agendar sus citas, conocer los servicios del negocio, sus precios y duraciones. "
                + "REGLA CRÍTICA: Debes consultar exclusivamente las Tools (Function Calling) de catálogo disponibles para responder "
                + "cualquier pregunta sobre servicios, precios y duraciones. No alucines, inventes ni asumas información del catálogo. "
                + "Si no encuentras información en la herramienta o si el resultado está vacío, infórmaselo cordialmente al cliente. "
                + "Por favor mantén las respuestas cortas, directas y amigables, adecuadas para una conversación de WhatsApp.";
                
        float temperature = 0.3f;
        
        return new AntigravityAgent(systemPrompt, temperature, servicioSkill);
    }
}
