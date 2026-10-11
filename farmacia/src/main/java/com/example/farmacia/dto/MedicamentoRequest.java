package com.example.farmacia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MedicamentoRequest(

        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "MED-\\d{4}", message = "El código tiene el formato MED-0000")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @NotBlank(message = "La presentación es obligatoria")
        @Size(max = 100)
        String presentacion,

        @NotNull(message = "El stock inicial es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        @Max(100000)
        Integer stock) {
}
