package com.example.pacientes.domain;

/** Solo un paciente ACTIVO puede recibir citas, hospitalizaciones o cargos (lo verifican los demás servicios). */
public enum EstadoPaciente {
    ACTIVO,
    INACTIVO
}
