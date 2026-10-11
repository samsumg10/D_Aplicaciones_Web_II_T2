package com.example.atenciones.dto;

import com.example.atenciones.domain.AreaAtencion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** El paciente no se envía: se toma de la cita, así no puede registrarse una atención para otro paciente. */
public record AtencionRequest(

        @NotNull(message = "La cita es obligatoria")
        @Positive
        Long citaId,

        @NotNull(message = "El área es obligatoria")
        AreaAtencion area,

        /** Observación administrativa breve; no se registran datos clínicos. */
        @Size(max = 200)
        String observacion) {
}
