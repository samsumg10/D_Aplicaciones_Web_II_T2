package com.example.farmacia.dto;

import java.time.LocalDateTime;

import com.example.farmacia.domain.Medicamento;

public record MedicamentoResponse(
        Long id,
        String codigo,
        String nombre,
        String presentacion,
        Integer stock,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion) {

    public static MedicamentoResponse from(Medicamento m) {
        return new MedicamentoResponse(m.getId(), m.getCodigo(), m.getNombre(), m.getPresentacion(), m.getStock(),
                m.getFechaRegistro(), m.getFechaActualizacion());
    }
}
