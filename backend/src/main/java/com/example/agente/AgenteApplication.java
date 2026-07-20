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
}
