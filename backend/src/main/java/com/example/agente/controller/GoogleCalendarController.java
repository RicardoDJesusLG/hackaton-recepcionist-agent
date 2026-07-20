package com.example.agente.controller;

import com.example.agente.model.GoogleCalendarConfig;
import com.example.agente.service.GoogleCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;

import java.util.Map;
import java.util.UUID;

/**
 * Controlador para el flujo de OAuth 2.0 con Google Calendar.
 *
 * Endpoints:
 * - GET /api/v1/google-calendar/auth-url?empresaId=... → Retorna la URL de autorización.
 * - GET /api/v1/auth/callback/google?code=...&state=... → Callback de Google tras autorizar.
 * - GET /api/v1/google-calendar/status?empresaId=... → Verifica si la empresa tiene Calendar vinculado.
 */
@RestController
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    @Value("${google.calendar.frontend-redirect-url}")
    private String frontendRedirectUrl;

    public GoogleCalendarController(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    /**
     * Genera y retorna la URL de autorización de Google Calendar para una empresa.
     * El propietario debe abrir esta URL en su navegador para vincular su cuenta.
     */
    @GetMapping("/api/v1/google-calendar/auth-url")
    public ResponseEntity<Map<String, String>> obtenerUrlAutorizacion(@RequestParam UUID empresaId) {
        String url = googleCalendarService.generarUrlAutorizacion(empresaId);
        return ResponseEntity.ok(Map.of(
                "url", url,
                "message", "Abre esta URL en tu navegador para vincular Google Calendar con tu empresa."
        ));
    }

    /**
     * Callback que Google invoca después de que el usuario autoriza la aplicación.
     * Recibe el código de autorización temporal y el empresaId en el parámetro 'state'.
     * Intercambia el código por tokens y los almacena en la BD.
     */
    @GetMapping("/api/v1/auth/callback/google")
    public ResponseEntity<?> callbackGoogle(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {

        HttpHeaders headers = new HttpHeaders();
        try {
            UUID empresaId = UUID.fromString(state);
            googleCalendarService.intercambiarCodigoPorTokens(code, empresaId);

            // Redirigir al frontend con parámetro de éxito
            headers.setLocation(URI.create(frontendRedirectUrl + "?googleCalendar=success"));
            return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
        } catch (Exception e) {
            System.err.println("[GoogleCalendarController] Error en callback: " + e.getMessage());
            // Redirigir al frontend con parámetro de error
            headers.setLocation(URI.create(frontendRedirectUrl + "?googleCalendar=error"));
            return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
        }
    }

    /**
     * Verifica si una empresa tiene Google Calendar vinculado.
     * Útil para el frontend: mostrar botón "Vincular" o indicador "Vinculado".
     */
    @GetMapping("/api/v1/google-calendar/status")
    public ResponseEntity<Map<String, Object>> verificarEstado(@RequestParam UUID empresaId) {
        boolean vinculado = googleCalendarService.estaVinculado(empresaId);
        return ResponseEntity.ok(Map.of(
                "empresaId", empresaId.toString(),
                "googleCalendarVinculado", vinculado
        ));
    }

    /**
     * Elimina la vinculación de Google Calendar para una empresa.
     */
    @DeleteMapping("/api/v1/google-calendar")
    public ResponseEntity<Map<String, Object>> desvincularCalendar(@RequestParam UUID empresaId) {
        try {
            googleCalendarService.desvincularCalendar(empresaId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Google Calendar desvinculado exitosamente."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error al desvincular Google Calendar: " + e.getMessage()
            ));
        }
    }
}
