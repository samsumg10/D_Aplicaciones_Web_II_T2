package com.example.citas.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.citas.domain.Cita;
import com.example.citas.domain.Especialidad;
import com.example.citas.domain.EstadoCita;

import reactor.test.StepVerifier;

/** Persistencia R2DBC de citas y consulta de horarios ocupados por estado (RN-05). */
@DataR2dbcTest
@ActiveProfiles("test")
class CitaRepositoryTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 3, 4, 10, 0);

    @Autowired
    private CitaRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll().block();
    }

    @Test
    void registraYConsultaUnaCitaPorCodigo() {
        StepVerifier.create(repository.save(cita("CIT-AAAA0001", EstadoCita.PENDIENTE))
                        .then(repository.findByCodigo("CIT-AAAA0001")))
                .assertNext(c -> {
                    assertThat(c.getId()).isNotNull();
                    assertThat(c.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
                    assertThat(c.getEspecialidad()).isEqualTo(Especialidad.CARDIOLOGIA);
                })
                .verifyComplete();
    }

    @Test
    void soloLasCitasActivasOcupanElHorario() {
        StepVerifier.create(repository.save(cita("CIT-AAAA0002", EstadoCita.CANCELADA))
                        .thenMany(repository.findByProfesionalCodigoAndFechaHoraAndEstadoIn("CAR-001", FECHA,
                                EstadoCita.ACTIVOS)))
                .verifyComplete();

        StepVerifier.create(repository.save(cita("CIT-AAAA0003", EstadoCita.CONFIRMADA))
                        .thenMany(repository.findByProfesionalCodigoAndFechaHoraAndEstadoIn("CAR-001", FECHA,
                                EstadoCita.ACTIVOS)))
                .assertNext(c -> assertThat(c.getCodigo()).isEqualTo("CIT-AAAA0003"))
                .verifyComplete();
    }

    private Cita cita(String codigo, EstadoCita estado) {
        return Cita.builder().codigo(codigo).pacienteId(1L).especialidad(Especialidad.CARDIOLOGIA).fechaHora(FECHA)
                .profesionalCodigo("CAR-001").estado(estado).fechaCreacion(LocalDateTime.now()).build();
    }
}
