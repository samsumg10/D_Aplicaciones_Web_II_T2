package com.example.laboratorio.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventoPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * RabbitTemplate hace I/O bloqueante, por eso se ejecuta en boundedElastic y no en un hilo de Netty.
     * Si RabbitMQ no está disponible, el error se registra y la operación de negocio ya guardada no se revierte:
     * el evento alimenta procesos secundarios (notificaciones, auditoría). Ver "Limitaciones" en el documento técnico.
     */
    public Mono<Void> publicar(String routingKey, EventoDominio evento) {
        return Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS, routingKey, evento))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.info("Evento {} publicado ({} -> {} {})", evento.tipo(), routingKey,
                        evento.recurso(), evento.recursoId()))
                .onErrorResume(ex -> {
                    log.error("No se pudo publicar el evento {} ({}): {}", evento.tipo(), routingKey, ex.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}
