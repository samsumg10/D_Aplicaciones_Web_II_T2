package com.example.atenciones.controller;

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

import com.example.atenciones.dto.AtencionRequest;
import com.example.atenciones.dto.AtencionResponse;
import com.example.atenciones.dto.CambioEstadoAtencionRequest;
import com.example.atenciones.service.AtencionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/atenciones")
@RequiredArgsConstructor
public class AtencionController {

    private final AtencionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AtencionResponse> registrar(@Valid @RequestBody AtencionRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<AtencionResponse> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }

    @GetMapping("/{id}")
    public Mono<AtencionResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/estado")
    public Mono<AtencionResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoAtencionRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
