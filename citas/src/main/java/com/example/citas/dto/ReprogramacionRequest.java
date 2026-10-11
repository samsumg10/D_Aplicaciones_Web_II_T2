package com.example.citas.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Nueva fecha de la cita. Si no se envía profesional, se mantiene el actual. */
public record ReprogramacionRequest(

        @NotNull(message = "La nueva fecha y hora son obligatorias")
        @Future(message = "La cita debe reprogramarse a una fecha futura")
        LocalDateTime fechaHora,

        @Pattern(regexp = "[A-Z]{3}-\\d{3}", message = "El código del profesional tiene el formato AAA-000 (ej. CAR-001)")
        String profesionalCodigo) {
}
