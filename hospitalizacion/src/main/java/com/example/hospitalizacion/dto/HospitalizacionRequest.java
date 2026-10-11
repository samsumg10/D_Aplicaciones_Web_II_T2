package com.example.hospitalizacion.dto;

import com.example.hospitalizacion.domain.AreaHospitalizacion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record HospitalizacionRequest(

        @NotNull(message = "El paciente es obligatorio")
        @Positive
        Long pacienteId,

        @NotNull(message = "El área es obligatoria")
        AreaHospitalizacion area,

        /** Motivo administrativo breve, sin datos clínicos. */
        @Size(max = 200)
        String motivoAdministrativo) {
}
