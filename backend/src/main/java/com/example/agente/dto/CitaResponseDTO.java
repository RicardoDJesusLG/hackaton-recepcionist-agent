package com.example.agente.dto;

import java.util.UUID;

public record CitaResponseDTO(
        UUID idCita,
        UUID idNegocio,
        String telefonoCliente,
        UUID idServicio,
        String nombreServicio,
        String fechaHoraInicio,
        String fechaHoraFin,
        String estado
) {}
