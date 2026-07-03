package com.example.agente.controller;

import com.example.agente.model.Owner;
import com.example.agente.model.Empresa;
import com.example.agente.model.AgendaConfig;
import com.example.agente.repository.OwnerRepository;
import com.example.agente.repository.EmpresaRepository;
import com.example.agente.repository.AgendaConfigRepository;
import com.example.agente.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OwnerRepository ownerRepository;
    private final EmpresaRepository empresaRepository;
    private final AgendaConfigRepository agendaConfigRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(OwnerRepository ownerRepository,
                          EmpresaRepository empresaRepository,
                          AgendaConfigRepository agendaConfigRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.ownerRepository = ownerRepository;
        this.empresaRepository = empresaRepository;
        this.agendaConfigRepository = agendaConfigRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registro de un nuevo propietario y opcionalmente creación de la empresa vinculada.
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> registrarOwner(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        Boolean crearNuevaEmpresa = (Boolean) request.get("crearNuevaEmpresa");

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Usuario y contraseña son requeridos."));
        }

        if (ownerRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "El nombre de usuario ya existe."));
        }

        UUID empresaId;

        if (Boolean.TRUE.equals(crearNuevaEmpresa)) {
            String nombreEmpresa = (String) request.get("nombreEmpresa");
            String whatsappPhoneId = (String) request.get("whatsappPhoneId");
            String whatsappToken = (String) request.get("whatsappToken");
            String direccion = (String) request.get("direccion");
            String descripcionNegocio = (String) request.get("descripcionNegocio");
            String telefonoContacto = (String) request.get("telefonoContacto");
            String mapsLink = (String) request.get("mapsLink");

            if (nombreEmpresa == null || nombreEmpresa.trim().isEmpty() || whatsappPhoneId == null || whatsappPhoneId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nombre de empresa y ID de teléfono de WhatsApp son requeridos."));
            }

            // 1. Crear empresa
            Empresa nuevaEmpresa = Empresa.builder()
                    .nombre(nombreEmpresa.trim())
                    .whatsappPhoneId(whatsappPhoneId.trim())
                    .whatsappToken(whatsappToken != null ? whatsappToken.trim() : null)
                    .direccion(direccion != null ? direccion.trim() : "")
                    .descripcionNegocio(descripcionNegocio != null ? descripcionNegocio.trim() : "")
                    .telefonoContacto(telefonoContacto != null ? telefonoContacto.trim() : "")
                    .mapsLink(mapsLink != null ? mapsLink.trim() : "")
                    .suscripcionActiva(true)
                    .build();

            nuevaEmpresa = empresaRepository.save(nuevaEmpresa);
            empresaId = nuevaEmpresa.getId();

            // 2. Generar configuración de agenda por defecto (Lunes a Sábado de 9:00 AM a 6:00 PM)
            for (int i = 1; i <= 6; i++) {
                AgendaConfig config = AgendaConfig.builder()
                        .empresaId(empresaId)
                        .diaSemana(i)
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(18, 0))
                        .build();
                agendaConfigRepository.save(config);
            }
        } else {
            String empresaIdStr = (String) request.get("empresaId");
            if (empresaIdStr == null || empresaIdStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El ID de la empresa es requerido si no estás creando una nueva."));
            }

            try {
                empresaId = UUID.fromString(empresaIdStr.trim());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "El ID de la empresa no tiene un formato válido."));
            }

            if (empresaRepository.findById(empresaId).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "La empresa con el ID proporcionado no existe."));
            }
        }

        // 3. Crear propietario
        Owner nuevoOwner = Owner.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(password))
                .empresaId(empresaId)
                .build();

        ownerRepository.save(nuevoOwner);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Usuario registrado exitosamente.",
                "empresaId", empresaId.toString()
        ));
    }

    /**
     * Inicio de sesión para obtener el token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Usuario y contraseña requeridos"));
        }

        Optional<Owner> ownerOpt = ownerRepository.findByUsername(username.trim());
        if (ownerOpt.isEmpty() || !passwordEncoder.matches(password, ownerOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }

        Owner owner = ownerOpt.get();
        String token = jwtUtil.generarToken(owner.getUsername(), owner.getEmpresaId());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", owner.getUsername(),
                "empresaId", owner.getEmpresaId().toString()
        ));
    }
}
