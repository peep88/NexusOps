package com.nexusops.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> responseStatus(ResponseStatusException exception, HttpServletRequest request) {
        String code = exception.getStatusCode().toString().replace(" ", "_");
        return ResponseEntity.status(exception.getStatusCode()).body(
                new ApiResponse<>(null, new ApiResponse.Meta(requestId(request)), new ApiResponse.ApiError(code, exception.getReason()))
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(
                    null,
                    new ApiResponse.Meta(requestId(request)),
                    new ApiResponse.ApiError(HttpStatus.BAD_REQUEST.toString().replace(" ", "_"), "Request validation failed")
                )
        );
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.HEADER);
    }
}
