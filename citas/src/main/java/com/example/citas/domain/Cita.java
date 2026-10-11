package com.example.citas.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tabla cita de la base hospital_citas. pacienteId es una referencia lógica al servicio de pacientes
 * (otra base de datos): no hay clave foránea, la existencia se valida llamando a ese servicio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Cita {

    @Id
    private Long id;
    private String codigo;
    private Long pacienteId;
    private Especialidad especialidad;
    private LocalDateTime fechaHora;
    private String profesionalCodigo;
    private String motivo;
    private EstadoCita estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
