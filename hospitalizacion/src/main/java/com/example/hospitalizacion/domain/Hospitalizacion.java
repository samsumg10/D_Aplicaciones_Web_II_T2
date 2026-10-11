package com.example.hospitalizacion.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Hospitalizacion {

    @Id
    private Long id;
    private String codigo;
    private Long pacienteId;
    private AreaHospitalizacion area;
    private String motivoAdministrativo;
    /** Cama asignada en el ingreso; se conserva como histórico después del alta. */
    private String camaCodigo;
    /**
     * Igual a camaCodigo mientras la hospitalización está ACTIVA y null en cualquier otro estado.
     * La columna es UNIQUE: la base de datos impide que una cama esté en dos hospitalizaciones activas
     * (RN-09) incluso si dos asignaciones llegan al mismo tiempo. UNIQUE admite varios null.
     */
    private String camaOcupada;
    private EstadoHospitalizacion estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaIngreso;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaActualizacion;
}
