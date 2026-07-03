package com.example.agente.controller;

import com.example.agente.dto.ServicioDTO;
import com.example.agente.model.Servicio;
import com.example.agente.repository.ServicioRepository;
import com.example.agente.service.ServicioSkill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador REST para exponer y administrar el catálogo de servicios.
 */
@RestController
@RequestMapping("/api/v1/servicios")
public class ServicioController {

    private final ServicioSkill servicioSkill;
    private final ServicioRepository servicioRepository;

    public ServicioController(ServicioSkill servicioSkill, ServicioRepository servicioRepository) {
        this.servicioSkill = servicioSkill;
        this.servicioRepository = servicioRepository;
    }

    /**
     * Endpoint GET público para obtener la lista de servicios activos de un negocio (usado por Vertex AI).
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

    /**
     * Endpoint GET privado para obtener TODOS los servicios (activos e inactivos) del negocio autenticado.
     */
    @GetMapping("/admin")
    public ResponseEntity<?> obtenerServiciosAdmin(HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        List<Servicio> servicios = servicioRepository.findByEmpresaId(empresaId);
        return ResponseEntity.ok(servicios);
    }

    /**
     * Endpoint POST privado para crear un nuevo servicio.
     */
    @PostMapping
    public ResponseEntity<?> crearServicio(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        String nombre = (String) body.get("nombre");
        String descripcion = (String) body.get("descripcion");
        Object precioObj = body.get("precio");
        Object duracionObj = body.get("duracionMinutos");
        Boolean activo = (Boolean) body.get("activo");

        if (nombre == null || nombre.trim().isEmpty() || precioObj == null || duracionObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre, precio y duración son requeridos."));
        }

        BigDecimal precio;
        try {
            if (precioObj instanceof Integer) {
                precio = BigDecimal.valueOf((Integer) precioObj);
            } else if (precioObj instanceof Double) {
                precio = BigDecimal.valueOf((Double) precioObj);
            } else {
                precio = new BigDecimal(precioObj.toString());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Precio inválido."));
        }

        Integer duracionMinutos;
        try {
            duracionMinutos = Integer.valueOf(duracionObj.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Duración inválida."));
        }

        Servicio nuevo = Servicio.builder()
                .empresaId(empresaId)
                .nombre(nombre.trim())
                .descripcion(descripcion != null ? descripcion.trim() : "")
                .precio(precio)
                .duracionMinutos(duracionMinutos)
                .activo(activo != null ? activo : true)
                .build();

        nuevo = servicioRepository.save(nuevo);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    /**
     * Endpoint PUT privado para actualizar un servicio existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarServicio(@PathVariable("id") UUID id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        Optional<Servicio> servicioOpt = servicioRepository.findById(id);
        if (servicioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Servicio no encontrado"));
        }

        Servicio servicio = servicioOpt.get();
        if (!servicio.getEmpresaId().equals(empresaId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No tienes permisos sobre este servicio"));
        }

        String nombre = (String) body.get("nombre");
        String descripcion = (String) body.get("descripcion");
        Object precioObj = body.get("precio");
        Object duracionObj = body.get("duracionMinutos");
        Boolean activo = (Boolean) body.get("activo");

        if (nombre != null && !nombre.trim().isEmpty()) {
            servicio.setNombre(nombre.trim());
        }
        if (descripcion != null) {
            servicio.setDescripcion(descripcion.trim());
        }
        if (precioObj != null) {
            try {
                BigDecimal precio;
                if (precioObj instanceof Integer) {
                    precio = BigDecimal.valueOf((Integer) precioObj);
                } else if (precioObj instanceof Double) {
                    precio = BigDecimal.valueOf((Double) precioObj);
                } else {
                    precio = new BigDecimal(precioObj.toString());
                }
                servicio.setPrecio(precio);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Precio inválido."));
            }
        }
        if (duracionObj != null) {
            try {
                servicio.setDuracionMinutos(Integer.valueOf(duracionObj.toString()));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Duración inválida."));
            }
        }
        if (activo != null) {
            servicio.setActivo(activo);
        }

        servicio = servicioRepository.save(servicio);
        return ResponseEntity.ok(servicio);
    }

    /**
     * Endpoint DELETE privado para eliminar o desactivar un servicio.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarServicio(@PathVariable("id") UUID id, HttpServletRequest request) {
        UUID empresaId = (UUID) request.getAttribute("empresaId");
        if (empresaId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        Optional<Servicio> servicioOpt = servicioRepository.findById(id);
        if (servicioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Servicio no encontrado"));
        }

        Servicio servicio = servicioOpt.get();
        if (!servicio.getEmpresaId().equals(empresaId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No tienes permisos sobre este servicio"));
        }

        try {
            servicioRepository.delete(servicio);
            return ResponseEntity.ok(Map.of("message", "Servicio eliminado con éxito"));
        } catch (Exception e) {
            // Soft delete en caso de existir citas asociadas (evita violación de llave foránea restrictiva)
            servicio.setActivo(false);
            servicioRepository.save(servicio);
            return ResponseEntity.ok(Map.of(
                    "message", "El servicio tiene citas asociadas. Se ha desactivado para nuevos agendamientos por seguridad.",
                    "softDeleted", true
            ));
        }
    }
}
