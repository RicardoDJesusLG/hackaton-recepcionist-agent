package com.example.agente.dto;

import java.util.UUID;

public record CitaRequestDTO(
        UUID idNegocio,
        String telefonoCliente,
        UUID idServicio,
        String fechaHoraInicio,
        String nombreCliente,
        String correoCliente
) {}
