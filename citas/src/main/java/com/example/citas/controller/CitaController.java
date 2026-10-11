package com.example.citas.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.citas.domain.EstadoCita;
import com.example.citas.dto.CambioEstadoCitaRequest;
import com.example.citas.dto.CitaRequest;
import com.example.citas.dto.CitaResponse;
import com.example.citas.dto.ReprogramacionRequest;
import com.example.citas.service.CitaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CitaResponse> registrar(@Valid @RequestBody CitaRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<CitaResponse> listar(@RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) EstadoCita estado) {
        return service.listar(pacienteId, estado);
    }

    /** Contrato síncrono que usa atenciones para validar la cita. */
    @GetMapping("/{id}")
    public Mono<CitaResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/codigo/{codigo}")
    public Mono<CitaResponse> buscarPorCodigo(@PathVariable String codigo) {
        return service.buscarPorCodigo(codigo);
    }

    @PatchMapping("/{id}/estado")
    public Mono<CitaResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoCitaRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @PutMapping("/{id}/reprogramacion")
    public Mono<CitaResponse> reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramacionRequest request) {
        return service.reprogramar(id, request);
    }
}
