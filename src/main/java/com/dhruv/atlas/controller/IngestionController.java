package com.dhruv.atlas.controller;
import com.dhruv.atlas.ingestion.IngestionOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IngestionController {
    private final IngestionOrchestrator ingestionOrchestrator;

    public IngestionController(IngestionOrchestrator ingestionOrchestrator) {
        this.ingestionOrchestrator = ingestionOrchestrator;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest() {
        ingestionOrchestrator.ingest();
        return ResponseEntity.ok("Documents ingested successfully. ");
    }
}