package com.dhruv.atlas.controller;

import com.dhruv.atlas.ingestion.DocumentIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class IngestionController {
    private final DocumentIngestionService documentIngestionService;

    public IngestionController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }
    @PostMapping("/ingest")
    public ResponseEntity<String> ingest() {
        try {
            documentIngestionService.ingestDocuments();
            return ResponseEntity.ok("Documents ingested successfully. ");
        }
        catch (IllegalStateException e) {
            log.error("Failed to index documents. ");
        }
        return ResponseEntity.ok("Unable to ingest all documents. ");
    }
}
