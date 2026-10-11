package com.example.auditoria.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auditoria.dto.RegistroAuditoriaResponse;
import com.example.auditoria.service.AuditoriaService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** API de solo lectura: los registros se crean exclusivamente a partir de eventos. */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService service;

    @GetMapping
    public Flux<RegistroAuditoriaResponse> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }

    /** Ejemplo: GET /api/auditoria/recursos/CITA/10 devuelve todos los cambios de estado de la cita 10. */
    @GetMapping("/recursos/{recurso}/{recursoId}")
    public Flux<RegistroAuditoriaResponse> historial(@PathVariable String recurso, @PathVariable String recursoId) {
        return service.historial(recurso, recursoId);
    }

    @GetMapping("/{id}")
    public Mono<RegistroAuditoriaResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
