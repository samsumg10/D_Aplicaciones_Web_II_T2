package com.example.pacientes.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pacientes solo publica eventos: declara el exchange compartido hospital.eventos (todos los servicios lo
 * declaran igual, la operación es idempotente) y no crea colas. Cada consumidor declara su propia cola.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "hospital.eventos";

    public static final String RK_PACIENTE_REGISTRADO = "paciente.registrado";
    public static final String RK_PACIENTE_ACTUALIZADO = "paciente.actualizado";
    public static final String RK_PACIENTE_ESTADO_ACTUALIZADO = "paciente.estado-actualizado";

    @Bean
    public TopicExchange hospitalEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    /** Los eventos viajan como JSON. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
