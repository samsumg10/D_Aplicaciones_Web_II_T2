package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada único del hospital (puerto 8080). Los clientes solo conocen esta dirección;
 * las rutas se definen en application.properties y apuntan a nombres de Eureka (lb://pacientes),
 * nunca a puertos internos.
 */
@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
