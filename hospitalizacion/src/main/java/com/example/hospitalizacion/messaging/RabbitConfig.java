package com.example.hospitalizacion.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "hospital.eventos";

    public static final String RK_SOLICITADA = "hospitalizacion.solicitada";
    public static final String RK_INGRESO = "hospitalizacion.ingreso";
    /** Lo consumen notificaciones (aviso de alta) y auditoría. */
    public static final String RK_ALTA = "hospitalizacion.alta";
    public static final String RK_CANCELADA = "hospitalizacion.cancelada";

    @Bean
    public TopicExchange hospitalEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
