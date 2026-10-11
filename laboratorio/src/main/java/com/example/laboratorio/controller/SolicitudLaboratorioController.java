package com.example.laboratorio.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.laboratorio.domain.EstadoSolicitud;
import com.example.laboratorio.dto.CambioEstadoSolicitudRequest;
import com.example.laboratorio.dto.SolicitudRequest;
import com.example.laboratorio.dto.SolicitudResponse;
import com.example.laboratorio.service.SolicitudLaboratorioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/laboratorio/solicitudes")
@RequiredArgsConstructor
public class SolicitudLaboratorioController {

    private final SolicitudLaboratorioService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SolicitudResponse> registrar(@Valid @RequestBody SolicitudRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<SolicitudResponse> listar(@RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long atencionId,
            @RequestParam(required = false) EstadoSolicitud estado) {
        return service.listar(pacienteId, atencionId, estado);
    }

    @GetMapping("/{id}")
    public Mono<SolicitudResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/estado")
    public Mono<SolicitudResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoSolicitudRequest request) {
        return service.cambiarEstado(id, request);
    }
}
