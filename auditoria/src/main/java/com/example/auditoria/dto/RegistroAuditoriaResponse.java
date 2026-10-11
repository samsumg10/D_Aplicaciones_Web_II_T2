package com.example.auditoria.dto;

import java.time.LocalDateTime;

import com.example.auditoria.domain.RegistroAuditoria;

public record RegistroAuditoriaResponse(
        Long id,
        String eventoId,
        String tipoEvento,
        String origen,
        String recurso,
        String recursoId,
        Long pacienteId,
        String estadoAnterior,
        String estadoNuevo,
        String detalle,
        LocalDateTime fechaEvento,
        LocalDateTime fechaRegistro) {

    public static RegistroAuditoriaResponse from(RegistroAuditoria r) {
        return new RegistroAuditoriaResponse(r.getId(), r.getEventoId(), r.getTipoEvento(), r.getOrigen(),
                r.getRecurso(), r.getRecursoId(), r.getPacienteId(), r.getEstadoAnterior(), r.getEstadoNuevo(),
                r.getDetalle(), r.getFechaEvento(), r.getFechaRegistro());
    }
}
