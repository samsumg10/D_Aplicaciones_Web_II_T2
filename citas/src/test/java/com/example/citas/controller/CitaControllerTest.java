package com.example.citas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.citas.domain.Especialidad;
import com.example.citas.domain.EstadoCita;
import com.example.citas.dto.CitaRequest;
import com.example.citas.dto.CitaResponse;
import com.example.citas.exception.BusinessException;
import com.example.citas.service.CitaService;

import reactor.core.publisher.Mono;

@WebFluxTest(CitaController.class)
class CitaControllerTest {

    private static final LocalDateTime LUNES_10 = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10, 0);

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CitaService service;

    @Test
    void registrarCitaDevuelve201() {
        when(service.registrar(any())).thenReturn(Mono.just(respuesta(EstadoCita.PENDIENTE)));

        webTestClient.post().uri("/api/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CitaRequest(1L, Especialidad.CARDIOLOGIA, LUNES_10, "CAR-001", "Control"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("CIT-0000001")
                .jsonPath("$.estado").isEqualTo("PENDIENTE");
    }

    @Test
    void consultarCitaDevuelve200() {
        when(service.buscarPorId(10L)).thenReturn(Mono.just(respuesta(EstadoCita.CONFIRMADA)));

        webTestClient.get().uri("/api/citas/10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pacienteId").isEqualTo(1)
                .jsonPath("$.estado").isEqualTo("CONFIRMADA");
    }

    @Test
    void registrarSinPacienteNiFechaDevuelve400() {
        webTestClient.post().uri("/api/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("especialidad", "CARDIOLOGIA", "profesionalCodigo", "car1"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errores.pacienteId").exists()
                .jsonPath("$.errores.fechaHora").exists()
                .jsonPath("$.errores.profesionalCodigo").exists();
    }

    @Test
    void transicionNoPermitidaDevuelve409() {
        when(service.cambiarEstado(10L, EstadoCita.CONFIRMADA)).thenReturn(Mono.error(new BusinessException(
                HttpStatus.CONFLICT, "RN-06", "No se puede cambiar una cita CANCELADA a CONFIRMADA")));

        webTestClient.patch().uri("/api/citas/10/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("estado", "CONFIRMADA"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("RN-06");
    }

    @Test
    void estadoInexistenteDevuelve400() {
        webTestClient.patch().uri("/api/citas/10/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("estado", "INVENTADO"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void citaInexistenteDevuelve404() {
        when(service.buscarPorId(99L)).thenReturn(Mono.error(new BusinessException(HttpStatus.NOT_FOUND,
                "CITA_NO_ENCONTRADA", "No existe una cita con id 99")));

        webTestClient.get().uri("/api/citas/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("CITA_NO_ENCONTRADA");
    }

    private CitaResponse respuesta(EstadoCita estado) {
        return new CitaResponse(10L, "CIT-0000001", 1L, Especialidad.CARDIOLOGIA, LUNES_10, "CAR-001", "Control",
                estado, LocalDateTime.now(), null);
    }
}
