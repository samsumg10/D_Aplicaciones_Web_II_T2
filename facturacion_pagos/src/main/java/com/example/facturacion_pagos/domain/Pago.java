package com.example.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pago simulado de un cargo. numeroOperacion es único (RN-11). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Pago {

    public static final String ESTADO_REGISTRADO = "REGISTRADO";

    @Id
    private Long id;
    private Long cargoId;
    private Long pacienteId;
    private String concepto;
    private BigDecimal monto;
    private MedioPago medioPago;
    private String numeroOperacion;
    private String estado;
    private LocalDateTime fechaPago;
}
