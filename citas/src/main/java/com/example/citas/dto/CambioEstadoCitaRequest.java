package com.example.citas.dto;

import com.example.citas.domain.EstadoCita;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoCitaRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoCita estado) {
}
