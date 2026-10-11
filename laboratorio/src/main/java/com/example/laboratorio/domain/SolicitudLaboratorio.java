package com.example.laboratorio.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tabla solicitud_laboratorio. pacienteId y atencionId son referencias lógicas a otros servicios. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class SolicitudLaboratorio {

    @Id
    private Long id;
    private String codigo;
    private Long pacienteId;
    private Long atencionId;
    private TipoExamen tipoExamen;
    private EstadoSolicitud estado;
    /** Texto de demostración, sin valor clínico. Solo se registra al completar la solicitud. */
    private String resultadoFicticio;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaActualizacion;
}
