package com.example.farmacia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReposicionRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad a reponer debe ser mayor que 0")
        @Max(10000)
        Integer cantidad) {
}
