package com.example.notificaciones.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aviso simulado: no se envía un correo ni un SMS real, se guarda el mensaje que se habría enviado.
 * eventoId es único para que una entrega duplicada del mismo evento no genere dos avisos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Notificacion {

    public static final String CANAL_SIMULADO = "SIMULADO";
    public static final String ESTADO_ENVIADA = "ENVIADA";

    @Id
    private Long id;
    private String eventoId;
    private String tipoEvento;
    private Long pacienteId;
    private String recurso;
    private String recursoId;
    private String canal;
    private String asunto;
    private String mensaje;
    private String estado;
    private LocalDateTime fechaEvento;
    private LocalDateTime fechaRegistro;
}
