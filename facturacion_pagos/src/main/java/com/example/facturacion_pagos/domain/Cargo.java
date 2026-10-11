package com.example.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cargo administrativo de ejemplo asociado a un paciente (y opcionalmente a una atención). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Cargo {

    @Id
    private Long id;
    private String codigo;
    private Long pacienteId;
    private Long atencionId;
    private String concepto;
    private BigDecimal monto;
    private EstadoCargo estado;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
}
