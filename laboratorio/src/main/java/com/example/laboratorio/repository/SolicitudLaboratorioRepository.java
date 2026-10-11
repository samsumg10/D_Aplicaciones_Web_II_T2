package com.example.laboratorio.repository;

import java.util.Collection;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.laboratorio.domain.EstadoSolicitud;
import com.example.laboratorio.domain.SolicitudLaboratorio;
import com.example.laboratorio.domain.TipoExamen;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SolicitudLaboratorioRepository extends ReactiveCrudRepository<SolicitudLaboratorio, Long> {

    Flux<SolicitudLaboratorio> findByPacienteIdOrderByFechaSolicitudDesc(Long pacienteId);

    Flux<SolicitudLaboratorio> findByAtencionId(Long atencionId);

    /** Evita pedir dos veces el mismo examen activo en una atención. */
    Mono<Boolean> existsByAtencionIdAndTipoExamenAndEstadoIn(Long atencionId, TipoExamen tipoExamen,
            Collection<EstadoSolicitud> estados);
}
