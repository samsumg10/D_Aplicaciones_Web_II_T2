package com.example.atenciones.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.atenciones.domain.Atencion;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AtencionRepository extends ReactiveCrudRepository<Atencion, Long> {

    Mono<Boolean> existsByCitaId(Long citaId);

    Flux<Atencion> findByPacienteIdOrderByFechaAtencionDesc(Long pacienteId);
}
