package com.example.laboratorio.dto;

import java.time.LocalDateTime;

import com.example.laboratorio.domain.EstadoSolicitud;
import com.example.laboratorio.domain.SolicitudLaboratorio;
import com.example.laboratorio.domain.TipoExamen;

public record SolicitudResponse(
        Long id,
        String codigo,
        Long pacienteId,
        Long atencionId,
        TipoExamen tipoExamen,
        EstadoSolicitud estado,
        String resultadoFicticio,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaActualizacion) {

    public static SolicitudResponse from(SolicitudLaboratorio s) {
        return new SolicitudResponse(s.getId(), s.getCodigo(), s.getPacienteId(), s.getAtencionId(), s.getTipoExamen(),
                s.getEstado(), s.getResultadoFicticio(), s.getFechaSolicitud(), s.getFechaActualizacion());
    }
}
