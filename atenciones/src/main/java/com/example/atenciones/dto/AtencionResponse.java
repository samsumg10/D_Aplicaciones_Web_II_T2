package com.example.atenciones.dto;

import java.time.LocalDateTime;

import com.example.atenciones.domain.AreaAtencion;
import com.example.atenciones.domain.Atencion;
import com.example.atenciones.domain.EstadoAtencion;

/** Contrato de respuesta. Laboratorio lo consume en GET /api/atenciones/{id}. */
public record AtencionResponse(
        Long id,
        String codigo,
        Long pacienteId,
        Long citaId,
        AreaAtencion area,
        String observacion,
        EstadoAtencion estado,
        LocalDateTime fechaAtencion,
        LocalDateTime fechaActualizacion) {

    public static AtencionResponse from(Atencion a) {
        return new AtencionResponse(a.getId(), a.getCodigo(), a.getPacienteId(), a.getCitaId(), a.getArea(),
                a.getObservacion(), a.getEstado(), a.getFechaAtencion(), a.getFechaActualizacion());
    }
}
