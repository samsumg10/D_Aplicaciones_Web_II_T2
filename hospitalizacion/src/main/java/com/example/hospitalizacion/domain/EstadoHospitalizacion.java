package com.example.hospitalizacion.domain;

import java.util.List;

/**
 * Estados de una hospitalización (RN-09).
 *
 * <pre>
 * SOLICITADA -> ACTIVA (ingreso con cama asignada) | CANCELADA
 * ACTIVA     -> ALTA (alta administrativa)
 * ALTA, CANCELADA: estados finales
 * </pre>
 */
public enum EstadoHospitalizacion {
    SOLICITADA,
    ACTIVA,
    ALTA,
    CANCELADA;

    /** Un paciente no puede tener más de una hospitalización en estos estados. */
    public static final List<EstadoHospitalizacion> ABIERTAS = List.of(SOLICITADA, ACTIVA);

    public boolean puedeCambiarA(EstadoHospitalizacion destino) {
        return switch (this) {
            case SOLICITADA -> destino == ACTIVA || destino == CANCELADA;
            case ACTIVA -> destino == ALTA;
            case ALTA, CANCELADA -> false;
        };
    }
}
