package com.example.atenciones.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registro administrativo de una atención simulada. No guarda historia clínica.
 * citaId y pacienteId son referencias lógicas a otros servicios (sin clave foránea).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Atencion {

    @Id
    private Long id;
    private String codigo;
    private Long pacienteId;
    private Long citaId;
    private AreaAtencion area;
    private String observacion;
    private EstadoAtencion estado;
    private LocalDateTime fechaAtencion;
    private LocalDateTime fechaActualizacion;
}
