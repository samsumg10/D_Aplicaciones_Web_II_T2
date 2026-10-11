package com.example.farmacia.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.farmacia.domain.Dispensacion;

import reactor.core.publisher.Flux;

public interface DispensacionRepository extends ReactiveCrudRepository<Dispensacion, Long> {

    Flux<Dispensacion> findByPacienteIdOrderByFechaDispensacionDesc(Long pacienteId);
}
