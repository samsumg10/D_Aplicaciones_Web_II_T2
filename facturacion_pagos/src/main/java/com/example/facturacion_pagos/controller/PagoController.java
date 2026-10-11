package com.example.facturacion_pagos.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.facturacion_pagos.dto.PagoRequest;
import com.example.facturacion_pagos.dto.PagoResponse;
import com.example.facturacion_pagos.service.PagoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PagoResponse> registrar(@Valid @RequestBody PagoRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<PagoResponse> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }

    @GetMapping("/{id}")
    public Mono<PagoResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/operacion/{numeroOperacion}")
    public Mono<PagoResponse> buscarPorOperacion(@PathVariable String numeroOperacion) {
        return service.buscarPorOperacion(numeroOperacion);
    }
}
