package com.example.citas.dto;

import java.time.LocalDateTime;

import com.example.citas.domain.Especialidad;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Datos para programar una cita. Las reglas de agenda (horario, bloques) las valida el servicio (RN-04). */
public record CitaRequest(

        @NotNull(message = "El paciente es obligatorio")
        @Positive
        Long pacienteId,

        @NotNull(message = "La especialidad es obligatoria")
        Especialidad especialidad,

        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La cita debe programarse en una fecha futura")
        LocalDateTime fechaHora,

        @NotBlank(message = "El código del profesional es obligatorio")
        @Pattern(regexp = "[A-Z]{3}-\\d{3}", message = "El código del profesional tiene el formato AAA-000 (ej. CAR-001)")
        String profesionalCodigo,

        /** Motivo administrativo breve; no se registran datos clínicos. */
        @Size(max = 200)
        String motivo) {
}
