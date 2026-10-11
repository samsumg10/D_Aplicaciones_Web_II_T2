package com.example.citas.repository;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.citas.domain.Cita;
import com.example.citas.domain.EstadoCita;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CitaRepository extends ReactiveCrudRepository<Cita, Long> {

    Mono<Cita> findByCodigo(String codigo);

    Flux<Cita> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

    /** Citas que ocupan el horario de un profesional (RN-05). */
    Flux<Cita> findByProfesionalCodigoAndFechaHoraAndEstadoIn(String profesionalCodigo, LocalDateTime fechaHora,
            Collection<EstadoCita> estados);

    /** Citas que ocupan el horario de un paciente (RN-05). */
    Flux<Cita> findByPacienteIdAndFechaHoraAndEstadoIn(Long pacienteId, LocalDateTime fechaHora,
            Collection<EstadoCita> estados);
}
