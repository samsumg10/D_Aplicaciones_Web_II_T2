package com.example.auditoria.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.auditoria.domain.RegistroAuditoria;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RegistroAuditoriaRepository extends ReactiveCrudRepository<RegistroAuditoria, Long> {

    Flux<RegistroAuditoria> findAllByOrderByFechaEventoDesc();

    /** Historial de estados de un recurso, del más antiguo al más reciente. */
    Flux<RegistroAuditoria> findByRecursoAndRecursoIdOrderByFechaEventoAsc(String recurso, String recursoId);

    Flux<RegistroAuditoria> findByPacienteIdOrderByFechaEventoDesc(Long pacienteId);

    Mono<RegistroAuditoria> findByEventoId(String eventoId);
}
