package com.example.agente.service;

import com.example.agente.dto.CitaRequestDTO;
import com.example.agente.dto.CitaResponseDTO;
import com.example.agente.model.*;
import com.example.agente.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendaConfigRepository agendaConfigRepository;

    public CitaService(CitaRepository citaRepository,
                       ServicioRepository servicioRepository,
                       UsuarioRepository usuarioRepository,
                       AgendaConfigRepository agendaConfigRepository) {
        this.citaRepository = citaRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendaConfigRepository = agendaConfigRepository;
    }

    /**
     * Calcula los bloques de tiempo libres para un servicio en una fecha específica.
     */
    public List<String> obtenerDisponibilidad(UUID empresaId, UUID servicioId, LocalDate fecha) {
        // 1. Obtener el servicio y su duración
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado con el ID: " + servicioId));
        int duracion = servicio.getDuracionMinutos();

        // 2. Obtener el día de la semana (0 = Domingo, 1 = Lunes, ..., 6 = Sábado)
        int diaSemanaSql = fecha.getDayOfWeek().getValue() % 7;

        // 3. Buscar la configuración de agenda de la empresa para ese día
        Optional<AgendaConfig> configOpt = agendaConfigRepository.findByEmpresaIdAndDiaSemana(empresaId, diaSemanaSql);
        LocalTime horaInicio = LocalTime.of(9, 0);
        LocalTime horaFin = LocalTime.of(18, 0);
        if (configOpt.isPresent()) {
            horaInicio = configOpt.get().getHoraInicio();
            horaFin = configOpt.get().getHoraFin();
        }

        // 4. Obtener citas existentes del día que no estén canceladas
        LocalDateTime inicioDia = fecha.atTime(LocalTime.MIN);
        LocalDateTime finDia = fecha.atTime(LocalTime.MAX);
        List<Cita> citasExistentes = citaRepository.findByEmpresaIdAndFechaHoraInicioBetweenAndEstadoNot(
                empresaId, inicioDia, finDia, EstadoCita.CANCELADA
        );

        // 5. Calcular bloques libres
        List<String> slotsLibres = new ArrayList<>();
        LocalTime actual = horaInicio;
        int paso = Math.min(30, duracion);

        while (actual.plusMinutes(duracion).isBefore(horaFin) || actual.plusMinutes(duracion).equals(horaFin)) {
            LocalTime slotStart = actual;
            LocalTime slotEnd = actual.plusMinutes(duracion);

            LocalDateTime slotStartDateTime = fecha.atTime(slotStart);
            LocalDateTime slotEndDateTime = fecha.atTime(slotEnd);

            boolean ocupado = false;
            for (Cita cita : citasExistentes) {
                if (slotStartDateTime.isBefore(cita.getFechaHoraFin()) && slotEndDateTime.isAfter(cita.getFechaHoraInicio())) {
                    ocupado = true;
                    break;
                }
            }

            if (!ocupado) {
                slotsLibres.add(slotStart.toString() + " - " + slotEnd.toString());
            }

            actual = actual.plusMinutes(paso);
        }

        return slotsLibres;
    }

    /**
     * Inserta una nueva cita en la base de datos previa validación de solapamiento.
     */
    @Transactional
    public CitaResponseDTO agendarCita(CitaRequestDTO request) {
        UUID empresaId = request.idNegocio();
        UUID servicioId = request.idServicio();
        String telefono = request.telefonoCliente();
        
        LocalDateTime startDateTime = LocalDateTime.parse(request.fechaHoraInicio());

        // 1. Obtener servicio y calcular hora fin
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado con el ID: " + servicioId));
        LocalDateTime endDateTime = startDateTime.plusMinutes(servicio.getDuracionMinutos());

        // 2. Buscar o crear el cliente en la tabla usuarios
        Usuario usuario = usuarioRepository.findByTelefonoWhatsapp(telefono)
                .orElseGet(() -> {
                    Usuario nuevo = Usuario.builder()
                            .telefonoWhatsapp(telefono)
                            .nombre("Cliente WhatsApp")
                            .build();
                    return usuarioRepository.save(nuevo);
                });

        // 3. Validar solapamiento con citas existentes
        LocalDateTime inicioDia = startDateTime.toLocalDate().atTime(LocalTime.MIN);
        LocalDateTime finDia = startDateTime.toLocalDate().atTime(LocalTime.MAX);
        List<Cita> citasExistentes = citaRepository.findByEmpresaIdAndFechaHoraInicioBetweenAndEstadoNot(
                empresaId, inicioDia, finDia, EstadoCita.CANCELADA
        );

        for (Cita cita : citasExistentes) {
            if (startDateTime.isBefore(cita.getFechaHoraFin()) && endDateTime.isAfter(cita.getFechaHoraInicio())) {
                throw new IllegalStateException("El horario solicitado ya se encuentra ocupado por otra cita.");
            }
        }

        // 4. Registrar la cita
        Cita nuevaCita = Cita.builder()
                .empresaId(empresaId)
                .usuarioId(usuario.getId())
                .servicioId(servicioId)
                .fechaHoraInicio(startDateTime)
                .fechaHoraFin(endDateTime)
                .estado(EstadoCita.CONFIRMADA)
                .build();

        nuevaCita = citaRepository.save(nuevaCita);

        // 5. Retornar DTO de confirmación plano
        return new CitaResponseDTO(
                nuevaCita.getId(),
                nuevaCita.getEmpresaId(),
                usuario.getTelefonoWhatsapp(),
                nuevaCita.getServicioId(),
                servicio.getNombre(),
                nuevaCita.getFechaHoraInicio().toString(),
                nuevaCita.getFechaHoraFin().toString(),
                nuevaCita.getEstado().name()
        );
    }

    /**
     * Cancela una cita por su ID, validando que pertenezca a la empresa indicada.
     * Usado por el skill "cancelarCita" del agente.
     */
    @Transactional
    public void cancelarCita(UUID idCita, UUID empresaId) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con el ID: " + idCita));

        if (!cita.getEmpresaId().equals(empresaId)) {
            throw new IllegalArgumentException("La cita no pertenece a la empresa indicada.");
        }

        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("La cita ya se encuentra cancelada.");
        }

        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);
        System.out.println("[CitaService] Cita cancelada exitosamente: " + idCita);
    }

    /**
     * Obtiene las citas futuras (no canceladas) de un cliente por su teléfono.
     * Usado por el skill "obtenerMisCitas" del agente.
     */
    public List<CitaResponseDTO> obtenerCitasPorTelefono(String telefono, UUID empresaId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTelefonoWhatsapp(telefono);
        if (usuarioOpt.isEmpty()) {
            return List.of(); // No tiene citas si no existe como usuario
        }

        UUID usuarioId = usuarioOpt.get().getId();
        LocalDateTime ahora = LocalDateTime.now();

        List<Cita> citas = citaRepository
                .findByUsuarioIdAndEmpresaIdAndFechaHoraInicioAfterAndEstadoNotOrderByFechaHoraInicioAsc(
                        usuarioId, empresaId, ahora, EstadoCita.CANCELADA);

        List<CitaResponseDTO> resultado = new java.util.ArrayList<>();
        for (Cita cita : citas) {
            String servicioNombre = "Servicio desconocido";
            Optional<Servicio> servicioOpt = servicioRepository.findById(cita.getServicioId());
            if (servicioOpt.isPresent()) {
                servicioNombre = servicioOpt.get().getNombre();
            }

            resultado.add(new CitaResponseDTO(
                    cita.getId(),
                    cita.getEmpresaId(),
                    telefono,
                    cita.getServicioId(),
                    servicioNombre,
                    cita.getFechaHoraInicio().toString(),
                    cita.getFechaHoraFin().toString(),
                    cita.getEstado().name()
            ));
        }

        return resultado;
    }

    /**
     * Obtiene los horarios de atención de una empresa para todos los días de la semana.
     * Usado por el skill "obtenerHorariosAtencion" del agente.
     */
    public String obtenerHorariosAtencion(UUID empresaId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Horarios de atención del negocio:\n");
        String[] dias = {"Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

        for (int i = 0; i < 7; i++) {
            Optional<AgendaConfig> configOpt = agendaConfigRepository.findByEmpresaIdAndDiaSemana(empresaId, i);
            if (configOpt.isPresent()) {
                sb.append("- ").append(dias[i]).append(": ")
                  .append(configOpt.get().getHoraInicio()).append(" a ").append(configOpt.get().getHoraFin()).append("\n");
            } else {
                if (i == 0) {
                    sb.append("- ").append(dias[i]).append(": Cerrado\n");
                } else {
                    sb.append("- ").append(dias[i]).append(": 09:00 a 18:00\n");
                }
            }
        }
        return sb.toString();
    }
}
