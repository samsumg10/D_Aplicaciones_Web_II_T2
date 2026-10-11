package com.example.farmacia.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Producto del catálogo de demostración. El stock es ficticio. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Medicamento {

    @Id
    private Long id;
    private String codigo;
    private String nombre;
    private String presentacion;
    private Integer stock;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
}
