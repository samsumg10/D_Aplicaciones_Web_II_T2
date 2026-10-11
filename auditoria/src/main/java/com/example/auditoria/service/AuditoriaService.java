package com.example.auditoria.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.auditoria.domain.RegistroAuditoria;
import com.example.auditoria.dto.RegistroAuditoriaResponse;
import com.example.auditoria.exception.BusinessException;
import com.example.auditoria.messaging.EventoDominio;
import com.example.auditoria.repository.RegistroAuditoriaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final RegistroAuditoriaRepository repository;

    /** Guarda la traza del evento. Idempotente: una entrega duplicada (mismo eventoId) se ignora. */
    public Mono<RegistroAuditoria> registrar(EventoDominio evento) {
        RegistroAuditoria registro = RegistroAuditoria.builder()
                .eventoId(evento.eventoId())
                .tipoEvento(evento.tipo())
                .origen(evento.origen())
                .recurso(evento.recurso())
                .recursoId(evento.recursoId())
                .pacienteId(evento.pacienteId())
                .estadoAnterior(evento.estadoAnterior())
                .estadoNuevo(evento.estadoNuevo())
                .detalle(evento.detalle())
                .fechaEvento(evento.fecha())
                .fechaRegistro(LocalDateTime.now())
                .build();

        return repository.save(registro)
                .doOnNext(r -> log.info("Auditoría: {} {} {} -> {} (origen {})", r.getRecurso(), r.getRecursoId(),
                        r.getEstadoAnterior(), r.getEstadoNuevo(), r.getOrigen()))
                .onErrorResume(DataIntegrityViolationException.class, e -> {
                    log.warn("Evento {} ya auditado; se ignora la entrega duplicada", evento.eventoId());
                    return Mono.empty();
                });
    }

    public Flux<RegistroAuditoriaResponse> listar(Long pacienteId) {
        Flux<RegistroAuditoria> registros = pacienteId == null ? repository.findAllByOrderByFechaEventoDesc()
                : repository.findByPacienteIdOrderByFechaEventoDesc(pacienteId);
        return registros.map(RegistroAuditoriaResponse::from);
    }

    /** Trazabilidad de un recurso: todos sus cambios de estado en orden cronológico. */
    public Flux<RegistroAuditoriaResponse> historial(String recurso, String recursoId) {
        return repository.findByRecursoAndRecursoIdOrderByFechaEventoAsc(recurso.toUpperCase(), recursoId)
                .map(RegistroAuditoriaResponse::from);
    }

    public Mono<RegistroAuditoriaResponse> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "REGISTRO_NO_ENCONTRADO",
                        "No existe un registro de auditoría con id " + id)))
                .map(RegistroAuditoriaResponse::from);
    }
}
