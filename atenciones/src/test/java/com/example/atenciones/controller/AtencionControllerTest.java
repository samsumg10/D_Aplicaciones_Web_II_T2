package com.example.atenciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.atenciones.domain.AreaAtencion;
import com.example.atenciones.domain.EstadoAtencion;
import com.example.atenciones.dto.AtencionResponse;
import com.example.atenciones.exception.BusinessException;
import com.example.atenciones.service.AtencionService;

import reactor.core.publisher.Mono;

@WebFluxTest(AtencionController.class)
class AtencionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AtencionService service;

    @Test
    void registrarAtencionDevuelve201() {
        when(service.registrar(any())).thenReturn(Mono.just(new AtencionResponse(5L, "ATE-0000005", 1L, 10L,
                AreaAtencion.CONSULTA_EXTERNA, null, EstadoAtencion.REGISTRADA, LocalDateTime.now(), null)));

        webTestClient.post().uri("/api/atenciones")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("citaId", 10, "area", "CONSULTA_EXTERNA"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.citaId").isEqualTo(10)
                .jsonPath("$.estado").isEqualTo("REGISTRADA");
    }

    @Test
    void citaNoConfirmadaDevuelve409() {
        when(service.registrar(any())).thenReturn(Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-07",
                "Solo se registran atenciones de citas CONFIRMADAS")));

        webTestClient.post().uri("/api/atenciones")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("citaId", 10, "area", "CONSULTA_EXTERNA"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("RN-07");
    }

    @Test
    void atencionInexistenteDevuelve404() {
        when(service.buscarPorId(99L)).thenReturn(Mono.error(new BusinessException(HttpStatus.NOT_FOUND,
                "ATENCION_NO_ENCONTRADA", "No existe una atención con id 99")));

        webTestClient.get().uri("/api/atenciones/99")
                .exchange()
                .expectStatus().isNotFound();
    }
}
