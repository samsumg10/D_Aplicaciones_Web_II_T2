package com.example.farmacia.controller;

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

import com.example.farmacia.dto.DispensacionRequest;
import com.example.farmacia.dto.DispensacionResponse;
import com.example.farmacia.service.DispensacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/farmacia/dispensaciones")
@RequiredArgsConstructor
public class DispensacionController {

    private final DispensacionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DispensacionResponse> registrar(@Valid @RequestBody DispensacionRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<DispensacionResponse> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }

    @GetMapping("/{id}")
    public Mono<DispensacionResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/anulacion")
    public Mono<DispensacionResponse> anular(@PathVariable Long id) {
        return service.anular(id);
    }
}
