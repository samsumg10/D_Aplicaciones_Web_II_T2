package com.example.atenciones.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * WebClient con balanceo de carga: una URL como http://pacientes se resuelve preguntando a Eureka por las
     * instancias registradas con ese nombre. Así ningún servicio depende del puerto interno de otro.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
