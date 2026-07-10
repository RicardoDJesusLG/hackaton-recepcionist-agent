package com.example.agente.service;

import com.example.agente.dto.ServicioDTO;
import com.example.agente.model.Empresa;
import com.example.agente.model.Servicio;
import com.example.agente.repository.EmpresaRepository;
import com.example.agente.repository.ServicioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
    private final EmpresaRepository empresaRepository;

    public ServicioSkill(ServicioRepository servicioRepository, EmpresaRepository empresaRepository) {
        this.servicioRepository = servicioRepository;
        this.empresaRepository = empresaRepository;
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

        Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
        String plan = empresaOpt.map(Empresa::getPlanSuscripcion).orElse("BASIC");

        int limit = Integer.MAX_VALUE;
        if ("BASIC".equalsIgnoreCase(plan)) {
            limit = 3;
        } else if ("PRO".equalsIgnoreCase(plan)) {
            limit = 10;
        }

        List<Servicio> servicios = servicioRepository.findByEmpresaIdAndActivoTrue(empresaId);

        return servicios.stream()
                .limit(limit)
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
