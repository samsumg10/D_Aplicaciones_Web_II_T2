package com.example.atenciones.client;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Vista mínima de la cita que necesita atenciones (contrato de GET /api/citas/{id}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CitaDto(Long id, String codigo, Long pacienteId, String especialidad, LocalDateTime fechaHora,
        String estado) {

    public boolean estaConfirmada() {
        return "CONFIRMADA".equals(estado);
    }
}
