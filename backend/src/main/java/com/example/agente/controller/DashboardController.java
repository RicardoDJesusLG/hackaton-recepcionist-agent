package com.example.agente.controller;

import com.example.agente.model.Cita;
import com.example.agente.model.EstadoCita;
import com.example.agente.model.Servicio;
import com.example.agente.model.Usuario;
import com.example.agente.repository.CitaRepository;
import com.example.agente.repository.ServicioRepository;
import com.example.agente.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardController(CitaRepository citaRepository,
                               ServicioRepository servicioRepository,
                               UsuarioRepository usuarioRepository) {
        this.citaRepository = citaRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * DTO interno para retornar información enriquecida de las citas al dashboard.
     */
    public record DashboardCitaDTO(
            UUID idCita,
            String clienteNombre,
            String clienteTelefono,
            String servicioNombre,
            String fechaHoraInicio,
            String fechaHoraFin,
            String estado
    ) {}

    /**
     * Obtiene todas las citas de la empresa a la que pertenece el propietario autenticado.
     */
    @GetMapping("/citas")
    public ResponseEntity<?> obtenerCitas(HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        List<Cita> citas = citaRepository.findByEmpresaIdOrderByFechaHoraInicioDesc(empresaId);
        List<DashboardCitaDTO> result = new ArrayList<>();

        for (Cita cita : citas) {
            String clienteNombre = "Desconocido";
            String clienteTelefono = "";
            String servicioNombre = "Servicio no encontrado";

            // Buscar usuario
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(cita.getUsuarioId());
            if (usuarioOpt.isPresent()) {
                clienteNombre = usuarioOpt.get().getNombre() != null ? usuarioOpt.get().getNombre() : "Cliente WhatsApp";
                clienteTelefono = usuarioOpt.get().getTelefonoWhatsapp();
            }

            // Buscar servicio
            Optional<Servicio> servicioOpt = servicioRepository.findById(cita.getServicioId());
            if (servicioOpt.isPresent()) {
                servicioNombre = servicioOpt.get().getNombre();
            }

            result.add(new DashboardCitaDTO(
                    cita.getId(),
                    clienteNombre,
                    clienteTelefono,
                    servicioNombre,
                    cita.getFechaHoraInicio().toString(),
                    cita.getFechaHoraFin().toString(),
                    cita.getEstado().name()
            ));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Cancela una cita específica.
     */
    @PostMapping("/citas/cancelar")
    public ResponseEntity<?> cancelarCita(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        String idCitaStr = requestBody.get("idCita");

        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }
        if (idCitaStr == null || idCitaStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de cita requerido"));
        }

        UUID idCita;
        try {
            idCita = UUID.fromString(idCitaStr.trim());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de cita no válido"));
        }

        Optional<Cita> citaOpt = citaRepository.findById(idCita);
        if (citaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Cita no encontrada"));
        }

        Cita cita = citaOpt.get();
        // Verificar que pertenezca a la empresa del dueño autenticado
        if (!cita.getEmpresaId().equals(empresaId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No tienes permisos para modificar esta cita"));
        }

        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);

        return ResponseEntity.ok(Map.of("message", "Cita cancelada con éxito", "idCita", cita.getId().toString()));
    }
}
