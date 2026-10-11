package com.example.auditoria.messaging;

import java.time.LocalDateTime;

/**
 * Copia local del contrato JSON de los eventos del hospital. El consumidor no depende de las clases del
 * productor: si un productor agrega un campo, aquí se ignora (el conversor no falla con campos desconocidos).
 */
public record EventoDominio(
        String eventoId,
        String tipo,
        String origen,
        String recurso,
        String recursoId,
        Long pacienteId,
        String estadoAnterior,
        String estadoNuevo,
        String detalle,
        LocalDateTime fecha) {
}
