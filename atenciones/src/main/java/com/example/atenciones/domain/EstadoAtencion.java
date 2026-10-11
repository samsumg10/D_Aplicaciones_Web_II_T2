package com.example.atenciones.domain;

/**
 * Estado administrativo de la atención.
 *
 * <pre>
 * REGISTRADA -> FINALIZADA | ANULADA
 * FINALIZADA, ANULADA: estados finales
 * </pre>
 */
public enum EstadoAtencion {
    REGISTRADA,
    FINALIZADA,
    ANULADA;

    public boolean puedeCambiarA(EstadoAtencion destino) {
        return this == REGISTRADA && (destino == FINALIZADA || destino == ANULADA);
    }
}
