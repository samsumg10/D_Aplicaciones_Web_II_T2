package com.example.citas.domain;

import java.util.List;

/**
 * Estados de una cita y sus transiciones permitidas (RN-06).
 *
 * <pre>
 * PENDIENTE  -> CONFIRMADA | CANCELADA
 * CONFIRMADA -> ATENDIDA | CANCELADA | AUSENTE
 * ATENDIDA, CANCELADA, AUSENTE: estados finales
 * </pre>
 *
 * Reprogramar no es una transición de este método: una cita PENDIENTE o CONFIRMADA cambia de fecha
 * y vuelve a PENDIENTE para que se confirme de nuevo.
 */
public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    ATENDIDA,
    CANCELADA,
    AUSENTE;

    /** Estados que ocupan un horario de la agenda. */
    public static final List<EstadoCita> ACTIVOS = List.of(PENDIENTE, CONFIRMADA);

    public boolean puedeCambiarA(EstadoCita destino) {
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == ATENDIDA || destino == CANCELADA || destino == AUSENTE;
            case ATENDIDA, CANCELADA, AUSENTE -> false;
        };
    }

    public boolean esActivo() {
        return ACTIVOS.contains(this);
    }
}
