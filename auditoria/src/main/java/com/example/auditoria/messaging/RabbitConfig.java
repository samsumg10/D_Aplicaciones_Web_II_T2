package com.example.auditoria.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auditoría registra todos los eventos de negocio: su cola se enlaza con la routing key "#",
 * que en un TopicExchange coincide con cualquier clave. Un servicio nuevo que publique en
 * hospital.eventos queda auditado sin cambiar nada aquí.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "hospital.eventos";
    public static final String COLA = "auditoria.eventos";
    public static final String COLA_DLQ = COLA + ".dlq";

    @Bean
    public TopicExchange hospitalEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public Queue colaAuditoria() {
        return QueueBuilder.durable(COLA)
                .deadLetterExchange("")
                .deadLetterRoutingKey(COLA_DLQ)
                .build();
    }

    @Bean
    public Queue colaAuditoriaDlq() {
        return QueueBuilder.durable(COLA_DLQ).build();
    }

    @Bean
    public Binding bindingTodosLosEventos(TopicExchange hospitalEventos, Queue colaAuditoria) {
        return BindingBuilder.bind(colaAuditoria).to(hospitalEventos).with("#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
