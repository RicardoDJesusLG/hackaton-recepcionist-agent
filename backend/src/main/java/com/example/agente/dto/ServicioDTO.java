package com.example.agente.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicioDTO(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos,
        String tipoPromocion,
        String valorPromocion,
        Boolean promocionActiva
) {}
