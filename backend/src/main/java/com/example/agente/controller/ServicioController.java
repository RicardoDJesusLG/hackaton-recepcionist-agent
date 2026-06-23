package com.example.agente.controller;

import com.example.agente.dto.ServicioDTO;
import com.example.agente.service.ServicioSkill;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST complementario para exponer el catálogo de servicios de forma directa por HTTP.
 */
@RestController
@RequestMapping("/api/v1/servicios")
public class ServicioController {

    private final ServicioSkill servicioSkill;

    public ServicioController(ServicioSkill servicioSkill) {
        this.servicioSkill = servicioSkill;
    }

    /**
     * Endpoint GET para obtener la lista de servicios de un negocio.
     *
     * @param idNegocio El ID de la empresa en formato String (convertido internamente a UUID).
     * @return Una respuesta HTTP conteniendo la lista de ServicioDTO del catálogo.
     */
    @GetMapping
    public ResponseEntity<List<ServicioDTO>> obtenerServicios(@RequestParam("id_negocio") String idNegocio) {
        if (idNegocio == null || idNegocio.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        UUID empresaId;
        try {
            empresaId = UUID.fromString(idNegocio.trim());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        List<ServicioDTO> servicios = servicioSkill.obtenerCatalogoServicios(empresaId);
        return ResponseEntity.ok(servicios);
    }
}
