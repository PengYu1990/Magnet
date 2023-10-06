package com.pengyu.magnet.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

/**
 * AOP for controllers, implement custom Exception Handling
 */
@ControllerAdvice
public class DefaultExceptionHandler {

    // 404 error
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiExceptionResponse> handleException(ResourceNotFoundException e,
                                                                HttpServletRequest request) {
        ApiExceptionResponse apiExceptionResponse = new ApiExceptionResponse(
                request.getRequestURI(),
                e.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(apiExceptionResponse, HttpStatus.NOT_FOUND);
    }

    // 401
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ApiExceptionResponse> handleException(InsufficientAuthenticationException e,
                                                                HttpServletRequest request) {
        ApiExceptionResponse apiExceptionResponse = new ApiExceptionResponse(
                request.getRequestURI(),
                e.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(apiExceptionResponse, HttpStatus.UNAUTHORIZED);
    }

    // 403
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiExceptionResponse> handleException(BadCredentialsException e,
                                                                HttpServletRequest request) {
        ApiExceptionResponse apiExceptionResponse = new ApiExceptionResponse(
                request.getRequestURI(),
                e.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(apiExceptionResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiExceptionResponse> handleException(AccessDeniedException e,
                                                                HttpServletRequest request) {
        ApiExceptionResponse apiExceptionResponse = new ApiExceptionResponse(
                request.getRequestURI(),
                e.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(apiExceptionResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiExceptionResponse> handleException(IllegalArgumentException e,
                                                                HttpServletRequest request) {
        ApiExceptionResponse apiExceptionResponse = new ApiExceptionResponse(
                request.getRequestURI(),
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(apiExceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiExceptionResponse> handleException(ApiException e,
                                                                HttpServletRequest request) {
        ApiExceptionResponse apiExceptionResponse = new ApiExceptionResponse(
                request.getRequestURI(),
                e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(apiExceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Other errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiExceptionResponse> handleException(Exception e,
                                                    HttpServletRequest request) {
        ApiExceptionResponse apiExceptionResponse = new ApiExceptionResponse(
                request.getRequestURI(),
                e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(apiExceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
