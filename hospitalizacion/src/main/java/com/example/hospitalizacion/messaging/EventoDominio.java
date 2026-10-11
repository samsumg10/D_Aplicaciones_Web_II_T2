package com.example.hospitalizacion.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sobre común de todos los eventos del hospital. Cada servicio tiene su propia copia de este record:
 * el contrato es el JSON, no una librería compartida, para que los servicios evolucionen por separado.
 *
 * @param eventoId       identificador único; los consumidores lo usan para ignorar entregas duplicadas
 * @param tipo           nombre del evento de negocio (PacienteRegistrado, CitaConfirmada...)
 * @param origen         servicio que lo publica
 * @param recurso        tipo de recurso afectado (PACIENTE, CITA...)
 * @param recursoId      identificador del recurso
 * @param pacienteId     paciente relacionado (solo el id: no viajan datos personales)
 * @param estadoAnterior estado antes del cambio (null si el recurso es nuevo)
 * @param estadoNuevo    estado después del cambio
 * @param detalle        resumen legible del cambio, sin datos sensibles
 * @param fecha          momento en que ocurrió el cambio
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

    public static final String ORIGEN = "hospitalizacion";

    public static EventoDominio de(String tipo, String recurso, Object recursoId, Long pacienteId,
            String estadoAnterior, String estadoNuevo, String detalle) {
        return new EventoDominio(UUID.randomUUID().toString(), tipo, ORIGEN, recurso, String.valueOf(recursoId),
                pacienteId, estadoAnterior, estadoNuevo, detalle, LocalDateTime.now());
    }
}
