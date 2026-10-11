package com.example.facturacion_pagos.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.facturacion_pagos.domain.Pago;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PagoRepository extends ReactiveCrudRepository<Pago, Long> {

    Mono<Boolean> existsByNumeroOperacion(String numeroOperacion);

    Mono<Pago> findByNumeroOperacion(String numeroOperacion);

    Flux<Pago> findByPacienteIdOrderByFechaPagoDesc(Long pacienteId);
}
