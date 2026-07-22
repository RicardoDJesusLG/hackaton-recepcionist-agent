package com.example.agente.controller;

import com.example.agente.model.Owner;
import com.example.agente.model.Empresa;
import com.example.agente.model.AgendaConfig;
import com.example.agente.repository.OwnerRepository;
import com.example.agente.repository.EmpresaRepository;
import com.example.agente.repository.AgendaConfigRepository;
import com.example.agente.security.JwtUtil;
import com.example.agente.service.EmailService;
import com.example.agente.service.WhatsAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OwnerRepository ownerRepository;
    private final EmpresaRepository empresaRepository;
    private final AgendaConfigRepository agendaConfigRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    // Caché en memoria para códigos de recuperación
    private final ConcurrentHashMap<String, RecoveryData> recoveryCache = new ConcurrentHashMap<>();

    private static class RecoveryData {
        private final String code;
        private final LocalDateTime expiry;

        public RecoveryData(String code, LocalDateTime expiry) {
            this.code = code;
            this.expiry = expiry;
        }

        public String getCode() { return code; }
        public LocalDateTime getExpiry() { return expiry; }
    }

    public AuthController(OwnerRepository ownerRepository,
                          EmpresaRepository empresaRepository,
                          AgendaConfigRepository agendaConfigRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          EmailService emailService,
                          WhatsAppService whatsAppService) {
        this.ownerRepository = ownerRepository;
        this.empresaRepository = empresaRepository;
        this.agendaConfigRepository = agendaConfigRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
    }


    /**
     * Registro de un nuevo propietario y opcionalmente creación de la empresa vinculada.
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> registrarOwner(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String password = (String) request.get("password");
        Boolean crearNuevaEmpresa = (Boolean) request.get("crearNuevaEmpresa");

        if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Correo electrónico y contraseña son requeridos."));
        }

        // Validación básica de formato de correo
        email = email.trim().toLowerCase();
        if (!email.matches("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "El formato del correo electrónico no es válido."));
        }

        if (ownerRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "El correo electrónico ya está registrado."));
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

            if (empresaRepository.findByWhatsappPhoneId(whatsappPhoneId.trim()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "El WhatsApp Phone ID ya está en uso por otro negocio. Registra un ID diferente."
                ));
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
                    .suscripcionActiva(false)
                    .build();

            nuevaEmpresa = empresaRepository.save(nuevaEmpresa);
            empresaId = nuevaEmpresa.getId();

            // Auto-suscribir webhook de la app en Meta
            if (nuevaEmpresa.getWhatsappPhoneId() != null && nuevaEmpresa.getWhatsappToken() != null) {
                whatsAppService.suscribirAppAWaba(nuevaEmpresa.getWhatsappPhoneId(), nuevaEmpresa.getWhatsappToken());
            }

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
                .email(email)
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
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Correo electrónico y contraseña requeridos"));
        }

        Optional<Owner> ownerOpt = ownerRepository.findByEmail(email.trim().toLowerCase());
        if (ownerOpt.isEmpty() || !passwordEncoder.matches(password, ownerOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }

        Owner owner = ownerOpt.get();
        String token = jwtUtil.generarToken(owner.getEmail(), owner.getEmpresaId());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", owner.getEmail(),
                "empresaId", owner.getEmpresaId().toString()
        ));
    }

    /**
     * Endpoint para solicitar la recuperación de contraseña.
     * Genera un código de 6 dígitos y lo imprime en consola (temporalmente).
     * En producción, se enviará por correo electrónico con SendGrid.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo electrónico es requerido."));
        }

        email = email.trim().toLowerCase();
        Optional<Owner> ownerOpt = ownerRepository.findByEmail(email);
        if (ownerOpt.isEmpty()) {
            // Retornamos 200 por seguridad para no revelar si el correo existe o no
            return ResponseEntity.ok(Map.of("message", "Si el correo está registrado, se ha enviado un código de verificación."));
        }

        // Generar un código aleatorio de 6 dígitos
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        
        // Guardar en la caché temporal (expira en 5 minutos)
        recoveryCache.put(email, new RecoveryData(code, LocalDateTime.now().plusMinutes(5)));
        
        // Enviar correo
        String subject = "Código de Recuperación - Recepción Inteligente";
        String messageBody = "Hola,\n\n"
                + "Has solicitado restablecer tu contraseña para el Panel Administrativo de Recepción Inteligente.\n\n"
                + "Tu código de verificación es: " + code + "\n\n"
                + "Este código es válido por 5 minutos.\n\n"
                + "Si no solicitaste este cambio, puedes ignorar este correo de forma segura.\n\n"
                + "Atentamente,\n"
                + "Soporte de Recepción Inteligente";
        
        emailService.enviarCorreo(email, subject, messageBody);

        return ResponseEntity.ok(Map.of("message", "Se ha enviado un código de verificación a tu correo electrónico."));
    }

    /**
     * Endpoint para restablecer la contraseña usando el código recibido.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        if (email == null || email.trim().isEmpty() ||
            code == null || code.trim().isEmpty() ||
            newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Correo, código y nueva contraseña son requeridos."));
        }

        email = email.trim().toLowerCase();
        code = code.trim();

        RecoveryData data = recoveryCache.get(email);
        if (data == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se ha solicitado una recuperación de contraseña para este correo o la sesión expiró."));
        }

        if (!data.getCode().equals(code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código de verificación incorrecto."));
        }

        if (data.getExpiry().isBefore(LocalDateTime.now())) {
            recoveryCache.remove(email);
            return ResponseEntity.badRequest().body(Map.of("error", "El código de verificación ha expirado. Por favor, solicita uno nuevo."));
        }

        Optional<Owner> ownerOpt = ownerRepository.findByEmail(email);
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Correo no encontrado."));
        }

        Owner owner = ownerOpt.get();
        owner.setPassword(passwordEncoder.encode(newPassword));
        ownerRepository.save(owner);

        // Limpiar la caché
        recoveryCache.remove(email);
        System.out.println("[Recovery] Contraseña restablecida con éxito para " + email);

        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida con éxito."));
    }
}
