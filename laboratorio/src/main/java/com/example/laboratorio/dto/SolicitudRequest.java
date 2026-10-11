package com.example.laboratorio.dto;

import com.example.laboratorio.domain.TipoExamen;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** El paciente se toma de la atención, que debe existir (RN-08). */
public record SolicitudRequest(

        @NotNull(message = "La atención es obligatoria")
        @Positive
        Long atencionId,

        @NotNull(message = "El tipo de examen es obligatorio")
        TipoExamen tipoExamen) {
}
