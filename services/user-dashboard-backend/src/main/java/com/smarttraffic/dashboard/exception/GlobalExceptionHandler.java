package com.smarttraffic.dashboard.exception;

import com.smarttraffic.dashboard.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones a respuestas HTTP con un cuerpo JSON uniforme
 * ({@link ErrorResponse}). Los controladores no tienen ni un try/catch.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Excepciones propias: cada una sabe su código (400, 404...). */
    @ExceptionHandler(SmartTrafficException.class)
    public ResponseEntity<ErrorResponse> handleSmartTraffic(SmartTrafficException exception, HttpServletRequest request) {
        log.debug("Petición rechazada {}: {}", request.getRequestURI(), exception.getMessage());
        return build(exception.getStatus(), exception.getMessage(), request);
    }

    /** Falta un parámetro obligatorio, por ejemplo zoneId. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio '" + exception.getParameterName() + "'", request);
    }

    /** Un parámetro tiene un formato inválido, por ejemplo una fecha mal escrita. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Valor inválido para '" + exception.getName() + "': " + exception.getValue()
                        + " (las fechas van en ISO-8601, por ejemplo 2026-11-22T00:00:00Z)",
                request);
    }

    /** PostgreSQL no disponible: el estado en vivo sigue funcionando, el historial no. */
    @ExceptionHandler({DataAccessException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseUnavailable(Exception exception, HttpServletRequest request) {
        log.error("Error de base de datos en {}", request.getRequestURI(), exception);
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "El historial no está disponible en este momento; intente más tarde", request);
    }

    /**
     * Cualquier otro error. Las excepciones estándar de Spring (ruta
     * inexistente = 404, método no permitido = 405...) conservan su código;
     * el resto es un 500 sin exponer detalles internos al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        if (exception instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, status.getReasonPhrase(), request);
        }
        log.error("Error inesperado en {}", request.getRequestURI(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
