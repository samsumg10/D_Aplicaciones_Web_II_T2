package com.example.notificaciones.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.notificaciones.domain.Notificacion;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificacionRepository extends ReactiveCrudRepository<Notificacion, Long> {

    Flux<Notificacion> findAllByOrderByFechaRegistroDesc();

    Flux<Notificacion> findByPacienteIdOrderByFechaRegistroDesc(Long pacienteId);

    Mono<Notificacion> findByEventoId(String eventoId);
}
