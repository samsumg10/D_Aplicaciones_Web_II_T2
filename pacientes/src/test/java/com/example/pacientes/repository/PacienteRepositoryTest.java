package com.example.pacientes.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.pacientes.domain.EstadoPaciente;
import com.example.pacientes.domain.Paciente;
import com.example.pacientes.domain.TipoDocumento;

import reactor.test.StepVerifier;

/** Persistencia real con R2DBC: usa schema.sql sobre H2 en memoria (modo PostgreSQL). */
@DataR2dbcTest
@ActiveProfiles("test")
class PacienteRepositoryTest {

    @Autowired
    private PacienteRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll().block();
    }

    @Test
    void guardaYConsultaPorDocumento() {
        StepVerifier.create(repository.save(paciente("70000001"))
                        .then(repository.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "70000001")))
                .assertNext(p -> {
                    assertThat(p.getId()).isNotNull();
                    assertThat(p.getEstado()).isEqualTo(EstadoPaciente.ACTIVO);
                })
                .verifyComplete();

        StepVerifier.create(repository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "70000001"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void laRestriccionUniqueImpideDocumentosDuplicados() {
        StepVerifier.create(repository.save(paciente("70000002")).then(repository.save(paciente("70000002"))))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    private Paciente paciente(String numero) {
        return Paciente.builder()
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(numero)
                .nombres("Luis")
                .apellidos("Demo Ficticio")
                .fechaNacimiento(LocalDate.of(1985, 1, 15))
                .telefono("900000002")
                .correo("luis.demo@correo.test")
                .estado(EstadoPaciente.ACTIVO)
                .fechaRegistro(LocalDateTime.now())
                .build();
    }
}
