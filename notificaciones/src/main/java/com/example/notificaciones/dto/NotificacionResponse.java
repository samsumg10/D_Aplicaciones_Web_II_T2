package com.example.notificaciones.dto;

import java.time.LocalDateTime;

import com.example.notificaciones.domain.Notificacion;

public record NotificacionResponse(
        Long id,
        String eventoId,
        String tipoEvento,
        Long pacienteId,
        String recurso,
        String recursoId,
        String canal,
        String asunto,
        String mensaje,
        String estado,
        LocalDateTime fechaEvento,
        LocalDateTime fechaRegistro) {

    public static NotificacionResponse from(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getEventoId(), n.getTipoEvento(), n.getPacienteId(),
                n.getRecurso(), n.getRecursoId(), n.getCanal(), n.getAsunto(), n.getMensaje(), n.getEstado(),
                n.getFechaEvento(), n.getFechaRegistro());
    }
}
