package com.example.facturacion_pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.facturacion_pagos.domain.Cargo;
import com.example.facturacion_pagos.domain.EstadoCargo;

public record CargoResponse(
        Long id,
        String codigo,
        Long pacienteId,
        Long atencionId,
        String concepto,
        BigDecimal monto,
        EstadoCargo estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion) {

    public static CargoResponse from(Cargo c) {
        return new CargoResponse(c.getId(), c.getCodigo(), c.getPacienteId(), c.getAtencionId(), c.getConcepto(),
                c.getMonto(), c.getEstado(), c.getFechaRegistro(), c.getFechaActualizacion());
    }
}
