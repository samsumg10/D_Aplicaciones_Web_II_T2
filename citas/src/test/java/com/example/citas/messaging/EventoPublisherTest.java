package com.example.citas.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import reactor.test.StepVerifier;

/** Componente de mensajería del productor: a qué exchange y con qué routing key sale el evento. */
@ExtendWith(MockitoExtension.class)
class EventoPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventoPublisher publisher;

    @Test
    void publicaCitaConfirmadaEnElExchangeDelHospital() {
        EventoDominio evento = EventoDominio.de("CitaConfirmada", "CITA", 10L, 1L, "PENDIENTE", "CONFIRMADA", "detalle");

        StepVerifier.create(publisher.publicar(RabbitConfig.RK_CITA_CONFIRMADA, evento)).verifyComplete();

        verify(rabbitTemplate).convertAndSend("hospital.eventos", "cita.confirmada", evento);
    }

    @Test
    void siRabbitNoEstaDisponibleNoPropagaElError() {
        EventoDominio evento = EventoDominio.de("CitaConfirmada", "CITA", 10L, 1L, "PENDIENTE", "CONFIRMADA", "detalle");
        doThrow(new AmqpConnectException(new java.net.ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        StepVerifier.create(publisher.publicar(RabbitConfig.RK_CITA_CONFIRMADA, evento)).verifyComplete();

        verify(rabbitTemplate).convertAndSend(eq("hospital.eventos"), eq("cita.confirmada"), any(Object.class));
    }
}
