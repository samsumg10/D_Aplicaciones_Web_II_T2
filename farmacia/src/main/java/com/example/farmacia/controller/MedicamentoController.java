package com.example.farmacia.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.farmacia.dto.MedicamentoRequest;
import com.example.farmacia.dto.MedicamentoResponse;
import com.example.farmacia.dto.ReposicionRequest;
import com.example.farmacia.service.MedicamentoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/farmacia/medicamentos")
@RequiredArgsConstructor
public class MedicamentoController {

    private final MedicamentoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MedicamentoResponse> registrar(@Valid @RequestBody MedicamentoRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<MedicamentoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Mono<MedicamentoResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/stock")
    public Mono<MedicamentoResponse> reponerStock(@PathVariable Long id, @Valid @RequestBody ReposicionRequest request) {
        return service.reponerStock(id, request.cantidad());
    }
}
