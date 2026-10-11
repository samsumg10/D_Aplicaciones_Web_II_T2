package com.example.hospitalizacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IngresoRequest(
        @NotBlank(message = "La cama es obligatoria")
        @Pattern(regexp = "[A-Z]{3}-\\d{3}", message = "El código de cama tiene el formato AAA-000 (ej. MED-101)")
        String camaCodigo) {
}
