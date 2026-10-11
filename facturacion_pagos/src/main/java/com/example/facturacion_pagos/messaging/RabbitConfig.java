package com.example.facturacion_pagos.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "hospital.eventos";

    public static final String RK_CARGO_REGISTRADO = "cargo.registrado";
    public static final String RK_CARGO_ANULADO = "cargo.anulado";
    /** Lo consumen notificaciones (aviso de pago) y auditoría. */
    public static final String RK_PAGO_REGISTRADO = "pago.registrado";

    @Bean
    public TopicExchange hospitalEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
