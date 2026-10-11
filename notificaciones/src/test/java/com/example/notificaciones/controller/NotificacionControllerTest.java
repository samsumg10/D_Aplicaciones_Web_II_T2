package com.example.notificaciones.controller;

import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.notificaciones.dto.NotificacionResponse;
import com.example.notificaciones.exception.BusinessException;
import com.example.notificaciones.service.NotificacionService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(NotificacionController.class)
class NotificacionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private NotificacionService service;

    @Test
    void listaLasNotificacionesDeUnPaciente() {
        when(service.listar(1L)).thenReturn(Flux.just(new NotificacionResponse(1L, "e-1", "CitaConfirmada", 1L,
                "CITA", "10", "SIMULADO", "Confirmación de cita", "Cita CIT-0000010 confirmada", "ENVIADA",
                LocalDateTime.now(), LocalDateTime.now())));

        webTestClient.get().uri("/api/notificaciones?pacienteId=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].asunto").isEqualTo("Confirmación de cita");
    }

    @Test
    void notificacionInexistenteDevuelve404() {
        when(service.buscarPorId(99L)).thenReturn(Mono.error(new BusinessException(HttpStatus.NOT_FOUND,
                "NOTIFICACION_NO_ENCONTRADA", "No existe una notificación con id 99")));

        webTestClient.get().uri("/api/notificaciones/99")
                .exchange()
                .expectStatus().isNotFound();
    }
}
