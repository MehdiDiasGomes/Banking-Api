package com.mehdi.banking_api.exception;

import com.mehdi.banking_api.common.ApiResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(
                new ApiResult<>(false, 404, ex.getMessage(), null, LocalDateTime.now())
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(400).body(
                new ApiResult<>(false, 400, ex.getMessage(), null, LocalDateTime.now())
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResult<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(403).body(
                new ApiResult<>(false, 403, ex.getMessage(), null, LocalDateTime.now())
        );
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResult<Void>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(
                new ApiResult<>(false, ex.getStatus(), ex.getMessage(), null, LocalDateTime.now())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(
                new ApiResult<>(false, 409, "A resource with this data already exists", null, LocalDateTime.now())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(400).body(
                new ApiResult<>(false, 400, "Invalid data", errors, LocalDateTime.now())
        );
    }
}