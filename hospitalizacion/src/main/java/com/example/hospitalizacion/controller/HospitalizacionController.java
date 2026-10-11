package com.example.hospitalizacion.controller;

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

import com.example.hospitalizacion.domain.EstadoHospitalizacion;
import com.example.hospitalizacion.dto.HospitalizacionRequest;
import com.example.hospitalizacion.dto.HospitalizacionResponse;
import com.example.hospitalizacion.dto.IngresoRequest;
import com.example.hospitalizacion.service.HospitalizacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/hospitalizaciones")
@RequiredArgsConstructor
public class HospitalizacionController {

    private final HospitalizacionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<HospitalizacionResponse> solicitar(@Valid @RequestBody HospitalizacionRequest request) {
        return service.solicitar(request);
    }

    @GetMapping
    public Flux<HospitalizacionResponse> listar(@RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) EstadoHospitalizacion estado) {
        return service.listar(pacienteId, estado);
    }

    @GetMapping("/{id}")
    public Mono<HospitalizacionResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/ingreso")
    public Mono<HospitalizacionResponse> registrarIngreso(@PathVariable Long id,
            @Valid @RequestBody IngresoRequest request) {
        return service.registrarIngreso(id, request.camaCodigo());
    }

    @PatchMapping("/{id}/alta")
    public Mono<HospitalizacionResponse> darDeAlta(@PathVariable Long id) {
        return service.darDeAlta(id);
    }

    @PatchMapping("/{id}/cancelacion")
    public Mono<HospitalizacionResponse> cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}
