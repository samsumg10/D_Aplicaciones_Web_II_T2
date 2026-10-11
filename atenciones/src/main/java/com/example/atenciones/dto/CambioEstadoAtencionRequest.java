package com.example.atenciones.dto;

import com.example.atenciones.domain.EstadoAtencion;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoAtencionRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoAtencion estado) {
}
