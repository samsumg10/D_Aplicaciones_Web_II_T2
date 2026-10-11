package com.example.pacientes.dto;

import com.example.pacientes.domain.EstadoPaciente;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPaciente estado) {
}
