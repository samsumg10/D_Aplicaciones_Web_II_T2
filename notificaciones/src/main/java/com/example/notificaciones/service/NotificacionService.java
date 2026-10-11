package com.example.notificaciones.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.notificaciones.domain.Notificacion;
import com.example.notificaciones.dto.NotificacionResponse;
import com.example.notificaciones.exception.BusinessException;
import com.example.notificaciones.messaging.EventoDominio;
import com.example.notificaciones.repository.NotificacionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repository;

    /**
     * Canal en vivo para GET /api/notificaciones/stream (Server-Sent Events). Es un Flux "caliente":
     * cada cliente conectado recibe los avisos nuevos en cuanto se generan, sin consultar la base.
     */
    private final Sinks.Many<NotificacionResponse> enVivo = Sinks.many().multicast().directBestEffort();

    /**
     * Convierte un evento en un aviso simulado. Es idempotente: si RabbitMQ entrega dos veces el mismo
     * evento, el UNIQUE de evento_id lo detecta y el segundo se ignora.
     */
    public Mono<Notificacion> procesar(EventoDominio evento) {
        Notificacion notificacion = Notificacion.builder()
                .eventoId(evento.eventoId())
                .tipoEvento(evento.tipo())
                .pacienteId(evento.pacienteId())
                .recurso(evento.recurso())
                .recursoId(evento.recursoId())
                .canal(Notificacion.CANAL_SIMULADO)
                .asunto(asunto(evento.tipo()))
                .mensaje(evento.detalle())
                .estado(Notificacion.ESTADO_ENVIADA)
                .fechaEvento(evento.fecha())
                .fechaRegistro(LocalDateTime.now())
                .build();

        return repository.save(notificacion)
                .doOnNext(guardada -> {
                    // Solo se registra el id del paciente: no se escriben datos personales en el log
                    log.info("[NOTIFICACIÓN SIMULADA] paciente {} | {} | {}", guardada.getPacienteId(),
                            guardada.getAsunto(), guardada.getMensaje());
                    enVivo.emitNext(NotificacionResponse.from(guardada),
                            Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(100)));
                })
                .onErrorResume(DataIntegrityViolationException.class, e -> {
                    log.warn("Evento {} ya procesado; se ignora la entrega duplicada", evento.eventoId());
                    return Mono.empty();
                });
    }

    public Flux<NotificacionResponse> listar(Long pacienteId) {
        Flux<Notificacion> notificaciones = pacienteId == null ? repository.findAllByOrderByFechaRegistroDesc()
                : repository.findByPacienteIdOrderByFechaRegistroDesc(pacienteId);
        return notificaciones.map(NotificacionResponse::from);
    }

    public Mono<NotificacionResponse> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "NOTIFICACION_NO_ENCONTRADA",
                        "No existe una notificación con id " + id)))
                .map(NotificacionResponse::from);
    }

    public Flux<NotificacionResponse> enVivo() {
        return enVivo.asFlux();
    }

    static String asunto(String tipoEvento) {
        return switch (tipoEvento) {
            case "CitaRegistrada" -> "Registro de cita";
            case "CitaConfirmada" -> "Confirmación de cita";
            case "CitaReprogramada" -> "Reprogramación de cita";
            case "CitaCancelada" -> "Cancelación de cita";
            case "SolicitudLaboratorioActualizada" -> "Cambio de estado en solicitud de laboratorio";
            case "HospitalizacionDadaDeAlta" -> "Alta administrativa registrada";
            case "PagoRegistrado" -> "Pago registrado";
            default -> "Aviso del hospital";
        };
    }
}
