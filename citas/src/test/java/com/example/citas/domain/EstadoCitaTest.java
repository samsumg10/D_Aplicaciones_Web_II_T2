package com.example.citas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** RN-06: tabla completa de transiciones de una cita. */
class EstadoCitaTest {

    @ParameterizedTest(name = "{0} -> {1} permitido")
    @CsvSource({
            "PENDIENTE, CONFIRMADA",
            "PENDIENTE, CANCELADA",
            "CONFIRMADA, ATENDIDA",
            "CONFIRMADA, CANCELADA",
            "CONFIRMADA, AUSENTE"
    })
    void transicionesPermitidas(EstadoCita origen, EstadoCita destino) {
        assertThat(origen.puedeCambiarA(destino)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} no permitido")
    @CsvSource({
            "CANCELADA, CONFIRMADA",
            "CANCELADA, ATENDIDA",
            "PENDIENTE, ATENDIDA",
            "PENDIENTE, AUSENTE",
            "ATENDIDA, CANCELADA",
            "AUSENTE, CONFIRMADA",
            "CONFIRMADA, PENDIENTE"
    })
    void transicionesNoPermitidas(EstadoCita origen, EstadoCita destino) {
        assertThat(origen.puedeCambiarA(destino)).isFalse();
    }
}
