package com.example.facturacion_pagos.domain;

/** PENDIENTE -> PAGADO (al registrar su pago) | ANULADO. PAGADO y ANULADO son finales. */
public enum EstadoCargo {
    PENDIENTE,
    PAGADO,
    ANULADO
}
