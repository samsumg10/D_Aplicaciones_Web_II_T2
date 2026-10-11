package com.example.atenciones.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Atenciones publica sus cambios en el exchange compartido; auditoría los registra. */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "hospital.eventos";

    public static final String RK_ATENCION_REGISTRADA = "atencion.registrada";
    public static final String RK_ATENCION_FINALIZADA = "atencion.finalizada";
    public static final String RK_ATENCION_ANULADA = "atencion.anulada";

    @Bean
    public TopicExchange hospitalEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
