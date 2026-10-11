package com.example.auditoria.messaging;

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

import com.example.auditoria.repository.RegistroAuditoriaRepository;

/**
 * La cola de auditoría usa la routing key "#": recibe eventos de cualquier servicio.
 * Se publican dos eventos de orígenes distintos y se verifica que ambos quedan auditados.
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
    private RegistroAuditoriaRepository repository;

    @Test
    void auditaEventosDeCualquierServicio() {
        String citaConfirmada = publicar("cita.confirmada", "CitaConfirmada", "citas", "CITA", "PENDIENTE", "CONFIRMADA");
        String pagoRegistrado = publicar("pago.registrado", "PagoRegistrado", "facturacion-pagos", "PAGO", null, "REGISTRADO");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(repository.findByEventoId(citaConfirmada).block()).isNotNull();
            assertThat(repository.findByEventoId(pagoRegistrado).block()).isNotNull();
        });
    }

    private String publicar(String routingKey, String tipo, String origen, String recurso, String anterior, String nuevo) {
        String eventoId = UUID.randomUUID().toString();
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS, routingKey, new EventoDominio(eventoId, tipo,
                origen, recurso, "1", 1L, anterior, nuevo, "evento de prueba", LocalDateTime.now()));
        return eventoId;
    }
}
