package com.example.citas.dto;

import java.time.LocalDateTime;

import com.example.citas.domain.Cita;
import com.example.citas.domain.Especialidad;
import com.example.citas.domain.EstadoCita;

/** Contrato de respuesta. Atenciones lo consume en GET /api/citas/{id}. */
public record CitaResponse(
        Long id,
        String codigo,
        Long pacienteId,
        Especialidad especialidad,
        LocalDateTime fechaHora,
        String profesionalCodigo,
        String motivo,
        EstadoCita estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {

    public static CitaResponse from(Cita c) {
        return new CitaResponse(c.getId(), c.getCodigo(), c.getPacienteId(), c.getEspecialidad(), c.getFechaHora(),
                c.getProfesionalCodigo(), c.getMotivo(), c.getEstado(), c.getFechaCreacion(), c.getFechaActualizacion());
    }
}
