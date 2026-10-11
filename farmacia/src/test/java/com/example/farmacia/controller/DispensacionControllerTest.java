package com.example.farmacia.controller;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.farmacia.service.DispensacionService;

@WebFluxTest(DispensacionController.class)
class DispensacionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DispensacionService service;

    @Test
    void cantidadCeroONegativaDevuelve400() {
        webTestClient.post().uri("/api/farmacia/dispensaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("medicamentoId", 1, "pacienteId", 1, "cantidad", 0, "referenciaAdministrativa", "ATE-1"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errores.cantidad").isEqualTo("La cantidad debe ser mayor que 0");
    }
}
