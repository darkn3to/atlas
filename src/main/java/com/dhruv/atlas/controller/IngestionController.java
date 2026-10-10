package com.dhruv.atlas.controller;

import com.dhruv.atlas.ingestion.DocumentIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IngestionController {
    private final DocumentIngestionService documentIngestionService;

    public IngestionController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }
    @PostMapping("/ingest")
    public ResponseEntity<String> ingest() {
        documentIngestionService.ingestDocuments();
        return ResponseEntity.ok("Documents ingested successfully. ");
    }
}
