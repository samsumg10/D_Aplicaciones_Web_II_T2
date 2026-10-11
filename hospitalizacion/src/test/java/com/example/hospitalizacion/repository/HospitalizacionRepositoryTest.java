package com.example.hospitalizacion.repository;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.hospitalizacion.domain.AreaHospitalizacion;
import com.example.hospitalizacion.domain.EstadoHospitalizacion;
import com.example.hospitalizacion.domain.Hospitalizacion;

import reactor.test.StepVerifier;

/** RN-09 en la base de datos: la restricción UNIQUE de cama_ocupada. */
@DataR2dbcTest
@ActiveProfiles("test")
class HospitalizacionRepositoryTest {

    @Autowired
    private HospitalizacionRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll().block();
    }

    @Test
    void laBaseDeDatosImpideDosHospitalizacionesActivasEnLaMismaCama() {
        StepVerifier.create(repository.save(hospitalizacion("HOS-A", 1L, "MED-101"))
                        .then(repository.save(hospitalizacion("HOS-B", 2L, "MED-101"))))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void lasHospitalizacionesSinCamaOcupadaNoChocanEntreSi() {
        StepVerifier.create(repository.save(hospitalizacion("HOS-C", 1L, null))
                        .then(repository.save(hospitalizacion("HOS-D", 2L, null)))
                        .then(repository.existsByCamaOcupada("MED-101")))
                .expectNext(false)
                .verifyComplete();
    }

    private Hospitalizacion hospitalizacion(String codigo, Long pacienteId, String camaOcupada) {
        return Hospitalizacion.builder().codigo(codigo).pacienteId(pacienteId).area(AreaHospitalizacion.CIRUGIA)
                .camaCodigo(camaOcupada).camaOcupada(camaOcupada)
                .estado(camaOcupada != null ? EstadoHospitalizacion.ACTIVA : EstadoHospitalizacion.ALTA)
                .fechaSolicitud(LocalDateTime.now()).build();
    }
}
