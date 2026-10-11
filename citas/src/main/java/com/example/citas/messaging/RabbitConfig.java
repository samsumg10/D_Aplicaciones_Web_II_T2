package com.example.citas.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Citas publica en el exchange compartido hospital.eventos. No declara colas: notificaciones y auditoría
 * declaran las suyas y deciden qué routing keys les interesan. Así citas no conoce a sus consumidores.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "hospital.eventos";

    public static final String RK_CITA_REGISTRADA = "cita.registrada";
    public static final String RK_CITA_CONFIRMADA = "cita.confirmada";
    public static final String RK_CITA_REPROGRAMADA = "cita.reprogramada";
    public static final String RK_CITA_CANCELADA = "cita.cancelada";
    public static final String RK_CITA_ATENDIDA = "cita.atendida";
    public static final String RK_CITA_AUSENTE = "cita.ausente";

    @Bean
    public TopicExchange hospitalEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
