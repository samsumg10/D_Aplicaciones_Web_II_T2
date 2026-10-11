package com.example.farmacia.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dispensación de ejemplo: no representa una prescripción ni una indicación clínica. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Dispensacion {

    @Id
    private Long id;
    private String codigo;
    private Long medicamentoId;
    private Long pacienteId;
    private Integer cantidad;
    /** Referencia administrativa libre, por ejemplo el código de la atención (ATE-XXXXXXXX). */
    private String referenciaAdministrativa;
    private EstadoDispensacion estado;
    private LocalDateTime fechaDispensacion;
    private LocalDateTime fechaActualizacion;
}
