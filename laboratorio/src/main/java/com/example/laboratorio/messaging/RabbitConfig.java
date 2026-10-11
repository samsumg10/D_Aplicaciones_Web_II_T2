package com.example.laboratorio.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Laboratorio publica el registro y cada cambio de estado de sus solicitudes. */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "hospital.eventos";

    public static final String RK_SOLICITUD_REGISTRADA = "laboratorio.solicitud-registrada";
    /** Lo consumen notificaciones (aviso al paciente) y auditoría. */
    public static final String RK_ESTADO_ACTUALIZADO = "laboratorio.estado-actualizado";

    @Bean
    public TopicExchange hospitalEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
