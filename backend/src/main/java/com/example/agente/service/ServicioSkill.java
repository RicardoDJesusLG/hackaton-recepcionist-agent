package com.example.agente.service;

import com.example.agente.dto.ServicioDTO;
import com.example.agente.model.Servicio;
import com.example.agente.repository.ServicioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio que actúa como Skill/Tool nativa para el SDK de Antigravity.
 * Expone de forma descriptiva un método para invocar el catálogo de servicios activos
 * de una empresa, permitiendo que el modelo Gemini active Function Calling de manera autónoma.
 */
@Service
public class ServicioSkill {

    private final ServicioRepository servicioRepository;

    public ServicioSkill(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    /**
     * Obtiene el catálogo de servicios activos ofrecidos por una empresa específica.
     * Debe ser invocado cuando el usuario pregunte por los servicios disponibles, precios,
     * tratamientos, o la duración de las citas del negocio.
     *
     * @param empresaId El identificador único (UUID) de la empresa/negocio de la cual se desea obtener el catálogo.
     * @return Una lista de DTOs conteniendo la información detallada de los servicios activos.
     */
    public List<ServicioDTO> obtenerCatalogoServicios(UUID empresaId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("El ID de la empresa no puede ser nulo");
        }

        List<Servicio> servicios = servicioRepository.findByEmpresaIdAndActivoTrue(empresaId);

        return servicios.stream()
                .map(s -> new ServicioDTO(
                        s.getId(),
                        s.getNombre(),
                        s.getDescripcion(),
                        s.getPrecio(),
                        s.getDuracionMinutos(),
                        s.getTipoPromocion() != null ? s.getTipoPromocion().name() : "NINGUNA",
                        s.getValorPromocion(),
                        s.getPromocionActiva()
                ))
                .collect(Collectors.toList());
    }
}
