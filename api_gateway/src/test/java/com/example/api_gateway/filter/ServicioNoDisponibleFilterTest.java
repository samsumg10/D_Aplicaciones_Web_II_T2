package com.example.api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ServicioNoDisponibleFilterTest {

    private final ServicioNoDisponibleFilter filtro = new ServicioNoDisponibleFilter();

    @Test
    void conexionRechazadaPorElMicroservicioSeRespondeCon503() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/pacientes/1"));

        StepVerifier.create(filtro.filter(exchange, ex -> Mono.error(new ConnectException("Connection refused"))))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(cuerpo -> assertThat(cuerpo).contains("SERVICIO_NO_DISPONIBLE"))
                .verifyComplete();
    }

    @Test
    void otrosErroresNoSeOcultan() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/pacientes/1"));

        StepVerifier.create(filtro.filter(exchange, ex -> Mono.error(new IllegalStateException("otro error"))))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
