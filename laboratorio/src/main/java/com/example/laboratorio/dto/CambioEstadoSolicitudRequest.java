package com.example.laboratorio.dto;

import com.example.laboratorio.domain.EstadoSolicitud;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Al pasar a COMPLETADO se exige un resultado ficticio; en los demás estados no se acepta. */
public record CambioEstadoSolicitudRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoSolicitud estado,

        @Size(max = 300)
        String resultadoFicticio) {
}
