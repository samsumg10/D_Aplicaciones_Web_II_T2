package com.example.facturacion_pagos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CargoRequest(

        @NotNull(message = "El paciente es obligatorio")
        @Positive
        Long pacienteId,

        /** Opcional: atención que origina el cargo. */
        @Positive
        Long atencionId,

        @NotBlank(message = "El concepto es obligatorio")
        @Size(max = 150)
        String concepto,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que 0")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 8 enteros y 2 decimales")
        BigDecimal monto) {
}
