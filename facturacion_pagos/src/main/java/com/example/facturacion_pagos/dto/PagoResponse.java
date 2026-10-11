package com.example.facturacion_pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.facturacion_pagos.domain.MedioPago;
import com.example.facturacion_pagos.domain.Pago;

public record PagoResponse(
        Long id,
        Long cargoId,
        Long pacienteId,
        String concepto,
        BigDecimal monto,
        MedioPago medioPago,
        String numeroOperacion,
        String estado,
        LocalDateTime fechaPago) {

    public static PagoResponse from(Pago p) {
        return new PagoResponse(p.getId(), p.getCargoId(), p.getPacienteId(), p.getConcepto(), p.getMonto(),
                p.getMedioPago(), p.getNumeroOperacion(), p.getEstado(), p.getFechaPago());
    }
}
