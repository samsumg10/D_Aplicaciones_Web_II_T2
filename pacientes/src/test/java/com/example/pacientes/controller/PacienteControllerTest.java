package com.example.pacientes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.pacientes.domain.EstadoPaciente;
import com.example.pacientes.domain.TipoDocumento;
import com.example.pacientes.dto.PacienteRequest;
import com.example.pacientes.dto.PacienteResponse;
import com.example.pacientes.exception.BusinessException;
import com.example.pacientes.service.PacienteService;

import reactor.core.publisher.Mono;

/** Prueba de la capa HTTP: rutas, validaciones y códigos de respuesta. El servicio se reemplaza por un mock. */
@WebFluxTest(PacienteController.class)
class PacienteControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PacienteService service;

    @Test
    void registrarPacienteValidoDevuelve201() {
        when(service.registrar(any())).thenReturn(Mono.just(respuesta()));

        webTestClient.post().uri("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("70000001"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.estado").isEqualTo("ACTIVO");
    }

    @Test
    void registrarPacienteDuplicadoDevuelve409() {
        when(service.registrar(any())).thenReturn(Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-01",
                "Ya existe un paciente registrado con el documento DNI 70000001")));

        webTestClient.post().uri("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("70000001"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("RN-01");
    }

    @Test
    void registrarConDatosInvalidosDevuelve400ConLosCampos() {
        Map<String, Object> invalido = Map.of("tipoDocumento", "DNI", "numeroDocumento", "1",
                "nombres", "", "correo", "no-es-correo");

        webTestClient.post().uri("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalido)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.errores.numeroDocumento").exists()
                .jsonPath("$.errores.correo").exists();
    }

    @Test
    void consultarPacienteInexistenteDevuelve404() {
        when(service.buscarPorId(99L)).thenReturn(Mono.error(new BusinessException(HttpStatus.NOT_FOUND,
                "PACIENTE_NO_ENCONTRADO", "No existe un paciente con id 99")));

        webTestClient.get().uri("/api/pacientes/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("PACIENTE_NO_ENCONTRADO");
    }

    @Test
    void consultarPacientePorIdDevuelve200() {
        when(service.buscarPorId(1L)).thenReturn(Mono.just(respuesta()));

        webTestClient.get().uri("/api/pacientes/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numeroDocumento").isEqualTo("70000001");
    }

    private PacienteRequest request(String numero) {
        return new PacienteRequest(TipoDocumento.DNI, numero, "Ana", "Prueba Ficticia", LocalDate.of(1990, 5, 20),
                "900000001", "ana.prueba@correo.test", null);
    }

    private PacienteResponse respuesta() {
        return new PacienteResponse(1L, TipoDocumento.DNI, "70000001", "Ana", "Prueba Ficticia",
                LocalDate.of(1990, 5, 20), "900000001", "ana.prueba@correo.test", null, EstadoPaciente.ACTIVO,
                LocalDateTime.now(), null);
    }
}
