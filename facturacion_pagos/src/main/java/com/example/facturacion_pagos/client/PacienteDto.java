package com.example.facturacion_pagos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vista mínima del paciente que necesita citas. Se ignoran los demás campos del contrato de pacientes,
 * así un campo nuevo en ese servicio no rompe a este.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PacienteDto(Long id, String nombres, String apellidos, String estado) {

    public boolean estaActivo() {
        return "ACTIVO".equals(estado);
    }
}
