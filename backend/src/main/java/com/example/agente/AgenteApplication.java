package com.example.agente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AgenteApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgenteApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner seedDefaultUser(
			com.example.agente.repository.OwnerRepository ownerRepository,
			com.example.agente.repository.EmpresaRepository empresaRepository,
			org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
		return args -> {
			if (ownerRepository.count() == 0) {
				com.example.agente.model.Empresa empresa = new com.example.agente.model.Empresa();
				empresa.setNombre("Barberia San Jose");
				empresa.setWhatsappPhoneId("1124241077447249");
				empresa.setDescripcionNegocio("Somos una barbería profesional.");
				empresa.setPlanSuscripcion("PRO");
				empresa.setSuscripcionActiva(true);
				empresa = empresaRepository.save(empresa);

				com.example.agente.model.Owner owner = new com.example.agente.model.Owner();
				owner.setEmail("admin@admin.com");
				owner.setPassword(passwordEncoder.encode("admin123"));
				owner.setEmpresaId(empresa.getId());
				ownerRepository.save(owner);

				com.example.agente.model.Owner owner3 = new com.example.agente.model.Owner();
				owner3.setEmail("woyeh55165@fisedo.com");
				owner3.setPassword(passwordEncoder.encode("NarutoUzumaki1."));
				owner3.setEmpresaId(empresa.getId());
				ownerRepository.save(owner3);

				System.out.println("[Seed] Usuarios de prueba creados exitosamente.");
			}
		};
	}
}
