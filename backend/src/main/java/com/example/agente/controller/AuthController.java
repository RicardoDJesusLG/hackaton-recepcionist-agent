package com.example.agente.controller;

import com.example.agente.model.Owner;
import com.example.agente.repository.OwnerRepository;
import com.example.agente.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OwnerRepository ownerRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(OwnerRepository ownerRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registro de un nuevo propietario para el dashboard.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registrarOwner(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String empresaIdStr = request.get("empresaId");

        if (username == null || password == null || empresaIdStr == null ||
            username.trim().isEmpty() || password.trim().isEmpty() || empresaIdStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Todos los campos son obligatorios"));
        }

        if (ownerRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "El nombre de usuario ya existe"));
        }

        UUID empresaId;
        try {
            empresaId = UUID.fromString(empresaIdStr.trim());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "El ID de la empresa no tiene un formato válido"));
        }

        Owner nuevoOwner = Owner.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(password))
                .empresaId(empresaId)
                .build();

        ownerRepository.save(nuevoOwner);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Usuario registrado exitosamente"));
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
