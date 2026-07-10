package com.example.agente.repository;

import com.example.agente.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
    Optional<Empresa> findByWhatsappPhoneId(String whatsappPhoneId);
    Optional<Empresa> findByStripeSubscriptionId(String stripeSubscriptionId);
}
