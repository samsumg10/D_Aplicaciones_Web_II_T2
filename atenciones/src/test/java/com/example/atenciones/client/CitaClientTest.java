package com.example.atenciones.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.atenciones.exception.BusinessException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Contrato síncrono atenciones -> citas, con respuestas HTTP simuladas. */
class CitaClientTest {

    @Test
    void obtieneLaCitaYEnviaElCambioAAtendidaConPatch() {
        CitaClient client = new CitaClient(WebClient.builder().exchangeFunction(request -> {
            String estado = request.method() == HttpMethod.PATCH ? "ATENDIDA" : "CONFIRMADA";
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"id\":10,\"codigo\":\"CIT-0000010\",\"pacienteId\":1,\"estado\":\"" + estado + "\"}")
                    .build());
        }), "http://citas", Duration.ofSeconds(1));

        StepVerifier.create(client.obtenerCita(10L))
                .assertNext(c -> assertThat(c.estaConfirmada()).isTrue())
                .verifyComplete();
        StepVerifier.create(client.marcarAtendida(10L))
                .assertNext(c -> assertThat(c.estado()).isEqualTo("ATENDIDA"))
                .verifyComplete();
    }

    @Test
    void un409DeCitasSeTraduceEnReglaDeNegocio() {
        CitaClient client = new CitaClient(WebClient.builder().exchangeFunction(
                request -> Mono.just(ClientResponse.create(HttpStatus.CONFLICT).build())), "http://citas", Duration.ofSeconds(1));

        StepVerifier.create(client.marcarAtendida(10L))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-07");
                })
                .verify();
    }

    @Test
    void un404DeCitasEsCitaNoEncontrada() {
        CitaClient client = new CitaClient(WebClient.builder().exchangeFunction(
                request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())), "http://citas", Duration.ofSeconds(1));

        StepVerifier.create(client.obtenerCita(99L))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("CITA_NO_ENCONTRADA"))
                .verify();
    }
}
