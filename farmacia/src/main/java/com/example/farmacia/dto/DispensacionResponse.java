package com.example.farmacia.dto;

import java.time.LocalDateTime;

import com.example.farmacia.domain.Dispensacion;
import com.example.farmacia.domain.EstadoDispensacion;

public record DispensacionResponse(
        Long id,
        String codigo,
        Long medicamentoId,
        Long pacienteId,
        Integer cantidad,
        String referenciaAdministrativa,
        EstadoDispensacion estado,
        LocalDateTime fechaDispensacion,
        LocalDateTime fechaActualizacion) {

    public static DispensacionResponse from(Dispensacion d) {
        return new DispensacionResponse(d.getId(), d.getCodigo(), d.getMedicamentoId(), d.getPacienteId(),
                d.getCantidad(), d.getReferenciaAdministrativa(), d.getEstado(), d.getFechaDispensacion(),
                d.getFechaActualizacion());
    }
}
