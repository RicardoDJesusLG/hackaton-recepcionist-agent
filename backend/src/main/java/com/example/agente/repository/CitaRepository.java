package com.example.agente.repository;

import com.example.agente.model.Cita;
import com.example.agente.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CitaRepository extends JpaRepository<Cita, UUID> {
    List<Cita> findByEmpresaIdAndFechaHoraInicioBetweenAndEstadoNot(
            UUID empresaId,
            LocalDateTime start,
            LocalDateTime end,
            EstadoCita estado
    );

    List<Cita> findByEmpresaIdOrderByFechaHoraInicioDesc(UUID empresaId);

    /**
     * Busca citas futuras (no canceladas) de un usuario en una empresa específica.
     * Usado por el skill "obtenerMisCitas" del agente.
     */
    List<Cita> findByUsuarioIdAndEmpresaIdAndFechaHoraInicioAfterAndEstadoNotOrderByFechaHoraInicioAsc(
            UUID usuarioId,
            UUID empresaId,
            LocalDateTime after,
            EstadoCita estado
    );
}
