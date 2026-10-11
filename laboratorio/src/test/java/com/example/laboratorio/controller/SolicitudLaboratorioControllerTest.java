package com.example.laboratorio.controller;

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

import com.example.laboratorio.domain.EstadoSolicitud;
import com.example.laboratorio.domain.TipoExamen;
import com.example.laboratorio.dto.SolicitudResponse;
import com.example.laboratorio.exception.BusinessException;
import com.example.laboratorio.service.SolicitudLaboratorioService;

import reactor.core.publisher.Mono;

@WebFluxTest(SolicitudLaboratorioController.class)
class SolicitudLaboratorioControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SolicitudLaboratorioService service;

    @Test
    void registrarSolicitudDevuelve201() {
        when(service.registrar(any())).thenReturn(Mono.just(new SolicitudResponse(3L, "LAB-00000003", 1L, 5L,
                TipoExamen.GLUCOSA, EstadoSolicitud.SOLICITADO, null, LocalDateTime.now(), null)));

        webTestClient.post().uri("/api/laboratorio/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("atencionId", 5, "tipoExamen", "GLUCOSA"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("SOLICITADO");
    }

    @Test
    void atencionInexistenteDevuelve404() {
        when(service.registrar(any())).thenReturn(Mono.error(new BusinessException(HttpStatus.NOT_FOUND,
                "ATENCION_NO_ENCONTRADA", "No existe una atención con id 99")));

        webTestClient.post().uri("/api/laboratorio/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("atencionId", 99, "tipoExamen", "GLUCOSA"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("ATENCION_NO_ENCONTRADA");
    }

    @Test
    void tipoDeExamenInexistenteDevuelve400() {
        webTestClient.post().uri("/api/laboratorio/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("atencionId", 5, "tipoExamen", "RAYOS_X"))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
