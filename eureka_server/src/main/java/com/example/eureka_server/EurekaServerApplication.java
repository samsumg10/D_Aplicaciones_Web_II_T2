package com.example.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Registro de servicios. Cada microservicio se registra al iniciar con su nombre (pacientes, citas...)
 * y envía un latido cada 30 s. El Gateway y los clientes WebClient con @LoadBalanced consultan este
 * registro para encontrar una instancia disponible. Panel: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaServerApplication.class, args);
	}

}
