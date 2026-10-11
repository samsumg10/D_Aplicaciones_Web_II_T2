package com.example.pacientes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.pacientes.domain.EstadoPaciente;
import com.example.pacientes.domain.Paciente;
import com.example.pacientes.domain.TipoDocumento;

/** Contrato de respuesta. Citas, hospitalización y facturación lo consumen en GET /api/pacientes/{id}. */
public record PacienteResponse(
        Long id,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String telefono,
        String correo,
        String direccion,
        EstadoPaciente estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion) {

    public static PacienteResponse from(Paciente p) {
        return new PacienteResponse(p.getId(), p.getTipoDocumento(), p.getNumeroDocumento(), p.getNombres(),
                p.getApellidos(), p.getFechaNacimiento(), p.getTelefono(), p.getCorreo(), p.getDireccion(),
                p.getEstado(), p.getFechaRegistro(), p.getFechaActualizacion());
    }
}
