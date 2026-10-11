package com.example.notificaciones.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.example.notificaciones.domain.Notificacion;
import com.example.notificaciones.repository.NotificacionRepository;

/**
 * Publicación y consumo reales: levanta un RabbitMQ en Docker, publica CitaConfirmada en el exchange
 * hospital.eventos con la routing key cita.confirmada (igual que el servicio de citas) y espera a que
 * este servicio lo consuma y guarde el aviso. Si no hay Docker disponible, la prueba se omite.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@ActiveProfiles("test")
class FlujoEventoRabbitMQTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-management");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificacionRepository repository;

    @Test
    void citaConfirmadaPublicadaEnRabbitGeneraNotificacion() {
        String eventoId = UUID.randomUUID().toString();
        EventoDominio evento = new EventoDominio(eventoId, "CitaConfirmada", "citas", "CITA", "10", 1L, "PENDIENTE",
                "CONFIRMADA", "Cita CIT-0000010 de CARDIOLOGIA el 2030-03-04T10:00 (CAR-001): CONFIRMADA",
                LocalDateTime.now());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS, "cita.confirmada", evento);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Notificacion guardada = repository.findByEventoId(eventoId).block();
            assertThat(guardada).isNotNull();
            assertThat(guardada.getAsunto()).isEqualTo("Confirmación de cita");
            assertThat(guardada.getPacienteId()).isEqualTo(1L);
        });
    }
}
