package com.example.facturacion_pagos.controller;

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

import com.example.facturacion_pagos.domain.EstadoCargo;
import com.example.facturacion_pagos.dto.CargoRequest;
import com.example.facturacion_pagos.dto.CargoResponse;
import com.example.facturacion_pagos.service.CargoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cargos")
@RequiredArgsConstructor
public class CargoController {

    private final CargoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CargoResponse> registrar(@Valid @RequestBody CargoRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<CargoResponse> listar(@RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) EstadoCargo estado) {
        return service.listar(pacienteId, estado);
    }

    @GetMapping("/{id}")
    public Mono<CargoResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/anulacion")
    public Mono<CargoResponse> anular(@PathVariable Long id) {
        return service.anular(id);
    }
}
