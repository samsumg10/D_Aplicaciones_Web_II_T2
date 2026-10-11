package com.example.notificaciones.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de notificaciones. La cola es propia de este servicio: recibe su copia de cada evento aunque
 * auditoría consuma el mismo. Solo se enlazan las routing keys que generan un aviso al paciente.
 * Los mensajes que fallan pasan a la cola .dlq para revisarlos sin bloquear la cola principal.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "hospital.eventos";
    public static final String COLA = "notificaciones.eventos";
    public static final String COLA_DLQ = COLA + ".dlq";

    /** Eventos que generan una notificación (Proceso 8 del caso). */
    public static final String[] ROUTING_KEYS = {
            "cita.registrada",
            "cita.confirmada",
            "cita.reprogramada",
            "cita.cancelada",
            "laboratorio.estado-actualizado",
            "hospitalizacion.alta",
            "pago.registrado"
    };

    @Bean
    public TopicExchange hospitalEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public Queue colaNotificaciones() {
        return QueueBuilder.durable(COLA)
                .deadLetterExchange("")                 // exchange por defecto: enruta por nombre de cola
                .deadLetterRoutingKey(COLA_DLQ)
                .build();
    }

    @Bean
    public Queue colaNotificacionesDlq() {
        return QueueBuilder.durable(COLA_DLQ).build();
    }

    @Bean
    public Declarables bindingsNotificaciones(TopicExchange hospitalEventos, Queue colaNotificaciones) {
        Binding[] bindings = new Binding[ROUTING_KEYS.length];
        for (int i = 0; i < ROUTING_KEYS.length; i++) {
            bindings[i] = BindingBuilder.bind(colaNotificaciones).to(hospitalEventos).with(ROUTING_KEYS[i]);
        }
        return new Declarables(bindings);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
