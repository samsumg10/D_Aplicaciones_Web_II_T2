package com.example.citas.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.example.citas.exception.BusinessException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Contrato síncrono citas -> pacientes. Se simula la respuesta HTTP con un ExchangeFunction,
 * sin levantar el servicio de pacientes ni Eureka.
 */
class PacienteClientTest {

    @Test
    void devuelveElPacienteCuandoExiste() {
        PacienteClient client = cliente(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"id\":1,\"nombres\":\"Ana\",\"apellidos\":\"Prueba\",\"estado\":\"ACTIVO\",\"correo\":\"x@correo.test\"}")
                .build()));

        StepVerifier.create(client.obtenerPaciente(1L))
                .assertNext(p -> {
                    assertThat(p.id()).isEqualTo(1L);
                    assertThat(p.estaActivo()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void un404SeTraduceAPacienteNoEncontradoSinReintentar() {
        AtomicInteger llamadas = new AtomicInteger();
        PacienteClient client = cliente(request -> {
            llamadas.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        });

        StepVerifier.create(client.obtenerPaciente(99L))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("PACIENTE_NO_ENCONTRADO");
                })
                .verify();
        assertThat(llamadas).hasValue(1);
    }

    @Test
    void siPacientesNoRespondeSeReintentaYLuegoDevuelve503() {
        AtomicInteger llamadas = new AtomicInteger();
        PacienteClient client = cliente(request -> {
            llamadas.incrementAndGet();
            return Mono.error(new WebClientRequestException(new ConnectException("Connection refused"),
                    HttpMethod.GET, URI.create("http://pacientes/api/pacientes/1"), new HttpHeaders()));
        });

        StepVerifier.create(client.obtenerPaciente(1L))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("PACIENTES_NO_DISPONIBLE");
                })
                .verify(Duration.ofSeconds(5));
        assertThat(llamadas).hasValue(3);   // intento original + 2 reintentos
    }

    @Test
    void siPacientesTardaMasDelTimeoutDevuelve503() {
        PacienteClient client = new PacienteClient(
                WebClient.builder().exchangeFunction(request -> Mono.never()), "http://pacientes", Duration.ofMillis(100));

        StepVerifier.create(client.obtenerPaciente(1L))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .verify(Duration.ofSeconds(5));
    }

    private PacienteClient cliente(ExchangeFunction respuesta) {
        return new PacienteClient(WebClient.builder().exchangeFunction(respuesta), "http://pacientes", Duration.ofSeconds(1));
    }
}
