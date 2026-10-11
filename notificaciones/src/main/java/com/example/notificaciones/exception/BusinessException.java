package com.example.notificaciones.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Violación de una regla de negocio. El código identifica la regla (RN-01, RN-02...) o el tipo de error. */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public BusinessException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }
}
