package com.example.hospitalizacion.dto;

import java.time.LocalDateTime;

import com.example.hospitalizacion.domain.AreaHospitalizacion;
import com.example.hospitalizacion.domain.EstadoHospitalizacion;
import com.example.hospitalizacion.domain.Hospitalizacion;

public record HospitalizacionResponse(
        Long id,
        String codigo,
        Long pacienteId,
        AreaHospitalizacion area,
        String motivoAdministrativo,
        String camaCodigo,
        EstadoHospitalizacion estado,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaIngreso,
        LocalDateTime fechaAlta) {

    public static HospitalizacionResponse from(Hospitalizacion h) {
        return new HospitalizacionResponse(h.getId(), h.getCodigo(), h.getPacienteId(), h.getArea(),
                h.getMotivoAdministrativo(), h.getCamaCodigo(), h.getEstado(), h.getFechaSolicitud(),
                h.getFechaIngreso(), h.getFechaAlta());
    }
}
