package com.example.notificaciones.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import lombok.extern.slf4j.Slf4j;

/** Traduce las excepciones a respuestas JSON con un código HTTP apropiado. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> negocio(BusinessException ex) {
        return respuesta(ex.getStatus(), ex.getCodigo(), ex.getMessage(), null);
    }

    /** Falla alguna anotación del request (@NotBlank, @Pattern, @Email...). */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> validacion(WebExchangeBindException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", "Datos de entrada inválidos", campos);
    }

    /** JSON mal formado, un enum que no existe o un id que no es numérico. */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<Map<String, Object>> entrada(ServerWebInputException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "ENTRADA_INVALIDA", ex.getReason(), null);
    }

    /** Errores HTTP propios de Spring (415 tipo de contenido no soportado, 406...): se respeta su código. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> estadoHttp(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return respuesta(status, status.name(), ex.getReason(), null);
    }

    /** Cualquier otro error: se registra en el log y no se exponen detalles internos al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error inesperado", null);
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String codigo, String mensaje,
            Map<String, String> campos) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("fecha", LocalDateTime.now());
        cuerpo.put("status", status.value());
        cuerpo.put("codigo", codigo);
        cuerpo.put("mensaje", mensaje);
        if (campos != null) {
            cuerpo.put("errores", campos);
        }
        return ResponseEntity.status(status).body(cuerpo);
    }
}
