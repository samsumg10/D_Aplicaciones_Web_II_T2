package com.example.auditoria.messaging;

import java.time.Duration;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.auditoria.service.AuditoriaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventoListener {

    private final AuditoriaService service;

    /** Consumidor reactivo (ack al terminar el Mono). Si falla tras 2 reintentos, el mensaje va a la .dlq. */
    @RabbitListener(queues = RabbitConfig.COLA)
    public Mono<Void> recibir(EventoDominio evento) {
        log.info("Evento {} recibido desde {} ({} {})", evento.tipo(), evento.origen(), evento.recurso(),
                evento.recursoId());
        return service.registrar(evento)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .then();
    }
}
