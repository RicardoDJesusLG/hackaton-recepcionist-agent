package com.example.agente.repository;

import com.example.agente.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {
    List<Servicio> findByEmpresaIdAndActivoTrue(UUID empresaId);
    long countByEmpresaIdAndActivoTrue(UUID empresaId);
}
