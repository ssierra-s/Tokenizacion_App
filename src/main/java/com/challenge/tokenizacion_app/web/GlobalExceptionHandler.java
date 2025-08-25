package com.challenge.tokenizacion_app.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* -------- Helpers -------- */

    private static ProblemDetail pd(HttpStatus status, String title, String detail, HttpServletRequest req) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        p.setType(URI.create("about:blank"));
        p.setProperty("timestamp", OffsetDateTime.now().toString());
        p.setProperty("path", req.getRequestURI());
        return p;
    }

    /* -------- Validación -------- */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .collect(Collectors.toList());
        var pd = pd(HttpStatus.BAD_REQUEST, "Validación fallida", "Algunos campos no son válidos.", req);
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(BindException.class)
    ProblemDetail handleBind(BindException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .collect(Collectors.toList());
        var pd = pd(HttpStatus.BAD_REQUEST, "Bind fallido", "No se pudieron convertir algunos parámetros.", req);
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        var errors = ex.getConstraintViolations().stream()
                .map(cv -> Map.of("property", String.valueOf(cv.getPropertyPath()), "message", cv.getMessage()))
                .collect(Collectors.toList());
        var pd = pd(HttpStatus.BAD_REQUEST, "Validación fallida", "Restricciones de validación incumplidas.", req);
        pd.setProperty("errors", errors);
        return pd;
    }

    /* -------- Errores comunes de request -------- */

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return pd(HttpStatus.BAD_REQUEST, "Parámetro requerido faltante",
                "Falta el parámetro: " + ex.getParameterName(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return pd(HttpStatus.BAD_REQUEST, "Cuerpo inválido", "El cuerpo de la petición no es válido o está vacío.", req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail handleMedia(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return pd(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Media type no soportado", ex.getMessage(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return pd(HttpStatus.METHOD_NOT_ALLOWED, "Método no soportado", ex.getMessage(), req);
    }

    /* -------- Dominio/Datos -------- */

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return pd(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage(), req);
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        return pd(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return pd(HttpStatus.CONFLICT, "Conflicto de datos", "Violación de integridad de datos.", req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimistic(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return pd(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "El recurso fue modificado por otra transacción. Intenta de nuevo.", req);
    }

    /* -------- Seguridad -------- */

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccess(AccessDeniedException ex, HttpServletRequest req) {
        return pd(HttpStatus.FORBIDDEN, "Acceso denegado", "No tienes permisos para acceder a este recurso.", req);
    }

    /* -------- Fallback -------- */

    @ExceptionHandler(Exception.class)
    ProblemDetail handleAny(Exception ex, HttpServletRequest req) {
        return pd(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", ex.getMessage(), req);
    }
}
