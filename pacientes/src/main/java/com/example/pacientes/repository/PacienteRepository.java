package com.example.pacientes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.pacientes.domain.EstadoPaciente;
import com.example.pacientes.domain.Paciente;
import com.example.pacientes.domain.TipoDocumento;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Repositorio reactivo: cada método devuelve Mono o Flux y nunca bloquea el hilo que lo invoca. */
public interface PacienteRepository extends ReactiveCrudRepository<Paciente, Long> {

    Mono<Paciente> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    Mono<Boolean> existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    Flux<Paciente> findByEstado(EstadoPaciente estado);
}
