package com.example.hospitalizacion.repository;

import java.util.Collection;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.hospitalizacion.domain.EstadoHospitalizacion;
import com.example.hospitalizacion.domain.Hospitalizacion;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HospitalizacionRepository extends ReactiveCrudRepository<Hospitalizacion, Long> {

    Flux<Hospitalizacion> findByPacienteIdOrderByFechaSolicitudDesc(Long pacienteId);

    Flux<Hospitalizacion> findByEstado(EstadoHospitalizacion estado);

    /** RN-09: ¿la cama ya está ocupada por una hospitalización activa? */
    Mono<Boolean> existsByCamaOcupada(String camaCodigo);

    /** RN-09: un paciente no tiene dos hospitalizaciones abiertas a la vez. */
    Mono<Boolean> existsByPacienteIdAndEstadoIn(Long pacienteId, Collection<EstadoHospitalizacion> estados);
}
