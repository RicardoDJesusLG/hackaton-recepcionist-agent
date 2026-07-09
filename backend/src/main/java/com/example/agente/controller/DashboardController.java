package com.example.agente.controller;

import com.example.agente.model.Cita;
import com.example.agente.model.EstadoCita;
import com.example.agente.model.Servicio;
import com.example.agente.model.Usuario;
import com.example.agente.model.Empresa;
import com.example.agente.model.AgendaConfig;
import com.example.agente.repository.CitaRepository;
import com.example.agente.repository.ServicioRepository;
import com.example.agente.repository.UsuarioRepository;
import com.example.agente.repository.EmpresaRepository;
import com.example.agente.repository.AgendaConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
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
    private final EmpresaRepository empresaRepository;
    private final AgendaConfigRepository agendaConfigRepository;

    public DashboardController(CitaRepository citaRepository,
                               ServicioRepository servicioRepository,
                               UsuarioRepository usuarioRepository,
                               EmpresaRepository empresaRepository,
                               AgendaConfigRepository agendaConfigRepository) {
        this.citaRepository = citaRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.agendaConfigRepository = agendaConfigRepository;
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

    /**
     * Obtiene la información de la empresa del propietario autenticado.
     */
    @GetMapping("/empresa")
    public ResponseEntity<?> obtenerDetalleEmpresa(HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
        if (empresaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Empresa no encontrada"));
        }

        Empresa empresa = empresaOpt.get();
        String maskedToken = null;
        if (empresa.getWhatsappToken() != null && !empresa.getWhatsappToken().trim().isEmpty()) {
            String token = empresa.getWhatsappToken().trim();
            if (token.length() > 10) {
                maskedToken = token.substring(0, 6) + "..." + token.substring(token.length() - 4);
            } else {
                maskedToken = "********";
            }
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", empresa.getId());
        response.put("nombre", empresa.getNombre() != null ? empresa.getNombre() : "");
        response.put("whatsappPhoneId", empresa.getWhatsappPhoneId() != null ? empresa.getWhatsappPhoneId() : "");
        response.put("whatsappToken", maskedToken != null ? maskedToken : "");
        response.put("direccion", empresa.getDireccion() != null ? empresa.getDireccion() : "");
        response.put("descripcionNegocio", empresa.getDescripcionNegocio() != null ? empresa.getDescripcionNegocio() : "");
        response.put("suscripcionActiva", empresa.getSuscripcionActiva());
        response.put("telefonoContacto", empresa.getTelefonoContacto() != null ? empresa.getTelefonoContacto() : "");
        response.put("mapsLink", empresa.getMapsLink() != null ? empresa.getMapsLink() : "");


        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza la información de la empresa del propietario autenticado.
     */
    @PutMapping("/empresa")
    public ResponseEntity<?> actualizarEmpresa(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
        if (empresaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Empresa no encontrada"));
        }

        Empresa empresa = empresaOpt.get();

        String nombre = (String) requestBody.get("nombre");
        String direccion = (String) requestBody.get("direccion");
        String descripcionNegocio = (String) requestBody.get("descripcionNegocio");
        String whatsappPhoneId = (String) requestBody.get("whatsappPhoneId");
        String whatsappToken = (String) requestBody.get("whatsappToken");
        String telefonoContacto = (String) requestBody.get("telefonoContacto");
        String mapsLink = (String) requestBody.get("mapsLink");


        if (esPromptSospechoso(descripcionNegocio)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Se detectaron directivas sospechosas que intentan alterar la seguridad de la IA (Inyección de Prompt). Por favor, introduce solo reglas informativas de tu negocio sin intentar cambiar la personalidad del bot."
            ));
        }

        if (nombre != null && !nombre.trim().isEmpty()) {
            empresa.setNombre(nombre.trim());
        }
        if (whatsappPhoneId != null && !whatsappPhoneId.trim().isEmpty()) {
            empresa.setWhatsappPhoneId(whatsappPhoneId.trim());
        }
        if (whatsappToken != null && !whatsappToken.trim().isEmpty() && !whatsappToken.contains("...") && !whatsappToken.contains("****")) {
            empresa.setWhatsappToken(whatsappToken.trim());
        }
        empresa.setDireccion(direccion);
        empresa.setDescripcionNegocio(descripcionNegocio);
        empresa.setTelefonoContacto(telefonoContacto);
        empresa.setMapsLink(mapsLink);
        


        empresaRepository.save(empresa);

        // Retornar la versión enmascarada para ser consistentes
        String maskedToken = null;
        if (empresa.getWhatsappToken() != null && !empresa.getWhatsappToken().trim().isEmpty()) {
            String token = empresa.getWhatsappToken().trim();
            if (token.length() > 10) {
                maskedToken = token.substring(0, 6) + "..." + token.substring(token.length() - 4);
            } else {
                maskedToken = "********";
            }
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", empresa.getId());
        response.put("nombre", empresa.getNombre());
        response.put("whatsappPhoneId", empresa.getWhatsappPhoneId());
        response.put("whatsappToken", maskedToken != null ? maskedToken : "");
        response.put("direccion", empresa.getDireccion());
        response.put("descripcionNegocio", empresa.getDescripcionNegocio());
        response.put("suscripcionActiva", empresa.getSuscripcionActiva());
        response.put("telefonoContacto", empresa.getTelefonoContacto());
        response.put("mapsLink", empresa.getMapsLink());


        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene la configuración de horarios (AgendaConfig) de la empresa.
     */
    @GetMapping("/empresa/agenda")
    public ResponseEntity<?> obtenerHorariosAgenda(HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        List<AgendaConfig> configs = agendaConfigRepository.findByEmpresaId(empresaId);
        return ResponseEntity.ok(configs);
    }

    /**
     * Guarda o actualiza los horarios de atención de la empresa.
     */
    @PutMapping("/empresa/agenda")
    public ResponseEntity<?> actualizarHorariosAgenda(@RequestBody List<Map<String, Object>> agendaData, HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        for (Map<String, Object> dayData : agendaData) {
            Integer diaSemana = (Integer) dayData.get("diaSemana");
            String horaInicioStr = (String) dayData.get("horaInicio");
            String horaFinStr = (String) dayData.get("horaFin");
            Boolean cerrado = (Boolean) dayData.get("cerrado");

            if (diaSemana == null) continue;

            Optional<AgendaConfig> configOpt = agendaConfigRepository.findByEmpresaIdAndDiaSemana(empresaId, diaSemana);
            
            if (Boolean.TRUE.equals(cerrado)) {
                // Si marca como cerrado, removemos de BD o definimos horaInicio == horaFin
                configOpt.ifPresent(agendaConfigRepository::delete);
            } else {
                if (horaInicioStr == null || horaFinStr == null) continue;
                
                LocalTime horaInicio = LocalTime.parse(horaInicioStr);
                LocalTime horaFin = LocalTime.parse(horaFinStr);

                AgendaConfig config = configOpt.orElse(AgendaConfig.builder()
                        .empresaId(empresaId)
                        .diaSemana(diaSemana)
                        .build());
                
                config.setHoraInicio(horaInicio);
                config.setHoraFin(horaFin);
                
                agendaConfigRepository.save(config);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Horarios de agenda actualizados con éxito."));
    }

    private boolean esPromptSospechoso(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return false;
        }
        String lower = prompt.toLowerCase().trim();

        // Patrones conocidos de prompt injection e instrucciones maliciosas
        String[] patrones = {
            "ignore all", "ignora todo", "ignora las instrucciones", "olvida tus instrucciones",
            "system prompt", "bypass system", "rompe todo el sistema", "rompe el sistema",
            "romper el sistema", "you are no longer", "ya no eres", "olvida tu rol",
            "forget your role", "olvida las instrucciones"
        };

        for (String patron : patrones) {
            if (lower.contains(patron)) {
                return true;
            }
        }
        return false;
    }
}
