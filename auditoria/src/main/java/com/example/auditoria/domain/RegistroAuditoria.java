package com.example.auditoria.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Traza de un cambio importante: fecha, recurso, estado anterior, estado nuevo y servicio de origen.
 * Los registros solo se insertan (no se modifican ni se eliminan).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class RegistroAuditoria {

    @Id
    private Long id;
    private String eventoId;
    private String tipoEvento;
    private String origen;
    private String recurso;
    private String recursoId;
    private Long pacienteId;
    private String estadoAnterior;
    private String estadoNuevo;
    private String detalle;
    private LocalDateTime fechaEvento;
    private LocalDateTime fechaRegistro;
}
