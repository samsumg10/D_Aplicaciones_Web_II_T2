package com.example.pacientes.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tabla paciente de la base hospital_pacientes. R2DBC deriva los nombres de la clase y de los campos
 * (Paciente -> paciente, numeroDocumento -> numero_documento) y guarda los enums como texto.
 * No se fija el nombre en @Table para que el dialecto ajuste mayúsculas/minúsculas (PostgreSQL y H2 de pruebas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Paciente {

    @Id
    private Long id;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String correo;
    private String direccion;
    private EstadoPaciente estado;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
}
