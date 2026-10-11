package com.example.notificaciones.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.notificaciones.dto.NotificacionResponse;
import com.example.notificaciones.service.NotificacionService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Las notificaciones se crean solo por eventos de RabbitMQ; la API es de consulta. */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService service;

    @GetMapping
    public Flux<NotificacionResponse> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }

    /** Flujo en vivo (Server-Sent Events): la conexión queda abierta y recibe cada aviso nuevo. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificacionResponse>> enVivo() {
        return service.enVivo()
                .map(n -> ServerSentEvent.builder(n).id(String.valueOf(n.id())).event("notificacion").build());
    }

    @GetMapping("/{id}")
    public Mono<NotificacionResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
