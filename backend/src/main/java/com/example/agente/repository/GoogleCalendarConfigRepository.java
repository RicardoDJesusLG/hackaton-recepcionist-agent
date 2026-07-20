package com.example.agente.repository;

import com.example.agente.model.GoogleCalendarConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoogleCalendarConfigRepository extends JpaRepository<GoogleCalendarConfig, UUID> {

    /**
     * Busca la configuración de Google Calendar para una empresa específica.
     */
    Optional<GoogleCalendarConfig> findByEmpresaId(UUID empresaId);

    /**
     * Verifica si una empresa ya tiene tokens de Google Calendar configurados.
     */
    boolean existsByEmpresaId(UUID empresaId);
}
