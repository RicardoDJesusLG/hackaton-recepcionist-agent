package com.example.agente.service;

import com.example.agente.model.Cita;
import com.example.agente.model.GoogleCalendarConfig;
import com.example.agente.model.Servicio;
import com.example.agente.repository.CitaRepository;
import com.example.agente.repository.GoogleCalendarConfigRepository;
import com.example.agente.repository.ServicioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio encargado de la integración con Google Calendar API:
 * - Generación de URL de autorización OAuth 2.0
 * - Intercambio de código por tokens (Access + Refresh)
 * - Refresco automático del Access Token
 * - Sincronización de eventos (crear, actualizar, eliminar)
 */
@Service
public class GoogleCalendarService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_CALENDAR_API_BASE = "https://www.googleapis.com/calendar/v3";
    private static final String SCOPES = "https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/calendar.events";

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    private final GoogleCalendarConfigRepository configRepository;
    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleCalendarService(GoogleCalendarConfigRepository configRepository,
                                  CitaRepository citaRepository,
                                  ServicioRepository servicioRepository) {
        this.configRepository = configRepository;
        this.citaRepository = citaRepository;
        this.servicioRepository = servicioRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ====================================================================
    // 1. FLUJO DE AUTORIZACIÓN OAUTH 2.0
    // ====================================================================

    /**
     * Genera la URL de autorización de Google para que el propietario
     * vincule su cuenta de Google Calendar.
     * El empresaId se envía en el parámetro "state" para identificar
     * la empresa al recibir el callback.
     */
    public String generarUrlAutorizacion(UUID empresaId) {
        return GOOGLE_AUTH_URL
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + SCOPES.replace(" ", "%20")
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + empresaId.toString();
    }

    /**
     * Intercambia el código de autorización temporal por tokens de acceso.
     * Guarda los tokens en la base de datos vinculados a la empresa.
     */
    public GoogleCalendarConfig intercambiarCodigoPorTokens(String code, UUID empresaId) {
        // Preparar la solicitud POST al endpoint de tokens de Google
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken = jsonResponse.has("refresh_token") ? jsonResponse.get("refresh_token").asText() : null;
            long expiresIn = jsonResponse.get("expires_in").asLong();
            String tokenType = jsonResponse.has("token_type") ? jsonResponse.get("token_type").asText() : "Bearer";
            String scope = jsonResponse.has("scope") ? jsonResponse.get("scope").asText() : SCOPES;

            // Calcular la fecha de expiración absoluta
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);

            // Buscar si ya existe una configuración para esta empresa (para actualizar)
            Optional<GoogleCalendarConfig> existingConfig = configRepository.findByEmpresaId(empresaId);

            GoogleCalendarConfig config;
            if (existingConfig.isPresent()) {
                config = existingConfig.get();
                config.setAccessToken(accessToken);
                if (refreshToken != null) {
                    config.setRefreshToken(refreshToken);
                }
                config.setExpiresAt(expiresAt);
                config.setTokenType(tokenType);
                config.setScope(scope);
            } else {
                config = GoogleCalendarConfig.builder()
                        .empresaId(empresaId)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expiresAt(expiresAt)
                        .tokenType(tokenType)
                        .scope(scope)
                        .calendarId("primary")
                        .build();
            }

            return configRepository.save(config);

        } catch (Exception e) {
            System.err.println("[GoogleCalendarService] Error al intercambiar código por tokens: " + e.getMessage());
            throw new RuntimeException("Error al conectar con Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Refresca el access_token de una empresa usando el refresh_token almacenado.
     * Se llama automáticamente cuando el token ha expirado.
     */
    private String refrescarAccessToken(GoogleCalendarConfig config) {
        if (config.getRefreshToken() == null) {
            throw new RuntimeException("No hay refresh_token almacenado para la empresa " + config.getEmpresaId()
                    + ". El propietario debe volver a vincular su Google Calendar.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", config.getRefreshToken());
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            String newAccessToken = jsonResponse.get("access_token").asText();
            long expiresIn = jsonResponse.get("expires_in").asLong();

            config.setAccessToken(newAccessToken);
            config.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            configRepository.save(config);

            System.out.println("[GoogleCalendarService] Access token refrescado exitosamente para empresa: " + config.getEmpresaId());
            return newAccessToken;

        } catch (Exception e) {
            System.err.println("[GoogleCalendarService] Error al refrescar access token: " + e.getMessage());
            throw new RuntimeException("Error al refrescar token de Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene un access_token válido para la empresa, refrescándolo automáticamente si ha expirado.
     */
    private String obtenerAccessTokenValido(GoogleCalendarConfig config) {
        // Si el token expira en menos de 60 segundos, refrescarlo
        if (config.getExpiresAt() != null && config.getExpiresAt().isBefore(LocalDateTime.now().plusSeconds(60))) {
            return refrescarAccessToken(config);
        }
        return config.getAccessToken();
    }

    // ====================================================================
    // 2. SINCRONIZACIÓN DE EVENTOS CON GOOGLE CALENDAR
    // ====================================================================

    /**
     * Crea un evento en Google Calendar de forma asíncrona cuando se agenda una cita.
     * Actualiza la cita con el googleEventId resultante.
     */
    @Async
    public void crearEventoEnCalendar(Cita cita) {
        try {
            Optional<GoogleCalendarConfig> configOpt = configRepository.findByEmpresaId(cita.getEmpresaId());
            if (configOpt.isEmpty()) {
                System.out.println("[GoogleCalendarService] La empresa " + cita.getEmpresaId()
                        + " no tiene Google Calendar vinculado. Omitiendo sincronización.");
                return;
            }

            GoogleCalendarConfig config = configOpt.get();
            String accessToken = obtenerAccessTokenValido(config);
            String calendarId = config.getCalendarId() != null ? config.getCalendarId() : "primary";

            // Obtener nombre del servicio
            String servicioNombre = "Cita";
            Optional<Servicio> servicioOpt = servicioRepository.findById(cita.getServicioId());
            if (servicioOpt.isPresent()) {
                servicioNombre = servicioOpt.get().getNombre();
            }

            // Construir el cuerpo del evento
            Map<String, Object> evento = construirEventoCalendar(servicioNombre, cita);

            // Hacer la petición POST a Google Calendar API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(evento, headers);
            String url = GOOGLE_CALENDAR_API_BASE + "/calendars/" + calendarId + "/events";

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String eventId = jsonResponse.get("id").asText();

                // Guardar el ID del evento de Google Calendar en la cita
                cita.setGoogleEventId(eventId);
                citaRepository.save(cita);

                System.out.println("[GoogleCalendarService] Evento creado en Google Calendar: " + eventId
                        + " para cita: " + cita.getId());
            }

        } catch (Exception e) {
            System.err.println("[GoogleCalendarService] Error al crear evento en Google Calendar: " + e.getMessage());
            // No lanzar excepción para no interrumpir el flujo principal de la cita
        }
    }

    /**
     * Elimina (cancela) un evento en Google Calendar cuando se cancela una cita.
     */
    @Async
    public void eliminarEventoDeCalendar(Cita cita) {
        try {
            if (cita.getGoogleEventId() == null || cita.getGoogleEventId().isBlank()) {
                System.out.println("[GoogleCalendarService] La cita " + cita.getId()
                        + " no tiene googleEventId. Omitiendo eliminación.");
                return;
            }

            Optional<GoogleCalendarConfig> configOpt = configRepository.findByEmpresaId(cita.getEmpresaId());
            if (configOpt.isEmpty()) {
                return;
            }

            GoogleCalendarConfig config = configOpt.get();
            String accessToken = obtenerAccessTokenValido(config);
            String calendarId = config.getCalendarId() != null ? config.getCalendarId() : "primary";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = GOOGLE_CALENDAR_API_BASE + "/calendars/" + calendarId + "/events/" + cita.getGoogleEventId();

            restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

            System.out.println("[GoogleCalendarService] Evento eliminado de Google Calendar: " + cita.getGoogleEventId()
                    + " para cita: " + cita.getId());

        } catch (Exception e) {
            System.err.println("[GoogleCalendarService] Error al eliminar evento de Google Calendar: " + e.getMessage());
        }
    }

    /**
     * Verifica si una empresa tiene Google Calendar vinculado.
     */
    public boolean estaVinculado(UUID empresaId) {
        return configRepository.existsByEmpresaId(empresaId);
    }

    /**
     * Elimina la configuración de Google Calendar de una empresa (desvincula la cuenta).
     */
    public void desvincularCalendar(UUID empresaId) {
        Optional<GoogleCalendarConfig> configOpt = configRepository.findByEmpresaId(empresaId);
        if (configOpt.isPresent()) {
            configRepository.delete(configOpt.get());
            System.out.println("[GoogleCalendarService] Google Calendar desvinculado para empresa: " + empresaId);
        }
    }

    // ====================================================================
    // 3. HELPERS
    // ====================================================================

    /**
     * Construye el objeto JSON del evento para la API de Google Calendar.
     */
    private Map<String, Object> construirEventoCalendar(String servicioNombre, Cita cita) {
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("summary", "📅 " + servicioNombre);
        evento.put("description", "Cita agendada automáticamente por Recepcionista AI.\nID de cita: " + cita.getId());

        // Zona horaria de México Central (ajustar según sea necesario)
        String timeZone = "America/Mexico_City";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        Map<String, String> start = new LinkedHashMap<>();
        start.put("dateTime", cita.getFechaHoraInicio().format(formatter));
        start.put("timeZone", timeZone);
        evento.put("start", start);

        Map<String, String> end = new LinkedHashMap<>();
        end.put("dateTime", cita.getFechaHoraFin().format(formatter));
        end.put("timeZone", timeZone);
        evento.put("end", end);

        // Añadir un recordatorio por defecto (10 minutos antes)
        Map<String, Object> reminders = new LinkedHashMap<>();
        reminders.put("useDefault", false);
        Map<String, Object> override = new LinkedHashMap<>();
        override.put("method", "popup");
        override.put("minutes", 10);
        reminders.put("overrides", List.of(override));
        evento.put("reminders", reminders);

        return evento;
    }
}
