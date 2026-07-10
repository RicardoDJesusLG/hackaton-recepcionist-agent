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
    private final EmpresaRepository empresaRepository;

    public CitaService(CitaRepository citaRepository,
                       ServicioRepository servicioRepository,
                       UsuarioRepository usuarioRepository,
                       AgendaConfigRepository agendaConfigRepository,
                       EmpresaRepository empresaRepository) {
        this.citaRepository = citaRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendaConfigRepository = agendaConfigRepository;
        this.empresaRepository = empresaRepository;
    }

    public AgendaConfigRepository getAgendaConfigRepository() {
        return agendaConfigRepository;
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
        if (configOpt.isEmpty()) {
            // Si no hay configuración para este día, el negocio está CERRADO.
            return List.of();
        }
        
        LocalTime horaInicio = configOpt.get().getHoraInicio();
        LocalTime horaFin = configOpt.get().getHoraFin();

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
                // No mostrar horarios que ya pasaron si la fecha consultada es hoy
                if (!fecha.equals(LocalDate.now()) || slotStartDateTime.isAfter(LocalDateTime.now())) {
                    slotsLibres.add(slotStart.toString() + " - " + slotEnd.toString());
                }
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

        // 0. Validar límite del plan de suscripción del negocio
        Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
        if (empresaOpt.isPresent()) {
            Empresa empresa = empresaOpt.get();
            String plan = empresa.getPlanSuscripcion();
            int maxCitasMes = Integer.MAX_VALUE;
            if ("BASIC".equalsIgnoreCase(plan)) {
                maxCitasMes = 30;
            } else if ("PRO".equalsIgnoreCase(plan)) {
                maxCitasMes = 150;
            }

            if (maxCitasMes < Integer.MAX_VALUE) {
                LocalDateTime ahora = LocalDateTime.now();
                LocalDateTime inicioMes = ahora.withDayOfMonth(1).with(LocalTime.MIN);
                LocalDateTime finMes = ahora.plusMonths(1).withDayOfMonth(1).minusDays(1).with(LocalTime.MAX);
                long citasActivasEsteMes = citaRepository.countByEmpresaIdAndFechaHoraInicioBetweenAndEstadoNot(
                        empresaId, inicioMes, finMes, EstadoCita.CANCELADA
                );

                if (citasActivasEsteMes >= maxCitasMes) {
                    throw new IllegalStateException("El negocio ha alcanzado el límite mensual de citas de su plan de suscripción.");
                }
            }
        }

        // 1. Obtener servicio y calcular hora fin
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado con el ID: " + servicioId));
        LocalDateTime endDateTime = startDateTime.plusMinutes(servicio.getDuracionMinutos());

        // 1.5 Validar que el día y la hora solicitada estén dentro de la disponibilidad oficial de la agenda
        int diaSemanaSql = startDateTime.toLocalDate().getDayOfWeek().getValue() % 7;
        Optional<AgendaConfig> configOpt = agendaConfigRepository.findByEmpresaIdAndDiaSemana(empresaId, diaSemanaSql);
        
        if (configOpt.isEmpty()) {
            throw new IllegalStateException("Lo siento, el negocio está CERRADO los días " 
                    + (diaSemanaSql == 0 ? "Domingo" : diaSemanaSql == 1 ? "Lunes" : diaSemanaSql == 2 ? "Martes" : diaSemanaSql == 3 ? "Miércoles" : diaSemanaSql == 4 ? "Jueves" : diaSemanaSql == 5 ? "Viernes" : "Sábado") + ".");
        }

        LocalTime horaInicioTrabajo = configOpt.get().getHoraInicio();
        LocalTime horaFinTrabajo = configOpt.get().getHoraFin();
        LocalTime horaInicioCita = startDateTime.toLocalTime();
        LocalTime horaFinCita = endDateTime.toLocalTime();

        if (horaInicioCita.isBefore(horaInicioTrabajo) || horaFinCita.isAfter(horaFinTrabajo)) {
            throw new IllegalStateException("El horario solicitado (" + horaInicioCita + " a " + horaFinCita + ") está fuera del horario de atención del negocio (" + horaInicioTrabajo + " a " + horaFinTrabajo + ").");
        }

        // 2. Buscar o crear el cliente en la tabla usuarios
        Usuario usuario = usuarioRepository.findByTelefonoWhatsapp(telefono)
                .orElseGet(() -> {
                    Usuario nuevo = Usuario.builder()
                            .telefonoWhatsapp(telefono)
                            .nombre("Cliente WhatsApp")
                            .build();
                    return usuarioRepository.save(nuevo);
                });

        // 2.5 Validar límite de citas pendientes (anti-spam)
        List<Cita> citasFuturas = citaRepository.findByUsuarioIdAndEmpresaIdAndFechaHoraInicioAfterAndEstadoNotOrderByFechaHoraInicioAsc(
                usuario.getId(), empresaId, LocalDateTime.now(), EstadoCita.CANCELADA
        );
        if (citasFuturas.size() >= 3) {
            throw new IllegalStateException("Has alcanzado el límite máximo de 3 citas programadas. Por favor asiste a tus citas pendientes o cancela alguna antes de agendar una nueva.");
        }
        long citasMismoDia = citasFuturas.stream()
                .filter(c -> c.getFechaHoraInicio().toLocalDate().equals(startDateTime.toLocalDate()))
                .count();
        if (citasMismoDia >= 3) {
            throw new IllegalStateException("Has alcanzado el límite de 3 citas programadas para el día " + startDateTime.toLocalDate() + ". Por favor elige otra fecha.");
        }

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
                sb.append("- ").append(dias[i]).append(": Cerrado\n");
            }
        }
        return sb.toString();
    }
}
