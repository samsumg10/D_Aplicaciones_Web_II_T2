package com.example.facturacion_pagos.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.facturacion_pagos.domain.Cargo;

import reactor.core.publisher.Flux;

public interface CargoRepository extends ReactiveCrudRepository<Cargo, Long> {

    Flux<Cargo> findByPacienteIdOrderByFechaRegistroDesc(Long pacienteId);
}
