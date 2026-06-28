package com.example.agente.controller;

import com.example.agente.dto.CitaRequestDTO;
import com.example.agente.dto.CitaResponseDTO;
import com.example.agente.service.CitaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /**
     * Endpoint para obtener disponibilidad de bloques de tiempo.
     * GET /api/v1/disponibilidad?id_negocio=...&id_servicio=...&fecha_solicitada=YYYY-MM-DD
     */
    @GetMapping("/disponibilidad")
    public ResponseEntity<List<String>> obtenerDisponibilidad(
            @RequestParam("id_negocio") UUID idNegocio,
            @RequestParam("id_servicio") UUID idServicio,
            @RequestParam("fecha_solicitada") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSolicitada) {
        
        if (idNegocio == null || idServicio == null || fechaSolicitada == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<String> disponibles = citaService.obtenerDisponibilidad(idNegocio, idServicio, fechaSolicitada);
            return ResponseEntity.ok(disponibles);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint para agendar una cita.
     * POST /api/v1/citas
     */
    @PostMapping("/citas")
    public ResponseEntity<CitaResponseDTO> agendarCita(@RequestBody CitaRequestDTO request) {
        if (request.idNegocio() == null || request.idServicio() == null || 
            request.telefonoCliente() == null || request.fechaHoraInicio() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            CitaResponseDTO response = citaService.agendarCita(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // 409 Conflict en caso de solapamiento
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
