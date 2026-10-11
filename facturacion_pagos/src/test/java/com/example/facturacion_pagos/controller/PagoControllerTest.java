package com.example.facturacion_pagos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.facturacion_pagos.service.PagoService;

@WebFluxTest(PagoController.class)
class PagoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PagoService service;

    @ParameterizedTest(name = "monto {0} -> 400")
    @ValueSource(strings = { "0", "0.00", "-10.50" })
    void montoCeroONegativoDevuelve400(String monto) {
        webTestClient.post().uri("/api/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("cargoId", 7, "monto", Double.valueOf(monto), "medioPago", "EFECTIVO",
                        "numeroOperacion", "OP-000001"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.errores.monto").isEqualTo("El monto debe ser mayor que 0");

        verify(service, never()).registrar(any());
    }
}
