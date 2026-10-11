package com.example.facturacion_pagos.dto;

import java.math.BigDecimal;

import com.example.facturacion_pagos.domain.MedioPago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Pago total de un cargo. El paciente y el concepto se toman del cargo. */
public record PagoRequest(

        @NotNull(message = "El cargo es obligatorio")
        @Positive
        Long cargoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que 0")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 8 enteros y 2 decimales")
        BigDecimal monto,

        @NotNull(message = "El medio de pago es obligatorio")
        MedioPago medioPago,

        @NotBlank(message = "El número de operación es obligatorio")
        @Pattern(regexp = "[A-Z0-9-]{6,20}", message = "El número de operación tiene de 6 a 20 caracteres (A-Z, 0-9, -)")
        String numeroOperacion) {
}
