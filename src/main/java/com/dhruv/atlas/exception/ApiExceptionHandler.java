package com.dhruv.atlas.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DocumentIngestionException.class)
    public ResponseEntity<Map<String, String>> handleIngestionFailure(
            DocumentIngestionException exception) {
        log.error("Document ingestion failed. ", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Document ingestion failed. "));
    }

    @ExceptionHandler(SearchServiceException.class)
    public ResponseEntity<Map<String, String>> handleSearchFailure(SearchServiceException exception) {
        log.error("Search request failed", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Search service is unavailable. "));
    }
}
