package com.example.agente.repository;

import com.example.agente.model.AgendaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgendaConfigRepository extends JpaRepository<AgendaConfig, UUID> {
    Optional<AgendaConfig> findByEmpresaIdAndDiaSemana(UUID empresaId, Integer diaSemana);
    java.util.List<AgendaConfig> findByEmpresaId(UUID empresaId);
}
