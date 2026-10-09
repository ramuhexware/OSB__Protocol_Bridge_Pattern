package com.hexaware.osb.protocolbridge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalBridgeExceptionHandler {

    @ExceptionHandler(ProtocolBridgeException.class)
    public ResponseEntity<Map<String, Object>> handleProtocolBridgeException(ProtocolBridgeException ex) {
        Map<String, Object> body = Map.of(
            "timestamp", Instant.now().toString(),
            "error", "OSB Protocol Bridge Exception",
            "stage", ex.getStage(),
            "status", ex.getStatusCode(),
            "message", ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = Map.of(
            "timestamp", Instant.now().toString(),
            "error", "Internal Server Error",
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "message", ex.getMessage() != null ? ex.getMessage() : ex.toString()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
