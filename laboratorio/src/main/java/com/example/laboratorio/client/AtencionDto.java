package com.example.laboratorio.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Vista mínima de la atención (contrato de GET /api/atenciones/{id}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AtencionDto(Long id, String codigo, Long pacienteId, Long citaId, String estado) {

    public boolean estaAnulada() {
        return "ANULADA".equals(estado);
    }
}
