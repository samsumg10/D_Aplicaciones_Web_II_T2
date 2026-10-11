package com.example.notificaciones.messaging;

import java.time.Duration;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.notificaciones.service.NotificacionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventoListener {

    private final NotificacionService service;

    /**
     * Consumidor reactivo: al devolver Mono, Spring AMQP confirma (ack) el mensaje cuando el Mono termina
     * y no bloquea el hilo del contenedor. Si falla tras 2 reintentos, el mensaje se rechaza y va a la .dlq.
     */
    @RabbitListener(queues = RabbitConfig.COLA)
    public Mono<Void> recibir(EventoDominio evento) {
        log.info("Evento {} recibido ({} {})", evento.tipo(), evento.recurso(), evento.recursoId());
        return service.procesar(evento)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .then();
    }
}
