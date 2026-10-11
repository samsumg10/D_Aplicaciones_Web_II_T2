package com.example.pacientes.controller;

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

import com.example.pacientes.domain.EstadoPaciente;
import com.example.pacientes.domain.TipoDocumento;
import com.example.pacientes.dto.CambioEstadoRequest;
import com.example.pacientes.dto.PacienteRequest;
import com.example.pacientes.dto.PacienteResponse;
import com.example.pacientes.service.PacienteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PacienteResponse> registrar(@Valid @RequestBody PacienteRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<PacienteResponse> listar(@RequestParam(required = false) EstadoPaciente estado) {
        return service.listar(estado);
    }

    /** Contrato síncrono que usan citas, hospitalización y facturación para validar al paciente. */
    @GetMapping("/{id}")
    public Mono<PacienteResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/documento/{tipo}/{numero}")
    public Mono<PacienteResponse> buscarPorDocumento(@PathVariable TipoDocumento tipo, @PathVariable String numero) {
        return service.buscarPorDocumento(tipo, numero);
    }

    @PutMapping("/{id}")
    public Mono<PacienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public Mono<PacienteResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
