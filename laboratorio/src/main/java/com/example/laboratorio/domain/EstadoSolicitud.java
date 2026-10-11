package com.example.laboratorio.domain;

import java.util.List;

/**
 * Estados de una solicitud de laboratorio (RN-08).
 *
 * <pre>
 * SOLICITADO -> EN_PROCESO | CANCELADO
 * EN_PROCESO -> COMPLETADO | CANCELADO
 * COMPLETADO, CANCELADO: estados finales
 * </pre>
 */
public enum EstadoSolicitud {
    SOLICITADO,
    EN_PROCESO,
    COMPLETADO,
    CANCELADO;

    public static final List<EstadoSolicitud> ACTIVOS = List.of(SOLICITADO, EN_PROCESO);

    public boolean puedeCambiarA(EstadoSolicitud destino) {
        return switch (this) {
            case SOLICITADO -> destino == EN_PROCESO || destino == CANCELADO;
            case EN_PROCESO -> destino == COMPLETADO || destino == CANCELADO;
            case COMPLETADO, CANCELADO -> false;
        };
    }
}
