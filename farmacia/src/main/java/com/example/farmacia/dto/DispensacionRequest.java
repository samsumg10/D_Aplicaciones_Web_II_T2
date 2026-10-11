package com.example.farmacia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DispensacionRequest(

        @NotNull(message = "El medicamento es obligatorio")
        @Positive
        Long medicamentoId,

        @NotNull(message = "El paciente es obligatorio")
        @Positive
        Long pacienteId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que 0")
        @Max(1000)
        Integer cantidad,

        @NotBlank(message = "La referencia administrativa es obligatoria")
        @Size(max = 30)
        String referenciaAdministrativa) {
}
